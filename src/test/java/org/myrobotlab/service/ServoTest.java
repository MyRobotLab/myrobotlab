package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.myrobotlab.test.AbstractTest;

/**
 * 
 * @author GroG FIXME - test one servo against EVERY TYPE OF CONTROLLER
 *         (virtualized) !!!! iterate through all types
 * 
 *         FIXME - what is expected behavior when a s1 is attached at pin 3,
 *         then a new servo s2 is requested to attach at pin 3 ?
 * 
 *         FIXME - test attach and isAttached on every controller
 */
public class ServoTest extends AbstractTest {

  static final String port01 = "COM9";
  Integer pin = 5;

  @Test
  public void testAttach() throws Exception {
    // FIXME - test state change - mrl gets restarted arduino doesn't what
    // happens - how to handle gracefully
    // FIXME - test enabled Events
    // FIXME - make abstract class from interfaces to attempt to do Java 8
    // interfaces with default
    // creation ...

    // Adafruit16CServoDriver afdriverx = (Adafruit16CServoDriver)
    // Runtime.start("afdriver", "Adafruit16CServoDriver");
    Servo servo01 = (Servo) Runtime.start("s1", "Servo");
    Servo servo02 = (Servo) Runtime.start("s2", "Servo");
    // initialize an arduinos
    Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    
    arduino01.connect(port01);
    
    
    // because I'm just so tired of race conditions !
    // just friggen wait 1/2 a second
//    Service.sleep(500);
  
    log.warn("arduino01 connected {}", arduino01.isConnected());

    Serial serial = arduino01.getSerial();
    // really I have to call refresh first ? :P
    log.info("ports {}", Arrays.toString(serial.getPortNames().toArray()));

    servo01.moveTo(30.0);
    servo01.attach(arduino01, 8, 40.0);
    servo01.attach(arduino01, 8, 30.0);

    servo02.attach(arduino01, 7, 40.0);
    // servo01.eventsEnable();
    // FIXME is attach re-entrant ???
    servo01.broadcastState();
    servo02.broadcastState();

    // sub speed single move
    servo01.moveTo(30.0);
    servo01.moveTo(31.0);
    servo01.moveTo(30.0);
    servo01.moveTo(31.0);
    servo01.moveTo(30.0);

    servo01.setSpeed(3.0);
    servo01.moveTo(130.0);
    servo02.moveTo(130.0);
    servo01.setSpeed(30.0);
    servo01.moveTo(30.0);
    servo02.moveTo(30.0);

    arduino01.setDebug(true);

    // detaching the device
    servo01.detach(arduino01); // test servo02.detach(arduino01);
    // error ?
    // servo02.detach(afdriver); // TEST CASE - THIS FAILED - THEN RE-ATTACHED
    // DID SPLIT BRAIN FIXME
    servo02.detach(arduino01);

    // errors / boundary cases
    // servo01.attach(arduino01, 8, 40);
    servo02.attach(arduino01, 8, 40.0); // same pin?
    servo01.attach(arduino01, 7, 40.0); // already attached ?

    servo01.moveTo(130.0);
    servo02.moveTo(130.0);
    servo01.moveTo(30.0);
    servo02.moveTo(30.0);

    servo01.broadcastState();
    servo02.broadcastState();

    servo01.setSpeed(0.2);
    servo02.setSpeed(0.2);
    servo01.moveTo(130.0);
    servo02.moveTo(130.0);
    servo01.moveTo(30.0);
    servo02.moveTo(30.0);
    servo01.moveTo(130.0);
    servo01.setSpeed(1.0);
    servo01.moveTo(30.0);
    servo01.moveTo(130.0);
    servo01.moveTo(30.0);
    servo01.moveTo(130.0);

    servo01.detach();

    // no move after detach test
    servo01.moveTo(30.0);
    servo01.moveTo(130.0);
    servo01.moveTo(30.0);
    servo01.moveTo(130.0);

    servo01.attach(arduino01);

    // move after detach/re-attach
    servo01.enable();
    servo01.moveTo(30.0);
    servo01.moveTo(130.0);
    servo01.moveTo(30.0);
    servo01.moveTo(130.0);

    // servo02.attach(afdriver, 8);

    // this is valid
    // FIXME --- THIS IS NOT RE-ENTRANT !!!
    // servo01.attach(arduino01, 8, 40); // this attaches the device, calls
    // Servo.attach(8), then Servo.write(40)
    // FIXME --- THIS IS NOT RE-ENTRANT !!!
    // servo02.attach(afdriver, 8, 40);
    // IS IT Equivalent to this ?

    // energize to different pin
    // servo01.attach(7);
    arduino01.setDebug(true);

    servo01.moveTo(130.0);
    servo01.moveTo(30.0);
    // servo02.attach(7.0);

    // servo move methods
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);

    servo02.detach();
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);

    arduino01.attach(servo02);
    servo02.enable();
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);

    // servo detach
    servo01.disable();
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);

    // should re-attach
    // with the same pin & pos
    servo01.enable();
    servo02.enable();

    servo02.moveTo(30.0);
    servo02.moveTo(130.0);
    servo02.moveTo(30.0);
    servo02.moveTo(130.0);
    
    //servo01.releaseService();
    //servo02.releaseService();
  }

  @Test
  public void testAllControllers() {
    System.out.println("ServoTest.testAllControllers() -> FIXME - implement !!!");
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
    assertTrue(s.isEnabled());
    s.disable();
    assertFalse(s.isEnabled());

    s.detach(arduino01);
  

  }


  @Test
  public void testAutoDisable() throws Exception {
    if (!isHeadless()) {
      // Runtime.start("gui", "SwingGui");
      // Runtime.start("gui", "WebGui");
    }
    
    Servo servo01 = (Servo) Runtime.start("servo01", "Servo");
    servo01.releaseService();

    Arduino arduino01 = (Arduino) Runtime.start("arduino01", "Arduino");
    arduino01.connect(port01);

    servo01 = (Servo) Runtime.start("servo01", "Servo");
    servo01.detach();
    servo01.setPin(pin);
    
    arduino01.attach(servo01);
    sleep(100);
    assertTrue("verifying servo should be enabled", servo01.isEnabled());
    servo01.setAutoDisable(false);
    servo01.setSpeed(50.0);
    assertFalse("setting autoDisable false", servo01.getAutoDisable());
    servo01.setAutoDisable(true);
    log.warn("thread list {}", getThreadNames());
    assertTrue("setting autoDisable true", servo01.getAutoDisable());
    servo01.moveTo(130.0);
    log.warn("thread list {}", getThreadNames());
    sleep(8000); // waiting for disable
    log.warn("thread list {}", getThreadNames());
    assertFalse("servo should have been disabled", servo01.isEnabled());

  }

}