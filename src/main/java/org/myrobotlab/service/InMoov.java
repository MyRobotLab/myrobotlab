package org.myrobotlab.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.myrobotlab.document.Classification;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.inmoov.LanguagePack;
import org.myrobotlab.inmoov.Utils;
import org.myrobotlab.inmoov.Vision;
import org.myrobotlab.jme3.InMoov3DApp;
import org.myrobotlab.kinematics.DHLinkType;
import org.myrobotlab.kinematics.GravityCenter;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.openni.Skeleton;
import org.myrobotlab.service.data.AudioData;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.IKJointAngleListener;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.PinArrayControl;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

/**
 * InMoov - The InMoov Service.
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on his blog (http://inmoov.blogspot.com/ and
 * http://www.inmoov.fr/). InMoov is a composite of servos, Arduinos,
 * microphone, camera, kinect and computer. The InMoov service is composed of
 * many other peer services, and allows easy initialization and control of these
 * sub systems.
 *
 */

// FIXME - EVERYTHING .. ya EVERYTHING a local top level reference !
// TODO ALL PEERS NEED TO BE PRIVATE - ACCESS THROUGH GETTERS
// TODO ATTACH THINGS ...
// TODO implement generic bodypart to remove lot of things from here

public class InMoov extends Service implements IKJointAngleListener, JoystickListener {

  public static int attachPauseMs = 100;
  public static LinkedHashMap<String, String> languages = new LinkedHashMap<String, String>();

  public static List<String> languagesIndex = new ArrayList<String>();

  // ---------------------------------------------------------------
  // start variables declaration
  // TODO: inventory all of those vars..
  // ---------------------------------------------------------------

