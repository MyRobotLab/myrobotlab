package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.ChassisConfig;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

public class Chassis extends Service<ChassisConfig>
{

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Chassis.class);

  String joystickLeftAxisId;
  String joystickRightAxisId;

  MotorController controller;
  MotorControl left;
  MotorControl right;
  Joystick joystick;

  public Chassis(String n, String id) {
    super(n, id);
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
      // String port = "/dev/ttyUSB0";

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
      m1.setAxis("y");
      m2.setAxis("rz");
      // m1.attach(joy.getAxis("y"));
      // m2.attach(joy.getAxis("rz"));

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