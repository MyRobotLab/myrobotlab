package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.WebGui;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.repo.Category;
import org.myrobotlab.framework.repo.ServiceData;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechRecognizer;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;
import org.myrobotlab.inmoov.LanguagePack;
import org.myrobotlab.inmoov.Utils;

/**
 * InMoov2 - The InMoov Service ( very WIP ).
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on http://www.inmoov.fr/). InMoov is a
 * composite of servos, Arduinos, microphone, camera, kinect and computer. The
 * InMoov service is composed of many other services, and allows easy
 * initialization and control of these sub systems.
 *
 */
public class InMoov2 extends Service {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);
  transient private static Runtime myRuntime = null; //
  transient private static ServiceData serviceData = null; // =

  // interfaces declaration needed by InMoov
  transient public SpeechSynthesis mouth;
  transient public SpeechRecognizer ear;

  //
  // this is for gui combo content
  //
  public static LinkedHashMap<String, String> languages = new LinkedHashMap<String, String>();
  public static List<String> languagesIndex = new ArrayList<String>();
  public static List<String> speechEngines = new ArrayList<String>();
  public static List<String> earEngines = new ArrayList<String>();

  //
  // Multidimensional hashmap for unlimited attached skeleton members, main key is free skeleton type ( hand, arm ... )
  //
  private HashMap<String, HashMap<String, Skeleton>> inMoovSkeleton = new HashMap<String, HashMap<String, Skeleton>>();

  // ---------------------------------------------------------------
  // Store parameters inside json related service
  // ---------------------------------------------------------------

  String language;
  boolean mute;
  String speechEngine;
  String earEngine;

  // ---------------------------------------------------------------
  // end of config
  // ---------------------------------------------------------------

  private boolean gestureIsRunning = false;

  public InMoov2(String n) {
    super(n);
    if (myRuntime == null) {
      myRuntime = (Runtime) Runtime.getInstance();
      ;
    }

    if (serviceData == null) {
      serviceData = myRuntime.getServiceData();
    }
    // TODO : use locale
    languages.put("en-US", "English - United States");
    languages.put("fr-FR", "French - France");
    languagesIndex = new ArrayList<String>(languages.keySet());

  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      Runtime.start("gui", "SwingGui");
      String leftPort = "COM3";
      String rightPort = "COM4";

      VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft", "VirtualArduino");
      VirtualArduino vright = (VirtualArduino) Runtime.start("vright", "VirtualArduino");
      Python python = (Python) Runtime.start("python", "Python");
      vleft.connect(leftPort);
      vright.connect(rightPort);

      Arduino arduinoLeft = (Arduino) Runtime.start("arduinoLeft", "Arduino");
      Arduino arduinoRight = (Arduino) Runtime.start("arduinoRight", "Arduino");
      arduinoLeft.connect(leftPort);
      arduinoRight.connect(rightPort);

      // virtual arduino can't simulate velocity at this time
      // i2c service connected onto virtual arduino will do the job
      // https://github.com/MyRobotLab/myrobotlab/issues/99
      Adafruit16CServoDriver adafruit16CServoDriverLeft = (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriverLeft", "Adafruit16CServoDriver");
      adafruit16CServoDriverLeft.attach(arduinoLeft, "0", "0x40");

      Adafruit16CServoDriver adafruit16CServoDriverRight = (Adafruit16CServoDriver) Runtime.start("adafruit16CServoDriverRight", "Adafruit16CServoDriver");
      adafruit16CServoDriverRight.attach(arduinoRight, "0", "0x40");

      InMoov2 inMoov = (InMoov2) Runtime.start("inMoov", "InMoov2");

      WebkitSpeechRecognition ear = (WebkitSpeechRecognition) Runtime.start("ear", "WebkitSpeechRecognition");

      Skeleton lHand = (Skeleton) Runtime.start("lHand", "Skeleton");
      Skeleton rHand = (Skeleton) Runtime.start("rHand", "Skeleton");
      Skeleton lArm = (Skeleton) Runtime.start("lArm", "Skeleton");
      Skeleton rArm = (Skeleton) Runtime.start("rArm", "Skeleton");
      Skeleton torso = (Skeleton) Runtime.start("torso", "Skeleton");
      Skeleton head = (Skeleton) Runtime.start("head", "Skeleton");

      Servo lthumb = (Servo) Runtime.start("lthumb", "Servo");
      Servo lindex = (Servo) Runtime.start("lindex", "Servo");
      Servo lmajeure = (Servo) Runtime.start("lmajeure", "Servo");
      Servo lringfinger = (Servo) Runtime.start("lringfinger", "Servo");
      Servo lpinky = (Servo) Runtime.start("lpinky", "Servo");
      Servo lwrist = (Servo) Runtime.start("lwrist", "Servo");

      Servo rthumb = (Servo) Runtime.start("rthumb", "Servo");
      Servo rindex = (Servo) Runtime.start("rindex", "Servo");
      Servo rmajeure = (Servo) Runtime.start("rmajeure", "Servo");
      Servo rringfinger = (Servo) Runtime.start("rringfinger", "Servo");
      Servo rpinky = (Servo) Runtime.start("rpinky", "Servo");
      Servo rwrist = (Servo) Runtime.start("rwrist", "Servo");

      lthumb.attach(adafruit16CServoDriverLeft, 1);
      lindex.attach(adafruit16CServoDriverLeft, 2);
      lmajeure.attach(adafruit16CServoDriverLeft, 3);
      lringfinger.attach(adafruit16CServoDriverLeft, 4);
      lpinky.attach(adafruit16CServoDriverLeft, 5);
      lwrist.attach(adafruit16CServoDriverLeft, 6);
      rthumb.attach(adafruit16CServoDriverRight, 1);
      rindex.attach(adafruit16CServoDriverRight, 2);
      rmajeure.attach(adafruit16CServoDriverRight, 3);
      rringfinger.attach(adafruit16CServoDriverRight, 4);
      rpinky.attach(adafruit16CServoDriverRight, 5);
      rwrist.attach(adafruit16CServoDriverRight, 6);

      lHand.attach(lthumb, lindex, lmajeure, lringfinger, lpinky, lwrist);
      rHand.attach(rthumb, rindex, rmajeure, rringfinger, rpinky, rwrist);

      inMoov.attach(lHand, "hand", "left");
      inMoov.attach(rHand, "hand", "right");
      inMoov.attach(lArm, "arm", "left");
      inMoov.attach(rArm, "arm", "right");
      inMoov.attach(torso, "torso");
      inMoov.attach(head, "head");
      //inMoov.attach(ear);
      inMoov.startMouth();
      //inMoov.loadGestures();

      LoggingFactory.init(Level.WARN);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void startService() {
    super.startService();

    InMoov2.speechEngines = getServicesFromCategory("speech");
    InMoov2.earEngines = getServicesFromCategory("speech recognition");
    this.language = getLanguage();
    this.speechEngine = getSpeechEngine();
    this.earEngine = getEarEngine();
    loadLanguagePack();
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoov2.class);
    meta.setAvailable(false);
    return meta;
  }

  // list services from meta category (pasted from RuntimeGui.java)
  public List<String> getServicesFromCategory(final String filter) {
    List<String> servicesFromCategory = new ArrayList<String>();
    Category category = serviceData.getCategory(filter);
    HashSet<String> filtered = null;
    filtered = new HashSet<String>();
    ArrayList<String> f = category.serviceTypes;
    for (int i = 0; i < f.size(); ++i) {
      filtered.add(f.get(i));
    }

    // populate with serviceData
    List<ServiceType> possibleService = serviceData.getServiceTypes();
    for (int i = 0; i < possibleService.size(); ++i) {
      ServiceType serviceType = possibleService.get(i);
      if (filtered.contains(serviceType.getName())) {
        if (serviceType.isAvailable()) {
          // log.debug("serviceType : " + serviceType.getName());
          servicesFromCategory.add(serviceType.getSimpleName());
        }
      }

    }
    return servicesFromCategory;
  }

  // ---------------------------------------------------------------
  // attach interfaces
  // ---------------------------------------------------------------
  public void attach(Attachable attachable) {
    attach(attachable, null, null);
  }

  public void attach(Attachable attachable, String type) {
    attach(attachable, type, null);
  }

  public void attach(Attachable attachable, String type, String side) {
    // todo : detach
    if (attachable instanceof SpeechSynthesis) {

      mouth = (SpeechSynthesis) attachable;
      try {
        mouth.speakBlocking(LanguagePack.get("STARTINGMOUTH"));
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      broadcastState();

    } else if (attachable instanceof SpeechRecognizer) {
      try {
        mouth.speakBlocking(LanguagePack.get("STARTINGEAR"));
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      if (attachable.getClass().getSimpleName().equals("WebkitSpeechRecognition")) {

        WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
        webgui.autoStartBrowser(false);
        webgui.startService();
        webgui.startBrowser("http://localhost:8888/#/service/ear");

      }
      ear = (SpeechRecognizer) attachable;
      broadcastState();

    } else if (attachable instanceof Skeleton) {
      if (!inMoovSkeleton.containsKey(type)) {
        inMoovSkeleton.put(type, new HashMap<String, Skeleton>());
      }
      inMoovSkeleton.get(type).put(side, (Skeleton) attachable);
      broadcastState();

    } else

    {
      log.error("don't know how to attach a {}", attachable.getName());
    }
  }

  // ---------------------------------------------------------------
  // start core interfaces, used by GUI
  // ---------------------------------------------------------------
  /**
   * Start InMoov speech engine also called "mouth"
   * 
   * @return started SpeechSynthesis service
   */
  public SpeechSynthesis startMouth() {
    SpeechSynthesis mouth = (SpeechSynthesis) Runtime.start(this.getIntanceName() + ".mouth", getSpeechEngine());

    this.attach((Attachable) mouth);
    broadcastState();
    //return mouth;
    return mouth;
  }

  /**
   * Start InMoov ear engine
   * 
   * @return started SpeechRecognizer service
   */
  public SpeechRecognizer startEar() {
    ear = (SpeechRecognizer) Runtime.start(this.getIntanceName() + ".ear", getEarEngine());
    broadcastState();
    return ear;
  }
  // ---------------------------------------------------------------
  // END core interfaces
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // Common gestures methods
  // ---------------------------------------------------------------

  public void setHandVelocity(String side, Double... velocity) {

    setSkeletonVelocity("hand", side, velocity);

  }

  public void setArmVelocity(String side, Double... velocity) {

    setSkeletonVelocity("arm", side, velocity);
  }

  public void setHeadVelocity(Double... velocity) {

    setSkeletonVelocity("head", null, velocity);
  }

  public void moveHead(Double... servoPos) {

    moveSkeleton("head", null, servoPos);

  }

  public void moveHeadBlocking(Double... servoPos) {

    moveSkeletonBlocking("hand", null, servoPos);

  }

  public void moveHand(String side, Double... servoPos) {

    moveSkeleton("hand", side, servoPos);

  }

  public void moveHandBlocking(String side, Double... servoPos) {

    moveSkeletonBlocking("hand", side, servoPos);

  }

  public void moveArm(String side, Double... servoPos) {

    moveSkeleton("arm", side, servoPos);

  }

  public void moveArmBlocking(String side, Double... servoPos) {

    moveSkeletonBlocking("arm", side, servoPos);

  }

  public void setSkeletonVelocity(String type, String side, Double... velocity) {

    Skeleton member = getSkeletonPart(type, side);
    if (member != null) {
      member.checkParameters(velocity.length);
      for (int i = 0; i < velocity.length && i < member.getServos().size(); i++) {
        member.getServos().get(i).setVelocity(velocity[i]);
      }
    } else {
      error("Seem the hand : " + side + " is unknown...");
    }

  }

  /** 
   * Generic move a group of Inmoov servo
   * moveTo order is based on attach order, very important !
   * TODO Universal main nervous system service
   */
  public void moveSkeleton(String type, String side, Double... servoPos) {
    Skeleton member = getSkeletonPart(type, side);

    if (member != null) {
      member.moveTo(servoPos);
    } else {
      error("Seem the member  : " + side + " " + type + " is unknown...");
    }

  }

  /** 
   * Generic block move a group of Inmoov servo
   * moveTo order is based on attach order, very important !
   * TODO Universal main nervous system service
   */
  public void moveSkeletonBlocking(String type, String side, Double... servoPos) {
    Skeleton member = getSkeletonPart(type, side);

    if (member != null) {
      member.moveToBlocking(servoPos);
    } else {
      error("Seem the member  : " + side + " " + type + " is unknown...");
    }

  }

  /**
   *  over every skeleton members for setOverrideAutoDisable
   * 
   */
  private void setOverrideAutoDisable(Boolean param) {
    inMoovSkeleton.forEach((skeletonType, skeletonHash) -> skeletonHash.forEach((side, skeleton) -> skeleton.setOverrideAutoDisable(param)));
  }

  /**
   *  over every skeleton members for waitTargetPos
   * 
   */
  private void waitTargetPos() {
    inMoovSkeleton.forEach((skeletonType, skeletonHash) -> skeletonHash.forEach((side, skeleton) -> skeleton.waitTargetPos()));
  }

  public void loadGestures() {
    loadGestures("InMoov2" + File.separator + "gestures");
  }

  /**
   * This method will look at all of the .py files in a directory. One by one it
   * will load the files into the python interpreter. A gesture python file
   * should contain 1 method definition that is the same as the filename.
   * 
   * @param directory
   *          - the directory that contains the gesture python files.
   */
  public boolean loadGestures(String directory) {

    // iterate over each of the python files in the directory
    // and load them into the python interpreter.
    File dir = new File(directory);
    boolean loaded = true;
    for (File f : dir.listFiles()) {
      loaded = Utils.loadPythonFile(f.getAbsolutePath(), this.getIntanceName());
    }
    return true;
  }

  public void loadLanguagePack() {
    try {
      LanguagePack.load(getLanguage(), this.getIntanceName());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void stopGesture() {
    Python p = (Python) Runtime.getService("python");
    p.stop();
  }

  public void startedGesture() {
    startedGesture("unknown");
  }

  public void startedGesture(String nameOfGesture) {
    if (gestureIsRunning) {
      warn("Warning 1 gesture already running, this can break spacetime and lot of things");
    } else {
      gestureIsRunning = true;
      //RobotCanMoveRandom = false;
      setOverrideAutoDisable(true);
    }
  }

  public void finishedGesture() {
    finishedGesture("unknown");
  }

  public void finishedGesture(String nameOfGesture) {
    if (gestureIsRunning) {
      waitTargetPos();
      //RobotCanMoveRandom = true;
      setOverrideAutoDisable(false);
      gestureIsRunning = false;
    }
  }

  // ---------------------------------------------------------------
  // END Common methods
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // setters & getters
  // ---------------------------------------------------------------

  public Skeleton getSkeletonPart(String type, String side) {
    try {
      return inMoovSkeleton.get(type).get(side);
    } catch (

    Exception e) {

      return null;
    }
  }

  public String getSpeechEngine() {
    if (this.speechEngine == null) {
      setSpeechEngine("MarySpeech");
    }
    return speechEngine;
  }

  public void setSpeechEngine(String speechEngine) {
    this.speechEngine = speechEngine;
    info("Set InMoov speech engine : %s", speechEngine);
    broadcastState();
  }

  public String getEarEngine() {
    if (this.earEngine == null) {
      setEarEngine("WebkitSpeechRecognition");
    }
    return earEngine;
  }

  public void setEarEngine(String earEngine) {
    this.earEngine = earEngine;
    info("Set InMoov ear engine : %s", earEngine);
    broadcastState();
  }

  /**
   * @return the mute startup state ( InMoov vocal startup actions )
   */
  public Boolean getMute() {
    return mute;
  }

  /**
   * @param mute
   *          the startup mute state to set ( InMoov vocal startup actions )
   */
  public void setMute(Boolean mute) {
    this.mute = mute;
    info("Set InMoov to mute at startup : %s", mute);
    broadcastState();
  }

  /**
   * TODO : use system locale
   * set language for InMoov service used by chatbot + ear + mouth
   * 
   * @param i
   *          - format : java Locale
   */
  public void setLanguage(String l) {
    if (languages.containsKey(Locale.getDefault().toLanguageTag())) {
      this.language = l;
      info("Set Runtime language to %s", languages.get(l));
      Runtime runtime = Runtime.getInstance();
      runtime.setLocale(l);
      broadcastState();
    } else {
      error("InMoov not yet support {}", l);
    }
  }

  /**
   * get current language
   */
  public String getLanguage() {
    if (this.language == null) {
      //check if default locale supported by inmoov
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

  public HashMap<String, HashMap<String, Skeleton>> getSkeleton() {
    return this.inMoovSkeleton;
  }

  // ---------------------------------------------------------------
  // END setter & getter
  // ---------------------------------------------------------------
}
