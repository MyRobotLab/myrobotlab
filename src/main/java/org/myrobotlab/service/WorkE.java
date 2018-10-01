package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.abstracts.AbstractMotorController;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.abstracts.AbstractSpeechSynthesis;
import org.slf4j.Logger;

public class WorkE extends Service {

  public final static Logger log = LoggerFactory.getLogger(WorkE.class);

  private static final long serialVersionUID = 1L;

  // peer names
  final public static String MOTOR_LEFT = "motorLeft";
  final public static String MOTOR_RIGHT = "motorRight";

  final public static String JOYSTICK = "joystick";
  final public static String CONTROLLER = "controller";

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
    meta.addPeer("cv ", "OpenCV", "computer vision");
    meta.addPeer("speech ", "MarySpeech", "speech");
    meta.addPeer("recognizer ", "WebkitSpeechRecognition", "recognizer");
    meta.addPeer("brain", "ProgramAB", "recognizer");
    meta.addPeer("cli", "Cli", "commnnd line interface");

    meta.addDescription("used as a general worke");
    meta.addCategory("robot");
    return meta;
  }

  // joystick to motor axis defaults
  String axisLeft = "y";
  String axisRight = "rz";
  // peers references
  private transient Joystick joystick = null;
  private transient AbstractMotor motorLeft = null;
  private transient AbstractMotor motorRight = null;
  private transient AbstractMotorController controller = null;
  private transient OpenCV cv = null;
  private transient AbstractSpeechSynthesis speech = null;
  private transient AbstractSpeechRecognizer recognizer = null;
  private transient ProgramAB brain = null;

  // joystick controller default
  String joystickControllerName = "Rumble";

  // min max default
  Double maxz = null; // 20
  Double min = null; // -20;

  String motorPortLeft = "m2";

  String motorPortRight = "m1";
  String serialPort = "/dev/ttyUSB0";
  // FIXME - get/use defaults from controller ????
  Double minX = -1.0;
  Double maxX = 1.0;
  Double minY = -20.0;
  Double maxY = 20.0;

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
    // FIXME - do all createPeers here ???? No - can be left in startService as
    // a startPeer
    // motorLeft = (AbstractMotor) createPeer("motorLeft");
    // motorRight = (AbstractMotor) createPeer("motorRight");

    // joystick.setController(joystickControllerIndex);
    joystick.setController(joystickControllerName);
    ((MotorPort) motorLeft).setPort(motorPortLeft);
    ((MotorPort) motorRight).setPort(motorPortRight);

    controller.attach(motorLeft);
    controller.attach(motorRight);

    motorLeft.attach(joystick.getAxis(axisLeft));
    motorRight.attach(joystick.getAxis(axisRight));

    map(minX, maxX, minY, maxY);

    motorLeft.setInverted(true);

    brain.attach(recognizer);
    // setInverted(true);
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

  public AbstractMotorController getController() {
    return controller;
  }

  public ProgramAB getBrain() {
    if (brain == null) {
      brain = (ProgramAB) startPeer("brain");
    }
    return brain;
  }

  public OpenCV getCv() {
    return cv;
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

  public void setAxisLeft(String axisLeft) {
    this.axisLeft = axisLeft;
  }

  // FIXME - possible fix - how to handle "default" builds with MotorPorts ?
  // This one is "most" motor subtype specific
  // FIXME - "setting" the port should be an attribute of the MotorController
  // ????
  // FIXME - configuration builder ?
  /*
   * public void setMotorPorts() { ((MotorPort) motorLeft).setPort("m1");
   * ((MotorPort) motorRight).setPort("m2"); }
   */

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
      speech = (AbstractSpeechSynthesis) startPeer("speech");
      recognizer = (AbstractSpeechRecognizer) startPeer("recognizer");
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

  public Serial virtualize() throws IOException {
    return virtualize("src/test/resources/WorkE/joy-virtual-Logitech Cordless RumblePad 2-3.json");
  }

  public Serial virtualize(String virtualJoystickDefinitionFile) throws IOException {

    // controller virtualization
    Serial uart = Serial.connectVirtualUart(serialPort);
    uart.logRecv(true);// # dump bytes sent from controller

    // FIXME - this is "test" virtualization vs generalized virtualization -
    // rumble-pad tele-operation virtualization
    joystick = (Joystick) createPeer("joystick");
    joystick.loadVirtualController(virtualJoystickDefinitionFile);
    broadcastState();
    return uart;
  }

  // FIXME - CheckResult pass / fail with Status detail
  public void checkSystems() {
    // start voice - to report
    // reporting - visual, led, voice

    speech = (AbstractSpeechSynthesis) startPeer("speech");

    // making sure services are started
    startService();

    // FIXME - relays - giving power

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

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.WARN);

      // FIXME - should be allowed to do this..
      // Joystick.getControllerNames();

      // FIXME - test create & substitution
      // FIXME - setters & getters for peers
      WorkE worke = (WorkE) Runtime.create("worke", "WorkE");
      Runtime.start("gui", "SwingGui");

      ProgramAB brain = worke.getBrain();
      brain.setCurrentBotName("worke");
      log.info("response {}", brain.getResponse("hello there"));
      log.info("response {}", brain.getResponse("what is your name?"));
      log.info("response {}", brain.getResponse("who am i?"));
      log.info("response {}", brain.getResponse("how tall is the empire state building ?"));

      Runtime.start("worke", "WorkE");
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

      AbstractSpeechSynthesis speech = worke.getSpeech();
      speech.speak("hello, my name is worke");

      // FIXME configure stage
      // FIXME default builder ???
      // worke.configure();

      // "apply !! configuration"
      worke.attach();
      worke.connect();
      worke.stop();
      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("worke no worky !", e);
    }
  }
}