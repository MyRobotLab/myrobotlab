package org.myrobotlab.service;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

// TODO: This test takes too long to run, ignoring it from the build.
@Ignore
public class MotorTest2 {

  public final static Logger log = LoggerFactory.getLogger(MotorTest2.class);

  static MotorDualPwm motor01 = null;
  static Arduino arduino = null;

  @Before
  public void setUp() {
    LoggingFactory.init("WARN");
  }

  @Test
  public void testMove() throws Exception {
    Runtime.start("webgui", "WebGui");
    arduino = (Arduino)Runtime.start("arduino", "Arduino");
    motor01 = (MotorDualPwm)Runtime.start("motor01", "MotorDualPwm");
    motor01.setPwmPins(3, 4);
    motor01.attachMotorController(arduino);

    arduino.connect("COM5"); 


    motor01.move(0.3);
    motor01.move(0.1);
    motor01.move(1.0);
    motor01.move(5.0);

    motor01.move(-0.1);
    motor01.move(-0.2);
    motor01.move(0.0);
    motor01.stop();

    motor01.save();
    motor01.load();

    motor01.detachMotorController(arduino);
  }

}