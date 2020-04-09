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

  public Servo startServo(String port) {
    return startServo(port, 3);
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
