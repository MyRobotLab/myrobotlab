package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.myrobotlab.framework.Index;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.WebGui;
import org.myrobotlab.service.abstracts.AbstractBodyPart;
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
 * Generic bodyPart service is used for skeleton control.
 *
 */
public class InMoov2 extends AbstractBodyPart {
  
  //TODO : check if skeleton tree is serialized
  //TODO : migrate most of py code from inmoov repo
  //TODO : check all gestures

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2.class);
  transient private static Runtime myRuntime = null; //
  transient private static ServiceData serviceData = null; // =

  // interfaces reservation needed by InMoov
  transient public SpeechSynthesis mouth;
  transient public SpeechRecognizer ear;
  transient public ProgramAB brain;

  //
  // this is for GUI combo contents
  //
  public static LinkedHashMap<String, String> languages = new LinkedHashMap<String, String>();
  public static List<String> languagesIndex = new ArrayList<String>();
  public static List<String> speechEngines = new ArrayList<String>();
  public static List<String> earEngines = new ArrayList<String>();

  // ---------------------------------------------------------------
  // Store some parameters inside json related service
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

      BodyPart leftHand = (BodyPart) Runtime.start("leftHand", "BodyPart");
      BodyPart rightHand = (BodyPart) Runtime.start("rightHand", "BodyPart");
      BodyPart leftArm = (BodyPart) Runtime.start("leftArm", "BodyPart");
      BodyPart rightArm = (BodyPart) Runtime.start("rightArm", "BodyPart");
      BodyPart torso = (BodyPart) Runtime.start("torso", "BodyPart");
      BodyPart head = (BodyPart) Runtime.start("head", "BodyPart");

      Servo lthumb = (Servo) Runtime.start("leftHand.thumb", "Servo");
      Servo lindex = (Servo) Runtime.start("leftHand.index", "Servo");
      Servo lmajeure = (Servo) Runtime.start("leftHand.majeure", "Servo");
      Servo lringfinger = (Servo) Runtime.start("leftHand.ringfinger", "Servo");
      Servo lpinky = (Servo) Runtime.start("leftHand.pinky", "Servo");
      Servo lwrist = (Servo) Runtime.start("leftHand.wrist", "Servo");

      Servo rthumb = (Servo) Runtime.start("rightHand.thumb", "Servo");
      Servo rindex = (Servo) Runtime.start("rightHand.index", "Servo");
      Servo rmajeure = (Servo) Runtime.start("rightHand.majeure", "Servo");
      Servo rringfinger = (Servo) Runtime.start("rightHand.ringfinger", "Servo");
      Servo rpinky = (Servo) Runtime.start("rightHand.pinky", "Servo");
      Servo rwrist = (Servo) Runtime.start("rightHand.wrist", "Servo");

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

      leftHand.attach(lpinky, lthumb, lindex, lmajeure, lringfinger, lwrist);
      rightHand.attach(rthumb, rindex, rwrist, rmajeure, rringfinger, rpinky);
      rightArm.attach(rightHand);
      leftArm.attach(leftHand);
      inMoov.attach(leftArm);
      inMoov.attach(rightArm);

      //inMoov.attach(lHand, "hand", "left");
      //inMoov.attach(rightHand, "hand", "right");
      //inMoov.attach(leftArm, "arm", "left");
      //inMoov.attach(rArm, "arm", "right");
      //inMoov.attach(torso, "torso");
      //inMoov.attach(head, "head");
      //inMoov.attach(ear);
      inMoov.startMouth();
      inMoov.startEar();
      inMoov.loadGestures();
      ProgramAB bot = (ProgramAB) Runtime.start("bot", "ProgramAB");
      inMoov.attach(bot);

      //LoggingFactory.init(Level.WARN);

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
    try {
      LanguagePack.load(getLanguage(), this.getIntanceName());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InMoov2.class);
    meta.setAvailable(false);
    meta.addDependency("inmoov.fr", "inmoov2", "0.1", "zip");
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

  public void attach(Attachable attachable) throws Exception {
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

      mouth.speakBlocking(LanguagePack.get("STARTINGEAR"));

      if (attachable.getClass().getSimpleName().equals("WebkitSpeechRecognition")) {
        ear = (SpeechRecognizer) attachable;
        WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
        webgui.autoStartBrowser(false);
        webgui.startService();
        webgui.startBrowser("http://localhost:8888/#/service/" + attachable.getName());
      }

      // attach ear to mouth
      if (!(mouth == null)) {
        ear.addMouth(mouth);
      }
      broadcastState();

    } else if (attachable instanceof ProgramAB) {

      brain = (ProgramAB) attachable;
      brain.repetition_count(10);
      brain.setPath("InMoov2/chatBot");
      // check aiml / aimlif is synced
      if (!(brain.wasCleanyShutdowned == null) && brain.wasCleanyShutdowned.equals("nok")) {
        mouth.speakBlocking(LanguagePack.get("BADSHUTDOWN"));
      } else {
        mouth.speakBlocking(LanguagePack.get("CHATBOTLOADING"));
      }
      brain.startSession("default", "fi-FI");
      brain.setPredicate("default", "topic", "default");
      brain.setPredicate("default", "questionfirstinit", "");
      brain.setPredicate("default", "tmpname", "");
      brain.setPredicate("default", "null", "");
      //brain.setPredicate("default","MagicCommandToWakeUp",MagicCommandToWakeUp)
      if (!brain.getPredicate("default", "name").isEmpty()) {
        if (brain.getPredicate("default", "lastUsername").isEmpty() || brain.getPredicate("default", "lastUsername").equals("unknown")) {
          brain.setPredicate("default", "lastUsername", brain.getPredicate("default", "name"));
        }
      }
      brain.savePredicates();
      //start session based on last recognized person
      if (!brain.getPredicate("default", "lastUsername").isEmpty() && !brain.getPredicate("default", "lastUsername").equals("unknown")) {
        brain.setUsername(brain.getPredicate("default", "lastUsername"));

      }

      mouth.speakBlocking(LanguagePack.get("CHATBOTACTIVATED"));
      if (mouth == null || ear == null) {
        warn("Chatbot warning : it is better if InMoov have ear and mouth...");
      }
      brain.attach((Attachable) mouth);
      brain.attach((Attachable) ear);
      broadcastState();

    }

    else if (attachable instanceof BodyPart) {
      // attach the child to this node
      if (attachable instanceof BodyPart) {
        // store bodypart service inside the tree
        thisNode.put(this.getName() + "." + attachable.getName(), attachable);
        // store bodypart nodes
        ArrayList<Attachable> nodes = ((BodyPart) attachable).thisNode.flatten();
        for (Attachable service : nodes) {
          thisNode.put(this.getName() + "." + service.getName(), service);
        }
      }
      broadcastState();
    } else

    {
      log.error("don't know how to attach a {}", attachable.getName());
    }
  }

  // ---------------------------------------------------------------
  // start core interfaces, this is used by GUI
  // ---------------------------------------------------------------
  /**
   * Start InMoov speech engine also called "mouth"
   * 
   * @return started SpeechSynthesis service
   * @throws Exception 
   */
  public SpeechSynthesis startMouth() throws Exception {
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
   * @throws Exception 
   */
  public SpeechRecognizer startEar() throws Exception {
    ear = (SpeechRecognizer) Runtime.start(this.getIntanceName() + ".ear", getEarEngine());
    this.attach((Attachable) ear);
    broadcastState();
    return ear;
  }

  /**
   * Start InMoov brain engine
   * 
   * @return started SpeechRecognizer service
   * @throws Exception 
   */
  public ProgramAB startBrain() throws Exception {
    brain = (ProgramAB) Runtime.start(this.getIntanceName() + ".brain", "ProgramAB");
    this.attach(brain);
    broadcastState();
    return brain;
  }
  // ---------------------------------------------------------------
  // END core interfaces
  // ---------------------------------------------------------------

  // ---------------------------------------------------------------
  // Common gestures methods for bodypart control
  // ---------------------------------------------------------------

  public void setHandVelocity(String side, Double... velocity) {
    setVelocity(side + "Hand", velocity);
  }

  public void setArmVelocity(String side, Double... velocity) {
    setVelocity(side + "Arm", velocity);
  }

  public void setHeadVelocity(Double... velocity) {
    setVelocity("head", velocity);
  }

  public void moveHead(Double... servoPos) {
    moveTo("head", servoPos);
  }

  public void moveHeadBlocking(Double... servoPos) {
    moveToBlocking("head", servoPos);
  }

  public void moveHand(String side, Double... servoPos) {
    moveTo(side + "Hand", servoPos);
  }

  public void moveHandBlocking(String side, Double... servoPos) {
    moveToBlocking(side + "Hand", servoPos);
  }

  public void moveArm(String side, Double... servoPos) {
    moveTo(side + "Arm", servoPos);
  }

  public void moveArmBlocking(String side, Double... servoPos) {
    moveToBlocking(side + "Arm", servoPos);
  }

  public void setVelocity(String bodypart, Double... velocity) {

    BodyPart part = getBodyPart(bodypart);
    if (part != null) {
      part.setVelocity(velocity);
    } else {
      error("Seem the bodypart : " + bodypart + " is unknown...");
    }

  }

  /**
   *  Iterate over every skeleton members for setOverrideAutoDisable
   * 
   */
  private void setOverrideAutoDisable(Boolean param) {
    for (BodyPart service : getBodyParts()) {
      service.setOverrideAutoDisable(param);
    }
  }

  /**
   *  over every skeleton members for waitTargetPos
   * 
   */
  private void waitTargetPos() {
    for (BodyPart service : getBodyParts()) {
      service.waitTargetPos();
    }
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
      loaded = Utils.loadPythonFile(f.getAbsolutePath());
    }
    return true;
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

  // ---------------------------------------------------------------
  // END setter & getter
  // ---------------------------------------------------------------
}