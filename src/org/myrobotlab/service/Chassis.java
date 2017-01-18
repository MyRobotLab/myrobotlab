package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.JoystickData;
import org.myrobotlab.service.interfaces.JoystickListener;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

public class Chassis extends Service implements JoystickListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Chassis.class);

  String joystickLeftAxisId;
  String joystickRightAxisId;

  MotorController controller;

  MotorControl left;
  MotorControl right;
  Joystick joystick;

  public Chassis(String n) {
    super(n);
  }

  public MotorControl getLeft() {
    left = (MotorControl) createPeer("left");
    return left;
  }

  public MotorControl getRight() {
    right = (MotorControl) createPeer("right");
    return right;
  }

  public Joystick getJoystick() {
    joystick = (Joystick) createPeer("joystick");
    return joystick;
  }

  public MotorController getController() {
    controller = (MotorController) createPeer("controller");
    return controller;
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

    ServiceType meta = new ServiceType(Chassis.class.getCanonicalName());
    meta.addDescription("control platform");
    // add dependency if necessary
    // meta.addDependency("org.coolproject", "1.0.0");
    meta.addCategory("general");
    meta.addPeer("left", "Motor", "left drive motor");
    meta.addPeer("right", "Motor", "right drive motor");
    meta.addPeer("joystick", "Joystick", "joystick control");
    meta.addPeer("controller", "Sabertooth", "serial controller");
    return meta;
  }

  @Override
  public void onJoystickInput(JoystickData input) throws Exception {
    log.info(String.format("onJoystickInput %s", input.toString()));
    if (input.id.equals(joystickLeftAxisId)) {
      left.move(input.value);
    } else if (input.id.equals(joystickRightAxisId)) {
      right.move(input.value);
    }

  }

  public void attachJoystick(int index, String leftAxis, String rightAxis) {
    joystick = (Joystick) startPeer("joystick");
    joystick.addInputListener(this);
    joystick.setController(index);
    this.joystickLeftAxisId = leftAxis;
    this.joystickRightAxisId = rightAxis;
    joystick.addInputListener(this);
  }

  public void stop() {
    left.stop();
    right.stop();
  }

  public void connect(String port) throws Exception {
    // if controller type - Aruduino 57600 if Sabertooh 9600
    connect(port, Serial.BAUD_9600);
  }

  public void connect(String port, int rate) throws Exception {
    controller = getController();
    // controller.connect(port, rate, 8, 1, 0);
  }

  public void startService() {
    super.startService();
    left = (MotorControl) startPeer("left");
    right = (MotorControl) startPeer("right");
    joystick = (Joystick) startPeer("joystick");
    controller = (MotorController) startPeer("controller");
  }

  public void attachMotors(int leftPortNumber, int rightPortNumber) throws Exception {
    attachLeftMotor(leftPortNumber);
    attachRightMotor(rightPortNumber);
  }

  public void attachLeftMotor(int portNumber) throws Exception {
    MotorController mc = getController();
    left.attach(mc);
  }

  public void attachRightMotor(int portNumber) throws Exception {
    MotorController mc = getController();
    right.attach(mc);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      Chassis chassis = (Chassis) Runtime.start("chassis", "Chassis");
      chassis.connect("COM19");// , Serial.BAUD_9600);
      // TODO - when not set (error + info on what is wrong - possible
      // selections)
      chassis.attachMotors(2, 1);
      chassis.attachJoystick(5, "y", "rz");
      // Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
