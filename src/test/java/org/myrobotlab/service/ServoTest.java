package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.test.AbstractTest;

/**
 * 
 * @author GroG / kwatters
 * 
 *         FIXME - test one servo against EVERY TYPE OF CONTROLLER (virtualized)
 *         !!!! iterate through all types
 * 
 *         FIXME - what is expected behavior when a s1 is attached at pin 3,
 *         then a new servo s2 is requested to attach at pin 3 ?
 * 
 *         FIXME - test attach and isAttached on every controller
 */
public class ServoTest extends AbstractTest {

  static final String port01 = "servoTestPort";
  Integer pin = 3;
  static Arduino arduino01 = null;
  static Servo servo = null;

  // TODO - disconnected move tests
  // TODO - can more than on servo be attached to the same controller and pin?

  @Before /* start initial state */
  public void setUp() throws Exception {
    servo = (Servo) Runtime.start("servoServoTest", "Servo");
    arduino01 = (Arduino) Runtime.start("arduinoServoTest", "Arduino");
    arduino01.setVirtual(true);
    arduino01.connect(port01);
    servo.setPin(3);
    arduino01.attach(servo);
    servo.rest();
    servo.enable();
    servo.map(0, 180, 0, 180);
    servo.setRest(90.0);
  }

  @AfterClass
  static public void afterClass() throws Exception {
    servo.releaseService();
    arduino01.releaseService();
  }
  
  @Test
  public void autoDisableAfterAttach() {
    // enable servo
    servo.moveTo(100);
    servo.detach();
    assertTrue(!servo.isAttached());
    servo.setAutoDisable(true);
    servo.attach("arduinoServoTest");
    // after attach, must be disabled
    sleep(servo.getIdleTimeout() + 1000);
    assertTrue(!servo.isEnabled());
  }

  @Test
  public void disabledMove() throws Exception {
    // take off speed control
    servo.fullSpeed();
    servo.moveTo(0.0);
    servo.setInverted(false);
    Service.sleep(1000);

    // begin long slow move
    servo.setSpeed(5.0);
    servo.moveTo(180.0);
    Service.sleep(300);
    assertTrue(servo.isMoving());

    // after 1/10 of a second we should be moving
    assertTrue(servo.isMoving());
    double pos = servo.getCurrentInputPos();

    // disable move while it has not completed
    servo.disable();
    Service.sleep(300);
    assertTrue(!servo.isMoving());
    assertTrue(!servo.isSweeping());

    // wait a little after disabling
    // Service.sleep(100);
    double postDisablePos = servo.getCurrentInputPos();

    // servo expected to have stopped and not continued after
    // a control disable() method was called
    assertEquals(pos, postDisablePos, 0.03);

  }

  @Test
  public void invertTest() throws Exception {
    // verify the outputs are inverted as expected.
    servo.moveTo(30.0);
    assertEquals(30.0, servo.getTargetOutput(), 0.001);
    servo.setInverted(true);
    assertEquals(150.0, servo.getTargetOutput(), 0.001);
    servo.moveTo(20.0);
    assertEquals(160.0, servo.getTargetOutput(), 0.001);
    servo.setInverted(false);
    assertEquals(20.0, servo.getTargetOutput(), 0.001);
  }

  @Test
  public void mapTest() throws Exception {
    servo.setInverted(true);
    servo.map(10, 170, 20, 160);
    Mapper mapper = servo.getMapper();
    assertTrue(mapper.isInverted());
    servo.setInverted(false);
    servo.map(20, 160, 10, 170);
    assertFalse(mapper.isInverted());
    assertEquals(20.0, mapper.getMinX(), 0.001);
    assertEquals(160.0, mapper.getMaxX(), 0.001);
    assertEquals(10.0, mapper.getMinY(), 0.001);
    assertEquals(170.0, mapper.getMaxY(), 0.001);
  }

