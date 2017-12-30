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

public class Chassis extends Service {

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

  

  public static void main(String[] args) {
    try {

      LoggingFactory.init("INFO");

      boolean virtual = true;
      //////////////////////////////////////////////////////////////////////////////////
      // Sabertooth.py
      // categories: motor
      // more info @: http://myrobotlab.org/service/Sabertooth
      //////////////////////////////////////////////////////////////////////////////////
      // uncomment for virtual hardware
      // virtual = True

      String port = "COM14";
      //String port = "/dev/ttyUSB0";

      // start optional virtual serial service, used for test
      if (virtual) {
        // use static method Serial.connectVirtualUart to create
        // a virtual hardware uart for the serial service to
        // connect to
        Serial uart = Serial.connectVirtualUart(port);
        uart.logRecv(true); // dump bytes sent from sabertooth
      }
      // start the services
      Runtime.start("gui", "SwingGui");
      Sabertooth sabertooth = (Sabertooth) Runtime.start("sabertooth", "Sabertooth");
      MotorPort m1 = (MotorPort) Runtime.start("m1", "MotorPort");
      MotorPort m2 = (MotorPort) Runtime.start("m2", "MotorPort");
      Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
      // Arduino arduino = (Arduino)Runtime.start("arduino","Arduino");

      // configure services
      m1.setPort("m1");
      m2.setPort("m2");
      joy.setController(5); // 0 on Linux

      // attach services
      sabertooth.attach(m1);
      sabertooth.attach(m2);
      m1.attach(joy.getAxis("y"));
      m2.attach(joy.getAxis("rz"));
      
      m1.setInverted(true);
      m2.setInverted(true);

      sabertooth.connect(port);
      
      Chassis chassis = (Chassis) Runtime.start("m1", "MotorPort");
      // chassis.setLeftMotor(m1);
      // chassis.setRightMotor(m1);

      // m1.stop();
      // m2.stop();

      boolean done = true;
      if (done) {
        return;
      }

      // speed up the motor
      for (int i = 0; i < 100; ++i) {
        double pwr = i * .01;
        log.info("power {}", pwr);
        m1.move(pwr);
        sleep(100);
      }

      sleep(1000);

      // slow down the motor
      for (int i = 100; i > 0; --i) {
        double pwr = i * .01;
        log.info("power {}", pwr);
        m1.move(pwr);
        sleep(100);
      }

      // move motor clockwise
      m1.move(0.3);
      sleep(1000);
      m1.stop();

      // move motor counter-clockwise
      m1.move(-0.3);
      sleep(1);
      m1.stop();

      // TODO - stopAndLock

    } catch (Exception e) {
      Logging.logError(e);
    }
  }
  }