package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.abstracts.AbstractMotorController;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.myrobotlab.service.interfaces.StatusListener;
import org.slf4j.Logger;

/**
 * <pre>
 * FIXME  
 *        - auto-start peers
 *        - "easy" sensor fusion -  multi-callback channels lead to a single channel
 *        - FIXME - aiml integration
 *        - FIXME - running in mute mode - "Hello Greg, I had a problem starting today/most recent update/this evening - would you like to hear the log?"
 *        - FIXME - sorry to bother you, but I have several problems - could I tell you what they are ?"
 *        - virtual joystick 
 *        - FULL path displayed in ProgramAB "bots" 
 *        - jostick control of motors !!!! get it f*ing moving again !! 
 *        - NAVIGATE !!
 *        - FIXME - moveTo(35) // 35 cm using "all" encoders send to sensor fusion
 *        
 * -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=8080 -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=8080
 *  FOSCAM WORKY !!! - for IPCamera frame grabber
 * 
 * http://admin:admin@192.168.0.37/videostream.cgi?user=admin(and)pwd=admin
 *
 * </pre>
 * @author GroG
 *
 */
public class WorkE extends Service implements StatusListener {

  public final static Logger log = LoggerFactory.getLogger(WorkE.class);

  static final long serialVersionUID = 1L;

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(WorkE.class);

    // motor control - output
    meta.addPeer("joystick ", "Joystick", "joystick control");
    meta.addPeer("controller", "Sabertooth", "motor controller");
    meta.addPeer("motorLeft", "MotorPort", "left motor");
    meta.addPeer("motorRight", "MotorPort", "right motor");

    // vision - input
    // TODO - going to have several "spouts" - and bolts (storm analogy)
    meta.addPeer("cv ", "OpenCV", "computer vision");// webcam spout
    // meta.addPeer("leftFoscam ", "OpenCV", "computer vision");// webcam spout

    // speech - output
    meta.addPeer("mouth ", "Polly", "mouth");

    // ear - input
    meta.addPeer("ear", "WebkitSpeechRecognition", "ear");
    
    // brain - input/output
    meta.addPeer("brain", "ProgramAB", "ear");

    // meta.addPeer("cli", "Cli", "command line interface");
    // emoji - output
    meta.addPeer("emoji", "Emoji", "emotional state machine");

