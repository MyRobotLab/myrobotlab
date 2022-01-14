package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.inmoov.Vision;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis.Voice;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.data.Locale;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.LocaleProvider;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.Simulator;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

public class InMoov2 extends Service implements TextListener, TextPublisher, JoystickListener, LocaleProvider, IKJointAngleListener {

  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);

  public static LinkedHashMap<String, String> lpVars = new LinkedHashMap<String, String>();

  /**
   * these should be methods like setRobotCanMoveBodyRandom(true) - which do
   * what they need and then set config NOT STATIC PUBLIC VARS
   */
  @Deprecated
  public static boolean RobotCanMoveBodyRandom = true;

  public static boolean isRobotCanMoveBodyRandom() {
    return RobotCanMoveBodyRandom;
  }

  public static void setRobotCanMoveBodyRandom(boolean robotCanMoveBodyRandom) {
    RobotCanMoveBodyRandom = robotCanMoveBodyRandom;
  }

  public static boolean isRobotCanMoveHeadRandom() {
    return RobotCanMoveHeadRandom;
  }

  public static void setRobotCanMoveHeadRandom(boolean robotCanMoveHeadRandom) {
    RobotCanMoveHeadRandom = robotCanMoveHeadRandom;
  }

  public static boolean isRobotCanMoveEyesRandom() {
    return RobotCanMoveEyesRandom;
  }

  public static void setRobotCanMoveEyesRandom(boolean robotCanMoveEyesRandom) {
    RobotCanMoveEyesRandom = robotCanMoveEyesRandom;
  }

  public static boolean isRobotCanMoveRandom() {
    return RobotCanMoveRandom;
  }

  public static void setRobotCanMoveRandom(boolean robotCanMoveRandom) {
    RobotCanMoveRandom = robotCanMoveRandom;
  }

  public static boolean isRobotIsSleeping() {
    return RobotIsSleeping;
  }

  public static void setRobotIsSleeping(boolean robotIsSleeping) {
    RobotIsSleeping = robotIsSleeping;
  }

  public static boolean isRobotIsStarted() {
    return RobotIsStarted;
  }

  public static void setRobotIsStarted(boolean robotIsStarted) {
    RobotIsStarted = robotIsStarted;
  }

  @Deprecated
  public static boolean RobotCanMoveHeadRandom = true;

  @Deprecated
  public static boolean RobotCanMoveEyesRandom = true;

  @Deprecated
  public static boolean RobotCanMoveRandom = true;

  @Deprecated
  public static boolean RobotIsSleeping = false;

  @Deprecated
  public static boolean RobotIsStarted = false;

  private static final long serialVersionUID = 1L;

  static String speechRecognizer = "WebkitSpeechRecognition";

  protected boolean loadGestures = true;

  InMoov2Config config = new InMoov2Config();

  /**
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
   * Part of service life cycle - a new servo has been started
   */
  public void onStarted(String fullname) {
    log.info("{} started", fullname);
    try {
      ServiceInterface si = Runtime.getService(fullname);
      if ("Servo".equals(si.getSimpleName())) {
        log.info("sending setAutoDisable true to {}", fullname);
        send(fullname, "setAutoDisable", true);
        // ServoControl sc = (ServoControl)Runtime.getService(name);
      }
    } catch (Exception e) {
      log.error("onStarted threw", e);
    }
  }

  public void startService() {
    super.startService();
    Runtime runtime = Runtime.getInstance();
    // FIXME - shouldn't need this anymore
    runtime.subscribeToLifeCycleEvents(getName());

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

      // copy (if they don't already exist) the chatbots which came with InMoov2
      resourceBotDir = FileIO.gluePaths(getResourceDir(), "chatbot/bots");
      files = FileIO.getFileList(resourceBotDir);
      for (File f : files) {
        String botDir = "data/ProgramAB/" + f.getName();
        if (new File(botDir).exists()) {
          log.info("found data/ProgramAB/{} not copying", botDir);
        } else {
          log.info("will copy new data/ProgramAB/{}", botDir);
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

    if (loadGestures) {
      loadGestures();
    }

    runtime.invoke("publishConfigList");
  }

  public void onCreated(String fullname) {
    log.info("{} created", fullname);
  }

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

  boolean autoStartBrowser = false;

  transient ProgramAB chatBot;

  String currentConfigurationName = "default";
  transient SpeechRecognizer ear;

  transient OpenCV opencv;

  transient Tracking eyesTracking;
  // waiting controable threaded gestures we warn user
  boolean gestureAlreadyStarted = false;
  // FIXME - what the hell is this for ?
  Set<String> gestures = new TreeSet<String>();
  transient InMoov2Head head;

  transient Tracking headTracking;

  transient HtmlFilter htmlFilter;

  transient UltrasonicSensor ultrasonicRight;

  transient UltrasonicSensor ultrasonicLeft;

  transient Pir pir;

  transient ImageDisplay imageDisplay;

  /**
   * simple booleans to determine peer state of existence FIXME - should be an
   * auto-peer variable
   * 
   * FIXME - sometime in the future there should just be a single simple
   * reference to a loaded "config" but at the moment the new UI depends on
   * these individual values :(
   * 
   */

  boolean isChatBotActivated = false;

  boolean isEarActivated = false;

  boolean isOpenCvActivated = false;

  boolean isEyeLidsActivated = false;

  boolean isHeadActivated = false;

  boolean isLeftArmActivated = false;

  boolean isLeftHandActivated = false;

  boolean isMouthActivated = false;

  // adding to the problem :( :( :(
  boolean isAudioPlayerActivated = true;

  boolean isRightArmActivated = false;

  boolean isRightHandActivated = false;

  boolean isSimulatorActivated = false;

  boolean isTorsoActivated = false;

  boolean isNeopixelActivated = false;

  boolean isPirActivated = false;

  boolean isUltrasonicRightActivated = false;

  boolean isUltrasonicLeftActivated = false;

  boolean isServoMixerActivated = false;

  boolean isController3Activated = false;

  // TODO - refactor into a Simulator interface when more simulators are borgd
  transient JMonkeyEngine simulator;

  String lastGestureExecuted;

  Long lastPirActivityTime;

  transient InMoov2Arm leftArm;

  transient InMoov2Hand leftHand;

  /**
   * supported locales
   */
  Map<String, Locale> locales = null;

  int maxInactivityTimeSeconds = 120;

  transient SpeechSynthesis mouth;

  // FIXME ugh - new MouthControl service that uses AudioFile output
  transient public MouthControl mouthControl;

  boolean mute = false;

  transient NeoPixel neopixel;

  transient ServoMixer servoMixer;

  transient Python python;

  transient InMoov2Arm rightArm;

  transient InMoov2Hand rightHand;

  transient InMoov2Torso torso;

  @Deprecated
  public Vision vision;

  // FIXME - remove all direct references
  // transient private HashMap<String, InMoov2Arm> arms = new HashMap<>();

  protected List<Voice> voices = null;

  protected String voiceSelected;

  transient WebGui webgui;

  protected List<String> configList;

  private boolean isController4Activated;

  private boolean isLeftHandSensorActivated;

  private boolean isLeftPortActivated;

  private boolean isOpenCVActivated;

  private boolean isRightHandSensorActivated;

  private boolean isRightPortActivated;

  public InMoov2(String n, String id) {
    super(n, id);

    // InMoov2 has a huge amount of peers
    setAutoStartPeers(false);

    // by default all servos will auto-disable
    // Servo.setAutoDisableDefault(true); //until peer servo services for
    // InMoov2 have the auto disable behavior, we should keep this

    // same as created in runtime - send asyc message to all
    // registered services, this service has started
    // find all servos - set them all to autoDisable(true)
    // onStarted(name) will handle all future created servos
    List<ServiceInterface> services = Runtime.getServices();
    for (ServiceInterface si : services) {
      if ("Servo".equals(si.getSimpleName())) {
        send(si.getFullName(), "setAutoDisable", true);
      }
    }

    // dynamically gotten from filesystem/bots ?
    locales = Locale.getLocaleMap("en-US", "fr-FR", "es-ES", "de-DE", "nl-NL", "ru-RU", "hi-IN", "it-IT", "fi-FI", "pt-PT", "tr-TR");
    locale = Runtime.getInstance().getLocale();

    // REALLY NEEDS TO BE CLEANED UP - no direct references
    // "publish" scripts which should be executed :(
    // python = (Python) startPeer("python");
    python = (Python) Runtime.start("python", "Python"); // this crud should
                                                         // stop
    // load(locale.getTag()); WTH ?

    // get events of new services and shutdown
    Runtime r = Runtime.getInstance();
    subscribe(r.getName(), "shutdown");
    subscribe(r.getName(), "publishConfigList");

    // FIXME - Framework should auto-magically auto-start peers AFTER
    // construction - unless explicitly told not to
    // peers to start on construction
    // imageDisplay = (ImageDisplay) startPeer("imageDisplay");
  }

  @Override /* local strong type - is to be avoided - use name string */
  public void addTextListener(TextListener service) {
    // CORRECT WAY ! - no direct reference - just use the name in a subscription
    addListener("publishText", service.getName());
  }

  @Override
  public void attachTextListener(TextListener service) {
    attachTextListener(service.getName());
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

  /**
   * "re"-publishing runtime config list, because I don't want to fix the js
   * subscribeTo :P
   * 
   * @return list of config names
   */
  public List<String> publishConfigList() {
    return configList;
  }

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
    // imageDisplay.closeAll();
    log.error("implement webgui.closeAllImages");
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
    if (head != null) {
      head.disable();
    }
    if (rightHand != null) {
      rightHand.disable();
    }
    if (leftHand != null) {
      leftHand.disable();
    }
    if (rightArm != null) {
      rightArm.disable();
    }
    if (leftArm != null) {
      leftArm.disable();
    }
    if (torso != null) {
      torso.disable();
    }
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
    if (head != null) {
      head.enable();
    }
    if (rightHand != null) {
      rightHand.enable();
    }
    if (leftHand != null) {
      leftHand.enable();
    }
    if (rightArm != null) {
      rightArm.enable();
    }
    if (leftArm != null) {
      leftArm.enable();
    }
    if (torso != null) {
      torso.enable();
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

    lastGestureExecuted = gesture;
    if (python == null) {
      log.warn("execGesture : No jython engine...");
      return null;
    }
    subscribe(python.getName(), "publishStatus", this.getName(), "onGestureStatus");
    startedGesture(lastGestureExecuted);
    return python.evalAndWait(gesture);
  }

  public void finishedGesture() {
    finishedGesture("unknown");
  }

  public void finishedGesture(String nameOfGesture) {
    if (gestureAlreadyStarted) {
      waitTargetPos();
      RobotCanMoveRandom = true;
      gestureAlreadyStarted = false;
      log.info("gesture : {} finished...", nameOfGesture);
    }
  }

  public void fullSpeed() {
    if (head != null) {
      head.fullSpeed();
    }
    if (rightHand != null) {
      rightHand.fullSpeed();
    }
    if (leftHand != null) {
      leftHand.fullSpeed();
    }
    if (rightArm != null) {
      rightArm.fullSpeed();
    }
    if (leftArm != null) {
      leftArm.fullSpeed();
    }
    if (torso != null) {
      torso.fullSpeed();
    }
  }

  public String get(String key) {
    String ret = localize(key);
    if (ret != null) {
      return ret;
    }
    return "not yet translated";
  }

  public InMoov2Arm getArm(String side) {
    if ("left".equals(side)) {
      return leftArm;
    } else if ("right".equals(side)) {
      return rightArm;
    } else {
      log.error("can not get arm {}", side);
    }
    return null;
  }

  public InMoov2Hand getHand(String side) {
    if ("left".equals(side)) {
      return leftHand;
    } else if ("right".equals(side)) {
      return rightHand;
    } else {
      log.error("can not get arm {}", side);
    }
    return null;
  }

  public InMoov2Head getHead() {
    return head;
  }

  /**
   * finds most recent activity
   * 
   * @return the timestamp of the last activity time.
   */
  public long getLastActivityTime() {

    long lastActivityTime = 0;

    if (leftHand != null) {
      lastActivityTime = Math.max(lastActivityTime, leftHand.getLastActivityTime());
    }

    if (leftArm != null) {
      lastActivityTime = Math.max(lastActivityTime, leftArm.getLastActivityTime());
    }

    if (rightHand != null) {
      lastActivityTime = Math.max(lastActivityTime, rightHand.getLastActivityTime());
    }

    if (rightArm != null) {
      lastActivityTime = Math.max(lastActivityTime, rightArm.getLastActivityTime());
    }

    if (head != null) {
      lastActivityTime = Math.max(lastActivityTime, head.getLastActivityTime());
    }

    if (torso != null) {
      lastActivityTime = Math.max(lastActivityTime, torso.getLastActivityTime());
    }

    if (lastPirActivityTime != null) {
      lastActivityTime = Math.max(lastActivityTime, lastPirActivityTime);
    }

    if (lastActivityTime == 0) {
      error("invalid activity time - anything connected?");
      lastActivityTime = System.currentTimeMillis();
    }

    return lastActivityTime;
  }

  public InMoov2Arm getLeftArm() {
    return leftArm;
  }

  public InMoov2Hand getLeftHand() {
    return leftHand;
  }

  @Override
  public Map<String, Locale> getLocales() {
    return locales;
  }

  public InMoov2Arm getRightArm() {
    return rightArm;
  }

  public InMoov2Hand getRightHand() {
    return rightHand;
  }

  public Simulator getSimulator() {
    return simulator;
  }

  public InMoov2Torso getTorso() {
    return torso;
  }

  public void halfSpeed() {
    if (head != null) {
      head.setSpeed(25.0, 25.0, 25.0, 25.0, 100.0, 25.0);
    }

    if (rightHand != null) {
      rightHand.setSpeed(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    }
    if (leftHand != null) {
      leftHand.setSpeed(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    }
    if (rightArm != null) {
      rightArm.setSpeed(25.0, 25.0, 25.0, 25.0);
    }
    if (leftArm != null) {
      leftArm.setSpeed(25.0, 25.0, 25.0, 25.0);
    }
    if (torso != null) {
      torso.setSpeed(20.0, 20.0, 20.0);
    }

  }

  public boolean isCameraOn() {
    if (opencv != null) {
      if (opencv.isCapturing()) {
        return true;
      }
    }
    return false;
  }

  public boolean isEyeLidsActivated() {
    return isEyeLidsActivated;
  }

  public boolean isHeadActivated() {
    return isHeadActivated;
  }

  public boolean isLeftArmActivated() {
    return isLeftArmActivated;
  }

  public boolean isLeftHandActivated() {
    return isLeftHandActivated;
  }

  public boolean isMute() {
    return mute;
  }

  public boolean isNeopixelActivated() {
    return isNeopixelActivated;
  }

  public boolean isRightArmActivated() {
    return isRightArmActivated;
  }

  public boolean isRightHandActivated() {
    return isRightHandActivated;
  }

  public boolean isTorsoActivated() {
    return isTorsoActivated;
  }

  public boolean isPirActivated() {
    return isPirActivated;
  }

  public boolean isUltrasonicRightActivated() {
    return isUltrasonicRightActivated;
  }

  public boolean isUltrasonicLeftActivated() {
    return isUltrasonicLeftActivated;
  }
  // by default all servos will auto-disable
  // TODO: KW : make peer servo services for InMoov2 have the auto disable
  // behavior.
  // Servo.setAutoDisableDefault(true);

  public boolean isServoMixerActivated() {
    return isServoMixerActivated;
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
    speakBlocking(get("STARTINGGESTURES"));

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
      speakAlert(get("GESTURE_ERROR"));
      return false;
    }
    return true;
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

  public void moveLeftArm(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    moveArm("left", bicep, rotate, shoulder, omoplate);
  }

  public void moveRightArm(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    moveArm("right", bicep, rotate, shoulder, omoplate);
  }

  public void moveArm(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    InMoov2Arm arm = getArm(which);
    if (arm == null) {
      info("%s arm not started", which);
      return;
    }
    arm.moveTo(bicep, rotate, shoulder, omoplate);
  }

  public void moveEyelids(Double eyelidleftPos, Double eyelidrightPos) {
    if (head != null) {
      head.moveEyelidsTo(eyelidleftPos, eyelidrightPos);
    } else {
      log.warn("moveEyelids - I have a null head");
    }
  }

  public void moveEyes(Double eyeX, Double eyeY) {
    if (head != null) {
      head.moveTo(null, null, eyeX, eyeY, null, null);
    } else {
      log.warn("moveEyes - I have a null head");
    }
  }

  public void moveRightHand(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    moveHand("right", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void moveRightHand(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    moveHand("right", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public void moveLeftHand(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    moveHand("left", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void moveLeftHand(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    moveHand("left", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public void moveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    InMoov2Hand hand = getHand(which);
    if (hand == null) {
      log.warn("{} hand does not exist", hand);
      return;
    }
    hand.moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void moveHead(Double neck, Double rothead) {
    moveHead(neck, rothead, null, null, null, null);
  }

  public void moveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw) {
    moveHead(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveHead(Double neck, Double rothead, Double rollNeck) {
    moveHead(rollNeck, rothead, null, null, null, rollNeck);
  }

  public void moveHead(Integer neck, Integer rothead, Integer rollNeck) {
    moveHead((double) rollNeck, (double) rothead, null, null, null, (double) rollNeck);
  }

  public void moveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    if (head != null) {
      head.moveTo(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } else {
      log.error("I have a null head");
    }
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
    if (head != null) {
      head.moveToBlocking(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } else {
      log.error("I have a null head");
    }
  }

  public void moveTorso(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.moveTo(topStom, midStom, lowStom);
    } else {
      log.error("moveTorso - I have a null torso");
    }
  }

  public void moveTorsoBlocking(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.moveToBlocking(topStom, midStom, lowStom);
    } else {
      log.error("moveTorsoBlocking - I have a null torso");
    }
  }

  public void onGestureStatus(Status status) {
    if (!status.equals(Status.success()) && !status.equals(Status.warn("Python process killed !"))) {
      error("I cannot execute %s, please check logs", lastGestureExecuted);
    }
    finishedGesture(lastGestureExecuted);
    unsubscribe(python.getName(), "publishStatus", this.getName(), "onGestureStatus");
  }

  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    // TODO Auto-generated method stub

  }

  public OpenCVData onOpenCVData(OpenCVData data) {
    return data;
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

    python.execMethod("power_down");
  }

  // TODO FIX/CHECK this, migrate from python land
  public void powerUp() {
    enable();
    rest();

    if (ear != null) {
      ear.clearLock();
    }

    beginCheckingOnInactivity();

    python.execMethod("power_up");
  }

  /**
   * all published text from InMoov2 - including ProgramAB
   */
  @Override
  public String publishText(String text) {
    return text;
  }

  public void releaseService() {
    try {
      disable();
      releasePeers();
      super.releaseService();
    } catch (Exception e) {
      error(e);
    }
  }

  // FIXME NO DIRECT REFERENCES - publishRest --> (onRest) --> rest
  public void rest() {
    log.info("InMoov2.rest()");
    if (head != null) {
      head.rest();
    }
    if (rightHand != null) {
      rightHand.rest();
    }
    if (leftHand != null) {
      leftHand.rest();
    }
    if (rightArm != null) {
      rightArm.rest();
    }
    if (leftArm != null) {
      leftArm.rest();
    }
    if (torso != null) {
      torso.rest();
    }
  }

  public void setLeftArmSpeed(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setArmSpeed("left", bicep, rotate, shoulder, omoplate);
  }

  public void setLeftArmSpeed(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
    setArmSpeed("left", (double) bicep, (double) rotate, (double) shoulder, (double) omoplate);
  }

  public void setRightArmSpeed(Double bicep, Double rotate, Double shoulder, Double omoplate) {
    setArmSpeed("right", bicep, rotate, shoulder, omoplate);
  }

  public void setRightArmSpeed(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
    setArmSpeed("right", (double) bicep, (double) rotate, (double) shoulder, (double) omoplate);
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
    if (head != null) {
      head.setAutoDisable(param);
    }
    if (rightArm != null) {
      rightArm.setAutoDisable(param);
    }
    if (leftArm != null) {
      leftArm.setAutoDisable(param);
    }
    if (leftHand != null) {
      leftHand.setAutoDisable(param);
    }
    if (rightHand != null) {
      leftHand.setAutoDisable(param);
    }
    if (torso != null) {
      torso.setAutoDisable(param);
    }
    /*
     * if (eyelids != null) { eyelids.setAutoDisable(param); }
     */
  }

  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void setLeftHandSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    setHandSpeed("left", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void setLeftHandSpeed(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    setHandSpeed("left", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
  }

  public void setRightHandSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    setHandSpeed("right", thumb, index, majeure, ringFinger, pinky, wrist);
  }

  public void setRightHandSpeed(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
    setHandSpeed("right", (double) thumb, (double) index, (double) majeure, (double) ringFinger, (double) pinky, (double) wrist);
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
    setHeadSpeed(rothead, neck, null, null, null);
  }

  public void setHeadSpeed(Double rothead, Double neck, Double rollNeck) {
    setHeadSpeed(rothead, neck, null, null, null, rollNeck);
  }

  public void setHeadSpeed(Integer rothead, Integer neck, Integer rollNeck) {
    setHeadSpeed((double) rothead, (double) neck, null, null, null, (double) rollNeck);
  }

  public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setHeadSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
  }

  public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (head == null) {
      warn("setHeadSpeed - head not started");
      return;
    }
    head.setSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
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
    if (neopixel != null) {
      neopixel.setAnimation(animation, red, green, blue, speed);
    } else {
      warn("No Neopixel attached");
    }
  }

  public String setSpeechType(String speechType) {
    serviceType.setPeer("mouth", speechType);
    broadcastState();
    return speechType;
  }

  public void setTorsoSpeed(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.setSpeed(topStom, midStom, lowStom);
    } else {
      log.warn("setTorsoSpeed - I have no torso");
    }
  }

  public void setTorsoSpeed(Integer topStom, Integer midStom, Integer lowStom) {
    setTorsoSpeed((double) topStom, (double) midStom, (double) lowStom);
  }

  @Deprecated
  public void setTorsoVelocity(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.setVelocity(topStom, midStom, lowStom);
    } else {
      log.warn("setTorsoVelocity - I have no torso");
    }
  }

  public boolean setAllVirtual(boolean virtual) {
    Runtime.setAllVirtual(virtual);
    speakBlocking(get("STARTINGVIRTUALHARD"));
    return virtual;
  }

  public void setVoice(String name) {
    if (mouth != null) {
      mouth.setVoice(name);
      voiceSelected = name;
      speakBlocking(String.format("%s %s", get("SETLANG"), name));
    }
  }

  public void speak(String toSpeak) {
    sendToPeer("mouth", "speak", toSpeak);
  }

  public void speakAlert(String toSpeak) {
    speakBlocking(get("ALERT"));
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

    if (!mute && isPeerStarted("mouth")) {
      // sendToPeer("mouth", "speakBlocking", toSpeak);
      invokePeer("mouth", "speakBlocking", toSpeak);
    }
  }

  public void startAll() throws Exception {
    startAll(null, null);
  }

  public void startAll(String leftPort, String rightPort) throws Exception {
    startMouth();
    startChatBot();

    // startHeadTracking();
    // startEyesTracking();
    // startOpenCV();
    startEar();

    startServos(leftPort, rightPort);
    // startMouthControl(head.jaw, mouth);

    speakBlocking(get("STARTINGSEQUENCE"));
  }

  /**
   * start servos - no controllers
   * 
   * @throws Exception
   *           boom
   */
  public void startServos() throws Exception {
    startServos(null, null);
  }

  public ProgramAB startChatBot() {

    try {
      chatBot = (ProgramAB) startPeer("chatBot");
      isChatBotActivated = true;
      if (locale != null) {
        chatBot.setCurrentBotName(locale.getTag());
      }

      speakBlocking(get("CHATBOTACTIVATED"));

      chatBot.attachTextPublisher(ear);

      // this.attach(chatBot); FIXME - attach as a TextPublisher - then
      // re-publish
      // FIXME - deal with language
      // speakBlocking(get("CHATBOTACTIVATED"));
      chatBot.repetitionCount(10);
      // chatBot.setPath(getResourceDir() + fs + "chatbot");
      chatBot.setPath(getDataDir() + fs + "chatbot");
      chatBot.startSession("default", locale.getTag());
      // reset some parameters to default...
      chatBot.setPredicate("topic", "default");
      chatBot.setPredicate("questionfirstinit", "");
      chatBot.setPredicate("tmpname", "");
      chatBot.setPredicate("null", "");
      // load last user session
      if (!chatBot.getPredicate("name").isEmpty()) {
        if (chatBot.getPredicate("lastUsername").isEmpty() || chatBot.getPredicate("lastUsername").equals("unknown")) {
          chatBot.setPredicate("lastUsername", chatBot.getPredicate("name"));
        }
      }
      chatBot.setPredicate("parameterHowDoYouDo", "");
      try {
        chatBot.savePredicates();
      } catch (IOException e) {
        log.error("saving predicates threw", e);
      }
      // start session based on last recognized person
      // if (!chatBot.getPredicate("default", "lastUsername").isEmpty() &&
      // !chatBot.getPredicate("default", "lastUsername").equals("unknown")) {
      // chatBot.startSession(chatBot.getPredicate("lastUsername"));
      // }
      if (!chatBot.getPredicate("Friend", "firstinit").isEmpty() && !chatBot.getPredicate("Friend", "firstinit").equals("unknown")
          && !chatBot.getPredicate("Friend", "firstinit").equals("started")) {
        chatBot.getResponse("FIRST_INIT");
      } else {
        chatBot.getResponse("WAKE_UP");
      }

      htmlFilter = (HtmlFilter) startPeer("htmlFilter");// Runtime.start("htmlFilter",
      // "HtmlFilter");
      chatBot.attachTextListener(htmlFilter);
      htmlFilter.attachTextListener((TextListener) getPeer("mouth"));
      chatBot.attachTextListener(this);
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
    isEarActivated = true;

    ear.attachSpeechSynthesis((SpeechSynthesis) getPeer("mouth"));
    ear.attachTextListener(chatBot);

    speakBlocking(get("STARTINGEAR"));
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
      RobotCanMoveRandom = false;
    }
  }

  // FIXME - universal (good) way of handling all exceptions - ie - reporting
  // back to the user the problem in a short concise way but have
  // expandable detail in appropriate places
  public OpenCV startOpenCV() throws Exception {
    speakBlocking(get("STARTINGOPENCV"));
    opencv = (OpenCV) startPeer("opencv");
    subscribeTo(opencv.getName(), "publishOpenCVData");
    isOpenCvActivated = true;
    return opencv;
  }

  public OpenCV getOpenCV() {
    return opencv;
  }

  public void setOpenCV(OpenCV opencv) {
    this.opencv = opencv;
  }

  public Tracking startEyesTracking() throws Exception {
    if (head == null) {
      startHead();
    }
    // TODO: pass the PID values for the eye tracking
    return startHeadTracking(head.eyeX, head.eyeY);
  }

  public Tracking startEyesTracking(ServoControl eyeX, ServoControl eyeY) throws Exception {
    if (opencv == null) {
      startOpenCV();
    }
    speakBlocking(get("TRACKINGSTARTED"));
    eyesTracking = (Tracking) this.startPeer("eyesTracking");
    eyesTracking.attach(opencv.getName());
    eyesTracking.attachPan(head.eyeX.getName());
    eyesTracking.attachTilt(head.eyeY.getName());
    return eyesTracking;
  }

  public InMoov2Head startHead() throws Exception {
    return startHead(null, null, null, null, null, null, null, null);
  }

  public InMoov2Head startHead(String port) throws Exception {
    return startHead(port, null, null, null, null, null, null, null);
  }

  // legacy inmoov head exposed pins
  public InMoov2Head startHead(String port, String type, Integer headYPin, Integer headXPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin) {

    speakBlocking(get("STARTINGHEAD"));

    head = (InMoov2Head) startPeer("head");
    isHeadActivated = true;

    if (headYPin != null) {
      head.setPins(headYPin, headXPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
    }

    // lame assumption - port is specified - it must be an Arduino :(
    if (port != null) {
      try {
        speakBlocking(port);
        Arduino arduino = (Arduino) startPeer("left");
        arduino.connect(port);

        arduino.attach(head.neck);
        arduino.attach(head.rothead);
        arduino.attach(head.eyeX);
        arduino.attach(head.eyeY);
        arduino.attach(head.jaw);
        // FIXME rollNeck and eyelids must be connected to right controller
        // arduino.attach(head.rollNeck);
        // arduino.attach(head.eyelidLeft);
        // arduino.attach(head.eyelidRight);

      } catch (Exception e) {
        error(e);
      }
    }

    speakBlocking(get("STARTINGMOUTHCONTROL"));
    mouthControl = (MouthControl) startPeer("mouthControl");
    mouthControl.attach(head.jaw);
    mouthControl.attach((Attachable) getPeer("mouth"));
    mouthControl.setmouth(10, 50);// <-- FIXME - not the right place for
    // config !!!

    return head;
  }

  public void startHeadTracking() throws Exception {
    if (opencv == null) {
      startOpenCV();
    }

    if (head == null) {
      startHead();
    }

    if (headTracking == null) {
      speakBlocking(get("TRACKINGSTARTED"));
      headTracking = (Tracking) this.startPeer("headTracking");
      
      headTracking.attach(opencv.getName());
      headTracking.attachPan(head.rothead.getName());
      headTracking.attachTilt(head.neck.getName());
      // TODO: where are the PID values?
    }
  }

  public Tracking startHeadTracking(ServoControl rothead, ServoControl neck) throws Exception {
    if (opencv == null) {
      startOpenCV();
    }

    if (headTracking == null) {
      speakBlocking(get("TRACKINGSTARTED"));
      headTracking = (Tracking) this.startPeer("headTracking");      
      
      headTracking.attach(opencv.getName());
      headTracking.attachPan(rothead.getName());
      headTracking.attachTilt(neck.getName());
      // TODO: where are the PID values?
    }
    return headTracking;
  }

  public InMoov2Arm startLeftArm() {
    return startLeftArm(null);
  }

  public InMoov2Arm startLeftArm(String port) {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    // speakBlocking(get("STARTINGHEAD") + " " + port);
    // ??? SHOULD THERE BE REFERENCES AT ALL ??? ... probably not

    speakBlocking(get("STARTINGLEFTARM"));
    leftArm = (InMoov2Arm) startPeer("leftArm");
    isLeftArmActivated = true;

    if (port != null) {
      try {
        speakBlocking(port);
        Arduino arduino = (Arduino) startPeer("left");
        arduino.connect(port);

        arduino.attach(leftArm.bicep);
        arduino.attach(leftArm.omoplate);
        arduino.attach(leftArm.rotate);
        arduino.attach(leftArm.shoulder);
      } catch (Exception e) {
        error(e);
      }
    }
    return leftArm;
  }

  public InMoov2Hand startLeftHand() {
    return startLeftHand(null);
  }

  public InMoov2Hand startLeftHand(String port) {

    speakBlocking(get("STARTINGLEFTHAND"));
    leftHand = (InMoov2Hand) startPeer("leftHand");
    isLeftHandActivated = true;

    if (port != null) {
      try {
        speakBlocking(port);
        Arduino arduino = (Arduino) startPeer("left");
        arduino.connect(port);

        arduino.attach(leftHand.thumb);
        arduino.attach(leftHand.index);
        arduino.attach(leftHand.majeure);
        arduino.attach(leftHand.ringFinger);
        arduino.attach(leftHand.pinky);
        arduino.attach(leftHand.wrist);

      } catch (Exception e) {
        error(e);
      }
    }

    return leftHand;
  }

  // TODO - general objective "might" be to reduce peers down to something
  // that does not need a reference - where type can be switched before creation
  // and the only thing needed is pubs/subs that are not handled in abstracts
  public SpeechSynthesis startMouth() {

    // FIXME - bad to have a reference, should only need the "name" of the
    // service !!!
    mouth = (SpeechSynthesis) startPeer("mouth");

    voices = mouth.getVoices();
    Voice voice = mouth.getVoice();
    if (voice != null) {
      voiceSelected = voice.getName();
    }

    isMouthActivated = true;

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

  public InMoov2Arm startRightArm() {
    return startRightArm(null);
  }

  public InMoov2Arm startRightArm(String port) {

    speakBlocking(get("STARTINGRIGHTARM"));

    rightArm = (InMoov2Arm) startPeer("rightArm");
    isRightArmActivated = true;

    if (port != null) {
      try {
        speakBlocking(port);
        Arduino arduino = (Arduino) startPeer("right");
        arduino.connect(port);

        arduino.attach(rightArm.bicep);
        arduino.attach(rightArm.omoplate);
        arduino.attach(rightArm.rotate);
        arduino.attach(rightArm.shoulder);
      } catch (Exception e) {
        error(e);
      }
    }

    return rightArm;
  }

  public InMoov2Hand startRightHand() {
    return startRightHand(null);
  }

  public InMoov2Hand startRightHand(String port) {

    speakBlocking(get("STARTINGRIGHTHAND"));
    rightHand = (InMoov2Hand) startPeer("rightHand");
    isRightHandActivated = true;

    if (port != null) {
      try {
        speakBlocking(port);
        Arduino arduino = (Arduino) startPeer("right");
        arduino.connect(port);

        arduino.attach(rightHand.thumb);
        arduino.attach(rightHand.index);
        arduino.attach(rightHand.majeure);
        arduino.attach(rightHand.ringFinger);
        arduino.attach(rightHand.pinky);
        arduino.attach(rightHand.wrist);

      } catch (Exception e) {
        error(e);
      }
    }
    return rightHand;
  }

  public Double getUltrasonicRightDistance() {
    if (ultrasonicRight != null) {
      return ultrasonicRight.range();
    } else {
      warn("No UltrasonicRight attached");
      return 0.0;
    }
  }

  public Double getUltrasonicLeftDistance() {
    if (ultrasonicLeft != null) {
      return ultrasonicLeft.range();
    } else {
      warn("No UltrasonicLeft attached");
      return 0.0;
    }
  }

  // public void publishPin(Pin pin) {
  // log.info("{} - {}", pin.pin, pin.value);
  // if (pin.value == 1) {
  // lastPIRActivityTime = System.currentTimeMillis();
  // }
  /// if its PIR & PIR is active & was sleeping - then wake up !
  // if (pin == pin.pin && startSleep != null && pin.value == 1) {
  // powerUp();
  // }
  // }

  public void startServos(String leftPort, String rightPort) throws Exception {
    startHead(leftPort);
    startLeftArm(leftPort);
    startLeftHand(leftPort);
    startRightArm(rightPort);
    startRightHand(rightPort);
    startTorso(leftPort);
  }

  // FIXME .. externalize in a json file included in InMoov2
  public Simulator startSimulator() throws Exception {

    speakBlocking(get("STARTINGVIRTUAL"));

    if (simulator != null) {
      log.info("start called twice - starting simulator is reentrant");
      return simulator;
    }

    simulator = (JMonkeyEngine) startPeer("simulator");

    // DEPRECATED - should just user peer info
    isSimulatorActivated = true;

    // adding InMoov2 asset path to the jmonkey simulator
    String assetPath = getResourceDir() + fs + JMonkeyEngine.class.getSimpleName();

    File check = new File(assetPath);
    log.info("loading assets from {}", assetPath);
    if (!check.exists()) {
      log.warn("%s does not exist");
    }

    // disable the frustrating servo events ...
    // Servo.eventsEnabledDefault(false);
    // jme.loadModels(assetPath); not needed - as InMoov2 unzips the model into
    // /resource/JMonkeyEngine/assets
    simulator.loadModels(assetPath);

    // ========== gael's calibrations begin ======================
    simulator.setRotation(getName() + ".head.jaw", "x");
    simulator.setRotation(getName() + ".head.neck", "x");
    simulator.setRotation(getName() + ".head.rothead", "y");
    simulator.setRotation(getName() + ".head.rollNeck", "z");
    simulator.setRotation(getName() + ".head.eyeY", "x");
    simulator.setRotation(getName() + ".head.eyeX", "y");
    //simulator.setRotation(getName() + ".head.eyelidLeft", "x");FIXME we need to modelize them in Blender
    //simulator.setRotation(getName() + ".head.eyelidRight", "x");FIXME we need to modelize them in Blender
    simulator.setRotation(getName() + ".torso.topStom", "z");
    simulator.setRotation(getName() + ".torso.midStom", "y");
    simulator.setRotation(getName() + ".torso.lowStom", "x");
    simulator.setRotation(getName() + ".rightArm.bicep", "x");
    simulator.setRotation(getName() + ".leftArm.bicep", "x");
    simulator.setRotation(getName() + ".rightArm.shoulder", "x");
    simulator.setRotation(getName() + ".leftArm.shoulder", "x");
    simulator.setRotation(getName() + ".rightArm.rotate", "y");
    simulator.setRotation(getName() + ".leftArm.rotate", "y");
    simulator.setRotation(getName() + ".rightArm.omoplate", "z");
    simulator.setRotation(getName() + ".leftArm.omoplate", "z");
    simulator.setRotation(getName() + ".rightHand.wrist", "y");
    simulator.setRotation(getName() + ".leftHand.wrist", "y");

    simulator.setMapper(getName() + ".head.jaw", 0, 180, -5, 80);
    simulator.setMapper(getName() + ".head.neck", 0, 180, 20, -20);
    simulator.setMapper(getName() + ".head.rollNeck", 0, 180, 30, -30);
    simulator.setMapper(getName() + ".head.eyeY", 0, 180, 40, 140);
    simulator.setMapper(getName() + ".head.eyeX", 0, 180, -10, 70); // HERE
                                                                    // there
                                                                    // need
    // to be
    // two eyeX (left and
    // right?)
    //simulator.setMapper(getName() + ".head.eyelidLeft", 0, 180, 40, 140);FIXME we need to modelize them in Blender
    //simulator.setMapper(getName() + ".head.eyelidRight", 0, 180, 40, 140);FIXME we need to modelize them in Blender
    simulator.setMapper(getName() + ".rightArm.bicep", 0, 180, 0, -150);
    simulator.setMapper(getName() + ".leftArm.bicep", 0, 180, 0, -150);

    simulator.setMapper(getName() + ".rightArm.shoulder", 0, 180, 30, -150);
    simulator.setMapper(getName() + ".leftArm.shoulder", 0, 180, 30, -150);
    simulator.setMapper(getName() + ".rightArm.rotate", 0, 180, 80, -80);
    simulator.setMapper(getName() + ".leftArm.rotate", 0, 180, -80, 80);
    simulator.setMapper(getName() + ".rightArm.omoplate", 0, 180, 10, -180);
    simulator.setMapper(getName() + ".leftArm.omoplate", 0, 180, -10, 180);

    simulator.setMapper(getName() + ".rightHand.wrist", 0, 180, -20, 60);
    simulator.setMapper(getName() + ".leftHand.wrist", 0, 180, 20, -60);

    simulator.setMapper(getName() + ".torso.topStom", 0, 180, -30, 30);
    simulator.setMapper(getName() + ".torso.midStom", 0, 180, 50, 130);
    simulator.setMapper(getName() + ".torso.lowStom", 0, 180, -30, 30);

    // ========== gael's calibrations end ======================

    // ========== 3 joint finger mapping and attaching begin ===

    // ========== Requires VinMoov5.j3o ========================

    simulator.multiMap(getName() + ".leftHand.thumb", getName() + ".leftHand.thumb1", getName() + ".leftHand.thumb2", getName() + ".leftHand.thumb3");
    simulator.setRotation(getName() + ".leftHand.thumb1", "y");
    simulator.setRotation(getName() + ".leftHand.thumb2", "x");
    simulator.setRotation(getName() + ".leftHand.thumb3", "x");

    simulator.multiMap(getName() + ".leftHand.index", getName() + ".leftHand.index", getName() + ".leftHand.index2", getName() + ".leftHand.index3");
    simulator.setRotation(getName() + ".leftHand.index", "x");
    simulator.setRotation(getName() + ".leftHand.index2", "x");
    simulator.setRotation(getName() + ".leftHand.index3", "x");

    simulator.multiMap(getName() + ".leftHand.majeure", getName() + ".leftHand.majeure", getName() + ".leftHand.majeure2", getName() + ".leftHand.majeure3");
    simulator.setRotation(getName() + ".leftHand.majeure", "x");
    simulator.setRotation(getName() + ".leftHand.majeure2", "x");
    simulator.setRotation(getName() + ".leftHand.majeure3", "x");

    simulator.multiMap(getName() + ".leftHand.ringFinger", getName() + ".leftHand.ringFinger", getName() + ".leftHand.ringFinger2", getName() + ".leftHand.ringFinger3");
    simulator.setRotation(getName() + ".leftHand.ringFinger", "x");
    simulator.setRotation(getName() + ".leftHand.ringFinger2", "x");
    simulator.setRotation(getName() + ".leftHand.ringFinger3", "x");

    simulator.multiMap(getName() + ".leftHand.pinky", getName() + ".leftHand.pinky", getName() + ".leftHand.pinky2", getName() + ".leftHand.pinky3");
    simulator.setRotation(getName() + ".leftHand.pinky", "x");
    simulator.setRotation(getName() + ".leftHand.pinky2", "x");
    simulator.setRotation(getName() + ".leftHand.pinky3", "x");

    // left hand mapping complexities of the fingers
    simulator.setMapper(getName() + ".leftHand.index", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.index2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.index3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.majeure", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.majeure2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.majeure3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.ringFinger", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.ringFinger2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.ringFinger3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.pinky", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.pinky2", 0, 180, -110, -179);
    simulator.setMapper(getName() + ".leftHand.pinky3", 0, 180, -110, -179);

    simulator.setMapper(getName() + ".leftHand.thumb1", 0, 180, -30, -100);
    simulator.setMapper(getName() + ".leftHand.thumb2", 0, 180, 80, 20);
    simulator.setMapper(getName() + ".leftHand.thumb3", 0, 180, 80, 20);

    // right hand

    simulator.multiMap(getName() + ".rightHand.thumb", getName() + ".rightHand.thumb1", getName() + ".rightHand.thumb2", getName() + ".rightHand.thumb3");
    simulator.setRotation(getName() + ".rightHand.thumb1", "y");
    simulator.setRotation(getName() + ".rightHand.thumb2", "x");
    simulator.setRotation(getName() + ".rightHand.thumb3", "x");

    simulator.multiMap(getName() + ".rightHand.index", getName() + ".rightHand.index", getName() + ".rightHand.index2", getName() + ".rightHand.index3");
    simulator.setRotation(getName() + ".rightHand.index", "x");
    simulator.setRotation(getName() + ".rightHand.index2", "x");
    simulator.setRotation(getName() + ".rightHand.index3", "x");

    simulator.multiMap(getName() + ".rightHand.majeure", getName() + ".rightHand.majeure", getName() + ".rightHand.majeure2", getName() + ".rightHand.majeure3");
    simulator.setRotation(getName() + ".rightHand.majeure", "x");
    simulator.setRotation(getName() + ".rightHand.majeure2", "x");
    simulator.setRotation(getName() + ".rightHand.majeure3", "x");

    simulator.multiMap(getName() + ".rightHand.ringFinger", getName() + ".rightHand.ringFinger", getName() + ".rightHand.ringFinger2", getName() + ".rightHand.ringFinger3");
    simulator.setRotation(getName() + ".rightHand.ringFinger", "x");
    simulator.setRotation(getName() + ".rightHand.ringFinger2", "x");
    simulator.setRotation(getName() + ".rightHand.ringFinger3", "x");

    simulator.multiMap(getName() + ".rightHand.pinky", getName() + ".rightHand.pinky", getName() + ".rightHand.pinky2", getName() + ".rightHand.pinky3");
    simulator.setRotation(getName() + ".rightHand.pinky", "x");
    simulator.setRotation(getName() + ".rightHand.pinky2", "x");
    simulator.setRotation(getName() + ".rightHand.pinky3", "x");

    simulator.setMapper(getName() + ".rightHand.index", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.index2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.index3", 0, 180, 70, -10);

    simulator.setMapper(getName() + ".rightHand.majeure", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.majeure2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.majeure3", 0, 180, 70, -10);

    simulator.setMapper(getName() + ".rightHand.ringFinger", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.ringFinger2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.ringFinger3", 0, 180, 70, -10);

    simulator.setMapper(getName() + ".rightHand.pinky", 0, 180, 65, -10);
    simulator.setMapper(getName() + ".rightHand.pinky2", 0, 180, 70, -10);
    simulator.setMapper(getName() + ".rightHand.pinky3", 0, 180, 60, -10);

    simulator.setMapper(getName() + ".rightHand.thumb1", 0, 180, 30, 110);
    simulator.setMapper(getName() + ".rightHand.thumb2", 0, 180, -100, -150);
    simulator.setMapper(getName() + ".rightHand.thumb3", 0, 180, -100, -160);

    // We set the correct location view
    simulator.cameraLookAt(getName() + ".torso.lowStom");

    // additional experimental mappings
    /*
     * simulator.attach(getName() + ".leftHand.pinky", getName() +
     * ".leftHand.index2"); simulator.attach(getName() + ".leftHand.thumb",
     * getName() + ".leftHand.index3"); simulator.setRotation(getName() +
     * ".leftHand.index2", "x"); simulator.setRotation(getName() +
     * ".leftHand.index3", "x"); simulator.setMapper(getName() +
     * ".leftHand.index", 0, 180, -90, -270); simulator.setMapper(getName() +
     * ".leftHand.index2", 0, 180, -90, -270); simulator.setMapper(getName() +
     * ".leftHand.index3", 0, 180, -90, -270);
     */
    return simulator;
  }

  public InMoov2Torso startTorso() {
    return startTorso(null);
  }

  public InMoov2Torso startTorso(String port) {
    if (torso == null) {
      speakBlocking(get("STARTINGTORSO"));
      isTorsoActivated = true;

      torso = (InMoov2Torso) startPeer("torso");

      if (port != null) {
        try {
          speakBlocking(port);
          Arduino left = (Arduino) startPeer("left");
          left.connect(port);
          left.attach(torso.lowStom);
          left.attach(torso.midStom);
          left.attach(torso.topStom);
        } catch (Exception e) {
          error(e);
        }
      }
    }
    return torso;
  }

  /**
   * called with only port - will default with defaulted pins
   * 
   * @param port
   *          port for the sensor
   * @return the ultrasonic sensor service
   */
  public UltrasonicSensor startUltrasonicRight(String port) {
    return startUltrasonicRight(port, 64, 63);
  }

  /**
   * called explicitly with pin values
   * 
   * @param port
   *          p
   * @param trigPin
   *          trigger pin
   * @param echoPin
   *          echo pin
   * @return the ultrasonic sensor
   * 
   */
  public UltrasonicSensor startUltrasonicRight(String port, int trigPin, int echoPin) {

    if (ultrasonicRight == null) {
      speakBlocking(get("STARTINGULTRASONICRIGHT"));
      isUltrasonicRightActivated = true;

      ultrasonicRight = (UltrasonicSensor) startPeer("ultrasonicRight");

      if (port != null) {
        try {
          speakBlocking(port);
          Arduino right = (Arduino) startPeer("right");
          right.connect(port);
          right.attach(ultrasonicRight, trigPin, echoPin);
        } catch (Exception e) {
          error(e);
        }
      }
    }
    return ultrasonicRight;
  }

  public UltrasonicSensor startUltrasonicLeft() {
    return startUltrasonicLeft(null, 64, 63);
  }

  public UltrasonicSensor startUltrasonicRight() {
    return startUltrasonicRight(null, 64, 63);
  }

  public UltrasonicSensor startUltrasonicLeft(String port) {
    return startUltrasonicLeft(port, 64, 63);
  }

  public UltrasonicSensor startUltrasonicLeft(String port, int trigPin, int echoPin) {

    if (ultrasonicLeft == null) {
      speakBlocking(get("STARTINGULTRASONICLEFT"));
      isUltrasonicLeftActivated = true;

      ultrasonicLeft = (UltrasonicSensor) startPeer("ultrasonicLeft");

      if (port != null) {
        try {
          speakBlocking(port);
          Arduino left = (Arduino) startPeer("left");
          left.connect(port);
          left.attach(ultrasonicLeft, trigPin, echoPin);
        } catch (Exception e) {
          error(e);
        }
      }
    }
    return ultrasonicLeft;
  }

  public Pir startPir(String port) {
    return startPir(port, 23);
  }

  public Pir startPir(String port, int pin) {

    if (pir == null) {
      speakBlocking(get("STARTINGPIR"));
      isPirActivated = true;

      pir = (Pir) startPeer("pir");

      if (port != null) {
        try {
          speakBlocking(port);
          Arduino right = (Arduino) startPeer("right");
          right.connect(port);
          right.attachPinListener(pir, pin);
        } catch (Exception e) {
          error(e);
        }
      }
    }
    return pir;
  }

  /**
   * config delegated startPir ... hopefully
   * 
   * @return
   */
  public Pir startPir() {

    if (pir == null) {
      speakBlocking(get("STARTINGPIR"));
      isPirActivated = true;

      pir = (Pir) startPeer("pir");
      try {
        pir.load(); // will this work ?
        // would be great if it did - offloading configuration
        // to the i01.pir.yml
      } catch (Exception e) {
        error(e);
      }
    }
    return pir;
  }

  public ServoMixer startServoMixer() {

    servoMixer = (ServoMixer) startPeer("servoMixer");
    isServoMixerActivated = true;

    speakBlocking(get("STARTINGSERVOMIXER"));
    broadcastState();
    return servoMixer;
  }

  public void stop() {
    if (head != null) {
      head.stop();
    }
    if (rightHand != null) {
      rightHand.stop();
    }
    if (leftHand != null) {
      leftHand.stop();
    }
    if (rightArm != null) {
      rightArm.stop();
    }
    if (leftArm != null) {
      leftArm.stop();
    }
    if (torso != null) {
      torso.stop();
    }
  }

  public void stopChatBot() {
    speakBlocking(get("STOPCHATBOT"));
    releasePeer("chatBot");
    isChatBotActivated = false;
  }

  public void stopHead() {
    speakBlocking(get("STOPHEAD"));
    releasePeer("head");
    releasePeer("mouthControl");
    isHeadActivated = false;
  }

  public void stopEar() {
    speakBlocking(get("STOPEAR"));
    releasePeer("ear");
    isEarActivated = false;
    broadcastState();
  }

  public void stopOpenCV() {
    speakBlocking(get("STOPOPENCV"));
    isOpenCvActivated = false;
    releasePeer("opencv");
  }

  public void stopGesture() {
    Python p = (Python) Runtime.getService("python");
    p.stop();
  }

  public void stopLeftArm() {
    speakBlocking(get("STOPLEFTARM"));
    releasePeer("leftArm");
    isLeftArmActivated = false;
  }

  public void stopLeftHand() {
    speakBlocking(get("STOPLEFTHAND"));
    releasePeer("leftHand");
    isLeftHandActivated = false;
  }

  public void stopMouth() {
    speakBlocking(get("STOPMOUTH"));
    releasePeer("mouth");
    // TODO - potentially you could set the field to null in releasePeer
    mouth = null;
    isMouthActivated = false;
  }

  public void stopRightArm() {
    speakBlocking(get("STOPRIGHTARM"));
    releasePeer("rightArm");
    isRightArmActivated = false;
  }

  public void stopRightHand() {
    speakBlocking(get("STOPRIGHTHAND"));
    releasePeer("rightHand");
    isRightHandActivated = false;
  }

  public void stopTorso() {
    speakBlocking(get("STOPTORSO"));
    releasePeer("torso");
    isTorsoActivated = false;
  }

  public void stopSimulator() {
    speakBlocking(get("STOPVIRTUAL"));
    releasePeer("simulator");
    simulator = null;
    isSimulatorActivated = false;
  }

  public void stopUltrasonicRight() {
    speakBlocking(get("STOPULTRASONIC"));
    releasePeer("ultrasonicRight");
    isUltrasonicRightActivated = false;
  }

  public void stopUltrasonicLeft() {
    speakBlocking(get("STOPULTRASONIC"));
    releasePeer("ultrasonicLeft");
    isUltrasonicLeftActivated = false;
  }

  public void stopPir() {
    speakBlocking(get("STOPPIR"));
    releasePeer("pir");
    isPirActivated = false;
  }

  public void stopNeopixelAnimation() {
    if (neopixel != null) {
      neopixel.clear();
    } else {
      warn("No Neopixel attached");
    }
  }

  public void stopServoMixer() {
    speakBlocking(get("STOPSERVOMIXER"));
    releasePeer("servoMixer");
    isServoMixerActivated = false;
  }

  public void waitTargetPos() {
    if (head != null) {
      head.waitTargetPos();
    }
    if (leftArm != null) {
      leftArm.waitTargetPos();
    }
    if (rightArm != null) {
      rightArm.waitTargetPos();
    }
    if (leftHand != null) {
      leftHand.waitTargetPos();
    }
    if (rightHand != null) {
      rightHand.waitTargetPos();
    }
    if (torso != null) {
      torso.waitTargetPos();
    }
  }

  public NeoPixel startNeopixel() {
    return startNeopixel(getName() + ".right", 2, 16);
  }

  public NeoPixel startNeopixel(String controllerName) {
    return startNeopixel(controllerName, 2, 16);
  }

  public NeoPixel startNeopixel(String controllerName, int pin, int numPixel) {

    if (neopixel == null) {
      try {
        neopixel = (NeoPixel) startPeer("neopixel");
        speakBlocking(get("STARTINGNEOPIXEL"));
        // FIXME - lame use peers
        isNeopixelActivated = true;
        neopixel.attach(Runtime.getService(controllerName));
      } catch (Exception e) {
        error(e);
      }
    }
    return neopixel;
  }

  @Override
  public void attachTextListener(String name) {
    addListener("publishText", name);
  }

  public Tracking getEyesTracking() {
    return eyesTracking;
  }

  public Tracking getHeadTracking() {
    return headTracking;
  }

  public void startBrain() {
    startChatBot();
  }

  public void startMouthControl() {
    speakBlocking(get("STARTINGMOUTHCONTROL"));
    mouthControl = (MouthControl) startPeer("mouthControl");
    mouthControl.attach(head.jaw);
    mouthControl.attach((Attachable) getPeer("mouth"));
  }

  @Deprecated /* wrong function name should be startPir */
  public void startPIR(String port, int pin) {
    startPir(port, pin);
  }

  // -----------------------------------------------------------------------------
  // These are methods added that were in InMoov1 that we no longer had in
  // InMoov2.
  // From original InMoov1 so we don't loose the

  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    log.info("onJointAngles {}", angleMap);
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
  public ServiceConfig getConfig() {
    InMoov2Config config = new InMoov2Config();

    // config.isController3Activated = isController3Activated;
    // config.isController4Activated = isController4Activated;
    // config.enableEyelids = isEyeLidsActivated;
    config.enableHead = isHeadActivated;
    config.enableLeftArm = isLeftArmActivated;
    config.enableLeftHand = isLeftHandActivated;
    config.enableLeftHandSensor = isLeftHandSensorActivated;
    // config.isLeftPortActivated = isLeftPortActivated;
    // config.enableNeoPixel = isNeopixelActivated;
    config.enableOpenCV = isOpenCVActivated;
    // config.enablePir = isPirActivated;
    config.enableUltrasonicRight = isUltrasonicRightActivated;
    config.enableUltrasonicLeft = isUltrasonicLeftActivated;
    config.enableRightArm = isRightArmActivated;
    config.enableRightHand = isRightHandActivated;
    config.enableRightHandSensors = isRightHandSensorActivated;
    // config.isRightPortActivated = isRightPortActivated;
    // config.enableSimulator = isSimulatorActivated;

    return config;
  }

  public void startAudioPlayer() {
    startPeer("audioPlayer");
  }

  public void stopAudioPlayer() {
    releasePeer("audioPlayer");
  }

  public ServiceConfig load(ServiceConfig c) {
    InMoov2Config config = (InMoov2Config) c;
    try {

      if (config.locale != null) {
        Runtime.setAllLocales(config.locale);
      }

      if (config.enableAudioPlayer) {
        startAudioPlayer();
      } else {
        stopAudioPlayer();
      }

      /**
       * <pre>
       * FIXME - 
       * - there simply should be a single reference to the entire config object in InMoov
       *   e.g. InMoov2Config config member
       * - if these booleans are symmetric - there should be corresponding functionality of "stopping/releasing" since they
       *   are currently starting
       * </pre>
       */

      /*
       * FIXME - very bad - need some coordination with this
       * 
       * if (config.isController3Activated) { startPeer("controller3"); // FIXME
       * ... this kills me :P exec("isController3Activated = True"); }
       * 
       * if (config.isController4Activated) { startPeer("controller4"); // FIXME
       * ... this kills me :P exec("isController4Activated = True"); }
       */
      /*
       * if (config.enableEyelids) { // the hell if I know ? }
       */

      if (config.enableHead) {
        startHead();
      } else {
        stopHead();
      }

      if (config.enableLeftArm) {
        startLeftArm();
      } else {
        stopLeftArm();
      }

      if (config.enableLeftHand) {
        startLeftHand();
      } else {
        stopLeftHand();
      }

      if (config.enableLeftHandSensor) {
        // the hell if I know ?
      }

      /*
       * if (config.isLeftPortActivated) { // the hell if I know ? is this an
       * Arduino ? startPeer("left"); } // else release peer ?
       * 
       * if (config.enableNeoPixel) { startNeopixel(); } else { //
       * stopNeopixelAnimation(); }
       */

      if (config.enableOpenCV) {
        startOpenCV();
      } else {
        stopOpenCV();
      }

      /*
       * if (config.enablePir) { startPir(); } else { stopPir(); }
       */

      if (config.enableRightArm) {
        startRightArm();
      } else {
        stopRightArm();
      }

      if (config.enableRightHand) {
        startRightHand();
      } else {
        stopRightHand();
      }

      if (config.enableRightHandSensors) {
        // the hell if I know ?
      }

      /*
       * if (config.isRightPortActivated) { // the hell if I know ? is this an
       * Arduino ? }
       */

      if (config.enableServoMixer) {
        startServoMixer();
      } else {
        stopServoMixer();
      }

      if (config.enableTorso) {
        startTorso();
      } else {
        stopTorso();
      }

      if (config.enableUltrasonicLeft) {
        startUltrasonicLeft();
      } else {
        stopUltrasonicLeft();  
      }

      if (config.enableUltrasonicRight) {
        startUltrasonicRight();
      } else {
        stopUltrasonicRight();  
      }

      if (config.loadGestures) {
        loadGestures = true;
        loadGestures();
        // will load in startService
      }

      if (config.enableSimulator) {
        startSimulator();
      }

    } catch (Exception e) {
      error(e);
    }

    return c;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      Platform.setVirtual(true);
      // Runtime.start("s01", "Servo");
      Runtime.start("intro", "Intro");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(false);
      webgui.startService();

      Random random = (Random) Runtime.start("random", "Random");

      InMoov2 i01 = (InMoov2) Runtime.start("i01", "InMoov2");

      random.addRandom(3000, 8000, "i01", "setLeftArmSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
      random.addRandom(3000, 8000, "i01", "setRightArmSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);

      random.addRandom(3000, 8000, "i01", "moveRightArm", 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);
      random.addRandom(3000, 8000, "i01", "moveLeftArm", 0.0, 5.0, 85.0, 95.0, 25.0, 30.0, 10.0, 15.0);

      random.addRandom(3000, 8000, "i01", "setLeftHandSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);
      random.addRandom(3000, 8000, "i01", "setRightHandSpeed", 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0, 8.0, 25.0);

      random.addRandom(3000, 8000, "i01", "moveRightHand", 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 130.0, 175.0);
      random.addRandom(3000, 8000, "i01", "moveLeftHand", 10.0, 160.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 10.0, 60.0, 5.0, 40.0);

      random.addRandom(200, 1000, "i01", "setHeadSpeed", 8.0, 20.0, 8.0, 20.0, 8.0, 20.0);
      random.addRandom(200, 1000, "i01", "moveHead", 70.0, 110.0, 65.0, 115.0, 70.0, 110.0);

      random.addRandom(200, 1000, "i01", "setTorsoSpeed", 2.0, 5.0, 2.0, 5.0, 2.0, 5.0);
      random.addRandom(200, 1000, "i01", "moveTorso", 85.0, 95.0, 88.0, 93.0, 70.0, 110.0);

      random.save();

      boolean done = true;
      if (done) {
        return;
      }

      // i01.setVirtual(false);
      // i01.displayFullScreen("https://upload.wikimedia.org/wikipedia/commons/8/87/InMoov_Wheel_1.jpg");
      // i01.getConfig();
      // i01.save();

      // Runtime.start("s02", "Servo");

      i01.startChatBot();

      i01.startAll("COM3", "COM4");
      Runtime.start("python", "Python");
      // Runtime.start("log", "Log");

      /*
       * OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");
       * cv.setCameraIndex(2);
       */
      // i01.startSimulator();
      /*
       * Arduino mega = (Arduino) Runtime.start("mega", "Arduino");
       * mega.connect("/dev/ttyACM0");
       */

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
