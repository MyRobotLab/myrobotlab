package org.myrobotlab.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Registration;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.StaticType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.programab.PredicateEvent;
import org.myrobotlab.programab.Response;
import org.myrobotlab.service.FiniteStateMachine.StateChange;
import org.myrobotlab.service.Log.LogEntry;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.OpenCVConfig;
import org.myrobotlab.service.config.SpeechSynthesisConfig;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.service.interfaces.SpeechListener;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class InMoov2 extends Service<InMoov2Config>
    implements ServiceLifeCycleListener, SpeechListener, TextListener, TextPublisher, JoystickListener, LocaleProvider, IKJointAngleListener {

  public class Heart implements Runnable {
    private final ReentrantLock lock = new ReentrantLock();
    private Thread thread;

    @Override
    public void run() {
      if (lock.tryLock()) {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            invoke("publishHeartbeat");
            Thread.sleep(config.heartbeatInterval);
          }
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        } finally {
          lock.unlock();
          log.info("heart stopping");
          thread = null;
        }
      }
    }

    public void start() {
      if (thread == null) {
        log.info("starting heart");
        thread = new Thread(this, String.format("%s-heart", getName()));
        thread.start();
        config.heartbeat = true;
      } else {
        log.info("heart already started");
      }
    }

    public void stop() {
      if (thread != null) {
        thread.interrupt();
        config.heartbeat = false;
      } else {
        log.info("heart already stopped");
      }
    }
  }

  public static class Heartbeat {
    double batteryLevel = 100;
    public long count = 0;
    public List<LogEntry> errors;
    public boolean isPirOn = false;
    public String state;
    public long ts = System.currentTimeMillis();

    public Heartbeat(InMoov2 inmoov) {
      this.state = inmoov.state;
      this.errors = inmoov.errors;
      this.count = inmoov.heartbeatCount;
      this.isPirOn = inmoov.isPirOn;
    }
  }

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  public static LinkedHashMap<String, String> lpVars = new LinkedHashMap<String, String>();

  private static final long serialVersionUID = 1L;

  static String speechRecognizer = "WebkitSpeechRecognition";

  /**
   * This method will load a python file into the python interpreter.
   * 
   * @param file
   *          file to load
   * @return success/failure
   */
  @Deprecated /* use execScript - this doesn't handle resources correctly */
  public static boolean loadFile(String file) {
    File f = new File(file);
    // FIXME cannot be casting to Python ! Py4j would break
    Python p = (Python) Runtime.getService("python");
    log.info("Loading  Python file {}", f.getAbsolutePath());
    if (p == null) {
      log.error("Python instance not found");
      return false;
    }

    boolean result = false;
    try {
      // This will open a gazillion tabs in InMoov
      // result = p.execFile(f.getAbsolutePath(), true);

      // old way - not using execFile :(
      String script = FileIO.toString(f.getAbsolutePath());
      result = p.exec(script, true);
    } catch (IOException e) {
      log.error("IO Error loading file : ", e);
      return false;
    }

    if (!result) {
      log.error("Error while loading file {}", f.getAbsolutePath());
      return false;
    } else {
      log.debug("Successfully loaded {}", f.getAbsolutePath());
    }
    return true;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.ERROR);
      // Platform.setVirtual(true);
      // Runtime.start("s01", "Servo");
      // Runtime.start("intro", "Intro");

      Runtime.startConfig("dev");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      // webgui.setPort(8888);
      webgui.startService();
      InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");

      boolean done = true;
      if (done) {
        return;
      }

      OpenCVConfig ocvConfig = i01.getPeerConfig("opencv", new StaticType<>() {
      });
      ocvConfig.flip = true;
      i01.setPeerConfigValue("opencv", "flip", true);
      // i01.savePeerConfig("", null);

      // Runtime.startConfig("default");

      // Runtime.main(new String[] { "--log-level", "info", "-s", "webgui",
      // "WebGui",
      // "intro", "Intro", "python", "Python" });

      Runtime.start("python", "Python");
      // Runtime.start("ros", "Ros");
      Runtime.start("intro", "Intro");
      // InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");
      // i01.startPeer("simulator");
      // Runtime.startConfig("i01-05");
      // Runtime.startConfig("pir-01");

      // Polly polly = (Polly)Runtime.start("i01.mouth", "Polly");
      // i01 = (InMoov2) Runtime.start("i01", "InMoov2");

      // polly.speakBlocking("Hi, to be or not to be that is the question,
      // wheather to take arms against a see of trouble, and by aposing them end
      // them, to sleep, to die");
      // i01.startPeer("mouth");
      // i01.speakBlocking("Hi, to be or not to be that is the question,
      // wheather to take arms against a see of trouble, and by aposing them end
      // them, to sleep, to die");

      Runtime.start("python", "Python");

      // i01.startSimulator();
      Plan plan = Runtime.load("webgui", "WebGui");
      // WebGuiConfig webgui = (WebGuiConfig) plan.get("webgui");
      // webgui.autoStartBrowser = false;
      Runtime.startConfig("webgui");
      Runtime.start("webgui", "WebGui");

      Random random = (Random) Runtime.start("random", "Random");

      random.addRandom(3000, 8000, "i01", "setLeftArmSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
      random.addRandom(3000, 8000, "i01", "setRightArmSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);

      random.addRandom(3000, 8000, "i01", "moveLeftArm", 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);
      random.addRandom(3000, 8000, "i01", "moveRightArm", 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);

      random.addRandom(3000, 8000, "i01", "setLeftHandSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
      random.addRandom(3000, 8000, "i01", "setRightHandSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);

      random.addRandom(3000, 8000, "i01", "moveRightHand", 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 130.0, 175.0);
      random.addRandom(3000, 8000, "i01", "moveLeftHand", 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 5.0, 40.0);

      random.addRandom(200, 1000, "i01", "setHeadSpeed", 8.0, 20.0, 8.0, 20.0, 8.0, 20.0);
      random.addRandom(200, 1000, "i01", "moveHead", 70.0, 110.0, 65.0, 115.0, 70.0, 110.0);

      random.addRandom(200, 1000, "i01", "setTorsoSpeed", 2.0, 5.0, 2.0, 5.0, 2.0, 5.0);
      random.addRandom(200, 1000, "i01", "moveTorso", 85.0, 95.0, 88.0, 93.0, 70.0, 110.0);

      random.save();

      // i01.startChatBot();
      //
      // i01.startAll("COM3", "COM4");
      Runtime.start("python", "Python");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  protected Double batteryLevel = 100.0;

  /**
   * number of times waited in boot state
   */
  protected int bootCount = 0;

  protected transient ProgramAB chatBot;

  protected List<String> configList;

  /**
   * map of events or states to sounds
   */
  protected Map<String, String> customSoundMap = new TreeMap<>();

  protected transient SpeechRecognizer ear;

  protected List<LogEntry> errors = new ArrayList<>();

  /**
   * The finite state machine is core to managing state of InMoov2. There is
   * very little benefit gained in having the interactions pub/sub. Therefore,
   * there will be a direct reference to the fsm. If different state graph is
   * needed, then the fsm can provide that service.
   */
  private transient FiniteStateMachine fsm = null;

  // waiting controable threaded gestures we warn user
  protected boolean gestureAlreadyStarted = false;

  protected Set<String> gestures = new TreeSet<String>();

  /**
   * Prevents actions or events from happening when InMoov2 is first booted
   */
  private boolean hasBooted = false;

  private transient final Heart heart = new Heart();

  protected long heartbeatCount = 0;

  protected boolean heartBeating = false;

  protected transient HtmlFilter htmlFilter;

  protected transient ImageDisplay imageDisplay;

  protected boolean isPirOn = false;

  protected boolean isSpeaking = false;

  protected String lastGestureExecuted;

  protected Long lastPirActivityTime;

  protected String lastState = null;

  /**
   * supported locales
   */
  protected Map<String, Locale> locales = null;

  protected int maxInactivityTimeSeconds = 120;

  protected transient SpeechSynthesis mouth;

  protected boolean mute = false;

  protected transient OpenCV opencv;

  protected transient Python python;

  /**
   * initial state - updated on any state change
   */
  String state = "boot";

  protected long stateLastIdleTime = System.currentTimeMillis();

  protected long stateLastRandomTime = System.currentTimeMillis();

  protected String voiceSelected;

  public InMoov2(String n, String id) {
    super(n, id);
    locales = Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL", "ru-RU", "hi-IN", "it-IT", "fi-FI", "pt-PT", "tr-TR");
  }

  // should be removed in favor of general listeners
  public void addTextListener(TextListener service) {
    // CORRECT WAY ! - no direct reference - just use the name in a subscription
    addListener("publishText", service.getName());
  }

  @Override
  public InMoov2Config apply(InMoov2Config c) {
    super.apply(c);
    try {

      if (c.locale != null) {
        setLocale(c.locale);
      } else {
        setLocale(getSupportedLocale(Runtime.getInstance().getLocale().toString()));
      }

    } catch (Exception e) {
      error(e);
    }
    return c;
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  @Override
  public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
  }

  @Override
  public void attachTextPublisher(String name) {
    subscribe(name, "publishText");
  }

  @Override
  public void attachTextPublisher(TextPublisher service) {
    subscribe(service.getName(), "publishText");
  }

  public void beginCheckingOnInactivity() {
    beginCheckingOnInactivity(maxInactivityTimeSeconds);
  }

  public void beginCheckingOnInactivity(int maxInactivityTimeSeconds) {
    this.maxInactivityTimeSeconds = maxInactivityTimeSeconds;
    // speakBlocking("power down after %s seconds inactivity is on",
    // this.maxInactivityTimeSeconds);
    log.info("power down after %s seconds inactivity is on", this.maxInactivityTimeSeconds);
    addTask("checkInactivity", 5 * 1000, 0, "checkInactivity");
  }

  /**
   * At boot all services specified through configuration have started, or if no
   * configuration has started minimally the InMoov2 service has started. During
   * the processing of config and starting other services data will have
   * accumulated, and at boot, some of data may now be inspected and processed
   * in a synchronous single threaded way. With reporting after startup, vs
   * during, other peer services are not needed (e.g. audioPlayer is no longer
   * needed to be started "before" InMoov2 because when boot is called
   * everything that is wanted has been started.
   *
   * This method gets called multiple times by the heart beat, it walks through
   * required processing and effectively waits and tries again if not finished.
   * While in "boot", nothing else should be allowed to process until this
   * process is completed.
   */
  synchronized public void boot() {

    Runtime runtime = Runtime.getInstance();

    try {

      if (hasBooted) {
        log.warn("will not boot again");
        return;
      }

      bootCount++;
      log.info("boot count {}", bootCount);

      // config has not finished processing yet..
      if (runtime.isProcessingConfig()) {
        log.warn("runtime still processing config set {}, waiting ....", runtime.getConfigName());
        return;
      }

      // check all required services are completely started - or
      // wait/return until they are

      // there is not much point in running InMoov2 without its
      // core dependencies - those dependencies are ProgramAB,
      // FiniteStatemachine and Py4j/Python - so boot will not
      // finish unless these services have loaded

      // Although this exposes type, it does use startPeer
      // which allows the potential of the user switching types of processors
      // if the processor

      // TODO - make Py4j without zombies and more robust
      /**
       * Py4j is not ready for primetime yet
       * 
       * <pre>
       * 
       * Py4j py4j = (Py4j) startPeer("py4j");
       * if (!py4j.isReady()) {
       *   log.warn("{} not ready....", getPeerName("py4j"));
       *   return;
       * }
       * String code = FileIO.toString(getResourceDir() + fs + "InMoov2.py");
       * py4j.exec(code);
       * 
       * </pre>
       */

      // load the InMoov2.py and publish it for Python/Jython or Py4j to consume
      execScript();

      // TODO - MAKE BOOT REPORT !!!! deliver it on a heartbeat
      runtime.invoke("publishConfigList");
      // FIXME - reduce the number of these
      if (config.loadAppsScripts) {
        loadAppsScripts();
      }

      if (config.loadInitScripts) {
        loadInitScripts();
      }

      if (config.loadGestures) {
        loadGestures();
      }

      if (config.startupSound) {
        String startupsound = FileIO.gluePaths(getResourceDir(), "/system/sounds/startupsound.mp3");
        invoke("publishPlayAudioFile", startupsound);
      }

      List<ServiceInterface> services = Runtime.getServices();
      for (ServiceInterface si : services) {
        if ("Servo".equals(si.getSimpleName())) {
          send(si.getFullName(), "setAutoDisable", true);
        }
      }
      hasBooted = true;
    } catch (Exception e) {
      hasBooted = false;
      error(e);
    }

    /**
     * TODO reporting on errors found in boot process TODO make a report on all
     * peers that have started, of the config processed if there was a config
     * set
     */
    if (config.reportOnBoot) {
      systemEvent("CONFIG STARTED %s", runtime.getConfigName());

      // TODO spin through all services in the order they were started
      // send all system events
      Collection<ServiceInterface> local = Runtime.getLocalServices().values();
      List<ServiceInterface> ordered = new ArrayList<>(local);
      ordered.removeIf(Objects::isNull);
      Collections.sort(ordered);

      Map<String, Peer> peers = getPeers();
      Set<String> peerNames = new HashSet<>();
      for (String peerKey : peers.keySet()) {
        Peer peer = peers.get(peerKey);
        if (peer.name == null) {
          peerNames.add(String.format("%s.%s", getName(), peerKey));
        } else {
          peerNames.add(peer.name);
        }
      }

      for (ServiceInterface si : ordered) {
        if (peerNames.contains(si.getName())) {
          systemEvent("STARTED %s", getPeerKey(si.getName()).replace(".", " "));
        }
      }

      // reporting on all services and config started
      systemEvent("CONFIG LOADED %s", runtime.getConfigName());
    }

    // say finished booting
    fire("wake");
  }

  public void cameraOff() {
    if (opencv != null) {
      opencv.stopCapture();
      opencv.disableAll();
    }
  }

  public void cameraOn() {
    try {
      if (opencv == null) {
        startOpenCV();
      }
      opencv.capture();
    } catch (Exception e) {
      error(e);
    }
  }

  public String captureGesture() {
    return captureGesture(null);
  }

  public String captureGesture(String gesture) {

    if (gesture == null) {
      gesture = "gesture";
    }

    StringBuffer script = new StringBuffer();

    // File file = new File

    // Date date = new Date();
    // script.append("# - " + date + " - Captured gesture :\n");
    script.append(String.format("# %s - captured gesture :\n", getName()));

    script.append(String.format("def %s():\n", gesture));

    String name = getName();

    try {
      // FIXME - types are exposed here - not a good thing
      // real fix should be use the ServoMixer
      InMoov2Head head = (InMoov2Head) getPeer("head");
      if (head != null) {
        script.append("  " + head.getScript(name));
      }

      InMoov2Arm leftArm = (InMoov2Arm) getPeer("leftArm");
      if (leftArm != null) {
        script.append("  " + leftArm.getScript(name));
      }

      InMoov2Arm rightArm = (InMoov2Arm) getPeer("rightArm");
      if (rightArm != null) {
        script.append("  " + rightArm.getScript(name));
      }

      InMoov2Hand leftHand = (InMoov2Hand) getPeer("leftHand");
      if (leftHand != null) {
        script.append("  " + leftHand.getScript(name));
      }

      InMoov2Hand rightHand = (InMoov2Hand) getPeer("rightHand");
      if (rightHand != null) {
        script.append("  " + rightHand.getScript(name));
      }

      InMoov2Torso torso = (InMoov2Torso) getPeer("torso");
      if (torso != null) {
        script.append("  " + torso.getScript(name));
      }
    } catch (Exception e) {
      error(e);
    }

    send("python", "appendScript", script.toString());
    return script.toString();
  }

  public long checkInactivity() {
    // speakBlocking("checking");
    long lastActivityTime = getLastActivityTime();
    long now = System.currentTimeMillis();
    long inactivitySeconds = (now - lastActivityTime) / 1000;
    if (inactivitySeconds > maxInactivityTimeSeconds) {
      // speakBlocking("%d seconds have passed without activity",
      // inactivitySeconds);
      powerDown();
    } else {
      // speakBlocking("%d seconds have passed without activity",
      // inactivitySeconds);
      info("checking checkInactivity - %d seconds have passed without activity", inactivitySeconds);
    }
    return lastActivityTime;
  }

  public void closeAllImages() {
    // FIXME - follow this pattern ?
    // CON npe possible although unlikely
    // PRO start it when its needed
    // PRO small easy to read - no clutter npe
    imageDisplay = (ImageDisplay) startPeer("imageDisplay");
    imageDisplay.closeAll();
  }

  public void cycleGestures() {
    // if not loaded load -
    // FIXME - this needs alot of "help" :P
    // WHY IS THIS DONE ?
    if (gestures.size() == 0) {
      loadGestures();
    }

    for (String gesture : gestures) {
      try {
        String methodName = gesture.substring(0, gesture.length() - 3);
        speakBlocking(methodName);
        log.info("executing gesture {}", methodName);
        python.eval(methodName + "()");

        // wait for finish - or timeout ?

      } catch (Exception e) {
        error(e);
      }
    }
  }

  public void disable() {
    sendToPeer("head", "disable");
    sendToPeer("rightHand", "disable");
    sendToPeer("leftHand", "disable");
    sendToPeer("rightArm", "disable");
    sendToPeer("leftArm", "disable");
    sendToPeer("torso", "disable");
  }

  public void displayFullScreen(String src) {
    try {
      if (imageDisplay == null) {
        imageDisplay = (ImageDisplay) startPeer("imageDisplay");
      }
      imageDisplay.displayFullScreen(src);
      log.error("implement webgui.displayFullScreen");
    } catch (Exception e) {
      error("could not display picture %s", src);
    }
  }

  public void enable() {
    sendToPeer("head", "enable");
    sendToPeer("rightHand", "enable");
    sendToPeer("leftHand", "enable");
    sendToPeer("rightArm", "enable");
    sendToPeer("leftArm", "enable");
    sendToPeer("torso", "enable");
  }

  /**
   * Single place for InMoov2 service to execute arbitrary code - needed
   * initially to set "global" vars in python
   * 
   * @param pythonCode
   * @return
   */
  public boolean exec(String pythonCode) {
    try {
      Python p = (Python) Runtime.start("python", "Python");
      return p.exec(pythonCode, true);
    } catch (Exception e) {
      error("unable to execute script %s", pythonCode);
      return false;
    }
  }

  /**
   * This method will try to launch a python command with error handling
   * 
   * @param gesture
   *          the gesture
   * @return gesture result
   */
  public String execGesture(String gesture) {

    // FIXME PUB SUB - THIS SHOULD JUST PUBLISH TO publishPython
    // although its problematic if this call is to be synchronous ...
    subscribe("python", "publishStatus", this.getName(), "onGestureStatus");
    startedGesture(gesture);
    lastGestureExecuted = gesture;
    Python python = (Python) Runtime.getService("python");
    if (python == null) {
      error("python service not started");
      return null;
    }
    return python.evalAndWait(gesture);
  }

  /**
   * Reload the InMoov2.py script
   */
  public void execScript() {
    execScript("InMoov2.py");
  }

  /**
   * FIXME - I think there was lots of confusion of executing resources or just
   * a file on the file system ... "execScript" I would expect to be just a file
   * on the file system.
   * 
   * If resource semantics are needed there should be a execResourceScript which
   * adds the context and calls the underlying execScript "which only" executes
   * a filesystem file :P
   * 
   * @param someScriptName
   *          execute a resource script
   * @return success or failure
   */
  public void execScript(String someScriptName) {
    String script = getResourceAsString(someScriptName);
    invoke("publishPython", script);
  }

  public void finishedGesture() {
    finishedGesture("unknown");
  }

  // FIXME - this may drive to locall references for all servos
  public void finishedGesture(String nameOfGesture) {
    if (gestureAlreadyStarted) {
      waitTargetPos();
      // RobotCanMoveRandom = true;
      gestureAlreadyStarted = false;
      log.info("gesture : {} finished...", nameOfGesture);
    }
  }

  /**
   * Fire an event to the FSM, potentially this can cause a state change
   * 
   * @param event
   */
  public void fire(String event) {
    // Should this be sent to chatbot too ?
    // invoke("publishEvent", event);
    fsm.fire(event);
  }

  public void fullSpeed() {
    sendToPeer("head", "fullSpeed");
    sendToPeer("rightHand", "fullSpeed");
    sendToPeer("leftHand", "fullSpeed");
    sendToPeer("rightArm", "fullSpeed");
    sendToPeer("leftArm", "fullSpeed");
    sendToPeer("torso", "fullSpeed");
  }

  // FIXME - remove all of this form of localization
  public String get(String key) {
    String ret = localize(key);
    if (ret != null) {
      return ret;
    }
    return "not yet translated";
  }

  public InMoov2Arm getArm(String side) {
    if (!"left".equals(side) && !"right".equals(side)) {
      error("side must be left or right - instead of %s", side);
      return null;
    }
    return (InMoov2Arm) getPeer(side + "Arm");
  }

  public InMoov2Hand getHand(String side) {
    if (!"left".equals(side) && !"right".equals(side)) {
      error("side must be left or right - instead of %s", side);
      return null;
    }
    return (InMoov2Hand) getPeer(side + "Hand");
  }

  public InMoov2Head getHead() {
    return (InMoov2Head) getPeer("head");
  }

  /**
   * finds most recent activity
   * 
   * @return the timestamp of the last activity time.
   */
  public Long getLastActivityTime() {
    Long head = (InMoov2Head) getPeer("head") != null ? ((InMoov2Head) getPeer("head")).getLastActivityTime() : null;
    Long leftArm = (InMoov2Arm) getPeer("leftArm") != null ? ((InMoov2Arm) getPeer("leftArm")).getLastActivityTime() : null;
    Long rightArm = (InMoov2Arm) getPeer("rightArm") != null ? ((InMoov2Arm) getPeer("rightArm")).getLastActivityTime() : null;
    Long leftHand = (InMoov2Hand) getPeer("leftHand") != null ? ((InMoov2Hand) getPeer("leftHand")).getLastActivityTime() : null;
    Long rightHand = (InMoov2Hand) getPeer("rightHand") != null ? ((InMoov2Hand) getPeer("rightHand")).getLastActivityTime() : null;
    Long torso = (InMoov2Torso) getPeer("torso") != null ? ((InMoov2Torso) getPeer("torso")).getLastActivityTime() : null;

    Long lastActivityTime = null;

    if (head != null || leftArm != null || rightArm != null || leftHand != null || rightHand != null || torso != null) {
      lastActivityTime = 0L;
      if (head != null)
        lastActivityTime = Math.max(lastActivityTime, head);
      if (leftArm != null)
        lastActivityTime = Math.max(lastActivityTime, leftArm);
      if (rightArm != null)
        lastActivityTime = Math.max(lastActivityTime, rightArm);
      if (leftHand != null)
        lastActivityTime = Math.max(lastActivityTime, leftHand);
      if (rightHand != null)
        lastActivityTime = Math.max(lastActivityTime, rightHand);
      if (torso != null)
        lastActivityTime = Math.max(lastActivityTime, torso);
    }

    return lastActivityTime;
  }

  public InMoov2Arm getLeftArm() {
    return (InMoov2Arm) getPeer("leftArm");
  }

  public InMoov2Hand getLeftHand() {
    return (InMoov2Hand) getPeer("leftHand");
  }

  @Override
  public Map<String, Locale> getLocales() {
    return locales;
  }

  public OpenCV getOpenCV() {
    return opencv;
  }

  public String getPredicate(String key) {
    return getPredicate(chatBot.getConfig().currentUserName, key);
  }

  public String getPredicate(String user, String key) {
    return chatBot.getPredicate(user, key);
  }

  /**
   * getResponse from ProgramAB
   * 
   * @param text
   * @return
   */
  public Response getResponse(String text) {
    Response response = chatBot.getResponse(text);
    return response;
  }

  public InMoov2Arm getRightArm() {
    return (InMoov2Arm) getPeer("rightArm");
  }

  public InMoov2Hand getRightHand() {
    return (InMoov2Hand) getPeer("rightHand");
  }

  public String getState() {
    FiniteStateMachine fsm = (FiniteStateMachine) getPeer("fsm");
    if (fsm == null) {
      return null;
    }
    return fsm.getCurrent();
  }

  /**
   * matches on language only not variant expands language match to full InMoov2
   * bot locale
   * 
   * @param inLocale
   * @return
   */
  public String getSupportedLocale(String inLocale) {
    String ret = "en-US";
    if (inLocale == null) {
      return ret;
    }

    int pos = inLocale.indexOf("-");
    if (pos > 0) {
      inLocale = inLocale.substring(0, pos);
    }

    for (String fullLocale : locales.keySet()) {
      if (fullLocale.startsWith(inLocale)) {
        return fullLocale;
      }
    }
    return ret;
  }

  public InMoov2Torso getTorso() {
    return (InMoov2Torso) getPeer("torso");
  }

  public InMoov2Config getTypedConfig() {
    return (InMoov2Config) config;
  }

  public void halfSpeed() {
    sendToPeer("head", "setSpeed", 25.0, 25.0, 25.0, 25.0, 100.0, 25.0);
    sendToPeer("rightHand", "setSpeed", 30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    sendToPeer("leftHand", "setSpeed", 30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    sendToPeer("rightArm", "setSpeed", 25.0, 25.0, 25.0, 25.0);
    sendToPeer("leftArm", "setSpeed", 25.0, 25.0, 25.0, 25.0);
    sendToPeer("torso", "setSpeed", 20.0, 20.0, 20.0);
  }

  /**
   * If there have been any errors
   * 
   * @return
   */
  public boolean hasErrors() {
    return errors.size() > 0;
  }

  /**
   * clear all errors
   */
  public void clearErrors() {
    errors.clear();
  }

  public boolean isCameraOn() {
    if (opencv != null) {
      if (opencv.isCapturing()) {
        return true;
      }
    }
    return false;
  }

  public boolean isMute() {
    return mute;
  }

  /**
   * execute python scripts in the app directory on startup of the service
   * 
   * @throws IOException
   */
  public void loadAppsScripts() throws IOException {
    loadScripts(getResourceDir() + fs + "gestures/InMoovApps/Rock_Paper_Scissors");
    loadScripts(getResourceDir() + fs + "gestures/InMoovApps/Kids_WordsGame");
  }

  public void loadGestures() {
    loadGestures(getResourceDir() + fs + "gestures");
  }

  /**
   * This blocking method will look at all of the .py files in a directory. One
   * by one it will load the files into the python interpreter. A gesture python
   * file should contain 1 method definition that is the same as the filename.
   * 
   * @param directory
   *          - the directory that contains the gesture python files.
   * @return true/false
   */
  public boolean loadGestures(String directory) {
    invoke("publishEvent", "LOAD GESTURES");

    // iterate over each of the python files in the directory
    // and load them into the python interpreter.
    String extension = "py";
    Integer totalLoaded = 0;
    Integer totalError = 0;

    File dir = new File(directory);
    dir.mkdirs();

    if (dir.exists()) {
      for (File f : dir.listFiles()) {
        if (FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase(extension)) {
          if (loadFile(f.getAbsolutePath()) == true) {
            totalLoaded += 1;
            String methodName = f.getName().substring(0, f.getName().length() - 3) + "()";
            gestures.add(methodName);
          } else {
            error("could not load %s", f.getName());
            totalError += 1;
          }
        } else {
          log.info("{} is not a {} file", f.getAbsolutePath(), extension);
        }
      }
    }
    info("%s Gestures loaded, %s Gestures with error", totalLoaded, totalError);
    broadcastState();
    if (totalError > 0) {
      invoke("publishEvent", "GESTURE_ERROR");
      return false;
    }
    return true;
  }

  /**
   * execute python scripts in the init directory on startup of the service
   * 
   * @throws IOException
   */
  public void loadInitScripts() throws IOException {
    loadScripts(getResourceDir() + fs + "init");
  }

  /**
   * Generalized directory python script loading method
   * 
   * @param directory
   * @throws IOException
   */
  public void loadScripts(String directory) throws IOException {
    File dir = new File(directory);

    if (!dir.exists() || !dir.isDirectory()) {
      invoke("publishEvent", "LOAD SCRIPTS ERROR");
      return;
    }

    String[] extensions = { "py" };

    for (String extension : extensions) {
      File[] files = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.toLowerCase().endsWith("." + extension);
        }
      });

      if (files != null) {
        for (File file : files) {
          Python p = (Python) Runtime.start("python", "Python");
          if (p != null) {
            p.execFile(file.getAbsolutePath());
          }
        }
      }
    }
  }

  public void moveArm(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    invoke("publishMoveArm", which, bicep, rotate, shoulder, omoplate);
  }

  public void moveEyelids(Double eyelidleftPos, Double eyelidrightPos) {
    sendToPeer("head", "moveEyelids", eyelidleftPos, eyelidrightPos);
  }

  public void moveEyes(Double eyeX, Double eyeY) {
    moveHead(null, null, eyeX, eyeY, null, null);
  }

  public void moveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    invoke("publishMoveHand", which, thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void moveHead(Double neck, Double rothead) {
    moveHead(neck, rothead, null, null, null, null);
  }

  public void moveHead(Double neck, Double rothead, Double rollNeck) {
    moveHead(neck, rothead, null, null, null, rollNeck);
  }

  public void moveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw) {
    moveHead(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    invoke("publishMoveHead", neck, rothead, eyeX, eyeY, jaw, rollNeck);
  }

  public void moveHead(Integer neck, Integer rothead, Integer rollNeck) {
    moveHead((double) neck, (double) rothead, null, null, null, (double) rollNeck);
  }

  public void moveHeadBlocking(Double neck, Double rothead) {
    moveHeadBlocking(neck, rothead, null);
  }

  public void moveHeadBlocking(Double neck, Double rothead, Double rollNeck) {
    moveHeadBlocking(neck, rothead, null, null, null, rollNeck);
  }

  public void moveHeadBlocking(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw) {
    moveHeadBlocking(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveHeadBlocking(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    try {
      sendBlocking(getPeerName("head"), "moveToBlocking", neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } catch (Exception e) {
      error(e);
    }
  }

  public void moveLeftArm(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    moveArm("left", bicep, rotate, shoulder, omoplate);
  }

  public void moveLeftHand(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    moveHand("left", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void moveLeftHand(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    moveHand("left", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public void moveRightArm(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    moveArm("right", bicep, rotate, shoulder, omoplate);
  }

  public void moveRightHand(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    moveHand("right", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void moveRightHand(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    moveHand("right", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public void moveTorso(Double topStom, Double midStom, Double lowStom) {
    // the "right" way
    invoke("publishMoveTorso", topStom, midStom, lowStom);
  }

  public void moveTorsoBlocking(Double topStom, Double midStom, Double lowStom) {
    // the "right" way
    sendToPeer("torso", "moveToBlocking", topStom, midStom, lowStom);
  }

  public PredicateEvent onChangePredicate(PredicateEvent event) {
    log.error("onChangePredicate {}", event);
    if (event.name.equals("topic")) {
      invoke("publishEvent", String.format("TOPIC CHANGED TO %s", event.value));
    }
    // depending on configuration ....
    // call python ?
    // fire fsm events ?
    // do defaults ?
    return event;
  }

  /**
   * comes in from runtime which owns the config list
   * 
   * @param configList
   *          list of configs
   */
  public void onConfigList(List<String> configList) {
    this.configList = configList;
    invoke("publishConfigList");
  }

  @Override
  public void onCreated(String fullname) {
    log.info("{} created", fullname);
  }

  @Override
  public void onEndSpeaking(String utterance) {
    isSpeaking = false;
  }

  public void onFinishedConfig(String configName) {
    log.info("onFinishedConfig");
    // invoke("publishEvent", "configFinished");
    invoke("publishConfigFinished", configName);
  }

  public void onGestureStatus(Status status) {
    if (!status.equals(Status.success()) && !status.equals(Status.warn("Python process killed !"))) {
      error("I cannot execute %s, please check logs", lastGestureExecuted);
    }
    finishedGesture(lastGestureExecuted);

    unsubscribe("python", "publishStatus", this.getName(), "onGestureStatus");
  }

  /**
   * Central hub of input motion control. Potentially, all input from joysticks,
   * quest2 controllers and headset, or any IK service could be sent here
   */
  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    log.debug("onJointAngles {}", angleMap);
    // here we can make decisions on what ik sets we want to use and
    // what body parts are to move
    for (String name : angleMap.keySet()) {
      ServiceInterface si = Runtime.getService(name);
      if (si != null && si instanceof ServoControl) {
        ((Servo) si).moveTo(angleMap.get(name));
      }
    }
  }

  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    // TODO timer ? to test and not send an event
    // switches to manual control ?
    invoke("publishEvent", "joystick");
  }

  /**
   * Centralized logging system will have all logging from all services,
   * including lower level logs that do not propegate as statuses
   * 
   * @param log
   *          - flushed log from Log service
   */
  public void onLogEvents(List<LogEntry> log) {
    // scan for warn or errors
    for (LogEntry entry : log) {
      if ("ERROR".equals(entry.level) && errors.size() < 100) {
        errors.add(entry);
        // invoke("publishError", entry);
      }
    }
  }

  public String onNewState(String state) {
    log.error("onNewState {}", state);

    // put configurable filter here !

    // state substitutions ?
    // let python subscribe directly to fsm.publishNewState

    // if
    invoke(state);
    // depending on configuration ....
    // call python ?
    // fire fsm events ?
    // do defaults ?
    return state;
  }

  public OpenCVData onOpenCVData(OpenCVData data) {
    // FIXME - publish event with or without data ? String file reference
    return data;
  }

  /**
   * onPeak volume callback TODO - maybe make it variable with volume ?
   * 
   * @param volume
   */
  public void onPeak(double volume) {
    if (config.neoPixelFlashWhenSpeaking && !"boot".equals(getState())) {
      if (volume > 0.5) {
        invoke("publishSpeakingFlash", "speaking");
      }
    }
  }

  /**
   * initial callback for Pir sensor Default behavior will be: send fsm event
   * onPirOn flash neopixel
   */
  public void onPirOn() {
    log.info("onPirOn");
    // FIXME flash on config.flashOnBoot
    invoke("publishFlash", "pir");
    String botState = chatBot.getPredicate("botState");
    if ("sleeping".equals(botState)) {
      invoke("publishEvent", "WAKE");
    }
  }

  public void onPirOff() {
    log.info("onPirOff");
  }

  // GOOD GOOD GOOD - LOOPBACK - flexible and replacable by python
  // yet provides a stable default, which can be fully replaced
  // Works using common pub/sub rules
  // TODO - this is a loopback power up
  // its replaceable by typical subscription rules
  public void onPowerUp() {
    // CON - type aware
    NeoPixel neoPixel = (NeoPixel) getPeer("neoPixel");
    // CON - necessary NPE checking
    if (neoPixel != null) {
      neoPixel.setColor(0, 130, 0);
      neoPixel.playAnimation("Larson Scanner");
    }
  }

  @Override
  public void onRegistered(Registration registration) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onReleased(String name) {
  }

  // I THINK THIS IS GOOD (good simple one)- need more info though
  public boolean onSense(boolean b) {
    // if wake on Pir config &&
    // setEvent("pir-sense-on" .. also sets it in config ?
    // config.handledEvents["pir-sense-on"]
    if (b) {
      invoke("publishEvent", "PIR ON");
    } else {
      invoke("publishEvent", "PIR OFF");
    }
    return b;
  }

  /**
   * runtime re-publish relay
   * 
   * @param configName
   */
  public void onStartConfig(String configName) {
    log.info("onStartConfig");
    invoke("publishConfigStarted", configName);
  }

  /**
   * Part of service life cycle - a new servo has been started
   * 
   * need a directed message sent to a callback simlar to this except it should
   * be a "key" not a "fullname" ! .. this could be created with minimal
   * structure .. i think
   */
  @Override
  public void onStarted(String name) {
    try {

      log.info("onStarted {}", name);
      // new servo
      ServiceInterface si = Runtime.getService(name);
      if ("Servo".equals(si.getSimpleName())) {
        log.info("sending setAutoDisable true to {}", name);
        // send(name, "setAutoDisable", true);
        Servo servo = (Servo) Runtime.getService(name);
        servo.setAutoDisable(true);
      }
    } catch (Exception e) {
      log.error("onStarted threw", e);
    }
  }

  // FIXME - rebroadcast these
  @Override
  public void onStartSpeaking(String utterance) {
    isSpeaking = true;
  }

  /**
   * publishStateChange
   * 
   * The integration between the FiniteStateMachine (fsm) and the InMoov2
   * service and potentially other services (Python, ProgramAB) happens here.
   * 
   * After boot all state changes get published here.
   * 
   * Some InMoov2 service methods will be called here for "default
   * implemenation" of states. If a user doesn't want to have that default
   * implementation, they can change it by changing the definition of the state
   * machine, and have a new state which will call a Python inmoov2 library
   * callback. Overriding, appending, or completely transforming the behavior is
   * all easily accomplished by managing the fsm and python inmoov2 library
   * callbacks.
   * 
   * Python inmoov2 callbacks ProgramAB topic switching
   * 
   * Depending on config:
   * 
   * @param stateChange
   * @return
   */
  public StateChange publishStateChange(StateChange stateChange) {
    log.info("publishStateChange {}", stateChange);

    log.info("onStateChange {}", stateChange);

    lastState = state;
    state = stateChange.state;

    processMessage("onStateChange", stateChange);

    return stateChange;
  }

  @Override
  public void onStopped(String name) {
    log.info("service {} has stopped");
    // using release peer for peer releasing
    // FIXME - auto remove subscriptions of peers?
  }

  @Override
  public void onText(String text) {
    // FIXME - we should be able to "re"-publish text but text is coming from
    // different sources
    // some might be coming from the ear - some from the mouth ... - there has
    // to be a distinction
    log.info("onText - {}", text);
    invoke("publishText", text);
  }

  // TODO FIX/CHECK this, migrate from python land
  public void powerDown() {

    rest();
    purgeTasks();
    disable();

    if (ear != null) {
      ear.lockOutAllGrammarExcept("power up");
    }

    // FIXME - DO NOT DO THIS !!!! SIMPLY PUBLISH A POWER DOWN EVENT AND PYTHON
    // CAN SUBSCRIBE
    // AND MAINTAIN A SET OF onPowerDown: callback methods
    python.execMethod("power_down");
  }

  // TODO FIX/CHECK this, migrate from python land
  // FIXME - defaultPowerUp switchable + override
  public void powerUp() {
    enable();
    rest();

    if (ear != null) {
      ear.clearLock();
    }

    beginCheckingOnInactivity();

    python.execMethod("power_up");
  }

  public void processMessage(String method) {
    processMessage(method, null);
  }

  /**
   * Will publish processing messages to the processor(s) currently subscribed.
   * 
   * @param method
   * @param data
   */
  public void processMessage(String method, Object data) {
    // User processing should not occur until after boot has completed
    if (!state.equals("boot")) {
      // FIXME - this needs to be in config
      // FIXME - change peer name to "processor"
      // String processor = getPeerName("py4j");
      String processor = "python";

      Message msg = Message.createMessage(getName(), processor, method, data);
      // FIXME - is this too much abstraction .. to publish as well as
      // configurable send ?
      invoke("publishProcessMessage", msg);
    }
  }

  /**
   * easy utility to publishMessage
   * 
   * @param name
   * @param method
   * @param data
   */
  public void publish(String name, String method, Object... data) {
    Message msg = Message.createMessage(getName(), name, method, data);
    invoke("publishMessage", msg);
  }

  public String publishConfigFinished(String configName) {
    info("config %s finished", configName);
    invoke("publishEvent", "CONFIG LOADED " + configName);

    return configName;
  }

  /**
   * "re"-publishing runtime config list, because I don't want to fix the js
   * subscribeTo :P
   * 
   * @return list of config names
   */
  public List<String> publishConfigList() {
    return configList;
  }

  public String publishConfigStarted(String configName) {
    info("config %s started", configName);
    invoke("publishEvent", "CONFIG STARTED " + configName);
    return configName;
  }

  /**
   * event publisher for the fsm - although other services potentially can
   * consume and filter this event channel
   * 
   * @param event
   * @return
   */
  public String publishEvent(String event) {
    return String.format("SYSTEM_EVENT %s", event);
  }

  /**
   * used to configure a flashing event - could use configuration to signal
   * different colors and states
   * 
   * @return
   */
  public String publishFlash(String flashName) {
    return flashName;
  }

  /**
   * FIXME - get rid of all functionality here - should all be controlled by
   * behaviors
   * 
   * A heartbeat that continues to check status, and fire events to the FSM.
   * Checks battery, flashes leds and processes all the configured checks in
   * onHeartbeat at a regular interval
   */
  public Heartbeat publishHeartbeat() {
    log.debug("publishHeartbeat");
    heartbeatCount++;
    Heartbeat heartbeat = new Heartbeat(this);
    try {

      if ("boot".equals(state)) {
        // continue booting - we don't put heartbeats in user/python space
        // until java-land is done booting
        log.info("boot hasn't completed, will not process heartbeat");
        boot();
        return heartbeat;
      }

      Long lastActivityTime = getLastActivityTime();

      // FIXME lastActivityTime != 0 is bogus - the value should be null if
      // never set
      if (config.stateIdleInterval != null && lastActivityTime != null && lastActivityTime != 0
          && lastActivityTime + (config.stateIdleInterval * 1000) < System.currentTimeMillis()) {
        stateLastIdleTime = lastActivityTime;
      }

      if (System.currentTimeMillis() > stateLastIdleTime + (config.stateIdleInterval * 1000)) {
        // idle event to be handled with the processor
        processMessage("onIdle");
        stateLastIdleTime = System.currentTimeMillis();
      }

      // interval event firing
      if (config.stateRandomInterval != null && System.currentTimeMillis() > stateLastRandomTime + (config.stateRandomInterval * 1000)) {
        // fsm.fire("random");
        stateLastRandomTime = System.currentTimeMillis();
      }

    } catch (Exception e) {
      error(e);
    }

    // if (config.pirOnFlash && isPeerStarted("pir") && isPirOn) {
    //// flash("pir");
    // }

    if (config.batteryInSystem) {
      double batteryLevel = Runtime.getBatteryLevel();
      invoke("publishBatteryLevel", batteryLevel);
      // FIXME - thresholding should always have old value or state
      // so we don't pump endless errors
      if (batteryLevel < 5) {
        error("battery level < 5 percent");
        // systemEvent(BATTERY ERROR)
      } else if (batteryLevel < 10) {
        warn("battery level < 10 percent");
        // systemEvent(BATTERY WARN)
      }
    }

    // flash error until errors are cleared
    if (config.flashOnErrors) {
      if (errors.size() > 0) {
        // invoke("publishFlash", "error");
      } else {
        // invoke("publishFlash", "heartbeat");
      }
    }

    // FIXME - add errors to heartbeat
    processMessage("onHeartbeat", heartbeat);
    return heartbeat;
  }

  /**
   * A more extensible interface point than publishEvent FIXME - create
   * interface for this
   * 
   * @param msg
   * @return
   */
  public Message publishMessage(Message msg) {
    return msg;
  }

  public HashMap<String, Double> publishMoveArm(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("bicep", bicep);
    map.put("rotate", rotate);
    map.put("shoulder", shoulder);
    map.put("omoplate", omoplate);
    if ("left".equals(which)) {
      invoke("publishMoveLeftArm", bicep, rotate, shoulder, omoplate);
    } else {
      invoke("publishMoveRightArm", bicep, rotate, shoulder, omoplate);
    }
    return map;
  }

  public HashMap<String, Object> publishMoveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    HashMap<String, Object> map = new HashMap<>();
    map.put("which", which);
    map.put("thumb", thumb);
    map.put("index", index);
    map.put("majeure", majeure);
    map.put("ringFinger", ringFinger);
    map.put("pinky", pinky);
    map.put("wrist", wrist);
    if ("left".equals(which)) {
      invoke("publishMoveLeftHand", thumb, index, majeure, ringFinger, pinky, wrist);
    } else {
      invoke("publishMoveRightHand", thumb, index, majeure, ringFinger, pinky, wrist);
    }
    return map;
  }

  public HashMap<String, Double> publishMoveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("neck", neck);
    map.put("rothead", rothead);
    map.put("eyeX", eyeX);
    map.put("eyeY", eyeY);
    map.put("jaw", jaw);
    map.put("rollNeck", rollNeck);
    return map;
  }

  public HashMap<String, Double> publishMoveLeftArm(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("bicep", bicep);
    map.put("rotate", rotate);
    map.put("shoulder", shoulder);
    map.put("omoplate", omoplate);
    return map;
  }

  public HashMap<String, Double> publishMoveLeftHand(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("thumb", thumb);
    map.put("index", index);
    map.put("majeure", majeure);
    map.put("ringFinger", ringFinger);
    map.put("pinky", pinky);
    map.put("wrist", wrist);
    return map;
  }

  public HashMap<String, Double> publishMoveRightArm(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("bicep", bicep);
    map.put("rotate", rotate);
    map.put("shoulder", shoulder);
    map.put("omoplate", omoplate);
    return map;
  }

  public HashMap<String, Double> publishMoveRightHand(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("thumb", thumb);
    map.put("index", index);
    map.put("majeure", majeure);
    map.put("ringFinger", ringFinger);
    map.put("pinky", pinky);
    map.put("wrist", wrist);
    return map;
  }

  public HashMap<String, Double> publishMoveTorso(Double topStom, Double midStom, Double lowStom) {
    HashMap<String, Double> map = new HashMap<>();
    map.put("topStom", topStom);
    map.put("midStom", midStom);
    map.put("lowStom", lowStom);
    return map;
  }

  public String publishPlayAudioFile(String filename) {
    return filename;
  }

  /**
   * Processing publishing point, where everything InMoov2 wants to be processed
   * is turned into a message and published.
   * 
   * @param msg
   * @return
   */
  public Message publishProcessMessage(Message msg) {
    return msg;
  }

  /**
   * Possible pub/sub way to interface with python - no blocking though
   * 
   * @param code
   * @return
   */
  public String publishPython(String code) {
    return code;
  }

  /**
   * publishes a name for NeoPixel.onFlash to consume, in a seperate channel to
   * potentially be used by "speaking only" leds
   * 
   * @param name
   * @return
   */
  public String publishSpeakingFlash(String name) {
    return name;
  }

  /**
   * stop animation event
   */
  public void publishStopAnimation() {
  }

  /**
   * event publisher for the fsm - although other services potentially can
   * consume and filter this event channel
   * 
   * @param event
   * @return
   */
  public String publishSystemEvent(String event) {
    // well, it turned out underscore was a goofy selection, as underscore in
    // aiml is wildcard ... duh
    return String.format("SYSTEM_EVENT %s", event);
  }

  /**
   * all published text from InMoov2 - including ProgramAB
   */
  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void releasePeer(String peerKey) {
    super.releasePeer(peerKey);
    if (peerKey != null) {
      invoke("publishEvent", "STOPPED " + peerKey);
    }
  }

  @Override
  public void releaseService() {
    try {
      disable();
      heart.stop();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  public void rest() {
    // FIXME publishRest / pub/sub way
    sendToPeer("head", "rest");
    sendToPeer("rightHand", "rest");
    sendToPeer("leftHand", "rest");
    sendToPeer("rightArm", "rest");
    sendToPeer("leftArm", "rest");
    sendToPeer("torso", "rest");
  }

  public boolean setAllVirtual(boolean virtual) {
    Runtime.setAllVirtual(virtual);
    speakBlocking(get("STARTINGVIRTUALHARD"));
    return virtual;
  }

  public void setArmSpeed(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    InMoov2Arm arm = getArm(which);
    if (arm == null) {
      warn("%s arm not started", which);
      return;
    }
    arm.setSpeed(bicep, rotate, shoulder, omoplate);
  }

  @Deprecated
  public void setArmVelocity(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setArmSpeed(which, bicep, rotate, shoulder, omoplate);
  }

  public void setAutoDisable(Boolean param) {

    sendToPeer("head", "setAutoDisable", param);
    sendToPeer("rightHand", "setAutoDisable", param);
    sendToPeer("leftHand", "setAutoDisable", param);
    sendToPeer("rightArm", "setAutoDisable", param);
    sendToPeer("leftArm", "setAutoDisable", param);
    sendToPeer("torso", "setAutoDisable", param);
  }

  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    InMoov2Hand hand = getHand(which);
    if (hand == null) {
      warn("%s hand not started", which);
      return;
    }
    hand.setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
  }

  @Deprecated
  public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  @Deprecated
  public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void setHeadSpeed(Double rothead, Double neck) {
    setHeadSpeed(rothead, neck, null, null, null, null);
  }

  public void setHeadSpeed(Double rothead, Double neck, Double rollNeck) {
    setHeadSpeed(rothead, neck, null, null, null, rollNeck);
  }

  public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
  }

  public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    sendToPeer("head", "setSpeed", rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
  }

  public void setHeadSpeed(Integer rothead, Integer neck, Integer rollNeck) {
    setHeadSpeed((double) rothead, (double) neck, null, null, null, (double) rollNeck);
  }

  @Deprecated
  public void setHeadVelocity(Double rothead, Double neck) {
    setHeadSpeed(rothead, neck, null, null, null, null);
  }

  @Deprecated
  public void setHeadVelocity(Double rothead, Double neck, Double rollNeck) {
    setHeadSpeed(rothead, neck, null, null, null, rollNeck);
  }

  @Deprecated
  public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
  }

  @Deprecated
  public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
  }

  public void setLeftArmSpeed(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setArmSpeed("left", bicep, rotate, shoulder, omoplate);
  }

  public void setLeftArmSpeed(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
    setArmSpeed("left", (double) bicep, (double) rotate, (double) shoulder, (double) omoplate);
  }

  public void setLeftHandSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    setHandSpeed("left", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void setLeftHandSpeed(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    setHandSpeed("left", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  @Override
  public void setLocale(String code) {

    if (code == null) {
      log.warn("setLocale null");
      return;
    }

    // filter of the set of supported locales
    if (!Locale.hasLanguage(locales, code)) {
      error("InMoov does not support %s only %s", code, locales.keySet());
      return;
    }

    super.setLocale(code);

    // super.setLocale(code);
    for (ServiceInterface si : Runtime.getLocalServices().values()) {
      if (!si.equals(this)) {
        si.setLocale(code);
      }
    }
  }

  public void setMute(boolean mute) {
    info("Set mute to %s", mute);
    this.mute = mute;
    sendToPeer("mouth", "setMute", mute);
    broadcastState();
  }

  public void setNeopixelAnimation(String animation, Integer red, Integer green, Integer blue, Integer speed) {
    sendToPeer("neopixel", "animation", red, green, blue, speed);
  }

  public void setOpenCV(OpenCV opencv) {
    this.opencv = opencv;
  }

  public boolean setPirPlaySounds(boolean b) {
    getTypedConfig().pirPlaySounds = b;
    return b;
  }

  public Object setPredicate(String key, Object data) {
    if (data == null) {
      chatBot.setPredicate(key, null); // "unknown" "null" other sillyness ?
    } else {
      chatBot.setPredicate(key, data.toString());
    }
    return data;
  }

  public void setRightArmSpeed(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setArmSpeed("right", bicep, rotate, shoulder, omoplate);
  }

  public void setRightArmSpeed(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
    setArmSpeed("right", (double) bicep, (double) rotate, (double) shoulder, (double) omoplate);
  }

  public void setRightHandSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    setHandSpeed("right", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void setRightHandSpeed(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    setHandSpeed("right", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public boolean setSpeechType(String speechType) {

    if (speechType == null) {
      error("cannot change speech type to null");
      return false;
    }

    if (!speechType.contains(".")) {
      speechType = "org.myrobotlab.service." + speechType;
    }

    Runtime runtime = Runtime.getInstance();
    String peerName = getName() + ".mouth";
    Plan plan = runtime.getDefault(peerName, speechType);
    try {
      SpeechSynthesisConfig mouth = (SpeechSynthesisConfig) plan.get(peerName);
      mouth.speechRecognizers = new String[] { getName() + ".ear" };

      savePeerConfig("mouth", plan.get(peerName));

      if (isPeerStarted("mouth")) {
        // restart
        releasePeer("mouth");
        startPeer("mouth");
      }

    } catch (Exception e) {
      error("could not create config for %s", speechType);
      return false;
    }

    return true;

    // updatePeerType("mouth" /* getPeerName("mouth") */, speechType);
    // return speechType;
  }

  public void setTopic(String topic) {
    chatBot.setTopic(topic);
  }

  public void setTorsoSpeed(Double topStom, Double midStom, Double lowStom) {
    sendToPeer("torso", "setSpeed", topStom, midStom, lowStom);
  }

  public void setTorsoSpeed(Integer topStom, Integer midStom, Integer lowStom) {
    setTorsoSpeed((double) topStom, (double) midStom, (double) lowStom);
  }

  // -----------------------------------------------------------------------------
  // These are methods added that were in InMoov1 that we no longer had in
  // InMoov2.
  // From original InMoov1 so we don't loose the

  @Deprecated /* use setTorsoSpeed */
  public void setTorsoVelocity(Double topStom, Double midStom, Double lowStom) {
    setTorsoSpeed(topStom, midStom, lowStom);
  }

  public void setVoice(String name) {
    if (mouth != null) {
      mouth.setVoice(name);
      voiceSelected = name;
      speakBlocking(String.format("%s %s", get("SETLANG"), name));
    }
  }

  public void sleeping() {
    log.error("sleeping");
  }

  public void speak(String toSpeak) {
    sendToPeer("mouth", "speak", toSpeak);
  }

  public void speakAlert(String toSpeak) {
    invoke("publishEvent", "ALERT");
    speakBlocking(toSpeak);
  }

  public void speakBlocking(String speak) {
    speakBlocking(speak, (Object[]) null);
  }

  // FIXME - publish text regardless if mouth exists ...
  public void speakBlocking(String format, Object... args) {

    if (format == null) {
      return;
    }

    String toSpeak = format;
    if (args != null) {
      toSpeak = String.format(format, args);
    }

    // FIXME - publish onText when listening
    invoke("publishText", toSpeak);

    // FIXME - mute is not normalized
    if (!mute && isPeerStarted("mouth")) {
      // sendToPeer("mouth", "speakBlocking", toSpeak);
      // invokePeer("mouth", "speakBlocking", toSpeak);
      // HEH, CANNOT DO THIS !! ITS NOT BLOCKING - NEED BLOCKING
      // BECAUSE A GAZILLION GESTURES DEPEND ON BLOCKING SPEECH !!!
      // sendToPeer("mouth", "speakBlocking", toSpeak);
      AbstractSpeechSynthesis mouth = (AbstractSpeechSynthesis) getPeer("mouth");
      if (mouth != null) {
        mouth.speak(toSpeak);
      }
    }
  }

  @Deprecated /* use startPeers */
  public void startAll() throws Exception {
    startAll(null, null);
  }

  @Deprecated /* use startPeers */
  public void startAll(String leftPort, String rightPort) throws Exception {
    startMouth();
    startChatBot();

    // startHeadTracking();
    // startEyesTracking();
    // startOpenCV();
    startEar();

    startServos();
    // startMouthControl(head.jaw, mouth);

    speakBlocking(get("STARTINGSEQUENCE"));
  }

  @Deprecated /* i01.startPeer("chatBot") - all details should be in config */
  public void startBrain() {
    startChatBot();
  }

  @Deprecated /* i01.startPeer("chatBot") - all details should be in config */
  public ProgramAB startChatBot() {

    try {

      if (locale != null) {
        chatBot.setCurrentBotName(locale.getTag());
      }

      // FIXME remove get en.properties stuff
      speakBlocking(get("CHATBOTACTIVATED"));

      chatBot.attachTextPublisher(ear);

      // this.attach(chatBot); FIXME - attach as a TextPublisher - then
      // re-publish
      // FIXME - deal with language
      // speakBlocking(get("CHATBOTACTIVATED"));
      chatBot.repetitionCount(10);
      // chatBot.setPath(getResourceDir() + fs + "chatbot");
      // chatBot.setPath(getDataDir() + "ProgramAB");
      chatBot.startSession("default", locale.getTag());
      // reset some parameters to default...
      chatBot.setPredicate("topic", "default");
      chatBot.setPredicate("questionfirstinit", "");
      chatBot.setPredicate("tmpname", "");
      chatBot.setPredicate("null", "");
      // load last user session
      if (!chatBot.getPredicate("name").isEmpty()) {
        if (chatBot.getPredicate("lastUsername").isEmpty() || chatBot.getPredicate("lastUsername").equals("unknown") || chatBot.getPredicate("lastUsername").equals("default")) {
          chatBot.setPredicate("lastUsername", chatBot.getPredicate("name"));
        }
      }
      chatBot.setPredicate("parameterHowDoYouDo", "");
      chatBot.savePredicates();
      htmlFilter = (HtmlFilter) startPeer("htmlFilter");// Runtime.start("htmlFilter",
      // "HtmlFilter");
      chatBot.attachTextListener(htmlFilter);
      htmlFilter.attachTextListener((TextListener) getPeer("mouth"));
      chatBot.attachTextListener(this);
      // start session based on last recognized person
      // if (!chatBot.getPredicate("default", "lastUsername").isEmpty() &&
      // !chatBot.getPredicate("default", "lastUsername").equals("unknown")) {
      // chatBot.startSession(chatBot.getPredicate("lastUsername"));
      // }
      if (chatBot.getPredicate("default", "firstinit").isEmpty() || chatBot.getPredicate("default", "firstinit").equals("unknown")
          || chatBot.getPredicate("default", "firstinit").equals("started")) {
        chatBot.startSession(chatBot.getPredicate("default", "lastUsername"));
        invoke("publishEvent", "FIRST INIT");
      } else {
        chatBot.startSession(chatBot.getPredicate("default", "lastUsername"));
        invoke("publishEvent", "WAKE UP");
      }
    } catch (Exception e) {
      speak("could not load chatBot");
      error(e.getMessage());
      speak(e.getMessage());
    }
    broadcastState();
    return chatBot;
  }

  @Deprecated /* use startPeer */
  public SpeechRecognizer startEar() {

    ear = (SpeechRecognizer) startPeer("ear");
    ear.attachSpeechSynthesis((SpeechSynthesis) getPeer("mouth"));
    ear.attachTextListener(chatBot);
    broadcastState();
    return ear;
  }

  public void startedGesture() {
    startedGesture("unknown");
  }

  public void startedGesture(String nameOfGesture) {
    if (gestureAlreadyStarted) {
      warn("Warning 1 gesture already running, this can break spacetime and lot of things");
    } else {
      log.info("Starting gesture : {}", nameOfGesture);
      gestureAlreadyStarted = true;
      // RobotCanMoveRandom = false;
    }
  }

  public void startHeartbeat() {
    heart.start();
  }

  // TODO - general objective "might" be to reduce peers down to something
  // that does not need a reference - where type can be switched before creation
  // and the only thing needed is pubs/subs that are not handled in abstracts
  @Deprecated /* use startPeer */
  public SpeechSynthesis startMouth() {

    // FIXME - set type ??? - maybe a good product of InMoov
    // if "new" type cannot necessarily grab yml file
    // setMouthType

    // FIXME - bad to have a reference, should only need the "name" of the
    // service !!!
    mouth = (SpeechSynthesis) startPeer("mouth");

    // voices = mouth.getVoices();
    // Voice voice = mouth.getVoice();
    // if (voice != null) {
    // voiceSelected = voice.getName();
    // }

    if (mute) {
      mouth.setMute(true);
    }

    mouth.attachSpeechRecognizer(ear);
    // mouth.attach(htmlFilter); // same as chatBot not needed

    // this.attach((Attachable) mouth);
    // if (ear != null) ....

    broadcastState();

    speakBlocking(get("STARTINGMOUTH"));
    if (Platform.isVirtual()) {
      speakBlocking(get("STARTINGVIRTUALHARD"));
    }
    speakBlocking(get("WHATISTHISLANGUAGE"));

    return mouth;
  }

  @Deprecated /* use startPeer */
  public OpenCV startOpenCV() {
    speakBlocking(get("STARTINGOPENCV"));
    opencv = (OpenCV) startPeer("opencv");
    subscribeTo(opencv.getName(), "publishOpenCVData");
    return opencv;
  }

  @Override
  public ServiceInterface startPeer(String peer) {
    ServiceInterface si = super.startPeer(peer);
    return si;
  }

  @Override
  public void startService() {
    super.startService();

    // This is required the core of InMoov is
    // a FSM ProgramAB and some form of Python/Jython
    fsm = (FiniteStateMachine) startPeer("fsm");

    // Chatbot is a required part of InMoov2
    chatBot = (ProgramAB) startPeer("chatBot");
    try {
      chatBot.startSession();
      chatBot.setPredicate("robot", getName());
    } catch (IOException e) {
      error(e);
    }

    // A python process is required - should be defined as a peer
    // of Type Python or Py4j

    // just for comparing config with current "default"
    // debugging only
    Runtime runtime = Runtime.getInstance();

    // if you hardcode subscriptions here - they should
    // be controlled/branched by config

    // get service start and release life cycle events
    runtime.attachServiceLifeCycleListener(getName());

    // FIXME all subscriptions should be in InMoov2Config
    // get events of new services and shutdown
    // we can't add listener's in config, perhaps there should be
    // "subscriptions" in config too ?
    subscribe("runtime", "shutdown");
    subscribe("runtime", "publishConfigList");

    runtime.invoke("publishConfigList");

    if (config.heartbeat) {
      startHeartbeat();
    } else {
      stopHeartbeat();
    }

  }

  public void startServos() {
    startPeer("head");
    startPeer("leftArm");
    startPeer("leftHand");
    startPeer("rightArm");
    startPeer("rightHand");
    startPeer("torso");
  }

  // FIXME .. externalize in a json file included in InMoov2
  public Simulator startSimulator() throws Exception {
    Simulator si = (Simulator) startPeer("simulator");
    return si;
  }

  public void stop() {
    sendToPeer("head", "stop");
    sendToPeer("rightHand", "stop");
    sendToPeer("leftHand", "stop");
    sendToPeer("rightArm", "stop");
    sendToPeer("leftArm", "stop");
    sendToPeer("torso", "stop");
  }

  public void stopGesture() {
    // FIXME cannot be casting to Python
    Python p = (Python) Runtime.getService("python");
    p.stop();
  }

  public void stopHeartbeat() {
    heart.stop();
  }

  public void stopNeopixelAnimation() {
    sendToPeer("neopixel", "clear");
  }

  public void systemCheck() {
    log.error("systemCheck()");
    Runtime runtime = Runtime.getInstance();
    int servoCount = 0;
    int servoAttachedCount = 0;
    for (ServiceInterface si : Runtime.getServices()) {
      if (si.getClass().getSimpleName().equals("Servo")) {
        servoCount++;
        if (((Servo) si).getController() != null) {
          servoAttachedCount++;
        }
      }
    }

    setPredicate("systemServoCount", servoCount);
    setPredicate("systemAttachedServoCount", servoAttachedCount);
    setPredicate("systemFreeMemory", Runtime.getFreeMemory());
    Platform platform = Runtime.getPlatform();
    setPredicate("system version", platform.getVersion());
    // ERROR buffer !!!
    systemEvent("SYSTEMCHECKFINISHED"); // wtf is this?
  }

  public String systemEvent(String eventMsg) {
    invoke("publishSystemEvent", eventMsg);
    return eventMsg;
  }

  public String systemEvent(String format, Object... ags) {
    String eventMsg = String.format(format, ags);
    return systemEvent(eventMsg);
  }

  // FIXME - if this is really desired it will drive local references for all
  // servos
  public void waitTargetPos() {
    // FIXME - consider actual reference for this
    sendToPeer("head", "waitTargetPos");
    sendToPeer("rightHand", "waitTargetPos");
    sendToPeer("leftHand", "waitTargetPos");
    sendToPeer("rightArm", "waitTargetPos");
    sendToPeer("leftArm", "waitTargetPos");
    sendToPeer("torso", "waitTargetPos");
  }

}
