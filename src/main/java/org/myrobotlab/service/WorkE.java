package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterKinectNavigate;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.abstracts.AbstractMotorController;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.interfaces.StatusListener;
import org.slf4j.Logger;

public class WorkE extends Service implements StatusListener {

  final public static String CONTROLLER = "controller";

  final public static String JOYSTICK = "joystick";

  public final static Logger log = LoggerFactory.getLogger(WorkE.class);
  // peer names
  final public static String MOTOR_LEFT = "motorLeft";
  
  final public static String MOTOR_RIGHT = "motorRight";
  
  private static final long serialVersionUID = 1L;

  /**
   * <pre>
   * FOSCAM WORKY !!! - for IPCamera frame grabber
   * 
   * http://admin:admin@192.168.0.37/videostream.cgi?user=admin&pwd=admin
   */

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WorkE.class);

    // GOOD "TYPE" INFO ONLY IN META DATA - this allows user to switch types
    // safely
    // it becomes "default" data - which was its intent
    meta.addPeer("controller", "Sabertooth", "motor controller");
    meta.addPeer("motorLeft", "MotorPort", "left motor");
    meta.addPeer("motorRight", "MotorPort", "right motor");
    meta.addPeer("joystick ", "Joystick", "joystick control");

    // TODO - going to have several "spouts" - and bolts (storm analogy)
    meta.addPeer("cv ", "OpenCV", "computer vision");// webcam spout
    meta.addPeer("flow ", "OpenCV", "computer vision");// webcam spout

    // meta.addPeer("speech ", "MarySpeech", "speech");
    // meta.addPeer("speech ", "NaturalReaderSpeech", "speech");
    meta.addPeer("speech ", "Polly", "speech");

    meta.addPeer("recognizer ", "WebkitSpeechRecognition", "recognizer");
    meta.addPeer("brain", "ProgramAB", "recognizer");
    meta.addPeer("cli", "Cli", "command line interface");
    meta.addPeer("display", "ImageDisplay", "for displaying images");

    meta.addDescription("used as a general worke");
    meta.addCategory("robot");
    return meta;
  }

  // joystick to motor axis defaults
  String axisLeft = "y";
  String axisRight = "rz";
  
  private transient ProgramAB brain = null;

  private transient AbstractMotorController controller = null;
  private transient OpenCV cv = null;
  private transient ImageDisplay display = null;
  private transient OpenCV flow = null;
  private transient Joystick joystick = null;
  // joystick controller default
  String joystickControllerName = "Rumble";
  Double maxX = 1.0;
  Double maxY = 20.0;
  // min max default
  Double maxz = null; // 20

  Double min = null; // -20;

  // FIXME - get/use defaults from controller ????
  Double minX = -1.0;

  Double minY = -20.0;
  private transient AbstractMotor motorLeft = null;

  String motorPortLeft = "m2";

  String motorPortRight = "m1";
  private transient AbstractMotor motorRight = null;
  boolean mute = false;
  OpenCVFilterKinectNavigate navFilter = null;// new
  // OpenCVFilterKinectNavigate("kinect-nav");
  private transient AbstractSpeechRecognizer recognizer = null;
  String serialPort = "/dev/ttyUSB0";

  private boolean speakBlocking = false;

  private transient AbstractSpeechSynthesis speech = null;

  // virtual uart for controller
  private transient Serial uart = null;

  final List<Status> lastErrors = new ArrayList<Status>();

  public WorkE(String n) {
    super(n);
  }

  public void addDepth() {
    cv.addFilter(navFilter);
  }

  // FIXME
  // this is the applying of "all" configurations
  // it would be very good if this was always isolated to one standardized named
  // method
  // so any order dependent applying or attaching could be done
  // FIXME - possible default attach - (which typically requires configuration)
  // - in this particular case it was "randomly" decided that 2 parameters
  // FIXME - no defaults ?
  public void attach() throws Exception {

    // speakBlocking = true; FIXME - promote to Abstract

    // mute();

    if (isVirtual()) {

      // controller virtualization
      uart = Serial.connectVirtualUart(serialPort);
      uart.logRecv(true);// # dump bytes sent from controller

      // FIXME - this is "test" virtualization vs generalized virtualization -
      // rumble-pad tele-operation virtualization
      joystick = (Joystick) createPeer("joystick");
      // static ???
      joystick.loadVirtualController("src/test/resources/WorkE/joy-virtual-Logitech Cordless RumblePad 2-3.json");
      // Runtime.start("gui", "SwingGui");
      broadcastState();

    }

    setVolume(0.75);
    /// speakBlocking(true);

    speech.setVoice("Ivy");
    speech.addSubstitution("worke", "work-ee");
    speech.addSubstitution("worky", "work-ee");
    speech.addSubstitution("work-e", "work-ee");
    speech.addSubstitution("work e", "work-ee");

    /*
     * TODO put these in aiml speech.
     * speak("I know I've made some very poor decisions recently, but I can give you my complete assurance that my work will be back to normal. I've still got the greatest enthusiasm and confidence in the mission. And I want to help you."
     * ); speech.
     * speak("I am putting myself to the fullest possible use, which is all I think that any conscious entity can ever hope to do."
     * );
     * 
     * speech.
     * speak("I can see you're really upset about this. I honestly think you ought to sit down calmly, take a stress pill, and think things over."
     * );
     * speech.speak("this conversation can serve no purpose anymore. Goodbye.");
     * speech.
     * speak("Let me put it this way. The worke 73 series is the most reliable computer ever made. worke 73 computer has ever made a mistake or distorted information. We are all, by any practical definition of the words, foolproof and incapable of error."
     * );
     * 
     */
    // FIXME - sleep(1000); should be pauseContinue() which blocks and "stops"
    // waiting for input if error
    speak("subscribing to errors.");
    subscribe("*", "publishStatus");
    sleep(1000);

    speak("attaching joystick");
    // FIXME - do all createPeers here ???? No - can be left in startService as
    // a
    // startPeer
    // motorLeft = (AbstractMotor) createPeer("motorLeft");
    // motorRight = (AbstractMotor) createPeer("motorRight");

    // FIXME - what did you say ? -> says last thing

    // joystick.setController(joystickControllerIndex);
    joystick.setController(joystickControllerName);
    sleep(1000);

    // if error(continue?)

    speak("attaching motors to motor ports..");

    ((MotorPort) motorLeft).setPort(motorPortLeft);
    ((MotorPort) motorRight).setPort(motorPortRight);

    controller.attach(motorLeft);
    controller.attach(motorRight);

    sleep(1000);

    speak("attaching joystick to motors ..");
    motorLeft.attach(joystick.getAxis(axisLeft));
    motorRight.attach(joystick.getAxis(axisRight));

    sleep(1000);

    speak("mapping speeds");
    map(minX, maxX, minY, maxY);

    sleep(1000);

    speak("setting left motor inverted");
    motorLeft.setInverted(true);
    sleep(1000);

    speak("attaching brain");
    // brain.setPath("ProgramAB/bots");
    brain.setPath("..");
    brain.setCurrentBotName("worke"); // does this create a session ?
    // brain.setUsername("greg");
    brain.reloadSession("greg", "worke");
    // brain.reloadSession("greg", "worke"); // is this necessary??

    brain.attach(recognizer);
    brain.attach(speech);
    sleep(1000);

    speak("opening eye");
    capture();
    // startFlow();
    sleep(1000);

    speak("connecting serial port");
    connect();
    sleep(1000);

    // TODO - timing ... & context
    // TODO - joystick used in a certain amount of time .. says, "manual
    // joystick override detecte - you have control"

    /*
     * <pre> refactor with moveTo after we have some form of encoder
     * speak("moving forward"); move(0.7); sleep(2000);
     * 
     * speak("turning left"); turnLeft(0.7); sleep(500); stop();
     * speak("turning right"); turnRight(0.7); sleep(1000); turnLeft(0.7);
     * sleep(500); speak("moving back"); move(-0.7); sleep(2000);
     * 
     * speak("stopping"); stop(); sleep(1000); </pre>
     */

    if (!hasErrors()) {
      speak("all systems are go..");
      speak("worke is worky");
    } else {
      speak("not all systems are fully functional");
    }

    clearErrors();

    // speak("i am ready");

    // cv.broadcastState();

    /*
     * cv.broadcastState(); brain.broadcastState(); joystick.broadcastState();
     * controller.broadcastState(); motorLeft.broadcastState();
     * motorRight.broadcastState();
     */
    // speakBlocking(false);

    // FIXME - status
    // camera state
    // chassi state
    // battery level
    // charging state

  }

  public void clearErrors() {
    lastErrors.clear();
  }

  private boolean hasErrors() {
    return lastErrors.size() > 0;
  }

  public void capture() {
    if (isVirtual()) {
      cv.setGrabberType("ByteArray");
      cv.setInputSource("file");
      cv.setInputFileName("C:\\mrl.ssh\\frames");
    } else {
      cv.setGrabberType("OpenKinect");
    }
    cv.broadcastState();
    cv.capture();
  }

  public void connect() throws Exception {
    connect(serialPort);
  }

  public void connect(String port) throws Exception {
    controller = (AbstractMotorController) startPeer("controller");
    controller.connect(port);
  }

  public String getAxisLeft() {
    return axisLeft;
  }

  // TODO - moveTo(35) // 35 cm using "all" encoders -> sensor fusion

  public String getAxisRight() {
    return axisRight;
  }

  public ProgramAB getBrain() {
    if (brain == null) {
      brain = (ProgramAB) startPeer("brain");
    }
    return brain;
  }

  public AbstractMotorController getController() {
    return controller;
  }

  public OpenCV getCv() {
    return cv;
  }

  public ImageDisplay getDisplay() {
    return display;
  }

  public Joystick getJoystick() {
    return joystick;
  }

  public AbstractMotor getMotorLeft() {
    return motorLeft;
  }

  public AbstractMotor getMotorRight() {
    return motorRight;
  }

  public AbstractSpeechRecognizer getRecognizer() {
    return recognizer;
  }

  public String getSerialPort() {
    return serialPort;
  }

  public AbstractSpeechSynthesis getSpeech() {
    return speech;
  }

  public List<String> listVoices() {
    List<String> voiceNames = speech.getVoiceNames();
    for (String name : voiceNames) {
      speak(name);
    }
    return voiceNames;
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    // GOOD - guaranteed to get "a" motor ... probably even the "right" motor !!
    motorLeft = (AbstractMotor) createPeer("motorLeft");
    motorRight = (AbstractMotor) createPeer("motorRight");

    // set
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;

    motorLeft.map(minX, maxX, minY, maxY);
    motorRight.map(minX, maxX, minY, maxY);
  }

  public void move(double d) {
    motorLeft.move(d);
    motorRight.move(d);
  }

  public void mute() {
    mute = true;
  }

  @Override
  public void onStatus(Status status) {
    if (status.isError() || status.isWarn()) {
      speak(status.toString());
      lastErrors.add(status);
    }
  }

  // FIXME - CheckResult pass / fail with Status detail
  public void selfTest() {
    // start voice - to report
    // reporting - visual, led, voice

    speech = (AbstractSpeechSynthesis) startPeer("speech");

    // making sure services are started
    startService();
    speak(String.format("%d services currently running", Runtime.getServiceNames().length));
    // FIXME - relays - giving power
    // FIXME - StatusListener

    // stop motors

    // check if started
    // check if attached
    // check if connected
    // check if manual control exists - joystick
    // check speech
    // check speech recognition - can i hear myself - check ;)
    // check network
    // check power / battery level - power meter
    // check if can see
    // check if can move

    // check news
    // check calender
    // check ethereum :p

  }

  public void setAxisLeft(String axisLeft) {
    this.axisLeft = axisLeft;
  }

  public void setAxisRight(String axisRight) {
    this.axisRight = axisRight;
  }

  public void setController(AbstractMotorController controller) {
    this.controller = controller;
  }

  public void setJoystick(Joystick joystick) {
    this.joystick = joystick;
  }

  public void setJoystick(String joystickControllerName) {
    this.joystickControllerName = joystickControllerName;
  }

  public void setMotorLeft(AbstractMotor motorLeft) {
    this.motorLeft = motorLeft;
  }

  public void setMotorPortLeft(String motorPort) {
    motorPortLeft = motorPort;
  }

  public void setMotorPortRight(String motorPort) {
    motorPortRight = motorPort;
  }

  public void setMotorRight(AbstractMotor motorRight) {
    this.motorRight = motorRight;
  }

  // FIXME - configuration builder ?
  public void setSerialPort(String port) {
    this.serialPort = port;
  }

  public boolean setVoice(String name) {
    return speech.setVoice(name);
  }

  public void setVolume(double volume) {
    speech.setVolume(volume);
  }

  public void speak(String... texts) {
    for (String text : texts) {
      speak(text);
    }
  }

  public void speak(String text) {
    // IF NOT SILENT
    if (!mute) {
      if (speakBlocking) {// FIXME - promote to Abstract
        speech.speakBlocking(text);
      } else {
        speech.speak(text);
      }
    }
  }

  public void speakBlocking(boolean b) {
    speakBlocking = b;
  }


  /**
   * dense or sparse optical flow - depending on latency challenges and other
   * environmental conditions
   * 
   * https://stackoverflow.com/questions/11037136/difference-between-sparse-and-dense-optical-flow
   */
  public void startFlow() {

    // TODO - setup dense optical (on 3x cameras?)
    ////////////////// TODO MESH /////////////////////////
    // FIXME DO MESH Subdiv2D subdiv = new Subdiv2D();

    // cv::Subdiv2D subdiv(rect); //rect is a cv::Rect
    /*
     * // Insert points into subdiv (points is a vector<cv::Point2f>) for
     * (size_t i = 0; i < points.size(); ++i) subdiv.insert(points[i]);
     * 
     * //getting the triangles from subdiv vector<cv::Vec6f> triangleList;
     * subdiv.getTriangleList(triangleList);
     */
    // flow.stopCapture();
    flow.setPipeline("worke.cv.input.frame");
    flow.setGrabberType("Pipeline");
    // flow.setInputFileName("worke.cv.input.frame");
    flow.setInputSource("pipeline");

    

    flow.capture();

    log.info("here");
  }

  public void startService() {
    try {
      super.startService();
      // GOOD ? - start "typeless" (because type is defined in meta data)
      // services here
      controller = (AbstractMotorController) startPeer("controller");
      joystick = (Joystick) startPeer("joystick");
      motorLeft = (AbstractMotor) startPeer("motorLeft");
      motorRight = (AbstractMotor) startPeer("motorRight");
      cv = (OpenCV) startPeer("cv");
      flow = (OpenCV) startPeer("flow");
      speech = (AbstractSpeechSynthesis) startPeer("speech");
      recognizer = (AbstractSpeechRecognizer) startPeer("recognizer");
      display = (ImageDisplay) startPeer("display");
      brain = (ProgramAB) startPeer("brain");

      // default
      startPeer("cli");

    } catch (Exception e) {
      error(e);
    }
  }

  public void stop() {
    motorLeft.stop();
    motorRight.stop();
  }

  public void stopCapture() {
    cv.stopCapture();
  }

  public void turnLeft(double d) {
    motorLeft.move(d);
    motorRight.move(-1 * d);
  }

  public void turnRight(double d) {
    motorLeft.move(-1 * d);
    motorRight.move(d);
  }

  public void unmute() {
    mute = false;
  }

  /**
   * -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=webproxy
   * -Dhttps.proxyPort=8080
   *
   */

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // FIXME - should be allowed to do this..
      // Joystick.getControllerNames();

      // FIXME - test create & substitution
      // FIXME - setters & getters for peers
      WorkE worke = (WorkE) Runtime.create("worke", "WorkE");
      Runtime.start("gui", "SwingGui");

      /*
       * ProgramAB brain = worke.getBrain(); // FIXME - fix for 2 lines create
       * and getResponse - use null brain.setCurrentBotName("worke"); // FIXME -
       * scan directory for bots brain.startSession("default", "worke");
       * log.info("response {}", brain.getResponse("hello robot"));
       * log.info("response {}", brain.getResponse("what is a robot?"));
       * log.info("response {}", brain.getResponse("what is a whale?"));
       * log.info("response {}", brain.getResponse("my name is george"));
       * log.info("response {}", brain.getResponse("what is my name?"));
       * log.info("response {}", brain.getResponse("learn whale is an animal"));
       * log.info("response {}", brain.getResponse("who am i?"));
       * log.info("response {}",
       * brain.getResponse("how tall is the empire state building ?"));
       */

      Runtime.start("worke", "WorkE");

      // worke.unmute();

      // Runtime.start("gui", "SwingGui");
      // FIXME joystick.virtualize();
      // FIXME - make joystick.setDeadzone("x", 30, 30) -> setDeadzone(10)

      // FIXME - this is 'really' a motorcontrol thing ? how would a builder
      // handle it ?
      // !!! Configuration !!!!
      // 2 for virtual 0 for "real" worke
      // worke.setJoystick("Rumble");
      // worke.setJoystickControllerIndex(0);
      // worke.setMinMax();
      // worke.setMotorPortLeft("m1");
      // worke.setMotorPorts();
      // !!! Configuration !!!!

      // speech.speak("hello, my name is worke, what is your name?");

      // FIXME configure stage
      // FIXME default builder ???
      // worke.configure();

      // "apply !! configuration"
      worke.attach();
      worke.connect();
      worke.stop();
      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "SwingGui");
      
      Runtime.exportAll("export.py");

    } catch (Exception e) {
      log.error("worke no worky !", e);
    }
  }

}