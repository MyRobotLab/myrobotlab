package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.myrobotlab.service.interfaces.ServoControl;

public class Intro extends Service {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Intro.class);

  public Intro(String n, String id) {
    super(n, id);
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

    ServiceType meta = new ServiceType(Intro.class);
    meta.addDescription("Introduction to MyRobotlab");
    meta.setAvailable(true); 
    meta.addCategory("general");
    meta.addPeer("servo", "Servo", "servo");
    meta.addPeer("controller", "Arduino", "Arduino controller for this servo");
    return meta;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("intro", "Intro");
      Runtime.start("servo", "Servo");
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  transient ServoControl servo;

  boolean isServoActivated = false;

  public boolean isServoActivated() {
    return isServoActivated;
  }
  

  public Servo(String n, String id) throws Exception {
    super(n, id);

    startPeers();

    servo.setPin(3);

    servo.setMinMax(0.0, 180.0);

    servo.setRest(90.0);

    servo.setPosition(90.0);

    setSpeed(100.0);
  }

  public void broadcastState() {
    super.broadcastState();
    servo.broadcastState();
  }

  public void disable() {
    servo.disable();
  }

  public void enable() {
    sleep(Intro.attachPauseMs);
    servo.enable();
  }

  public void fullSpeed() {
    servo.fullSpeed();
  }

  public ServoControl getServo() {
    return servo;
  }

  @Override
  public boolean save() {
    super.save();
    servo.save();
    return true;
  }

  public void setAutoDisable(Boolean idleTimeoutMs) {
    servo.setAutoDisable(idleTimeoutMs);
  }

  public void setLimits(double servoMin, double servoMax) {
    servo.setMinMax(servoMin, servoMax);
  }

  public void setServo(ServoControl servo) {
    this.servo = servo;
  }

  public void setSpeed(Double servo) {
    this.servo.setSpeed(servo);
  }

    public void stop() {
    servo.stop();
  }

  public void waitTargetPos() {
    servo.waitTargetPos();
  }



  public Servo startServo(String port) {
    return startServo(port, pin);
  }

  public Servo startServo(String port, int pin) {
  
    if (servo == null) {
      speakBlocking("starting servo");
      isServoActivated = true;

      servo = (Servo) startPeer("servo");

      if (port != null) {
        try {
          speakBlocking(port);
          Arduino controller = (Arduino) startPeer("controller");
          controller.connect(port);
          controller.attach(controller, pin);
        } catch (Exception e) {
          error(e);
        }
      }
    }
    return servo;
  }

  public void stopServo() {
    speakBlocking("stopping servo");
    releasePeer("servo");
    isServoActivated = false;
  }

}
