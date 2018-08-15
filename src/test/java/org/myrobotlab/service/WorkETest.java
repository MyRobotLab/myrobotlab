package org.myrobotlab.service;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class WorkETest {

  public final static Logger log = LoggerFactory.getLogger(WorkETest.class);

  static WorkE worke = null;
  static SwingGui swing = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    if (!Runtime.isHeadless()) {
      // swing = (SwingGui) Runtime.start("swing", "SwingGui");
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public final void integrationTest() throws Exception {

    // create initial service -- allows substitution and configuration before
    // "starting"
    worke = (WorkE) Runtime.create("worke", "WorkE");

    // opportunity to do substitutions
    // Joystick joy = (Joystick)Runtime.start("custom", "Joystick");
    // worke.setJoystick(joy);

    // set configuration
    // worke.virtualize("someController.json");
    worke.virtualize();

    // worke.setLeftAxis(leftAxis);
    Joystick joystick = worke.getJoystick();
    // log.info("{}", Arrays.toString(joystick.getControllerNames()));
    // joystick.setController("RumblePad");

    // worke.setJoystickControllerIndex(3); // TODO by name ?

    // start the services
    worke.startService();

    Runtime.start("gui", "SwingGui");

    // attach with existing configuration
    worke.attach();

    // connect
    worke.connect();

    // test
    String lefAxis = worke.getAxisLeft();
    String righAxis = worke.getAxisRight();

    joystick.moveTo(lefAxis, 0.16);
    joystick.moveTo(lefAxis, 0.32);
    joystick.moveTo(lefAxis, 0.64);
    joystick.moveTo(lefAxis, 1.0);

    joystick.pressButton("a");

  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("INFO");
      boolean quitNow = false;

      if (quitNow) {
        return;
      }

      // run junit as java app
      JUnitCore junit = new JUnitCore();
      Result result = junit.run(WorkETest.class);
      log.info("Result failures: {}", result.getFailureCount());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
