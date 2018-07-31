package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.slf4j.Logger;

public class WorkE extends Service {

  public final static Logger log = LoggerFactory.getLogger(WorkE.class);

  private static final long serialVersionUID = 1L;

  
  private transient Joystick joystick = null;
  int joystickControllerIndex = 0;
  private transient AbstractMotor left = null;

  String port = "/dev/ttyUSB0";

  private transient AbstractMotor right = null;

  private transient Sabertooth sabertooth = null;

  public WorkE(String n) {
    super(n);
  }

  public void attach() throws Exception {
    attach("y", "rz");
  }

  // FIXME - possible default attach - (which typically requires configuration)
  public void attach(String leftAxis, String rightAxis) throws Exception {
    joystick.setController(joystickControllerIndex);
    sabertooth.attach(left);
    sabertooth.attach(right);
    left.attach(joystick.getAxis("y"));
    right.attach(joystick.getAxis("rz"));
  }

  public Joystick getJoystick() {
    return joystick;
  }

  public AbstractMotor getLeft() {
    return left;
  }

  public AbstractMotor getRight() {
    return right;
  }

  public Sabertooth getSabertooth() {
    return sabertooth;
  }

  public void setInverted(boolean b) {
    left.setInverted(b);
    right.setInverted(b);
  }

  public void setJoystick(Joystick joystick) {
    this.joystick = joystick;
  }

  public void setJoystickControllerIndex(int joystickControllerIndex) {
    this.joystickControllerIndex = joystickControllerIndex;
  }

  public void setLeft(AbstractMotor left) {
    this.left = left;
  }

  public void setMinMax() {
    setMinMax(-0.20, 0.20);
  }

  public void setMinMax(double min, double max) {
    left.setMinMax(min, max);
    right.setMinMax(min, max);
  }

  // FIXME - possible fix - how to handle "default" builds with MotorPorts ?
  // This one is "most" motor subtype specific
  // FIXME - "setting" the port should be an attribute of the MotorController
  // ????
  public void setMotorPorts() {
    ((MotorPort) left).setPort("m1");
    ((MotorPort) right).setPort("m2");
  }

  public void setPort(String port) {
    this.port = port;
  }

  public void setRight(AbstractMotor right) {
    this.right = right;
  }

  public void setSabertooth(Sabertooth sabertooth) {
    this.sabertooth = sabertooth;
  }

  public void startService() {
    try {
      super.startService();
      // GOOD ? - start "typeless" (because type is defined in meta data) services here
      sabertooth = (Sabertooth) startPeer("sabertooth");
      joystick = (Joystick) startPeer("joystick");
      left = (AbstractMotor) startPeer("left");
      right = (AbstractMotor) startPeer("right");
    } catch (Exception e) {
      error(e);
    }
  }

  public void virtualize() throws IOException {
    Serial uart = Serial.connectVirtualUart(port);
    uart.logRecv(true);// # dump bytes sent from sabertooth
  }
  
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

    // GOOD "TYPE" INFO ONLY IN META DATA - this allows user to switch types safely
    // it becomes "default" data - which was its intent
    meta.addPeer("sabertooth", "Sabertooth", "motor controller");
    meta.addPeer("left", "MotorPort", "left motor");
    meta.addPeer("right", "MotorPort", "right motor");
    meta.addPeer("joystick ", "Joystick", "joystick control");

    meta.addDescription("used as a general worke");
    meta.addCategory("robot");
    return meta;
  }
  
  public void connect() throws Exception {
    connect(port);
  }

  public void connect(String port) throws Exception {
    sabertooth = (Sabertooth)startPeer("sabertooth");
    sabertooth.connect(port);
  }
  
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      
      // FIXME - should be allowed to do this..
      // Joystick.getControllerNames();

      // FIXME - test create & substitution
      // FIXME - setters & getters for peers
      WorkE worke = (WorkE) Runtime.start("worke", "WorkE");
      // FIXME joystick.virtualize();
      // FIXME - make joystick.setDeadzone("x", 30, 30) -> setDeadzone(10)
      // 2 for virtual 0 for "real" worke
      // worke.setJoystickControllerIndex(2);
      worke.setJoystickControllerIndex(0);
      // worke.setPort("/dev/ttyUSB0");
      // worke.virtualize();
      // FIXME - this is 'really' a motorcontrol thing ? how would a builder
      // handle it ?
      worke.setMotorPorts();
      
      // FIXME configure stage
      // FIXME default builder ???
      // worke.configure();
      
      worke.attach();
      worke.connect();

      // Runtime.start("servo", "Servo");
      // Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
  
}