    meta.addDescription("the worke bot !");
    meta.addCategory("robot");
    return meta;
  }

  public static void main(String[] args) {
    try {

      // LoggingFactory.init(Level.INFO);
      // Platform.setVirtual(true);
      // Runtime.getInstance(new String[] {"--virtual", "-l", "info"}); 
      
      // allows the runtime to be configured by cmdline
      Runtime.getInstance(args); 

      /*
       * Polly polly = (Polly)Runtime.start("polly", "Polly");
       * polly.getKeyNames(); Security security = Runtime.getSecurity();
       * 
       * security.setKey("amazon.polly.user.key", "xxx");
       * security.setKey("amazon.polly.user.secret", "xxx");
       */

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

      // mouth.speak("hello, my name is worke, what is your name?");

      // FIXME configure stage
      // FIXME default builder ???
      // worke.configure();

      // "apply !! configuration"
      worke.attach();
      worke.connect();
      worke.stop();
      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "SwingGui");

      Runtime.exportAll("worke.py");

    } catch (Exception e) {
      log.error("worke no worky !", e);
    }
  }

  // joystick to motor axis defaults
  String axisLeft = "y";
  String axisRight = "rz";

  // peers
  transient AbstractMotor motorRight;
  transient AbstractSpeechSynthesis mouth;
  transient ProgramAB brain;
  transient AbstractMotorController controller;
  transient OpenCV cv;
  transient ImageDisplay display;
  transient AbstractSpeechRecognizer ear;
  transient Emoji emoji;
  transient FiniteStateMachine fsm;
  transient Joystick joystick;
  transient AbstractMotor motorLeft;
  // virtual uart for controller
  transient Serial uart = null;

  // joystick controller default
  String joystickControllerName = "Rumble";

  final List<Status> lastErrors = new ArrayList<Status>();
  Double maxX = 1.0;
  Double maxY = 20.0;

  // FIXME - get/use defaults from controller ????
  Double minX = -1.0;
  Double minY = -20.0;

  String motorPortLeft = "m2";
  String motorPortRight = "m1";

  boolean mute = false;

  String serialPort = "/dev/ttyUSB0";

  boolean speakBlocking = false;

  public WorkE(String n) {
    super(n);
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

    // FIXME - running in mute mode - "Hello Greg, I had a problem starting today/most recent update/this evening - would you like to hear the log?"
    // FIXME - sorry to bother you, but I have several problems - could I tell you what they are ?"

    setVolume(0.75);
    /// speakBlocking(true);

    mouth.setVoice("Brian"); // Brian
    mouth.addSubstitution("worke", "work-ee");
    mouth.addSubstitution("worky", "work-ee");
    mouth.addSubstitution("work-e", "work-ee");
    mouth.addSubstitution("work e", "work-ee");
    
    if (isVirtual()) {
      speak("running in virtual mode");
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

    /*
     * TODO put these in aiml mouth.
     * speak("I know I've made some very poor decisions recently, but I can give you my complete assurance that my work will be back to normal. I've still got the greatest enthusiasm and confidence in the mission. And I want to help you."
     * ); mouth.
     * speak("I am putting myself to the fullest possible use, which is all I think that any conscious entity can ever hope to do."
     * );
     * 
     * mouth.
     * speak("I can see you're really upset about this. I honestly think you ought to sit down calmly, take a stress pill, and think things over."
     * );
     * mouth.speak("this conversation can serve no purpose anymore. Goodbye.");
     * mouth.
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
    brain.setPath("..");
    brain.setCurrentBotName("worke"); // does this create a session ?
    // brain.setUsername("greg");
    brain.reloadSession("greg", "worke");
    // brain.reloadSession("greg", "worke"); // is this necessary??

    speak("attaching ear to brain");
    brain.attach(ear);
    
    speak("attaching mouth to brain");
    brain.attach(mouth);
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
      speak("would you like a list of things not working?");
      // aiml Yes,Sure,Ok,Go For it,Fine || No, Nope, nuh uh, naw ?

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

  public void clearErrors() {
    lastErrors.clear();
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
    return ear;
  }

  public String getSerialPort() {
    return serialPort;
  }

  public AbstractSpeechSynthesis getSpeech() {
    return mouth;
  }

  boolean hasErrors() {
    return lastErrors.size() > 0;
  }

  public List<String> listVoices() {
    List<String> voiceNames = mouth.getVoiceNames();
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

    mouth = (AbstractSpeechSynthesis) startPeer("mouth");

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
    // check mouth
    // check mouth recognition - can i hear myself - check ;)
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

  public void setJoystick(String joystickControllerName) {
    this.joystickControllerName = joystickControllerName;
  }

  public void setMotorPortLeft(String motorPort) {
    motorPortLeft = motorPort;
  }

  public void setMotorPortRight(String motorPort) {
    motorPortRight = motorPort;
  }

  // FIXME - configuration builder ?
  public void setSerialPort(String port) {
    this.serialPort = port;
  }

  // aiml - "set voice to bob"
  public boolean setVoice(String name) {
    return mouth.setVoice(name);
  }

  // aiml - "set volume 30 percent"
  public void setVolume(double volume) {
    mouth.setVolume(volume);
  }

  public void speak(String text) {
    // IF NOT SILENT
    if (!mute) {
      if (speakBlocking) {// FIXME - promote to Abstract
        mouth.speakBlocking(text);
      } else {
        mouth.speak(text);
      }
    }
  }

  public void speakBlocking(boolean b) {
    speakBlocking = b;
  }

  public void startService() {
    try {
      super.startService();
      // GOOD ? - start "typeless" (because type is defined in meta data)
      // services here

      // FIXME FIXME FIXME - make worky through framework - no manual starts
      // here !!!
      controller = (AbstractMotorController) startPeer("controller");
      joystick = (Joystick) startPeer("joystick");
      motorLeft = (AbstractMotor) startPeer("motorLeft");
      motorRight = (AbstractMotor) startPeer("motorRight");
      cv = (OpenCV) startPeer("cv");
      mouth = (AbstractSpeechSynthesis) startPeer("mouth");
      ear = (AbstractSpeechRecognizer) startPeer("ear");
      emoji = (Emoji) startPeer("emoji");
      display = emoji.getDisplay();// (ImageDisplay) startPeer("display");
      fsm = emoji.getFsm();
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
}