  transient public final static String LEFT = "left";
  public final static Logger log = LoggerFactory.getLogger(InMoov.class);
  transient public final static String RIGHT = "right";
  public static boolean RobotCanMoveBodyRandom = true;
  public static boolean RobotCanMoveEyesRandom = true;
  public static boolean RobotCanMoveHeadRandom = true;
  public static boolean RobotCanMoveRandom = true;
  public static boolean RobotIsSleeping = false;
  public static boolean RobotIsStarted = false;
  private static final long serialVersionUID = 1L;
  static String speechRecognizer = "WebkitSpeechRecognition";
  static String speechService = "MarySpeech";
  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoov.class.getCanonicalName());
    meta.addDescription("The InMoov service");
    meta.addCategory("robot");
    // meta.addDependency("inmoov.fr", "1.0.0");
    // meta.addDependency("org.myrobotlab.inmoov", "1.0.0");
    meta.addDependency("inmoov.fr", "inmoov", "1.1.22", "zip");
    meta.addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");

    // SHARING !!! - modified key / actual name begin -------
    meta.sharePeer("head.arduino", "left", "Arduino", "shared left arduino");
    meta.sharePeer("torso.arduino", "left", "Arduino", "shared left arduino");

    meta.sharePeer("leftArm.arduino", "left", "Arduino", "shared left arduino");
    meta.sharePeer("leftHand.arduino", "left", "Arduino", "shared left arduino");
    // eyelidsArduino peer for backward compatibility
    meta.sharePeer("eyelidsArduino", "right", "Arduino", "shared right arduino");
    meta.sharePeer("rightArm.arduino", "right", "Arduino", "shared right arduino");
    meta.sharePeer("rightHand.arduino", "right", "Arduino", "shared right arduino");

    meta.sharePeer("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    meta.sharePeer("eyesTracking.controller", "left", "Arduino", "shared head Arduino");
    meta.sharePeer("eyesTracking.x", "head.eyeX", "Servo", "shared servo");
    meta.sharePeer("eyesTracking.y", "head.eyeY", "Servo", "shared servo");
    meta.sharePeer("mouthControl.mouth", "mouth", speechService, "shared Speech");
    meta.sharePeer("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
    meta.sharePeer("headTracking.controller", "left", "Arduino", "shared head Arduino");
    meta.sharePeer("headTracking.x", "head.rothead", "Servo", "shared servo");
    meta.sharePeer("headTracking.y", "head.neck", "Servo", "shared servo");

    // SHARING !!! - modified key / actual name end ------

    // Global - undecorated by self name
    meta.addRootPeer("python", "Python", "shared Python service");

    // put peer definitions in
    meta.addPeer("torso", "InMoovTorso", "torso");
    meta.addPeer("eyelids", "InMoovEyelids", "eyelids");
    meta.addPeer("leftArm", "InMoovArm", "left arm");
    meta.addPeer("leftHand", "InMoovHand", "left hand");
    meta.addPeer("rightArm", "InMoovArm", "right arm");
    meta.addPeer("rightHand", "InMoovHand", "right hand");
    // webkit speech.
    meta.addPeer("ear", speechRecognizer, "InMoov webkit speech recognition service");
    // meta.addPeer("ear", "Sphinx", "InMoov Sphinx speech recognition
    // service");
    meta.addPeer("eyesTracking", "Tracking", "Tracking for the eyes");
    meta.addPeer("head", "InMoovHead", "the head");
    meta.addPeer("headTracking", "Tracking", "Head tracking system");
    meta.addPeer("mouth", speechService, "InMoov speech service");
    meta.addPeer("mouthControl", "MouthControl", "MouthControl");
    meta.addPeer("openni", "OpenNi", "Kinect service");
    meta.addPeer("pid", "Pid", "Pid service");

    // For VirtualInMoov
    meta.addPeer("jme", "JMonkeyEngine", "Virtual inmoov");
    meta.addPeer("ik3d", "InverseKinematics3D", "Virtual inmoov");

    // For IntegratedMovement
    meta.addPeer("integratedMovement", "IntegratedMovement", "Inverse kinematic type movement");
    return meta;
  }
  public static void main(String[] args) throws Exception {

    LoggingFactory.init(Level.INFO);

    boolean done = false;

    Runtime.main(new String[] { "--interactive", "--id", "admin" });
    Platform.setVirtual(true);

    // Runtime.start("gui", "SwingGui");
    
    Python python = (Python) Runtime.start("python", "Python");
    python.loadServiceScript("InMoov");
    WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
    webgui.setPort(8887);
    webgui.autoStartBrowser(false);
    webgui.startService();

    // webgui.startBrowser("http://localhost:8888/#/service/i01.ear");

    if (done) {
      return;
    }

 //   String script = FileIO.toString("../pyrobotlab/service/InMoov.py");
 //   python.exec(script);

    String leftPort = "COM3";
    String rightPort = "COM4";

    InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");
    i01.startAll(leftPort, rightPort);
    i01.setLanguage("en-US");
    i01.startMouth();
    i01.startEar();

    webgui.autoStartBrowser(false);
    webgui.startService();
    webgui.startBrowser("http://localhost:8888/#/service/i01.ear");
    HtmlFilter htmlFilter = (HtmlFilter) Runtime.start("htmlFilter", "HtmlFilter");
    i01.chatBot = (ProgramAB) Runtime.start("i01.chatBot", "ProgramAB");
    i01.chatBot.addTextListener(htmlFilter);
    htmlFilter.addListener("publishText", "i01", "speak");
    i01.chatBot.attach((Attachable) i01.ear);
    i01.startBrain();
    i01.startHead(leftPort);
    i01.startMouthControl(i01.head.jaw, i01.mouth);
    i01.loadGestures("InMoov/gestures");
    i01.startVinMoov();
    i01.startOpenCV();
    i01.execGesture("BREAKITdaVinci()");
  }
  transient HashMap<String, ServoController> arduinos = new HashMap<String, ServoController>();
  transient private HashMap<String, InMoovArm> arms = new HashMap<String, InMoovArm>();
  public String CALIBRATION_FILE = "calibration.py";
  transient public ProgramAB chatBot;
  boolean copyGesture = false;
  private Boolean debugVinmoov = true;
  transient public SpeechRecognizer ear;
  transient public InMoovEyelids eyelids;
  transient public Tracking eyesTracking;
  boolean firstSkeleton = true;
  // waiting controable threaded gestures we warn user
  boolean gestureAlreadyStarted = false;
  private final String GESTURES_DIRECTORY = "gestures";
  public Set<String> gesturesList = new TreeSet<String>();
  transient private HashMap<String, InMoovHand> hands = new HashMap<String, InMoovHand>();
  transient public InMoovHead head;
  transient public Tracking headTracking;
  transient private InverseKinematics3D ik3d;
  Point ikPoint = null;
  transient private IntegratedMovement integratedMovement;
  transient private JMonkeyEngine jme; // TODO - should probably be a Simulator
  // interface
  transient private Joystick joystick;
  String language;
  transient LanguagePack languagePack = new LanguagePack();
  private String lastGestureExecuted = "";
  String lastInMoovError = "";

  // ---------------------------------------------------------------
  // end variables
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // services reservations & related extra class for custom methods & configs
  // ---------------------------------------------------------------

  Long lastPIRActivityTime = null;
  transient public InMoovArm leftArm;
  transient public InMoovHand leftHand;
  transient public Relay LeftRelay1;
  int maxInactivityTimeSeconds = 120;
  transient public SpeechSynthesis mouth;
  transient public MouthControl mouthControl;
  boolean mute;
  transient public NeoPixel neopixel;
  transient public Arduino neopixelArduino;
  transient public OpenCV opencv;
  transient public OpenNi openni;
  public boolean openNiLeftShoulderInverted = true;
  public boolean openNiRightShoulderInverted = true;
  public double openNiShouldersOffset = -50.0;
  transient public Pid pid;
  private PinArrayControl pirArduino;
  public Integer pirPin = null;
  transient public Python python;
  transient public InMoovArm rightArm;
  transient public InMoovHand rightHand;
  transient public Relay RightRelay1;
  boolean saveSkeletonFrame = false;
  boolean speakErrors = false;
  Long startSleep = null;
  transient public InMoovTorso torso;
                                       transient public UltrasonicSensor ultrasonicSensor;

  // ---------------------------------------------------------------
  // end services reservations
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // attach engine, more here !! later ..
  // ---------------------------------------------------------------

  boolean useEyesForTracking = false;

  // ---------------------------------------------------------------
  // end attach
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // VISION public methods
  // ---------------------------------------------------------------

  boolean useHeadForTracking = true;

  transient InMoov3DApp vinMoovApp;

  public String VinmoovBackGroundColor = "Grey";

  public Boolean VinmoovFullScreen = false;

  public int VinmoovHeight = 600;

  public Boolean VinmoovMonitorActivated = false;

  public int VinmoovWidth = 800;

  public Vision vision;

  public InMoov(String n, String id) {
    super(n, id);
  }

  public void attach(Attachable attachable) {
    // opencv
    if (attachable instanceof OpenCV) {
      opencv = (OpenCV) attachable;
      subscribe(opencv.getName(), "publishClassification");
    } else if (attachable instanceof SpeechSynthesis) {
      mouth = (SpeechSynthesis) attachable;
      if (ear != null) {
        ear.addMouth(mouth);
      }
    } else if (attachable instanceof SpeechRecognizer) {
      ear = (SpeechRecognizer) attachable;
      if (mouth != null) {
        ear.addMouth(mouth);
      }
    } else if (attachable instanceof ProgramAB) {
      chatBot = (ProgramAB) attachable;
    }
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

  public void cameraOff() {
    if (opencv != null) {
      opencv.stopCapture();
      opencv.disableAll();
    }
    // temporary fix overexpand windows
    SwingGui gui = (SwingGui) Runtime.getService("gui");
    if (gui != null) {
      gui.maximize();
    }
  }

  public void cameraOn() {
    if (opencv == null) {
      startOpenCV();
    }
    opencv.capture();
    vision.enablePreFilters();
  }

  // ---------------------------------------------------------------
  // END VISION methods
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // OPENNI methods
  // ---------------------------------------------------------------

  public String captureGesture() {
    return captureGesture(null);
  }

  public String captureGesture(String gestureName) {
    StringBuffer script = new StringBuffer();
    Date date = new Date();

    String indentSpace = "";
    script.append("# - " + date + " - Captured gesture :\n");

    if (gestureName != null) {
      indentSpace = "  ";
      script.append(String.format("def %s():\n", gestureName));
    }

    if (head != null) {
      script.append(indentSpace);
      script.append(head.getScript(getName()));
    }

    if (leftArm != null) {
      script.append(indentSpace);
      script.append(leftArm.getScript(getName()));
    }
    if (rightArm != null) {
      script.append(indentSpace);
      script.append(rightArm.getScript(getName()));
    }

    if (leftHand != null) {
      script.append(indentSpace);
      script.append(leftHand.getScript(getName()));
    }
    if (rightHand != null) {
      script.append(indentSpace);
      script.append(rightHand.getScript(getName()));
    }

    if (torso != null) {
      script.append(indentSpace);
      script.append(torso.getScript(getName()));
    }

    if (eyelids != null) {
      script.append(indentSpace);
      script.append(eyelids.getScript(getName()));
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

  // ---------------------------------------------------------------
  // END OPENNI methods
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // START GESTURES related methods
  // ---------------------------------------------------------------

  public void clearTrackingPoints() {
    if (headTracking == null) {
      error("attach head before tracking");
    } else {
      headTracking.clearTrackingPoints();
    }
  }

  public boolean copyGesture(boolean b) throws Exception {
    log.info("copyGesture {}", b);
    if (b) {
      if (openni == null) {
        openni = startOpenNI();
      }
      openni.startUserTracking();
    } else {
      if (openni != null) {
        openni.stopCapture();
        firstSkeleton = true;
      }
    }

    copyGesture = b;
    return b;
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
    if (eyelids != null) {
      eyelids.disable();
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
    if (eyelids != null) {
      eyelids.enable();
    }
  }

  /**
   * This method will try to launch a python command with error handling
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
      head.setVelocity(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
    }
    if (rightHand != null) {
      rightHand.setVelocity(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
    }
    if (leftHand != null) {
      leftHand.setVelocity(-1.0, -1.0, -1.0, -1.0, -1.0, -1.0);
    }
    if (rightArm != null) {
      rightArm.setVelocity(-1.0, -1.0, -1.0, -1.0);
    }
    if (leftArm != null) {
      leftArm.setVelocity(-1.0, -1.0, -1.0, -1.0);
    }
    if (torso != null) {
      torso.setVelocity(-1.0, -1.0, -1.0);
    }
    if (eyelids != null) {
      eyelids.setVelocity(-1.0, -1.0);
    }
  }

  String getBoardType(String side, String type) {
    if (type != null) {
      return type;
    }

    if (RIGHT.equals(side)) {
      return Arduino.BOARD_TYPE_MEGA;
    }

    return Arduino.BOARD_TYPE_MEGA;
  }

  /**
   * get current language
   */
  public String getLanguage() {
    if (this.language == null) {
      // check if default locale supported by inmoov
      if (languages.containsKey(Locale.getDefault().toLanguageTag())) {
        this.language = Locale.getDefault().toLanguageTag();
      } else {
        this.language = "en-US";
      }
    }
    // to be sure runtime == inmoov language
    if (!Locale.getDefault().toLanguageTag().equals(this.language)) {
      setLanguage(this.language);
    }
    return this.language;
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
    if (eyelids != null) {
      lastActivityTime = Math.max(lastActivityTime, eyelids.getLastActivityTime());
    }

    if (lastPIRActivityTime != null) {
      lastActivityTime = Math.max(lastActivityTime, lastPIRActivityTime);
    }

    if (lastActivityTime == 0) {
      error("invalid activity time - anything connected?");
      lastActivityTime = System.currentTimeMillis();
    }

    return lastActivityTime;
  }

  /**
   * @return the mute startup state ( InMoov vocal startup actions )
   */
  public Boolean getMute() {
    return mute;
  }

  public Python getPython() {
    try {
      if (python == null) {
        python = (Python) startPeer("python");
      }

    } catch (Exception e) {
      error(e);
    }
    return python;
  }

  public JMonkeyEngine getSimulator() {
    if (jme == null) {
      jme = (JMonkeyEngine) startPeer("jme");
    }
    return jme;
  }

  public Double getUltrasonicSensorDistance() {
    if (ultrasonicSensor != null) {
      return ultrasonicSensor.range();
    } else {
      warn("No UltrasonicSensor attached");
      return 0.0;
    }
  }

  public void halfSpeed() {
    if (head != null) {
      head.setVelocity(25.0, 25.0, 25.0, 25.0, -1.0, 25.0);
    }

    if (rightHand != null) {
      rightHand.setVelocity(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    }
    if (leftHand != null) {
      leftHand.setVelocity(30.0, 30.0, 30.0, 30.0, 30.0, 30.0);
    }
    if (rightArm != null) {
      rightArm.setVelocity(25.0, 25.0, 25.0, 25.0);
    }
    if (leftArm != null) {
      leftArm.setVelocity(25.0, 25.0, 25.0, 25.0);
    }
    if (torso != null) {
      torso.setVelocity(20.0, 20.0, 20.0);
    }
    if (eyelids != null) {
      eyelids.setVelocity(30.0, 30.0);
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

  // ---------------------------------------------------------------
  // END GESTURES
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // SKELETON RELATED METHODS
  // ---------------------------------------------------------------

  public boolean isMute() {
    return mute;
  }

  public void loadCalibration() {
    loadCalibration(CALIBRATION_FILE);
  }

  public void loadCalibration(String calibrationFilename) {
    File calibF = new File(calibrationFilename);
    if (calibF.exists()) {
      log.info("Loading Calibration Python file {}", calibF.getAbsolutePath());
      Python p = (Python) Runtime.getService("python");
      try {
        p.execFile(calibF.getAbsolutePath());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        log.warn("Error loading calibratoin file {}", calibF.getAbsolutePath());
        e.printStackTrace();
      }
    }
  }

  public void loadGestures() {
    loadGestures(GESTURES_DIRECTORY);
  }

  /**
   * This blocking method will look at all of the .py files in a directory. One
   * by one it will load the files into the python interpreter. A gesture python
   * file should contain 1 method definition that is the same as the filename.
   * 
   * @param directory
   *          - the directory that contains the gesture python files.
   */
  public boolean loadGestures(String directory) {

    // iterate over each of the python files in the directory
    // and load them into the python interpreter.
    String extension = "py";
    Integer totalLoaded = 0;
    Integer totalError = 0;
    File dir = Utils.makeDirectory(directory);
    if (dir.exists()) {
      for (File f : dir.listFiles()) {
        if (FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase(extension)) {
          if (Utils.loadFile(f.getAbsolutePath()) == true) {
            totalLoaded += 1;
            // FIXME - what is the purpose of this ????? 
            gesturesList.add(f.getName()); 
          } else {
            error("file %s failed", f.getName());
            totalError += 1;
          }
        } else {
          log.warn("{} is not a {} file", f.getAbsolutePath(), extension);
        }
      }
    }
    info("%s Gestures loaded, %s Gestures with error", totalLoaded, totalError);
    if (totalError > 0) {
      speakAlert(languagePack.get("GESTURE_ERROR"));
      return false;
    }
    return true;
  }

  private String mapGestureNameToPythonMethod(String gestureName) {
    // TODO: some fancier mapping?
    String methodName = gestureName.replaceAll(" ", "");
    return methodName;
  }

  public void moveArm(String which, double bicep, double rotate, double shoulder, double omoplate) {
    if (!arms.containsKey(which)) {
      error("setArmSpeed %s does not exist", which);
    } else {
      arms.get(which).moveTo(bicep, rotate, shoulder, omoplate);
    }
  }

  public void moveEyelids(double eyelidleft, double eyelidright) {
    if (eyelids != null) {
      eyelids.moveTo(eyelidleft, eyelidright);
    } else {
      log.error("moveEyelids - I have a null Eyelids");
    }
  }

  public void moveEyes(double eyeX, double eyeY) {
    if (head != null) {
      head.moveTo(null, null, eyeX, eyeY, null, null);
    } else {
      log.error("moveEyes - I have a null head");
    }
  }

  public void moveHand(String which, double thumb, double index, double majeure, double ringFinger, double pinky) {
    moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void moveHand(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (!hands.containsKey(which)) {
      error("moveHand %s does not exist", which);
    } else {
      hands.get(which).moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
    }
  }

  public void moveHead(double neck, double rothead) {
    moveHead(neck, rothead, null);
  }

  // ---------- canned gestures end ---------

  public void moveHead(double neck, double rothead, double eyeX, double eyeY, double jaw) {
    moveHead(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveHead(Double neck, Double rothead, Double rollNeck) {
    moveHead(neck, rothead, null, null, null, rollNeck);
  }

  public void moveHead(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    if (head != null) {
      head.moveTo(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } else {
      log.error("I have a null head");
    }
  }

  public void moveHeadBlocking(double neck, double rothead) {
    moveHeadBlocking(neck, rothead, null);
  }

  public void moveHeadBlocking(double neck, double rothead, Double rollNeck) {
    moveHeadBlocking(neck, rothead, null, null, null, rollNeck);
  }

  public void moveHeadBlocking(double neck, double rothead, Double eyeX, Double eyeY, Double jaw) {
    moveHeadBlocking(neck, rothead, eyeX, eyeY, jaw, null);
  }

  public void moveHeadBlocking(Double neck, Double rothead, Double eyeX, Double eyeY, Double jaw, Double rollNeck) {
    if (head != null) {
      head.moveToBlocking(neck, rothead, eyeX, eyeY, jaw, rollNeck);
    } else {
      log.error("I have a null head");
    }
  }

  public void moveTorso(double topStom, double midStom, double lowStom) {
    if (torso != null) {
      torso.moveTo(topStom, midStom, lowStom);
    } else {
      log.error("moveTorso - I have a null torso");
    }
  }

  public void moveTorsoBlocking(double topStom, double midStom, double lowStom) {
    if (torso != null) {
      torso.moveToBlocking(topStom, midStom, lowStom);
    } else {
      log.error("moveTorsoBlocking - I have a null torso");
    }
  }

  
  public void onClassification(TreeMap<String, List<Classification>> classifications) {
    vision.yoloInventory(classifications);
  }

  public void onGestureStatus(Status status) {
    if (!status.equals(Status.success()) && !status.equals(Status.warn("Python process killed !"))) {
      error("I cannot execute %s, please check logs", lastGestureExecuted);
    }
    finishedGesture(lastGestureExecuted);
    unsubscribe(python.getName(), "publishStatus", this.getName(), "onGestureStatus");
  }

  public void onIKServoEvent(ServoData data) {
    if (vinMoovApp != null) {
      vinMoovApp.updatePosition(data);
    }
  }

  @Override
  public void onJointAngles(Map<String, Double> angleMap) {
    // TODO Auto-generated method stub
    log.info("onJointAngles {}", angleMap);

    // here we can make decisions on what ik sets we want to use and
    // what body parts are to move

    for (String name : angleMap.keySet()) {
      ServiceInterface si = Runtime.getService(name);
      if (si instanceof Servo) {
        ((Servo) si).moveTo(angleMap.get(name));
      }
    }

  }

  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    // TODO Auto-generated method stub

  }

  public void onListeningEvent() {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.setMicro(true);
    }
  }

  public void onOpenNIData(OpenNiData data) {

    if (data != null) {
      Skeleton skeleton = data.skeleton;

      if (firstSkeleton) {
        firstSkeleton = false;
      }

      if (copyGesture) {

        if (leftArm != null) {

          if (!Double.isNaN(skeleton.leftElbow.getAngleXY())) {
            if (skeleton.leftElbow.getAngleXY() >= 0) {
              leftArm.bicep.moveTo((double) skeleton.leftElbow.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.leftShoulder.getAngleXY())) {
            if (skeleton.leftShoulder.getAngleXY() >= 0) {
              leftArm.omoplate.moveTo((double) skeleton.leftShoulder.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.leftShoulder.getAngleYZ())) {
            if (skeleton.leftShoulder.getAngleYZ() + openNiShouldersOffset >= 0) {
              leftArm.shoulder.moveTo((double) skeleton.leftShoulder.getAngleYZ() - 50);
            }
          }
        }

        if (rightArm != null) {

          if (!Double.isNaN(skeleton.rightElbow.getAngleXY())) {
            if (skeleton.rightElbow.getAngleXY() >= 0) {
              rightArm.bicep.moveTo((double) skeleton.rightElbow.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.rightShoulder.getAngleXY())) {
            if (skeleton.rightShoulder.getAngleXY() >= 0) {
              rightArm.omoplate.moveTo((double) skeleton.rightShoulder.getAngleXY());
            }
          }
          if (!Double.isNaN(skeleton.rightShoulder.getAngleYZ())) {
            if (skeleton.rightShoulder.getAngleYZ() + openNiShouldersOffset >= 0) {
              rightArm.shoulder.moveTo((double) skeleton.rightShoulder.getAngleYZ() - 50);
            }
          }
        }

      }
    }

    // TODO - route data appropriately
    // rgb & depth image to OpenCV
    // servos & depth image to gui (entire InMoov + references to servos)
  }

  public void onPauseListening() {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.setMicro(false);
    }
  }

  // TODO FIX/CHECK this, migrate from python land
  public void powerDown() {

    rest();
    purgeTasks();
    disable();

    if (ear != null) {
      ear.lockOutAllGrammarExcept("power up");
    }

    startSleep = System.currentTimeMillis();
    python.execMethod("power_down");
  }

  // TODO FIX/CHECK this, migrate from python land
  public void powerUp() {
    startSleep = null;
    enable();
    rest();
    if (ear != null) {
      ear.clearLock();
    }

    beginCheckingOnInactivity();

    python.execMethod("power_up");
  }

  @Override
  public void preShutdown() {

    RobotCanMoveRandom = false;
    stopTracking();
    speakBlocking(languagePack.get("SHUTDOWN"));
    halfSpeed();
    rest();
    waitTargetPos();

    // if relay used, we switch on power
    if (LeftRelay1 != null) {
      LeftRelay1.on();
    }
    if (RightRelay1 != null) {
      RightRelay1.on();
    }

    if (eyelids != null) {
      eyelids.autoBlink(false);
      eyelids.moveToBlocking(180, 180);
    }

    stopVinMoov();
    if (neopixel != null && neopixelArduino != null) {
      neopixel.animationStop();
      sleep(500);
      neopixel.detach(neopixelArduino);
      sleep(100);
      neopixelArduino.serial.disconnect();
      neopixelArduino.serial.stopRecording();
      neopixelArduino.disconnect();
    }
    disable();
    if (LeftRelay1 != null) {
      LeftRelay1.off();
    }
    if (RightRelay1 != null) {
      RightRelay1.off();
    }
  }

  // TODO FIX/CHECK this, migrate from python land
  public void publishPin(Pin pin) {
    log.info("{} - {}", pin.pin, pin.value);
    if (pin.value == 1) {
      lastPIRActivityTime = System.currentTimeMillis();
    }
    // if its PIR & PIR is active & was sleeping - then wake up !
    if (pirPin == pin.pin && startSleep != null && pin.value == 1) {
      // attach(); // good morning / evening / night... asleep for % hours
      powerUp();
      // Calendar now = Calendar.getInstance();

      /*
       * FIXME - make a getSalutation String salutation = "hello "; if
       * (now.get(Calendar.HOUR_OF_DAY) < 12) { salutation = "good morning "; }
       * else if (now.get(Calendar.HOUR_OF_DAY) < 16) { salutation =
       * "good afternoon "; } else { salutation = "good evening "; }
       * 
       * 
       * speakBlocking(String.format("%s. i was sleeping but now i am awake" ,
       * salutation));
       */
    }
  }

  @Override
  public void purgeTasks() {
    speakBlocking("purging all tasks");
    super.purgeTasks();
  }

  public void rest() {
    log.info("InMoov Native Rest Gesture Called");
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
    if (eyelids != null) {
      eyelids.rest();
    }
  }

  @Deprecated
  public boolean RobotIsOpenCvCapturing() {
    if (opencv != null)
      return opencv.isCapturing();
    return false;
  }

  // TODO:change -> isOpenNiCapturing
  @Deprecated
  public boolean RobotIsOpenNiCapturing() {
    if (openni != null) {
      if (openni.capturing) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean save() {
    super.save();
    if (leftHand != null) {
      leftHand.save();
    }

    if (rightHand != null) {
      rightHand.save();
    }

    if (rightArm != null) {
      rightArm.save();
    }

    if (leftArm != null) {
      leftArm.save();
    }

    if (head != null) {
      head.save();
    }

    if (openni != null) {
      openni.save();
    }

    return true;
  }

  public void saveCalibration() {
    saveCalibration(CALIBRATION_FILE);
  }

  public void saveCalibration(String calibrationFilename) {

    File calibFile = new File(calibrationFilename);
    FileWriter calibrationWriter = null;
    try {
      calibrationWriter = new FileWriter(calibFile);
      calibrationWriter.write("##################################\n");
      calibrationWriter.write("# InMoov auto generated calibration \n");
      calibrationWriter.write("# " + new Date() + "\n");
      calibrationWriter.write("##################################\n");
      // String inMoovName = this.getName();
      // iterate all the services that are running.
      // we want all servos that are currently in the system?
      for (ServiceInterface service : Runtime.getServices()) {
        if (service instanceof ServoControl) {
          ServoControl s = (Servo) service;
          if (!s.getName().startsWith(this.getName())) {
            continue;
          }
          calibrationWriter.write("\n");
          // first detach the servo.
          calibrationWriter.write("# Servo Config : " + s.getName() + "\n");
          calibrationWriter.write(s.getName() + ".detach()\n");
          calibrationWriter.write(s.getName() + ".setMinMax(" + s.getMin() + "," + s.getMax() + ")\n");
          calibrationWriter.write(s.getName() + ".setVelocity(" + s.getSpeed() + ")\n");
          calibrationWriter.write(s.getName() + ".setRest(" + s.getRest() + ")\n");
          if (s.getPin() != null) {
            calibrationWriter.write(s.getName() + ".setPin(" + s.getPin() + ")\n");
          } else {
            calibrationWriter.write("# " + s.getName() + ".setPin(" + s.getPin() + ")\n");
          }
          
          Mapper mapper = s.getMapper();

          
          // save the servo map
          calibrationWriter.write(s.getName() + ".map(" + mapper.getMinX() + "," + mapper.getMaxX() + "," + mapper.getMinY() + "," + mapper.getMaxY() + ")\n");
          // if there's a controller reattach it at rest

          for (String controller : s.getControllers()) {
            calibrationWriter.write(s.getName() + ".attach(\"" + controller + "\"," + s.getPin() + "," + s.getRest() + ")\n");
          }

          if (s.getAutoDisable()) {
            calibrationWriter.write(s.getName() + ".setAutoDisable(True)\n");
          }
        }

      }
      calibrationWriter.write("\n");
      calibrationWriter.close();
    } catch (IOException e) {
      log.warn("Error writing calibration file {}", calibrationFilename);
      e.printStackTrace();
      return;
    }
  }

  public void saveGesture(String gestureName) {
    // TODO: allow a user to save a gesture to the gestures directory
    saveGesture(gestureName, GESTURES_DIRECTORY);
  }

  public void saveGesture(String gestureName, String directory) {
    // TODO: consider the gestures directory as a property on the inmoov
    String gestureMethod = mapGestureNameToPythonMethod(gestureName);
    String gestureFilename = directory + File.separator + gestureMethod + ".py";
    File gestureFile = new File(gestureFilename);
    if (gestureFile.exists()) {
      log.warn("Gesture file {} already exists.. not overwiting it.", gestureFilename);
      return;
    }
    FileWriter gestureWriter = null;
    try {
      gestureWriter = new FileWriter(gestureFile);
      // print the first line of the python file
      gestureWriter.write("def " + gestureMethod + "():\n");
      // now for each servo, we should write out the approperiate moveTo
      // statement
      // TODO: consider doing this only for the inmoov services.. but for now..
      // i think
      // we want all servos that are currently in the system?
      for (ServiceInterface service : Runtime.getServices()) {
        if (ServoControl.class.isAssignableFrom(service.getClass())) {
          double pos = ((ServoControl) service).getPos();
          gestureWriter.write("  " + service.getName() + ".moveTo(" + pos + ")\n");
        }
      }
      gestureWriter.write("\n");
      gestureWriter.close();
    } catch (IOException e) {
      log.warn("Error writing gestures file {}", gestureFilename);
      e.printStackTrace();
      return;
    }
    // TODO: consider writing out cooresponding AIML?
  }

  public void savePose(String poseName) {
    // TODO: consider a prefix for the pose name?
    captureGesture(poseName);
  }

  public void setArmSpeed(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    if (!arms.containsKey(which)) {
      error("setArmSpeed %s does not exist", which);
    } else {
      arms.get(which).setSpeed(bicep, rotate, shoulder, omoplate);
    }
  }

  @Deprecated
  public void setArmVelocity(String which, Double bicep, Double rotate, Double shoulder, Double omoplate) {
    if (!arms.containsKey(which)) {
      error("setArmVelocity %s does not exist", which);
    } else {
      arms.get(which).setVelocity(bicep, rotate, shoulder, omoplate);
    }
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
    if (eyelids != null) {
      eyelids.setAutoDisable(param);
    }
  }

  public void setBatteryLevel(Integer level) {
    if (vinMoovApp != null && VinmoovMonitorActivated && RobotIsStarted) {
      vinMoovApp.setBatteryLevel(level);

    }
  }

  public void setEyelidsVelocity(Double eyelidleft, Double eyelidright) {
    if (eyelids != null) {
      eyelids.setVelocity(eyelidleft, eyelidright);
    } else {
      log.warn("setEyelidsVelocity - I have no eyelids");
    }
  }

  @Deprecated
  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  @Deprecated
  public void setHandSpeed(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (!hands.containsKey(which)) {
      error("setHandSpeed %s does not exist", which);
    } else {
      hands.get(which).setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
    }
  }

  public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky) {
    setHandVelocity(which, thumb, index, majeure, ringFinger, pinky, null);
  }

  public void setHandVelocity(String which, Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
    if (!hands.containsKey(which)) {
      error("setHandSpeed %s does not exist", which);
    } else {
      hands.get(which).setVelocity(thumb, index, majeure, ringFinger, pinky, wrist);
    }
  }

  // ---------------------------------------------------------------
  // GENERAL METHODS / TO SORT
  // ---------------------------------------------------------------

  public void setHead(InMoovHead head) {
    this.head = head;
  }

  @Deprecated
  public void setHeadSpeed(Double rothead, Double neck) {
    setHeadSpeed(rothead, neck, null, null, null);
  }

  @Deprecated
  public void setHeadSpeed(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    if (head != null) {
      head.setSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed);
    } else {
      log.warn("setHeadSpeed - I have no head");
    }
  }

  public void setHeadVelocity(Double rothead, Double neck) {
    setHeadVelocity(rothead, neck, null, null, null, null);
  }

  public void setHeadVelocity(Double rothead, Double neck, Double rollNeck) {
    setHeadVelocity(rothead, neck, null, null, null, rollNeck);
  }

  public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed) {
    setHeadVelocity(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, null);
  }

  @Deprecated
  public void setHeadVelocity(Double rothead, Double neck, Double eyeXSpeed, Double eyeYSpeed, Double jawSpeed, Double rollNeckSpeed) {
    if (head != null) {
      head.setVelocity(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed, rollNeckSpeed);
    } else {
      log.warn("setHeadVelocity - I have no head");
    }
  }

  // ======== possibly beginning of ik interface =================
  public void setIkPoint(double x, double y, double z) {
    if (ikPoint == null) {
      ikPoint = new Point(x, y, z);
      jme.addBox("ikPoint", x, y, z, "cc0000", true);
    }
    // move target marker
    jme.moveTo("ikPoint", x, y, z);

    if (ik3d == null) {
      ik3d = (InverseKinematics3D) startPeer("ik3d");
      ik3d.setCurrentArm("i01.leftArm", InMoovArm.getDHRobotArm("i01", "left"));
      ik3d.attach(this);
    }
    // move arm to target
    ik3d.moveTo("i01.leftArm", ikPoint);
  }

  /**
   * TODO : use system locale set language for InMoov service used by chatbot +
   * ear + mouth
   * 
   * @param l
   *          - format : java Locale
   */
  public boolean setLanguage(String l) {
    if (languages.containsKey(l)) {
      this.language = l;
      info("Set language to %s", languages.get(l));
      Runtime runtime = Runtime.getInstance();
      runtime.setLocale(l);
      languagePack.load(language);
      return true;
      // this.broadcastState();
    } else {
      error("InMoov not yet support {}", l);
      return false;
    }
  }

  // show some infos to jme screen
  public void setLeftArduinoConnected(boolean param) {
    vinMoovApp.setLeftArduinoConnected(param);
  }

  public void setMute(boolean mute) {
    info("Set mute to %s", mute);
    this.mute = mute;
  }

  public void setNeopixelAnimation(String animation, Integer red, Integer green, Integer blue, Integer speed) {
    if (neopixel != null && neopixelArduino != null) {
      neopixel.setAnimation(animation, red, green, blue, speed);
    } else {
      warn("No Neopixel attached");
    }
  }

  public void setRightArduinoConnected(boolean param) {
    vinMoovApp.setRightArduinoConnected(param);
  }

  @Deprecated
  public void setTorsoSpeed(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.setSpeed(topStom, midStom, lowStom);
    } else {
      log.warn("setTorsoSpeed - I have no torso");
    }
  }

  public void setTorsoVelocity(Double topStom, Double midStom, Double lowStom) {
    if (torso != null) {
      torso.setVelocity(topStom, midStom, lowStom);
    } else {
      log.warn("setTorsoVelocity - I have no torso");
    }
  }

  public List<AudioData> speak(String toSpeak) {
    if (mouth == null) {
      log.error("Speak is called, but my mouth is NULL...");
      return null;
    }
    if (!mute) {
      try {
        return mouth.speak(toSpeak);
      } catch (Exception e) {
        Logging.logError(e);
      }
    }
    return null;
  }

  public List<AudioData> speakAlert(String toSpeak) {
    speakBlocking(languagePack.get("ALERT"));
    return speakBlocking(toSpeak);
  }

  public List<AudioData> speakBlocking(String toSpeak) {
    if (mouth == null) {
      mouth = (SpeechSynthesis) startPeer("mouth");
    }

    if (mouth == null) {
      log.error("speakBlocking is called, but my mouth is NULL...");
      return null;
    }
    if (!mute) {
      try {
        return mouth.speakBlocking(toSpeak);
      } catch (Exception e) {
        log.error("speakBlocking threw", e);
      }
    }
    return null;
  }

  public boolean speakErrors(boolean b) {
    speakErrors = b;
    return b;
  }

  /*************
   * STARTS BEGIN
   * 
   * @param leftPort
   *          com port
   * @param rightPort
   *          com port
   * @throws Exception
   *           e
   * 
   ************************/

  // FIXME , later ... attach things !
  public void startAll(String leftPort, String rightPort) throws Exception {
    startMouth();
    startHead(leftPort);
    startOpenCV();
    startEar();
    startMouthControl(head.jaw, mouth);
    startLeftHand(leftPort);
    startRightHand(rightPort);
    // startEyelids(rightPort);
    startLeftArm(leftPort);
    startRightArm(rightPort);
    startTorso(leftPort);
    startHeadTracking();
    startEyesTracking();
    // TODO LP
    speakBlocking("startup sequence completed");
  }

  // gestures begin ---------------

  public InMoovArm startArm(String side, ServoController controller) throws Exception {
    // speakBlocking(languagePack.get("STARTINGLEFTARM"));
    InMoovArm arm = (InMoovArm) startPeer(String.format("%sArm", side));
    arm.setController(controller);
    arms.put(side, arm);
    arm.setSide(side);// FIXME WHO USES SIDE - THIS SHOULD BE NAME !!!
    if ("left".equals(side)) {
      leftArm = arm;
    } else if ("right".equals(side)) {
      rightArm = arm;
    }
    return arm;
  }

  public InMoovArm startArm(String side, String port, String type) throws Exception {
    // TODO rework this...
    if (type == "left") {
      speakBlocking(languagePack.get("STARTINGLEFTARM") + " " + port);
    } else {
      speakBlocking(languagePack.get("STARTINGRIGHTARM") + " " + port);
    }

    InMoovArm arm = (InMoovArm) startPeer(String.format("%sArm", side));
    arms.put(side, arm);
    arm.setSide(side);// FIXME WHO USES SIDE - THIS SHOULD BE NAME !!!
    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    // arm.arduino.setBoard(type); FIXME - this is wrong setting to Mega ...
    // what if its a USB or I2C ???
    arm.connect(port); // FIXME are all ServoControllers "connectable" ?
    arduinos.put(port, arm.controller);

    return arm;
  }

  /**
   * Start InMoov brain engine And extra stuffs, like "what is you name" ( TODO
   * finish migration )
   * 
   * @return started ProgramAB service
   * @throws IOException 
   */
  public ProgramAB startBrain() throws IOException {
    if (chatBot == null) {
      chatBot = (ProgramAB) Runtime.start(this.getIntanceName() + ".brain", "ProgramAB");
    }
    this.attach(chatBot);
    speakBlocking(languagePack.get("CHATBOTACTIVATED"));
    chatBot.repetitionCount(10);
    chatBot.setPath("InMoov/chatBot");
    chatBot.startSession("default", getLanguage());
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
    if (!chatBot.getPredicate("default", "lastUsername").isEmpty() && !chatBot.getPredicate("default", "lastUsername").equals("unknown")) {
      chatBot.startSession(chatBot.getPredicate("lastUsername"));
    }
    return chatBot;
  }

  // TODO TODO TODO - context & status report -
  // "current context is right hand"
  // FIXME - voice control for all levels (ie just a hand or head !!!!)
  public SpeechRecognizer startEar() {

    if (ear == null) {
      ear = (SpeechRecognizer) startPeer("ear");
    }
    this.attach((Attachable) ear);
    speakBlocking(languagePack.get("STARTINGEAR"));

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

  /*
   * New startEyelids attach method ( for testing );
   */
  public InMoovEyelids startEyelids(ServoController controller, Integer eyeLidLeftPin, Integer eyeLidRightPin) throws Exception {
    eyelids = (InMoovEyelids) startPeer("eyelids");
    eyelids.attach(controller, eyeLidLeftPin, eyeLidRightPin);
    return eyelids;
  }
  // ---------------------------------------------------------------
  // END SKELETON RELATED METHODS
  // ---------------------------------------------------------------

  public void startEyesTracking() {
    startEyesTracking(head.eyeX, head.eyeY);
  }

  public void startEyesTracking(ServoControl eyeX, ServoControl eyeY) {
    if (opencv == null) {
      log.warn("Tracking needs Opencv activated, I will try to lauch it. It is better if you DIY");
      startOpenCV();
    }
    speakBlocking(languagePack.get("TRACKINGSTARTED"));
    eyesTracking = (Tracking) this.startPeer("eyesTracking");
    eyesTracking.connect(this.opencv, head.eyeX, head.eyeY);
  }

  @Deprecated
  public void startEyesTracking(String port, Integer eyeX, Integer eyeY) {
    log.warn("Please use ServoControl : startEyesTracking(ServoX,ServoY), I will try to do it for you...");
    startEyesTracking();
  }

  public InMoovHand startHand(String side, ServoController sc) {
    InMoovHand hand = (InMoovHand) startPeer(String.format("%sHand", side));
    hand.setSide(side);
    hands.put(side, hand);
    hand.setController(sc);
    if ("left".equals(side)) {
      leftHand = hand;
    } else if ("right".equals(side)) {
      rightHand = hand;
    }
    return hand;
  }

  public InMoovHand startHand(String side, String port, String type) throws Exception {
    // TODO rework this...
    if (type == "left") {
      speakBlocking(languagePack.get("STARTINGLEFTHAND") + " " + port);
    } else {
      speakBlocking(languagePack.get("STARTINGRIGHTHAND") + " " + port);
    }

    InMoovHand hand = (InMoovHand) startPeer(String.format("%sHand", side));
    hand.setSide(side);
    hands.put(side, hand);
    // FIXME - this is wrong ! its configuratin of an Arduino, (we may not have
    // an Arduino !!!)
    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    // hand.arduino.setBoard(type);
    hand.connect(port);
    arduinos.put(port, hand.controller);
    return hand;
  }

  public InMoovHead startHead(ServoController controller) {
    speakBlocking(languagePack.get("STARTINGHEAD"));
    head = (InMoovHead) startPeer("head");
    head.setController(controller);
    // arduinos.put(port, head.controller); // FIXME - silly used by PIR -
    // refactor out ..
    return head;
  }

  public InMoovHead startHead(String port) throws Exception {
    return startHead(port, null, 12, 13, 22, 24, 26, 30);
  }

  public InMoovHead startHead(String port, Integer headYPin, Integer headXPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin) throws Exception {
    return startHead(port, null, headYPin, headXPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
  }

  public InMoovHead startHead(String port, String type) throws Exception {
    return startHead(port, type, 12, 13, 22, 24, 26, 30);
  }

  public InMoovHead startHead(String port, String type, Integer headYPin, Integer headXPin, Integer eyeXPin, Integer eyeYPin, Integer jawPin, Integer rollNeckPin)
      throws Exception {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    speakBlocking(languagePack.get("STARTINGHEAD") + " " + port);
    head = (InMoovHead) startPeer("head");

    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    // FIXME - !!! => cannot do this "here" ??? head.arduino.setBoard(type);
    head.connect(port, headYPin, headXPin, eyeXPin, eyeYPin, jawPin, rollNeckPin);
    arduinos.put(port, head.controller);
    return head;
  }

  // vinmoov cosmetics and optional vinmoov monitor idea ( poc i know nothing
  // about jme...)
  // just want to use jme as main screen and show some informations
  // like batterie / errors / onreconized text etc ...
  // i01.VinmoovMonitorActivated=1 before to start vinmoov

  public void startHeadTracking() {
    startHeadTracking(head.rothead, head.neck);
  }

  public void startHeadTracking(ServoControl rothead, ServoControl neck) {
    if (opencv == null) {
      log.warn("Tracking needs Opencv activated, I will try to lauch it. It is better if you DIY");
      startOpenCV();
    }

    if (headTracking == null) {
      speakBlocking(languagePack.get("TRACKINGSTARTED"));
      headTracking = (Tracking) this.startPeer("headTracking");
      headTracking.connect(this.opencv, rothead, neck);
    }
  }

  @Deprecated
  public void startHeadTracking(String port, Integer rothead, Integer neck) {
    log.warn("Please use ServoControl : startHeadTracking(YourServoRothead,YourServoNeck), I will try to do it for you...");
    startHeadTracking();
  }

  public void startIK3d() throws Exception {
    ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    ik3d.setCurrentArm("rightArm", InMoovArm.getDHRobotArm(getName(), "left"));

    // Runtime.createAndStart("gui", "SwingGui");
    // OpenCV cv1 = (OpenCV)Runtime.createAndStart("cv1", "OpenCV");
    // OpenCVFilterAffine aff1 = new OpenCVFilterAffine("aff1");
    // aff1.setAngle(270);
    // aff1.setDx(-80);
    // aff1.setDy(-80);
    // cv1.addFilter(aff1);
    //
    // cv1.setCameraIndex(0);
    // cv1.capture();
    // cv1.undockDisplay(true);

    /*
     * SwingGui gui = new SwingGui("gui"); gui.startService();
     */
    /*
     * Joystick joystick = (Joystick) Runtime.start("joystick", "Joystick");
     * joystick.setController(2);
     * 
     * // joystick.startPolling();
     * 
     * // attach the joystick input to the ik3d service. //
     * joystick.addInputListener(ik3d); joystick.attach(this);
     */
  }

  public void startIntegratedMovement() {
    integratedMovement = (IntegratedMovement) startPeer("integratedMovement");
    IntegratedMovement im = integratedMovement;
    // set the DH Links or each arms
    im.setNewDHRobotArm("leftArm");
    im.setNewDHRobotArm("rightArm");
    im.setNewDHRobotArm("kinect");
    if (torso != null) {
      im.setDHLink("leftArm", torso.midStom, 113, 90, 0, -90);
      im.setDHLink("rightArm", torso.midStom, 113, 90, 0, -90);
      im.setDHLink("kinect", torso.midStom, 113, 90, 0, -90);
      im.setDHLink("leftArm", torso.topStom, 0, 180, 292, 90);
      im.setDHLink("rightArm", torso.topStom, 0, 180, 292, 90);
      im.setDHLink("kinect", torso.topStom, 0, 180, 110, -90);
    } else {
      im.setDHLink("leftArm", "i01.torso.midStom", 113, 90, 0, -90);
      im.setDHLink("rightArm", "i01.torso.midStom", 113, 90, 0, -90);
      im.setDHLink("kinect", "i01.torso.midStom", 113, 90, 0, -90);
      im.setDHLink("leftArm", "i01.torso.topStom", 0, 180, 292, 90);
      im.setDHLink("rightArm", "i01.torso.topStom", 0, 180, 292, 90);
      im.setDHLink("kinect", "i01.torso.topStom", 0, 180, 110, -90);
    }
    im.setDHLink("leftArm", "leftS", 143, 180, 0, 90);
    im.setDHLink("rightArm", "rightS", -143, 180, 0, -90);
    if (arms.containsKey(LEFT)) {
      InMoovArm arm = arms.get(LEFT);
      im.setDHLink("leftArm", arm.omoplate, 0, -5.6, 45, -90);
      im.setDHLink("leftArm", arm.shoulder, 77, -30 + 90, 0, 90);
      im.setDHLink("leftArm", arm.rotate, 284, 90, 40, 90);
      im.setDHLink("leftArm", arm.bicep, 0, -7 + 24.4 + 90, 300, 90);
    } else {
      im.setDHLink("leftArm", "i01.leftArm.omoplate", 0, -5.6, 45, -90);
      im.setDHLink("leftArm", "i01.leftArm.shoulder", 77, -30 + 90, 0, 90);
      im.setDHLink("leftArm", "i01.leftArm.rotate", 284, 90, 40, 90);
      im.setDHLink("leftArm", "i01.leftArm.bicep", 0, -7 + 24.4 + 90, 300, 90);
    }
    if (arms.containsKey(RIGHT)) {
      InMoovArm arm = arms.get(RIGHT);
      im.setDHLink("rightArm", arm.omoplate, 0, -5.6, 45, 90);
      im.setDHLink("rightArm", arm.shoulder, -77, -30 + 90, 0, -90);
      im.setDHLink("rightArm", arm.rotate, -284, 90, 40, -90);
      im.setDHLink("rightArm", arm.bicep, 0, -7 + 24.4 + 90, 300, 0);
    } else {
      im.setDHLink("rightArm", "i01.rightArm.omoplate", 0, -5.6, 45, 90);
      im.setDHLink("rightArm", "i01.rightArm.shoulder", -77, -30 + 90, 0, -90);
      im.setDHLink("rightArm", "i01.rightArm.rotate", -284, 90, 40, -90);
      im.setDHLink("rightArm", "i01.rightArm.bicep", 0, -7 + 24.4 + 90, 300, 0);
    }
    if (hands.containsKey(LEFT)) {
      InMoovHand hand = hands.get(LEFT);
      im.setDHLink("leftArm", hand.wrist, 00, -90, 0, 0);
      im.setDHLinkType("i01.leftHand.wrist", DHLinkType.REVOLUTE_ALPHA);
    } else {
      im.setDHLink("leftArm", "i01.leftHand.wrist", 00, -90, 0, 0);
    }
    if (hands.containsKey(RIGHT)) {
      InMoovHand hand = hands.get(RIGHT);
      im.setDHLink("rightArm", hand.wrist, 00, -90, 0, 0);
      im.setDHLinkType("i01.rigtHand.wrist", DHLinkType.REVOLUTE_ALPHA);
    } else {
      im.setDHLink("rightArm", "i01.rightHand.wrist", 00, -90, 0, 0);
    }
    im.setDHLink("leftArm", "wristup", 0, -5, 90, 0);
    im.setDHLink("leftArm", "wristdown", 0, 0, 125, 45);
    im.setDHLink("leftArm", "finger", 5, -90, 5, 0);
    im.setDHLink("rightArm", "Rwristup", 0, 5, 90, 0);
    im.setDHLink("rightArm", "Rwristdown", 0, 0, 125, -45);
    im.setDHLink("rightArm", "Rfinger", 5, 90, 5, 0);
    im.setDHLink("kinect", "camera", 0, 90, 10, 90);

    // log.info("{}",im.createJointPositionMap("leftArm").toString());
    // start the kinematics engines

    // define object, each dh link are set as an object, but the
    // start point and end point will be update by the ik service, but still
    // need
    // a name and a radius
    im.clearObject();
    im.addObject(0.0, 0.0, 0.0, 0.0, 0.0, -150.0, "base", 150.0, false);
    im.addObject("i01.torso.midStom", 150.0);
    im.addObject("i01.torso.topStom", 10.0);
    im.addObject("i01.leftArm.omoplate", 10.0);
    im.addObject("i01.rightArm.omoplate", 10.0);
    im.addObject("i01.leftArm.shoulder", 50.0);
    im.addObject("i01.rightArm.shoulder", 50.0);
    im.addObject("i01.leftArm.rotate", 50.0);
    im.addObject("i01.rightArm.rotate", 50.0);
    im.addObject("i01.leftArm.bicep", 60.0);
    im.addObject("i01.rightArm.bicep", 60.0);
    im.addObject("i01.leftHand.wrist", 70.0);
    im.addObject("i01.rightHand.wrist", 70.0);
    im.objectAddIgnore("i01.rightArm.omoplate", "i01.leftArm.rotate");
    im.objectAddIgnore("i01.rightArm.omoplate", "i01.rightArm.rotate");
    im.addObject("leftS", 10);
    im.addObject("rightS", 10);
    im.objectAddIgnore("leftS", "rightS");
    im.objectAddIgnore("rightS", "i01.leftArm.shoulder");
    im.objectAddIgnore("leftS", "i01.rightArm.shoulder");
    im.addObject("wristup", 70);
    im.addObject("wristdown", 70);
    im.objectAddIgnore("i01.leftArm.bicep", "wristup");
    im.addObject("Rwristup", 70);
    im.addObject("Rwristdown", 70);
    im.objectAddIgnore("i01.rightArm.bicep", "Rwristup");

    im.startEngine("leftArm");
    im.startEngine("rightArm");
    im.startEngine("kinect");

    im.cog = new GravityCenter(im);
    im.cog.setLinkMass("i01.torso.midStom", 2.832, 0.5);
    im.cog.setLinkMass("i01.torso.topStom", 5.774, 0.5);
    im.cog.setLinkMass("i01.leftArm.omoplate", 0.739, 0.5);
    im.cog.setLinkMass("i01.rightArm.omoplate", 0.739, 0.5);
    im.cog.setLinkMass("i01.leftArm.rotate", 0.715, 0.5754);
    im.cog.setLinkMass("i01.rightArm.rotate", 0.715, 0.5754);
    im.cog.setLinkMass("i01.leftArm.shoulder", 0.513, 0.5);
    im.cog.setLinkMass("i01.rightArm.shoulder", 0.513, 0.5);
    im.cog.setLinkMass("i01.leftArm.bicep", 0.940, 0.4559);
    im.cog.setLinkMass("i01.rightArm.bicep", 0.940, 0.4559);
    im.cog.setLinkMass("i01.leftHand.wrist", 0.176, 0.7474);
    im.cog.setLinkMass("i01.rightHand.wrist", 0.176, 0.7474);

    im.setJmeApp(vinMoovApp);
    im.setOpenni(openni);

  }
  public InMoovArm startLeftArm(String port) throws Exception {
    return startLeftArm(port, null);
  }

  public InMoovArm startLeftArm(String port, String type) throws Exception {
    leftArm = startArm(LEFT, port, type);
    return leftArm;
  }
  public InMoovHand startLeftHand(String port) throws Exception {
    return startLeftHand(port, null);
  }
  public InMoovHand startLeftHand(String port, String type) throws Exception {
    leftHand = startHand(LEFT, port, type);
    return leftHand;
  }

  public SpeechSynthesis startMouth() {
    if (mouth == null) {
      mouth = (SpeechSynthesis) startPeer("mouth");
    }
    this.attach((Attachable) mouth);
    speakBlocking(languagePack.get("STARTINGMOUTH"));
    speakBlocking(languagePack.get("WHATISTHISLANGUAGE"));

    return mouth;
  }

  public MouthControl startMouthControl() {
    return startMouthControl(head.jaw, mouth);
  }

  // end vinmoov cosmetics and optional vinmoov monitor
  /* use the most recent virtual inmoov */

  public MouthControl startMouthControl(ServoControl jaw, SpeechSynthesis mouth) {
    speakBlocking(languagePack.get("STARTINGMOUTHCONTROL"));
    if (mouthControl == null) {
      mouthControl = (MouthControl) startPeer("mouthControl");
      mouthControl.attach(jaw);
      mouthControl.attach((Attachable) mouth);
      mouthControl.setmouth(10, 50);
    }
    return mouthControl;
  }

  @Deprecated
  public MouthControl startMouthControl(String port) {
    log.warn("Please use ServoControl : startMouthControl(YourServoJaw,mouthService), I will try to do it for you...");
    return startMouthControl();
  }

  public boolean startOpenCV() {
    speakBlocking(languagePack.get("STARTINGOPENCV"));
    if (opencv == null) {
      opencv = (OpenCV) Runtime.loadAndStart(this.getIntanceName() + ".opencv", "OpenCV");
    }
    this.attach(opencv);

    if (vision.openCVenabled) {
      // test for a worky opencv with hardware
      // TODO: revisit this test method. , maybe it should go away or be done
      // differently?
      // It forces capture
      if (vision.test()) {
        broadcastState();
        return true;
      } else {
        speakAlert(languagePack.get("OPENCVNOWORKY"));
        return false;
      }
    }
    return false;
  }

  public OpenNi startOpenNI() throws Exception {
    if (openni == null) {
      speakBlocking(languagePack.get("STARTINGOPENNI"));
      openni = (OpenNi) startPeer("openni");
      pid = (Pid) startPeer("pid");

      pid.setPID("kinect", 10.0, 0.0, 1.0);
      pid.setMode("kinect", Pid.MODE_AUTOMATIC);
      pid.setOutputRange("kinect", -1, 1);

      pid.setControllerDirection("kinect", 0);

      // re-mapping of skeleton !
      openni.skeleton.leftElbow.mapXY(0, 180, 180, 0);
      openni.skeleton.rightElbow.mapXY(0, 180, 180, 0);
      if (openNiLeftShoulderInverted) {
        openni.skeleton.leftShoulder.mapYZ(0, 180, 180, 0);
      }
      if (openNiRightShoulderInverted) {
        openni.skeleton.rightShoulder.mapYZ(0, 180, 180, 0);
      }

      // openni.skeleton.leftShoulder

      // openni.addListener("publishOpenNIData", this.getName(),
      // "getSkeleton");
      // openni.addOpenNIData(this);
      subscribe(openni.getName(), "publishOpenNIData");
    }
    return openni;
  }

  public void startPIR(String port, int pin) throws IOException {
    speakBlocking(String.format("starting pee. eye. are. sensor on port %s pin %d", port, pin));
    if (arduinos.containsKey(port)) {
      ServoController sc = arduinos.get(port);
      if (sc instanceof Arduino) {
        Arduino arduino = (Arduino) sc;
        // arduino.connect(port);
        // arduino.setSampleRate(8000);
        arduino.enablePin(pin, 10);
        pirArduino = arduino;
        pirPin = pin;
        arduino.addListener("publishPin", this.getName(), "publishPin");
      }

    } else {
      // FIXME - SHOULD ALLOW STARTUP AND LATER ACCESS VIA PORT ONCE OTHER
      // STARTS CHECK MAP FIRST
      log.error("{} arduino not found - start some other system first (head, arm, hand)", port);
    }

  }

  public InMoovArm startRightArm(String port) throws Exception {
    return startRightArm(port, null);
  }

  public InMoovArm startRightArm(String port, String type) throws Exception {
    if (rightArm != null) {
      info("right arm already started");
      return rightArm;
    }
    rightArm = startArm(RIGHT, port, type);
    return rightArm;
  }

  public InMoovHand startRightHand(String port) throws Exception {
    return startRightHand(port, null);
  }

  public InMoovHand startRightHand(String port, String type) throws Exception {
    rightHand = startHand(RIGHT, port, type);
    return rightHand;
  }

  // ---------------------------------------------------------------
  // END GENERAL METHODS / TO SORT
  // ---------------------------------------------------------------

  @Override
  public void startService() {
    super.startService();
    if (vision == null) {
      vision = new Vision();
      vision.init();
    }
    vision.instance = this;

    // TODO : use locale it-IT,fi-FI
    languages.put("en-US", "English - United States");
    languages.put("fr-FR", "French - France");
    languages.put("es-ES", "Spanish - Spain");
    languages.put("de-DE", "German - Germany");
    languages.put("nl-NL", "Dutch - Netherlands");
    languages.put("ru-RU", "Russian");
    languages.put("hi-IN", "Hindi - India");
    languages.put("it-IT", "Italian - Italia");
    languages.put("fi-FI", "Finnish - Finland");
    languages.put("pt-PT", "Portuguese - Portugal");
    languagesIndex = new ArrayList<String>(languages.keySet());
    this.language = getLanguage();
    python = getPython();
    languagePack.load(language);

    // get events of new services and shutdown
    Runtime r = Runtime.getInstance();
    subscribe(r.getName(), "shutdown");
  }

  public void startSimulator() throws Exception {
    if (jme == null) {
      jme = (JMonkeyEngine) startPeer("jme");
    }
    
    
    // adding InMoov2 asset path to the jonkey simulator
    String assetPath = /* getResourceDir()*/ getResourceRoot() + fs + InMoov2.class.getSimpleName() + fs + JMonkeyEngine.class.getSimpleName();

    File check = new File(assetPath);
    log.info("loading assets from {}", assetPath);
    if (!check.exists()) {
      log.warn("%s does not exist");
    }

    // disable the frustrating servo events ...
    // Servo.eventsEnabledDefault(false);
    jme.loadModels(assetPath);


    // disable the frustrating servo events ...
    // Servo.eventsEnabledDefault(false);

    // ========== gael's calibrations begin ======================
    jme.setRotation("i01.head.jaw", "x");
    jme.setRotation("i01.head.neck", "x");
    jme.setRotation("i01.head.rothead", "y");
    jme.setRotation("i01.head.rollNeck", "z");
    jme.setRotation("i01.head.eyeY", "x");
    jme.setRotation("i01.head.eyeX", "y");
    jme.setRotation("i01.torso.topStom", "z");
    jme.setRotation("i01.torso.midStom", "y");
    jme.setRotation("i01.torso.lowStom", "x");
    jme.setRotation("i01.rightArm.bicep", "x");
    jme.setRotation("i01.leftArm.bicep", "x");
    jme.setRotation("i01.rightArm.shoulder", "x");
    jme.setRotation("i01.leftArm.shoulder", "x");
    jme.setRotation("i01.rightArm.rotate", "y");
    jme.setRotation("i01.leftArm.rotate", "y");
    jme.setRotation("i01.rightArm.omoplate", "z");
    jme.setRotation("i01.leftArm.omoplate", "z");
    jme.setRotation("i01.rightHand.wrist", "y");
    jme.setRotation("i01.leftHand.wrist", "y");

    jme.setMapper("i01.head.jaw", 0, 180, -5, 80);
    jme.setMapper("i01.head.neck", 0, 180, 20, -20);
    jme.setMapper("i01.head.rollNeck", 0, 180, 30, -30);
    jme.setMapper("i01.head.eyeY", 0, 180, 40, 140);
    jme.setMapper("i01.head.eyeX", 0, 180, -10, 70); // HERE there need to be
                                                     // two eyeX (left and
                                                     // right?)
    jme.setMapper("i01.rightArm.bicep", 0, 180, 0, -150);
    jme.setMapper("i01.leftArm.bicep", 0, 180, 0, -150);

    jme.setMapper("i01.rightArm.shoulder", 0, 180, 30, -150);
    jme.setMapper("i01.leftArm.shoulder", 0, 180, 30, -150);
    jme.setMapper("i01.rightArm.rotate", 0, 180, 80, -80);
    jme.setMapper("i01.leftArm.rotate", 0, 180, -80, 80);
    jme.setMapper("i01.rightArm.omoplate", 0, 180, 10, -180);
    jme.setMapper("i01.leftArm.omoplate", 0, 180, -10, 180);

    jme.setMapper("i01.rightHand.wrist", 0, 180, -20, 60);
    jme.setMapper("i01.leftHand.wrist", 0, 180, 20, -60);

    jme.setMapper("i01.torso.topStom", 0, 180, -30, 30);
    jme.setMapper("i01.torso.midStom", 0, 180, 50, 130);
    jme.setMapper("i01.torso.lowStom", 0, 180, -30, 30);

    // ========== gael's calibrations end ======================

    // ========== 3 joint finger mapping and attaching begin ===

    // ========== Requires VinMoov5.j3o ========================

    jme.attach("i01.leftHand.thumb", "i01.leftHand.thumb1", "i01.leftHand.thumb2", "i01.leftHand.thumb3");
    jme.setRotation("i01.leftHand.thumb1", "y");
    jme.setRotation("i01.leftHand.thumb2", "x");
    jme.setRotation("i01.leftHand.thumb3", "x");

    jme.attach("i01.leftHand.index", "i01.leftHand.index", "i01.leftHand.index2", "i01.leftHand.index3");
    jme.setRotation("i01.leftHand.index", "x");
    jme.setRotation("i01.leftHand.index2", "x");
    jme.setRotation("i01.leftHand.index3", "x");

    jme.attach("i01.leftHand.majeure", "i01.leftHand.majeure", "i01.leftHand.majeure2", "i01.leftHand.majeure3");
    jme.setRotation("i01.leftHand.majeure", "x");
    jme.setRotation("i01.leftHand.majeure2", "x");
    jme.setRotation("i01.leftHand.majeure3", "x");

    jme.attach("i01.leftHand.ringFinger", "i01.leftHand.ringFinger", "i01.leftHand.ringFinger2", "i01.leftHand.ringFinger3");
    jme.setRotation("i01.leftHand.ringFinger", "x");
    jme.setRotation("i01.leftHand.ringFinger2", "x");
    jme.setRotation("i01.leftHand.ringFinger3", "x");

    jme.attach("i01.leftHand.pinky", "i01.leftHand.pinky", "i01.leftHand.pinky2", "i01.leftHand.pinky3");
    jme.setRotation("i01.leftHand.pinky", "x");
    jme.setRotation("i01.leftHand.pinky2", "x");
    jme.setRotation("i01.leftHand.pinky3", "x");

    // left hand mapping complexities of the fingers
    jme.setMapper("i01.leftHand.index", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.index2", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.index3", 0, 180, -110, -179);

    jme.setMapper("i01.leftHand.majeure", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.majeure2", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.majeure3", 0, 180, -110, -179);

    jme.setMapper("i01.leftHand.ringFinger", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.ringFinger2", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.ringFinger3", 0, 180, -110, -179);

    jme.setMapper("i01.leftHand.pinky", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.pinky2", 0, 180, -110, -179);
    jme.setMapper("i01.leftHand.pinky3", 0, 180, -110, -179);

    jme.setMapper("i01.leftHand.thumb1", 0, 180, -30, -100);
    jme.setMapper("i01.leftHand.thumb2", 0, 180, 80, 20);
    jme.setMapper("i01.leftHand.thumb3", 0, 180, 80, 20);

    // right hand

    jme.attach("i01.rightHand.thumb", "i01.rightHand.thumb1", "i01.rightHand.thumb2", "i01.rightHand.thumb3");
    jme.setRotation("i01.rightHand.thumb1", "y");
    jme.setRotation("i01.rightHand.thumb2", "x");
    jme.setRotation("i01.rightHand.thumb3", "x");

    jme.attach("i01.rightHand.index", "i01.rightHand.index", "i01.rightHand.index2", "i01.rightHand.index3");
    jme.setRotation("i01.rightHand.index", "x");
    jme.setRotation("i01.rightHand.index2", "x");
    jme.setRotation("i01.rightHand.index3", "x");

    jme.attach("i01.rightHand.majeure", "i01.rightHand.majeure", "i01.rightHand.majeure2", "i01.rightHand.majeure3");
    jme.setRotation("i01.rightHand.majeure", "x");
    jme.setRotation("i01.rightHand.majeure2", "x");
    jme.setRotation("i01.rightHand.majeure3", "x");

    jme.attach("i01.rightHand.ringFinger", "i01.rightHand.ringFinger", "i01.rightHand.ringFinger2", "i01.rightHand.ringFinger3");
    jme.setRotation("i01.rightHand.ringFinger", "x");
    jme.setRotation("i01.rightHand.ringFinger2", "x");
    jme.setRotation("i01.rightHand.ringFinger3", "x");

    jme.attach("i01.rightHand.pinky", "i01.rightHand.pinky", "i01.rightHand.pinky2", "i01.rightHand.pinky3");
    jme.setRotation("i01.rightHand.pinky", "x");
    jme.setRotation("i01.rightHand.pinky2", "x");
    jme.setRotation("i01.rightHand.pinky3", "x");

    jme.setMapper("i01.rightHand.index", 0, 180, 65, -10);
    jme.setMapper("i01.rightHand.index2", 0, 180, 70, -10);
    jme.setMapper("i01.rightHand.index3", 0, 180, 70, -10);

    jme.setMapper("i01.rightHand.majeure", 0, 180, 65, -10);
    jme.setMapper("i01.rightHand.majeure2", 0, 180, 70, -10);
    jme.setMapper("i01.rightHand.majeure3", 0, 180, 70, -10);

    jme.setMapper("i01.rightHand.ringFinger", 0, 180, 65, -10);
    jme.setMapper("i01.rightHand.ringFinger2", 0, 180, 70, -10);
    jme.setMapper("i01.rightHand.ringFinger3", 0, 180, 70, -10);

    jme.setMapper("i01.rightHand.pinky", 0, 180, 65, -10);
    jme.setMapper("i01.rightHand.pinky2", 0, 180, 70, -10);
    jme.setMapper("i01.rightHand.pinky3", 0, 180, 60, -10);

    jme.setMapper("i01.rightHand.thumb1", 0, 180, 30, 110);
    jme.setMapper("i01.rightHand.thumb2", 0, 180, -100, -150);
    jme.setMapper("i01.rightHand.thumb3", 0, 180, -100, -160);

    // additional experimental mappings
    /*
     * jme.attach("i01.leftHand.pinky", "i01.leftHand.index2");
     * jme.attach("i01.leftHand.thumb", "i01.leftHand.index3");
     * jme.setRotation("i01.leftHand.index2", "x");
     * jme.setRotation("i01.leftHand.index3", "x");
     * jme.setMapper("i01.leftHand.index", 0, 180, -90, -270);
     * jme.setMapper("i01.leftHand.index2", 0, 180, -90, -270);
     * jme.setMapper("i01.leftHand.index3", 0, 180, -90, -270);
     */

    // creating a virtual inmoov with virtual servo controller
    /*
     * ServoController sc = jme.getServoController(); InMoov i01 = (InMoov)
     * Runtime.start("i01", "InMoov"); i01.startHead(sc); i01.startArm("left",
     * sc); i01.startArm("right", sc); i01.startHand("left", sc);
     * i01.startHand("right", sc); i01.startTorso(sc); i01.startMouth();
     * i01.startMouthControl();
     * 
     * i01.rest();
     */

  }

  public InMoovTorso startTorso(ServoController controller) throws Exception {
    torso = (InMoovTorso) startPeer("torso");
    torso.setController(controller);
    return torso;
  }

  public InMoovTorso startTorso(String port) throws Exception {
    return startTorso(port, null);
  }

  public InMoovTorso startTorso(String port, String type) throws Exception {
    // log.warn(InMoov.buildDNA(myKey, serviceClass))
    speakBlocking(languagePack.get("STARTINGTORSO") + " " + port);

    torso = (InMoovTorso) startPeer("torso");
    if (type == null) {
      type = Arduino.BOARD_TYPE_MEGA;
    }

    // FIXME - needs to be a ServoController
    // torso.arduino.setBoard(type);
    torso.connect(port);
    arduinos.put(port, torso.controller);

    return torso;
  }

  public void startVinMoov() throws Exception {
    startSimulator();
  }

  public void stopGesture() {
    Python p = (Python) Runtime.getService("python");
    p.stop();
  }

  public void stopNeopixelAnimation() {
    if (neopixel != null && neopixelArduino != null) {
      neopixel.animationStop();
    } else {
      warn("No Neopixel attached");
    }
  }

  public void stopPIR() {
    if (pirArduino != null && pirPin != null) {
      pirArduino.disablePin(pirPin);
      pirPin = null;
      pirArduino = null;
    }
    /*
     * if (arduinos.containsKey(port)) { Arduino arduino = arduinos.get(port);
     * arduino.connect(port); arduino.setSampleRate(8000);
     * arduino.digitalReadPollStart(pin); pirPin = pin;
     * arduino.addListener("publishPin", this.getName(), "publishPin"); }
     */

  }

  public void stopTracking() {
    if (eyesTracking != null) {
      eyesTracking.stopTracking();
    }

    if (headTracking != null) {
      headTracking.stopTracking();
    }
  }

  public void stopVinMoov() {
    try {
      vinMoovApp.stop();
    } catch (NullPointerException e) {

    }
    vinMoovApp = null;
  }

  // This is an in-flight check vs power up or power down
  public void systemCheck() {
    speakBlocking("starting system check");
    speakBlocking("testing");

    rest();
    sleep(500);

    if (rightHand != null) {
      speakBlocking("testing right hand");
      rightHand.test();
    }

    if (rightArm != null) {
      speakBlocking("testing right arm");
      rightArm.test();
    }

    if (leftHand != null) {
      speakBlocking("testing left hand");
      leftHand.test();
    }

    if (leftArm != null) {
      speakBlocking("testing left arm");
      leftArm.test();
    }

    if (head != null) {
      speakBlocking("testing head");
      head.test();
    }

    if (torso != null) {
      speakBlocking("testing torso");
      torso.test();
    }

    if (eyelids != null) {
      speakBlocking("testing eyelids");
      eyelids.test();
    }

    sleep(500);
    rest();
    broadcastState();
    speakBlocking("system check completed");
  }

  public void trackHumans() {
    // FIXME can't have 2 PID for tracking
    // if (eyesTracking != null) {
    // eyesTracking.faceDetect();
    // }
    vision.enablePreFilters();
    startHeadTracking();
    if (headTracking != null) {
      headTracking.faceDetect();
    }
  }

  // FIXME check / test lk tracking..
  public void trackPoint() {
    vision.enablePreFilters();
    startHeadTracking();
    if (headTracking != null) {
      headTracking.startLKTracking();
      headTracking.trackPoint();
    }
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


  public void waitTargetPos() {
    if (head != null)
      head.waitTargetPos();
    if (eyelids != null)
      eyelids.waitTargetPos();
    if (leftArm != null)
      leftArm.waitTargetPos();
    if (rightArm != null)
      rightArm.waitTargetPos();
    if (leftHand != null)
      leftHand.waitTargetPos();
    if (rightHand != null)
      rightHand.waitTargetPos();
    if (torso != null)
      torso.waitTargetPos();
  }

}