  @Test
  public void testServo() throws Exception {
    // this basic test will create a servo and attach it to an arduino.
    // then detach
    Runtime runtime = Runtime.getInstance();
    runtime.setVirtual(true);

    Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    arduino01.connect(port01);

    Servo s = (Servo) Runtime.start("ser1", "Servo");
    Servo s2 = (Servo) Runtime.start("ser2", "Servo");

    // the pin should always be set to something.
    s.setPin(pin);
    assertEquals(pin + "", s.getPin());
    s.attach(arduino01);

    // maybe remove this interface
    // s.attach(ard1);
    // s.attachServoController(ard1);
    s.disable();

    s.attach(arduino01);

    // This is broken
    // assertTrue(s.controller == ard2);
    s.rest();
    assertEquals(s.getRest(), 90.0, 0.0);

    s.setRest(12.2);
    assertEquals(s.getRest(), 12.2, 0.0);

    s.rest();

    // depricated. i feel like if you do
    s.enable();
    s.moveTo(90.0);
    // test moving to the same position
    s.moveTo(90.0);
    // new position
    s.moveTo(91.0);
    s.disable();

    assertFalse(s.isEnabled());
    s.enable();
    assertTrue(s.isEnabled());

    // detach the servo.
    // ard2.detach(s);
    s.detach(arduino01);

    // detaching an re-attaching requires
    // asynch communication - time is needed
    // to come to 'eventual' synchronized consistency

    Service.sleep(300);
    s.setPin(10);
    s.setPosition(1);
    s.attach(arduino01);
    Service.sleep(300);
    s.enable();
    assertTrue(s.isEnabled());
    s.disable();
    assertFalse(s.isEnabled());

    s2.sync(s);
    s.getCurrentInputPos();
    s2.moveTo(100);
    Service.sleep(300); // FIXME - change to await on change with timeout 
    assertEquals(100.0, s.getCurrentInputPos(), 0.1f);
    s2.moveTo(60);
    Service.sleep(300);// FIXME - change to await on change
    assertEquals(60.0, s.getCurrentInputPos(), 0.1f);
    s2.unsync(s);
    s2.moveTo(50);
    Service.sleep(300);// FIXME - change to await on change
    assertEquals(60.0, s.getCurrentInputPos(), 0.1f);

    s.detach(arduino01);
    s.releaseService();
    arduino01.releaseService();
    s2.releaseService();

  }

  @Test
  public void releaseService() {
    Servo servo01 = (Servo) Runtime.start("servo01", "Servo");
    // Release the servo.
    servo01.releaseService();
    assertNull(Runtime.getService("servo01"));
  }

  @Test
  public void testAutoDisable() throws Exception {
    // Start the test servo
    arduino01 = (Arduino) Runtime.start(arduino01.getName(), "Arduino");
    arduino01.connect(port01);
    servo.detach();
    sleep(300);
    servo.setPin(pin);
    servo.setPosition(0.0);

    arduino01.attach(servo);
    assertTrue("verifying arduino is attached to the servo.", arduino01.isAttached(servo.getName()));
    assertTrue("verifying servo is attached to the arduino.", servo.isAttached(arduino01.getName()));
    servo.moveTo(30.0);
    assertTrue("verifying servo should be enabled", servo.isEnabled());

    // Disable auto disable.. and move the servo.
    servo.setAutoDisable(false);
    assertFalse("setting autoDisable false", servo.isAutoDisable());
    // choose a speed for this test.
    servo.setSpeed(180.0);

    // set the timeout to 1/2 a second for the unit test.
    // Ok. now if we disable.. and move the servo.. it should be disabled after
    // a certain amount of time.
    servo.setIdleTimeout(500);
    assertEquals(500, servo.getIdleTimeout());
    servo.setAutoDisable(true);
    servo.moveTo(1.0);
    // we should move it and make sure it remains enabled.
    sleep(servo.getIdleTimeout() + 500);
    assertTrue("Servo should be disabled.", !servo.isEnabled());

    log.warn("thread list {}", getThreadNames());
    assertTrue("setting autoDisable true", servo.isAutoDisable());
    servo.moveTo(2.0);
    sleep(servo.getIdleTimeout() + 1000); // waiting for disable
    assertFalse("servo should have been disabled", servo.isEnabled());

    assertEquals(2.0, servo.getCurrentInputPos(), 0.0001);

  }

