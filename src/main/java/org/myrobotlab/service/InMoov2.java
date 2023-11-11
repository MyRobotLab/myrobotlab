package org.myrobotlab.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.framework.Message;
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
import org.myrobotlab.service.Log.LogEntry;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.data.Event;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.data.LedDisplayData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.ServiceLifeCycleListener;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class InMoov2 extends Service<InMoov2Config> implements ServiceLifeCycleListener, TextListener, TextPublisher, JoystickListener, LocaleProvider, IKJointAngleListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  public static LinkedHashMap<String, String> lpVars = new LinkedHashMap<String, String>();

  private static final long serialVersionUID = 1L;

  protected static final Set<String> stateDefaults = new TreeSet<>();

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


  /**
   * the config that was processed before booting, if there was one.
   */
  protected String bootedConfig = null;
  
  protected LedDisplayData led = new LedDisplayData();

  protected transient ProgramAB chatBot;

  protected List<String> configList;
  /**
   * Configuration from runtime has started. This is when runtime starts
   * processing a configuration set for the first time since inmoov was started
   * 
   * When configuration is being processed this is true, otherwise false. It's a
   * state of the InMoov2 lifecycle, but the FSM isn't guaranteed to be started
   * (or configured) at this time, so a member variable was created to guarantee
   * this information is available
   */
  protected boolean configStarted = false;

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

  protected boolean isPirOn = false;

  protected transient HtmlFilter htmlFilter;

  protected transient ImageDisplay imageDisplay;

  protected String lastGestureExecuted;

  protected Long lastPirActivityTime;

  protected Map<String, LedDisplayData> ledDisplayMap = new TreeMap<>();

  /**
   * supported locales
   */
  protected Map<String, Locale> locales = null;

  protected transient SpeechSynthesis mouth;

  protected boolean mute = false;

  protected transient OpenCV opencv;

  protected List<String> peersStarted = new ArrayList<>();

  protected transient Python python;

  protected long stateLastIdleTime = System.currentTimeMillis();

  protected long stateLastRandomTime = System.currentTimeMillis();

  protected String voiceSelected;

  public InMoov2(String n, String id) {
    super(n, id);

    // add the default InMoov2 state handlers - so the FSM can invoke them
    // this is hardcode, because it requires Java methods in InMoov2
    // so it makes sense to hardcode them...
    // if a user needs something different, it will happen in pyton-land
    // consequence it this will need maintenance if there are new InMoov2 java
    // state handlers
    stateDefaults.add("wake");
    stateDefaults.add("firstInit");
    stateDefaults.add("idle");
    stateDefaults.add("random");
    stateDefaults.add("sleep"); // listens & dreams, no opencv, waits for
                                // wakeWord, pir active
    stateDefaults.add("powerDown"); // stops heartbeat, listening ?
    stateDefaults.add("shutdown");// ends mrl

    ledDisplayMap.put("error", new LedDisplayData(120, 0, 0, 3, 30, 30));
    ledDisplayMap.put("info", new LedDisplayData(0, 0, 120, 1, 30, 30));
    ledDisplayMap.put("success", new LedDisplayData(0, 0, 120, 2, 30, 30));
    ledDisplayMap.put("warn", new LedDisplayData(100, 100, 0, 3, 30, 30));
    ledDisplayMap.put("heartbeat", new LedDisplayData(210, 110, 0, 2, 100, 30));
    ledDisplayMap.put("pirOn", new LedDisplayData(60, 200, 90, 3, 100, 30));
    ledDisplayMap.put("onPeakColor", new LedDisplayData(180, 53, 21, 3, 60, 30));

    customSoundMap.put("boot", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/confirmation.wav"));
    customSoundMap.put("wake", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/ting.wav"));
    customSoundMap.put("firstInit", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/select.wav"));
    customSoundMap.put("idle", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/start.wav"));
    customSoundMap.put("random", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/reveal.wav"));
    customSoundMap.put("sleep", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/back.wav"));
    customSoundMap.put("powerDown", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/ting.wav"));
    customSoundMap.put("shutdown", FileIO.gluePaths(getResourceDir(), "system/sounds/Notifications/marimba.wav"));

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

      locales = Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL", "ru-RU", "hi-IN", "it-IT", "fi-FI", "pt-PT", "tr-TR");

      if (c.locale != null) {
        setLocale(c.locale);
      } else {
        setLocale(getSupportedLocale(Runtime.getInstance().getLocale().toString()));
      }

      if (c.loadInitScripts) {
        loadInitScripts();
      }
      loadAppsScripts();
      
      if (c.loadGestures) {
        loadGestures();
      }

      if (c.heartbeat) {
        startHeartbeat();
      } else {
        stopHeartbeat();
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
  public boolean execScript(String someScriptName) {
    try {
      Python p = (Python) Runtime.start("python", "Python");
      String script = getResourceAsString(someScriptName);
      return p.exec(script, true);
    } catch (Exception e) {
      error("unable to execute script %s", someScriptName);
      return false;
    }
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

  public void firstInit() {
    log.info("firstInit");
    // cheap way to prevent race condition
    // of "wake" firing a state change .. which will spawn
    // a system event of FIRST_INIT that will answer this
    // question ...
    sleep(2000);
    ProgramAB chatBot = (ProgramAB) getPeer("chatBot");
    if (chatBot != null) {
      chatBot.getResponse("FIRST_INIT");
    }
  }

  public void flash(String name) {
    LedDisplayData led = ledDisplayMap.get(name);
    if (led == null) {
      led = ledDisplayMap.get("default");
    }
    invoke("publishFlash", led.red, led.green, led.blue, led.count, led.timeOn, led.timeOff);
  }

  /**
   * used to configure a flashing event - could use configuration to signal
   * different colors and states
   * 
   * @return
   */
  public void flash() {
    if (ledDisplayMap.get("default") != null) {
      LedDisplayData led = ledDisplayMap.get("default");
      invoke("publishFlash", led.red, led.green, led.blue, led.count, led.timeOn, led.timeOff);
    }
  }

  public void flash(int r, int g, int b, int count) {
    // FIXME - this should be checking a protected "state"
    if (ledDisplayMap.get("default") != null) {
      LedDisplayData led = ledDisplayMap.get("default");
      invoke("publishFlash", r, g, b, count, led.timeOn, led.timeOff);
    }
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

  public Object getPredicate(String key) {
    ProgramAB chatBot = (ProgramAB) getPeer("chatBot");
    if (chatBot != null) {
      return chatBot.getPredicate(key);
    } else {
      error("no chatBot available");
    }
    return null;
  }

  public Response getResponse(String text) {
    ProgramAB chatBot = (ProgramAB) getPeer("chatBot");
    if (chatBot != null) {
      Response response = chatBot.getResponse(text);
      return response;
    } else {
      log.info("chatbot not ready");
    }
    return null;
  }

  public InMoov2Arm getRightArm() {
    return (InMoov2Arm) getPeer("rightArm");
  }

  public InMoov2Hand getRightHand() {
    return (InMoov2Hand) getPeer("rightHand");
  }

  public String getState() {
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

  public void halfSpeed() {
    sendToPeer("head", "setSpeed", 25.0, 25.0, 25.0, 25.0, 100.0, 25.0);
    sendToPeer("rightHand", "setSpeed", 30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    sendToPeer("leftHand", "setSpeed", 30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    sendToPeer("rightArm", "setSpeed", 25.0, 25.0, 25.0, 25.0);
    sendToPeer("leftArm", "setSpeed", 25.0, 25.0, 25.0, 25.0);
    sendToPeer("torso", "setSpeed", 20.0, 20.0, 20.0);
  }

  /**
   * pir active ear listening for wakeword
   */
  public void idle() {
    log.info("idle");
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
    systemEvent("LOAD GESTURES");

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
      systemEvent("LOAD SCRIPTS ERROR");
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
            // FIXME error("x") when an error occurs
            p.execFile(file.getAbsolutePath());
          }
        }
      }
    }
  }

  public void moveArm(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    HashMap<String, Double> map = new HashMap<>();
    Optional.ofNullable(bicep).ifPresent(value -> map.put("bicep", value));
    Optional.ofNullable(rotate).ifPresent(value -> map.put("rotate", value));
    Optional.ofNullable(shoulder).ifPresent(value -> map.put("shoulder", value));
    Optional.ofNullable(omoplate).ifPresent(value -> map.put("omoplate", value));

    if ("left".equals(which)) {
      invoke("publishMoveLeftArm", map);
    } else {
      invoke("publishMoveRightArm", map);
    }
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
    HashMap<String, Double> map = new HashMap<>();
    Optional.ofNullable(thumb).ifPresent(value -> map.put("thumb", value));
    Optional.ofNullable(index).ifPresent(value -> map.put("index", value));
    Optional.ofNullable(majeure).ifPresent(value -> map.put("majeure", value));
    Optional.ofNullable(ringFinger).ifPresent(value -> map.put("ringFinger", value));
    Optional.ofNullable(pinky).ifPresent(value -> map.put("pinky", value));
    Optional.ofNullable(wrist).ifPresent(value -> map.put("wrist", value));

    if ("left".equals(which)) {
      invoke("publishMoveLeftHand", map);
    } else {
      invoke("publishMoveRightHand", map);
    }
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
    HashMap<String, Double> map = new HashMap<>();
    Optional.ofNullable(neck).ifPresent(value -> map.put("neck", value));
    Optional.ofNullable(rothead).ifPresent(value -> map.put("rothead", value));
    Optional.ofNullable(eyeX).ifPresent(value -> map.put("eyeX", value));
    Optional.ofNullable(eyeY).ifPresent(value -> map.put("eyeY", value));
    Optional.ofNullable(jaw).ifPresent(value -> map.put("jaw", value));
    Optional.ofNullable(rollNeck).ifPresent(value -> map.put("rollNeck", value));
    invoke("publishMoveHead", map);
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
    HashMap<String, Double> map = new HashMap<>();
    Optional.ofNullable(topStom).ifPresent(value -> map.put("topStom", value));
    Optional.ofNullable(midStom).ifPresent(value -> map.put("midStom", value));
    Optional.ofNullable(lowStom).ifPresent(value -> map.put("lowStom", value));
    invoke("publishMoveTorso", map);
  }

  public void moveTorsoBlocking(Double topStom, Double midStom, Double lowStom) {
    // the "right" way
    sendToPeer("torso", "moveToBlocking", topStom, midStom, lowStom);
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
   */
  synchronized public void onBoot() {

    // thinking you shouldn't "boot" twice ?
    if (hasBooted) {
      log.warn("will not boot again");
      return;
    }

    List<ServiceInterface> services = Runtime.getServices();
    for (ServiceInterface si : services) {
      if ("Servo".equals(si.getSimpleName())) {
        send(si.getFullName(), "setAutoDisable", true);
      }
    }

    // FIXME - standardize multi-config examples should be available
    // moved from startService to allow more simple control
    // FIXME standard FileIO copyIfNotExists(src, dst)
    try {
      // copy config if it doesn't already exist
      String resourceBotDir = FileIO.gluePaths(getResourceDir(), "config");
      List<File> files = FileIO.getFileList(resourceBotDir);
      for (File f : files) {
        String botDir = "data/config/" + f.getName();
        File bDir = new File(botDir);
        if (bDir.exists() || !f.isDirectory()) {
          log.info("skipping data/config/{}", botDir);
        } else {
          log.info("will copy new data/config/{}", botDir);
          try {
            FileIO.copy(f.getAbsolutePath(), botDir);
          } catch (Exception e) {
            error(e);
          }
        }
      }
    } catch (Exception e) {
      error(e);
    }

    // FIXME - find good way of running an animation "through" a state
    if (config.neoPixelBootGreen && getPeer("neoPixel") != null) {
      NeoPixel neoPixel = (NeoPixel) getPeer("neoPixel");
      if (neoPixel != null) {
        invoke("publishPlayAnimation", config.bootAnimation);
      }
    }

    if (config.startupSound && getPeer("audioPlayer") != null) {
      ((AudioFile) getPeer("audioPlayer")).playBlocking(FileIO.gluePaths(getResourceDir(), "/system/sounds/startupsound.mp3"));
    }

    if (config.systemEventsOnBoot) {
      // reporting on all services and config started
      if (bootedConfig != null) {
        // configuration was processed before booting
        systemEvent("CONFIG STARTED %s", bootedConfig);
      }

      for (String peerKey : peersStarted) {
        systemEvent("STARTED %s", peerKey);
      }

      if (bootedConfig != null) {
        // configuration was processed before booting
        systemEvent("CONFIG LOADED %s", bootedConfig);
      }
    }

    // FIXME - important to do invoke & fsm needs to be consistent order

    // if speaking then turn off animation

    // publish all the errors

    // switch off animations

    // start heartbeat
    // say starting heartbeat
    if (config.heartbeat) {
      startHeartbeat();
    } else {
      stopHeartbeat();
    }

    // say finished booting

    fsm.fire("wake");

    // if (getPeer("mouth") != null) {
    // AbstractSpeechSynthesis<SpeechSynthesisConfig> mouth =
    // (AbstractSpeechSynthesis)getPeer("mouth");
    // mouth.setMute(wasMute);
    // }

    hasBooted = true;
  }

  public PredicateEvent onChangePredicate(PredicateEvent event) {
    log.error("onChangePredicate {}", event);
    if (event.name.equals("topic")) {
      systemEvent("TOPIC CHANGED TO %s", event.value);
    }
    // depending on configuration ....
    // call python ?
    // fire fsm events ?
    // do defaults ?
    return event;
  }

  public void onConfigFinished(String configName) {
    log.info("onConfigFinished");
    invoke("publishBoot");
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

  public void onGestureStatus(Status status) {
    if (!status.equals(Status.success()) && !status.equals(Status.warn("Python process killed !"))) {
      error("I cannot execute %s, please check logs", lastGestureExecuted);
    }
    finishedGesture(lastGestureExecuted);

    unsubscribe("python", "publishStatus", this.getName(), "onGestureStatus");
  }

  /**
   * A generalized recurring event which can preform checks and various other
   * methods or tasks. Heartbeats will not start until after boot stage.
   */
  public void onHeartbeat() {
    try {
      // heartbeats can start before config is
      // done processing - so the following should
      // not be dependent on config

      if (!hasBooted) {
        log.info("boot hasn't completed, will not process heartbeat");
        return;
      }

      Long lastActivityTime = getLastActivityTime();

      // FIXME lastActivityTime != 0 is bogus - the value should be null if
      // never set
      if (config.stateIdleInterval != null && lastActivityTime != null && lastActivityTime != 0
          && lastActivityTime + (config.stateIdleInterval * 1000) < System.currentTimeMillis()) {
        stateLastIdleTime = lastActivityTime;
      }

      if (System.currentTimeMillis() > stateLastIdleTime + (config.stateIdleInterval * 1000)) {
        fsm.fire("idle");
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

    if (config.pirOnFlash && isPeerStarted("pir") && isPirOn) {
      flash("pirOn");
    }

    if (config.batteryLevelCheck) {
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
    if (config.healthCheckFlash) {
      if (errors.size() > 0 && ledDisplayMap.containsKey("error")) {
        invoke("publishFlash", ledDisplayMap.get("error"));
      } else if (ledDisplayMap.containsKey("heartbeat")) {
        LedDisplayData heartbeat = ledDisplayMap.get("heartbeat");
        invoke("publishFlash", heartbeat);
      }
    }

  }

  public void onInactivity() {
    log.info("onInactivity");

    // powerDown ?

  }

  /**
   * Central hub of input motion control. Potentially, all input from
   * joysticks, quest2 controllers and headset, or any IK service could
   * be sent here
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
      }
    }
  }

  public void onMoveHead(Map<String, Double> map) {
    InMoov2Head head = (InMoov2Head) getPeer("head");
    if (head != null) {
      head.onMove(map);
    }
  }

  public void onMoveLeftArm(Map<String, Double> map) {
    InMoov2Arm leftArm = (InMoov2Arm) getPeer("leftArm");
    if (leftArm != null) {
      leftArm.onMove(map);
    }
  }

  public void onMoveLeftHand(Map<String, Double> map) {
    InMoov2Hand leftHand = (InMoov2Hand) getPeer("leftHand");
    if (leftHand != null) {
      leftHand.onMove(map);
    }
  }


  // public Message publishPython(String method, Object...data) {
  // return Message.createMessage(getName(), getName(), method, data);
  // }

  public void onMoveRightArm(Map<String, Double> map) {
    InMoov2Arm rightArm = (InMoov2Arm) getPeer("rightArm");
    if (rightArm != null) {
      rightArm.onMove(map);
    }
  }

  public void onMoveRightHand(Map<String, Double> map) {
    InMoov2Hand rightHand = (InMoov2Hand) getPeer("rightHand");
    if (rightHand != null) {
      rightHand.onMove(map);
    }
  }

  public void onMoveTorso(Map<String, Double> map) {
    InMoov2Torso torso = (InMoov2Torso) getPeer("torso");
    if (torso != null) {
      torso.onMove(map);
    }
  }


  
//  public Message publishPython(String method, Object...data) {
//    return Message.createMessage(getName(), getName(), method, data);
//  }

  public OpenCVData onOpenCVData(OpenCVData data) {
    // FIXME - publish event with or without data ? String file reference
    return data;
  }

  /**
   * onPeak volume callback TODO - maybe make it variable with volume ?
   * 
   * @param volume
   */
  public void onPirOn() {
    led.action = "flash";
    led.red = 50;
    led.green = 100;
    led.blue = 150;
    led.count = 5;
    led.timeOn = 500;
    led.timeOff = 10;
    // FIXME flash on config.flashOnBoot
    invoke("publishFlash");
    ProgramAB chatBot = (ProgramAB)getPeer("chatBot");
    if (chatBot != null) {
      String botState = chatBot.getPredicate("botState");
      if ("sleeping".equals(botState)) {
        invoke("publishEvent", "WAKE");
      }
    }
  }

  /**
   * Pir off callback
   */
  public void onPirOff() {
    isPirOn = false;
    fsm.fire("sleep");
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
      systemEvent("PIR ON");
    } else {
      systemEvent("PIR OFF");
    }
    return b;
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

      String peerKey = getPeerKey(name);
      if (peerKey != null) {
        peersStarted.add(peerKey);
      }

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

  /**
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
   * 
   * @param stateChange
   * @return
   */
  public FiniteStateMachine.StateChange onStateChange(FiniteStateMachine.StateChange stateChange) {
    try {
      log.error("onStateChange {}", stateChange);

      String current = stateChange.current;
      String last = stateChange.last;

      // leaving random state
      if ("random".equals(last) && !"random".equals(current) && isPeerStarted("random")) {
        Random random = (Random) getPeer("random");
        random.disable();
      }

      if ("wake".equals(last)) {
        invoke("publishStopAnimation");
      }

      if (config.systemEventStateChange) {
        systemEvent("ON STATE %s", current);
      }

      if (config.customSounds && customSoundMap.containsKey(current)) {
        invoke("publishPlayAudioFile", customSoundMap.get(current));
      }

      // TODO - only a few InMoov2 state defaults will be called here
      if (stateDefaults.contains(current)) {
        invoke(current);
      }

      // FIXME add topic changes to AIML here !
      // FIXME add clallbacks to inmmoov2 library

      // put configurable filter here !

      // state substitutions ?
      // let python subscribe directly to fsm.publishStateChange

      // if python && configured to do python inmoov2 library callbacks
      // do a callback ... default NOOPs should be in library

      // if
      // invoke(state);
      // depending on configuration ....
      // call python ?
      // fire fsm events ?
      // do defaults ?
    } catch (Exception e) {
      error(e);
    }
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

  public void powerDown() {
    // publishFlash(maxInactivityTimeSeconds, maxInactivityTimeSeconds,
    // maxInactivityTimeSeconds, maxInactivityTimeSeconds,
    // maxInactivityTimeSeconds, maxInactivityTimeSeconds)

    rest();
    purgeTasks(); // including heartbeat
    disable();

    if (chatBot != null) {
      chatBot.sleep();
    }

    if (ear != null) {
      // FIXME - bad remove it - what is needed ?
      // i think this is legacy wake word
      ear.lockOutAllGrammarExcept("power up");
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

  public double publishBatteryLevel(double d) {
    return d;
  }

  /** 
   * A heartbeat that continues to check status, and fire events to the FSM.
   * Checks battery, flashes leds and processes all the configured checks in
   * onHeartbeat at a regular interval
   * */
  public void publishHeartbeat() {
    log.info("publishHeartbeat");
  }

  public void publishBoot() {
    log.info("publishBoot");
  }

  public String publishConfigFinished(String configName) {
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

  public LedDisplayData publishFlash(int r, int g, int b, int count, long timeOn, long timeOff) {
    LedDisplayData data = new LedDisplayData();
    data.red = r;
    data.green = g;
    data.blue = b;
    data.count = count;
    data.timeOn = timeOn;
    data.timeOff = timeOff;
    return data;
  }

  public LedDisplayData publishFlash(LedDisplayData data) {
    return data;
  }

  /**
   * if inactivityTime configured, this event is published after there has not
   * been in activity since.
   */
  public void publishInactivity() {
    log.info("publishInactivity");
    fsm.fire("inactvity");
  }

  /**
   * used to configure a flashing event - could use configuration to signal
   * different colors and states
   * 
   * @return
   */
  public LedDisplayData publishFlash() {
    return led;
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

  public Map<String, Double> publishMoveHead(Map<String, Double> map) {
    return map;
  }

  public Map<String, Double> publishMoveLeftArm(Map<String, Double> map) {
    return map;
  }

  public Map<String, Double> publishMoveLeftHand(Map<String, Double> map) {
    return map;
  }

  public Map<String, Double> publishMoveRightArm(Map<String, Double> map) {
    return map;
  }

  public Map<String, Double> publishMoveRightHand(Map<String, Double> map) {
    return map;
  }

  public Map<String, Double> publishMoveTorso(Map<String, Double> map) {
    return map;
  }

  public String publishPlayAudioFile(String filename) {
    return filename;
  }

  public String publishPlayAnimation(String animation) {
    return animation;
  }

  /**
   * stop animation event
   */
  public void publishStopAnimation() {
  }

  public FiniteStateMachine.StateChange publishStateChange(FiniteStateMachine.StateChange state) {
    log.info("publishStateChange {}", state);
    return state;
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

  /**
   * default this will come from idle after some configurable time period
   */
  public void random() {
    Random random = (Random) getPeer("random");
    if (random != null) {
      random.enable();
    }
  }

  @Override
  public void releasePeer(String peerKey) {
    super.releasePeer(peerKey);
    if (peerKey != null) {
      systemEvent("STOPPED %s", peerKey);
    }
  }

  @Override
  public void releaseService() {
    try {
      disable();
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
    config.pirPlaySounds = b;
    return b;
  }

  public Object setPredicate(String key, Object data) {
    ProgramAB chatBot = (ProgramAB) getPeer("chatBot");
    if (chatBot != null) {
      if (data == null) {
        chatBot.setPredicate(key, null); // "unknown" "null" other sillyness ?
      } else {
        chatBot.setPredicate(key, data.toString());
      }
    } else {
      error("no chatBot available");
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

  // -----------------------------------------------------------------------------
  // These are methods added that were in InMoov1 that we no longer had in
  // InMoov2.
  // From original InMoov1 so we don't loose the

  public void setRightHandSpeed(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    setHandSpeed("right", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public String setSpeechType(String speechType) {
    updatePeerType("mouth" /* getPeerName("mouth") */, speechType);
    return speechType;
  }

  // -----------------------------------------------------------------------------
  // These are methods added that were in InMoov1 that we no longer had in
  // InMoov2.
  // From original InMoov1 so we don't loose the

  public void setTorsoSpeed(Double topStom, Double midStom, Double lowStom) {
    sendToPeer("torso", "setSpeed", topStom, midStom, lowStom);
  }

  public void setTorsoSpeed(Integer topStom, Integer midStom, Integer lowStom) {
    setTorsoSpeed((double) topStom, (double) midStom, (double) lowStom);
  }

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

  public void shutdown() {
    log.info("shutdown");
    Runtime.shutdown();
  }

  /**
   * ear still listening pir still active
   */
  public void sleep() {
    log.info("sleep");
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

  @Deprecated /* This needs to be removed ! */
  public ProgramAB startChatBot() {

    try {
      chatBot = (ProgramAB) startPeer("chatBot");

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
        // probably not necessary - state change events should be enough
        systemEvent("FIRST INIT");
      } else {
        chatBot.startSession(chatBot.getPredicate("default", "lastUsername"));
        // probably not necessary - state change events should be enough
        systemEvent("WAKE UP");
      }
    } catch (Exception e) {
      speak("could not load chatBot");
      error(e.getMessage());
      speak(e.getMessage());
    }
    broadcastState();
    return chatBot;
  }

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
    addTask(config.heartbeatInterval, "publishHeartbeat");
    config.heartbeat = true;
  }

  // TODO - general objective "might" be to reduce peers down to something
  // that does not need a reference - where type can be switched before creation
  // and the only thing needed is pubs/subs that are not handled in abstracts
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

  // FIXME - universal (good) way of handling all exceptions - ie - reporting
  // back to the user the problem in a short concise way but have
  // expandable detail in appropriate places
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
    fsm = (FiniteStateMachine) startPeer("fsm");
    fsm.init();

    Runtime runtime = Runtime.getInstance();

    // get service start and release life cycle events
    runtime.attachServiceLifeCycleListener(getName());

    List<ServiceInterface> services = Runtime.getServices();
    for (ServiceInterface si : services) {
      if ("Servo".equals(si.getSimpleName())) {
        send(si.getFullName(), "setAutoDisable", true);
      }
    }

    // get events of new services and shutdown
    subscribe("runtime", "shutdown");
    // power up loopback subscription
    addListener(getName(), "powerUp");

    subscribe("runtime", "publishConfigList");

    // subscribe to config processing events
    // runtime callbacks publish the same a local
    subscribe("runtime", "publishConfigStarted", "publishConfigStarted");
    subscribe("runtime", "publishConfigFinished", "publishConfigFinished");

    runtime.invoke("publishConfigList");

    // iterate through existing started service
    // add them to peers booted
    for (String name : Runtime.getServiceNames()) {
      String peerKey = getPeerKey(name);
      if (peerKey != null) {
        peersStarted.add(peerKey);
      }
    }

    if (runtime.isProcessingConfig()) {
      // if InMoov2 was started as part of a config set
      // set here so boot can be delayed until the config
      // set is done
      configStarted = true;
      bootedConfig = runtime.getConfigName();
    } else {
      invoke("publishBoot");
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
    Python p = (Python) Runtime.getService("python");
    p.stop();
  }

  public void stopHeartbeat() {
    purgeTask("publishHeartbeat");
    config.heartbeat = false;
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

  public void closeRightHand() {

    // if InMoov2Hand.close/open is used directly
    // it prevents user's interception of the data
    // and forces InMoov2Hand type to be used :(
    // pub/sub is the way

    // hardcoded, but if necessary can be put in config
    HashMap<String, Double> map = new HashMap<>();
    map.put("thumb", 130.0);
    map.put("index", 180.0);
    map.put("majeure", 180.0);
    map.put("ringFinger", 180.0);
    map.put("pinky", 180.0);
    invoke("publishMoveRightHand", map);

  }

  public void openRightHand() {
    // if InMoov2Hand.close/open is used directly
    // it prevents user's interception of the data
    // and forces InMoov2Hand type to be used :(
    // pub/sub is the way

    // hardcoded, but if necessary can be put in config
    HashMap<String, Double> map = new HashMap<>();
    map.put("thumb", 0.0);
    map.put("index", 0.0);
    map.put("majeure", 0.0);
    map.put("ringFinger", 0.0);
    map.put("pinky", 0.0);
    invoke("publishMoveRightHand", map);
  }
  
  public void closeLeftHand() {

    // if InMoov2Hand.close/open is used directly
    // it prevents user's interception of the data
    // and forces InMoov2Hand type to be used :(
    // pub/sub is the way

    // hardcoded, but if necessary can be put in config
    HashMap<String, Double> map = new HashMap<>();
    map.put("thumb", 130.0);
    map.put("index", 180.0);
    map.put("majeure", 180.0);
    map.put("ringFinger", 180.0);
    map.put("pinky", 180.0);
    invoke("publishMoveLeftHand", map);

  }

  public void openLeftHand() {
    // if InMoov2Hand.close/open is used directly
    // it prevents user's interception of the data
    // and forces InMoov2Hand type to be used :(
    // pub/sub is the way

    // hardcoded, but if necessary can be put in config
    HashMap<String, Double> map = new HashMap<>();
    map.put("thumb", 0.0);
    map.put("index", 0.0);
    map.put("majeure", 0.0);
    map.put("ringFinger", 0.0);
    map.put("pinky", 0.0);
    invoke("publishMoveLeftHand", map);
  }

  public void openHands() {
    openLeftHand();
    openRightHand();
  }

  public void closeHands() {
    closeLeftHand();
    closeRightHand();
  }
  
  public Event onEvent(Event event) {
    
    return event;
  }
  
  public void wake() {
    log.info("wake");
    // do waking things - based on config

    // blink

    // wake gesture
    // callback
    // imoov2[{name}]["onWake"](this)
    /**
     * <pre>
     i01.speakBlocking("I was sleeping")
     lookrightside()
     sleep(2)
     lookleftside()
     sleep(4)
     relax()
     ear.clearLock()
     sleep(2)
     i01.finishedGesture()
     * </pre>
     */

    /**
     * <pre>
     * // legacy
     * enable();
     * rest();
     * 
     * if (ear != null) {
     *   ear.clearLock();
     * }
     * 
     * // beginCheckingOnInactivity();
     * // BAD BAD BAD !!!
     * publishEvent("powerUp"); // before or after loopback
     * </pre>
     **/
    // was a relax gesture .. might want to ask about it ..

    // if ear start listening
    AbstractSpeechRecognizer<?> ear = (AbstractSpeechRecognizer) getPeer("ear");
    if (ear != null) {
      ear.startListening();
    }

    // attempt recognize where its at

    // attempt to recognize people

    // look for activity

    // say hello

    // start animation (configurable)

    rest();

    // should "session be determined by recognition?"
    ProgramAB chatBot = (ProgramAB) getPeer("chatBot");

    if (chatBot != null) {
      String firstinit = chatBot.getPredicate("firstinit");
      // wtf - "ok" really, for a boolean?
      if (!"ok".equals(firstinit)) {
        fsm.fire("firstInit");
      }
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.ERROR);
      // identical to command line start
      // Runtime.startConfig("inmoov2");
      Runtime.main(new String[] { "--log-level", "info", "-s", "webgui", "WebGui", "intro", "Intro", "python", "Python" });
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
