package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.framework.Service;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.test.AbstractTest;

/**
 * 
 * @author GroG / kwatters 
 * 
 * FIXME - test one servo against EVERY TYPE OF CONTROLLER
 *         (virtualized) !!!! iterate through all types
 * 
 *         FIXME - what is expected behavior when a s1 is attached at pin 3,
 *         then a new servo s2 is requested to attach at pin 3 ?
 * 
 *         FIXME - test attach and isAttached on every controller
 */
public class ServoTest extends AbstractTest {

  static final String port01 = "/dev/ttyACM0";
  Integer pin = 3;
  static Arduino arduino01 = null;
  static Servo servo01 = null;
  
  // TODO - disconnected move tests
  // TODO - can more than on servo be attached to the same controller and pin?
  
  @Before /* start initial state */
  public void setUp() throws Exception {
    
    // Platform.setVirtual(false); - force to make real servo
    
    servo01 = (Servo) Runtime.start("s1", "Servo");
    arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    arduino01.connect(port01);  
    servo01.setPin(3);
    servo01.attach(arduino01, 3, 40.0);
    servo01.rest();
    servo01.enable();
    servo01.map(0,180,0,180);
    servo01.setRest(90.0);
  }
  
  @Test
  public void disabledMove() throws Exception {
    // take off speed control
    log.error("HERE 0 ----------------");
    servo01.fullSpeed();    
    servo01.moveTo(0.0);
    servo01.setInverted(false);
    Service.sleep(1000);
    log.error("HERE 1 ----------------");
    
    // begin long slow move
    servo01.setSpeed(5.0);
    servo01.moveTo(180.0);
    Service.sleep(300);
    assertTrue(servo01.isMoving());
    log.error("HERE 2 ----------------");
    // after 1/10 of a second we should be moving
    assertTrue(servo01.isMoving());
    double pos = servo01.getCurrentInputPos();
    
    // disable move while it has not completed
    servo01.disable();
    Service.sleep(300);
    assertTrue(!servo01.isMoving());
    assertTrue(!servo01.isSweeping());
    
    // wait a little after disabling
    // Service.sleep(100);
    double postDisablePos = servo01.getCurrentInputPos();
    
    // servo expected to have stopped and not continued after 
    // a control disable() method was called
    assertEquals(pos, postDisablePos, 0.03);
    
  }
  

  @Test
  public void invertTest() throws Exception {
    // verify the outputs are inverted as expected.
    servo01.moveTo(30.0);
    assertEquals(30.0, servo01.getTargetOutput(), 0.001);
    servo01.setInverted(true);
    assertEquals(150.0, servo01.getTargetOutput(), 0.001);
    servo01.moveTo(20.0);
    assertEquals(160.0, servo01.getTargetOutput(), 0.001);
    servo01.setInverted(false);
    assertEquals(20.0, servo01.getTargetOutput(), 0.001);
  }  
  
  @Test
  public void mapTest() throws Exception {
    servo01.setInverted(true);
    servo01.map(10, 170, 20, 160);
    Mapper mapper = servo01.getMapper();
    assertTrue(mapper.isInverted());
    servo01.setInverted(false);
    servo01.map(20, 160, 10, 170);
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
    // Runtime.start("gui", "SwingGui");
    
    Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    arduino01.connect(port01);

    Servo s = (Servo) Runtime.start("ser1", "Servo");

    
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
   

    //
    s.attach(arduino01, 10, 1.0);
    s.enable();
    assertTrue(s.isEnabled());
    s.disable();
    assertFalse(s.isEnabled());

    s.detach(arduino01);
  

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
    // Start the test servo.
    servo01.detach();
    servo01.setPin(pin);
    servo01.setPosition(0.0);
    
    arduino01.attach(servo01);
    assertTrue("verifying servo is attached to the arduino.", servo01.isAttached("arduino01"));
    servo01.moveTo(30.0);
    assertTrue("verifying servo should be enabled", servo01.isEnabled());
    
    // Disable auto disable.. and move the servo.
    servo01.setAutoDisable(false);
    assertFalse("setting autoDisable false", servo01.isAutoDisable());
    // choose a speed for this test.  
    servo01.setSpeed(180.0);
    
    // set the timeout to 1/2 a second for the unit test.
    // Ok. now if we disable.. and move the servo.. it should be disabled after a certain amount of time.
    servo01.setIdleTimeout(500);
    assertEquals(500, servo01.getIdleTimeout());
    servo01.setAutoDisable(true);
    servo01.moveTo(1.0);
    // we should move it and make sure it remains enabled. 
    sleep(servo01.getIdleTimeout() + 500);
    assertTrue("Servo should be disabled.", !servo01.isEnabled());
    
        
    log.warn("thread list {}", getThreadNames());
    assertTrue("setting autoDisable true", servo01.isAutoDisable());
    servo01.moveTo(2.0);
    sleep(servo01.getIdleTimeout()+1000); // waiting for disable
    assertFalse("servo should have been disabled", servo01.isEnabled());
    
    assertEquals(2.0, servo01.getCurrentInputPos(), 0.0001);

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
    assertTrue("Move to blocking should have taken more than 3 seconds.", delta > 3000);
    // log.info("Move to blocking took {} milliseconds", delta);
    assertTrue("Servo should be ebabled", servo01.isEnabled());
    assertFalse("Servo should not be moving now.", servo01.isMoving());
    // Now let's wait for the idle disable timer to kick off + 1000ms
    // TODO: figure out why smaller values like 100ms cause this test to fail.
    // there seems to be some lag
    Thread.sleep(servo01.getIdleTimeout() + 1000);
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
    servo01.attach("arduino01", 8, 1.0, 360.0);
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