  @Test
  public void moveToBlockingTest() throws Exception {
    Servo servo01 = (Servo) Runtime.start("servo01", "Servo");
    Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    arduino01.connect(port01);
    servo01.setPosition(0.0);
    servo01.setPin(7);
    // use auto disable with a blocking move.
    servo01.setAutoDisable(true);
    // 60 degrees per second.. move from 0 to 180 in 3 seconds
    servo01.setSpeed(60.0);
    arduino01.attach(servo01);
    // Do I need to enable it?!
    servo01.enable();

    long start = System.currentTimeMillis();
    servo01.moveToBlocking(180.0);
    long delta = System.currentTimeMillis() - start;
    assertTrue("Move to blocking should have taken 3 seconds or more. Time was " + delta, delta >= 3000);
    // log.info("Move to blocking took {} milliseconds", delta);
    assertTrue("Servo should be enabled", servo01.isEnabled());
    assertFalse("Servo should not be moving now.", servo01.isMoving());
    
    CountDownLatch moveLatch = new CountDownLatch(1);
    CountDownLatch targetLatch = new CountDownLatch(1);
    CountDownLatch disableLatch = new CountDownLatch(1);

    start = System.currentTimeMillis();

    new Thread(() -> {
        log.info("starting at {}", System.currentTimeMillis());
        servo01.moveTo(0);
        moveLatch.countDown();
    }).start();

    // wait for the move to start
    moveLatch.await();

    // wait for the move to complete using waitTargetPos
    new Thread(() -> {
        servo01.waitTargetPos();
        targetLatch.countDown();
    }).start();

    // wait for the move to complete
    targetLatch.await();
    log.info("finished at {}", System.currentTimeMillis());

    delta = System.currentTimeMillis() - start;
    assertTrue("Move to blocking should have taken 3 seconds or more. Time was " + delta, delta >= 3000);

    log.info("Move to blocking took {} milliseconds", delta);
    assertTrue("Servo should be enabled", servo01.isEnabled());

    // wait for the servo to stop moving
    disableLatch.await(servo01.getIdleTimeout() + 1000, TimeUnit.MILLISECONDS);
    assertFalse("Servo should not be moving now.", servo01.isMoving());

    // verify disabled after autoDisable time
    assertFalse("Servo should be disabled.", servo01.isEnabled());

  }

  @Test
  public void testHelperMethods() throws Exception {
    Servo servo01 = (Servo) Runtime.start("servo01", "Servo");
    Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    arduino01.connect(port01);

    servo01.setPosition(0.0);
    // 60 degrees per second.. move from 0 to 180 in 3 seconds
    servo01.setSpeed(60.0);
    servo01.setPin(7);
    servo01.setPin(8);
    servo01.setSpeed(1.0);
    servo01.setController("blah");
    assertEquals("blah", servo01.getController());
    servo01.attach("arduino01");
    assertEquals("arduino01", servo01.getController());
    assertEquals(Integer.valueOf(8).toString(), servo01.getPin());

    servo01.unsetSpeed();
    assertNull("Speed should be unset", servo01.getSpeed());

    // verify that setMinMax sets both the input and output min/max values.
    servo01.setMinMax(10, 20);
    Mapper m = servo01.getMapper();
    assertEquals(10, m.getMinX(), 0.001);
    assertEquals(10, m.getMinY(), 0.001);
    assertEquals(20, m.getMaxX(), 0.001);
    assertEquals(20, m.getMaxY(), 0.001);

    // now let's update the output mapping only.
    servo01.setMinMaxOutput(90, 180);
    assertEquals(10, m.getMinX(), 0.001);
    assertEquals(20, m.getMaxX(), 0.001);
    assertEquals(90, m.getMinY(), 0.001);
    assertEquals(180, m.getMaxY(), 0.001);

    servo01.map(0, 180, 42, 43);
    m = servo01.getMapper();
    assertEquals(0, m.getMinX(), 0.001);
    assertEquals(180, m.getMaxX(), 0.001);
    assertEquals(42, m.getMinY(), 0.001);
    assertEquals(43, m.getMaxY(), 0.001);

    assertEquals(42, servo01.getMin(), 0.001);
    assertEquals(43, servo01.getMax(), 0.001);

    servo01.moveTo(90.0);
    assertEquals(90.0, servo01.getTargetPos(), 0.001);

    servo01.setSpeed(180.0);
    // servo velocity is speed now..
    assertEquals(180.0, servo01.getVelocity(), 0.001);

    servo01.setVelocity(90.0);
    // servo velocity is speed now..
    assertEquals(90.0, servo01.getSpeed(), 0.001);

    // by default the servo is not inverted
    assertFalse(servo01.isInverted());

    servo01.setInverted(true);
    assertTrue(servo01.isInverted());

    // you know for ol' times sake.
    servo01.enableAutoDisable(true);
    assertTrue(servo01.isAutoDisable());

  }

  // TODO: test sweeping
  // TODO: test stopping a servo in the middle of a movement.
  // TOOD: implement and test waitTargetPos for servo.
  // TODO: publishMoveTo doesn't get exercised in this test
  // TODO: publishServoEnable is not exercised
  // TODO: publishServoMoveTo
  // TODO: publishServoStop
  // TODO: setting a custom mapper

}