package org.myrobotlab.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.test.TestUtils;

public class ServoTest {

  private static final String V_PORT_1 = "test_port_1";
  private static final String V_PORT_2 = "test_port_2";

  public Arduino ard1;
  public Arduino ard2;
  
	@Before
	public void setup() throws Exception {
	  // setup the test environment , and create an arduino with a virtual backend for it.
    TestUtils.initEnvirionment();
    // initialize 2 serial ports (virtual arduino)
    VirtualArduino va1 = (VirtualArduino)Runtime.start("va1", "VirtualArduino");
    VirtualArduino va2 = (VirtualArduino)Runtime.start("va2", "VirtualArduino");
    va1.connect(V_PORT_1);
    va2.connect(V_PORT_2);
    // initialize an arduino
    ard1 = (Arduino) Runtime.start("ard1", "Arduino");
    ard1.connect(V_PORT_1);
    ard2 = (Arduino) Runtime.start("ard2", "Arduino");
    ard2.connect(V_PORT_2);	
	}

	
	@Test
	public void testServo() throws Exception {	  
	  // this basic test will create a servo and attach it to an arduino.
	  // then detach	  
	  Servo s = (Servo)Runtime.start("ser1", "Servo");
	  Integer pin = 1;
	  // the pin should always be set to something.
	  s.setPin(pin);
	  assertEquals(pin, s.getPin());
	  
	  s.attach(ard1);
	  
	  // maybe remove this interface
	  s.attachServoController(ard1);
    // s.attachServoController(ard1);
    s.disable();
    
    s.attach(ard2, pin);
    
    // This is broken 
    // assertTrue(s.controller  == ard2);
    s.rest();
    assertEquals(s.getRest(), 90.0, 0.0);
    
    s.setRest(12.2);
    assertEquals(s.getRest(), 12.2, 0.0);

    s.rest();
    
    // depricated. i feel like if you do 
    s.attach();
    s.enable();
    s.moveTo(90);
    // test moving to the same position
    s.moveTo(90);
    // new position
    s.moveTo(91.0);
    s.disable();
    
    assertFalse(s.isEnabled());    
    s.enable();
    assertTrue(s.isEnabled());
    
    // detach the servo.
    //ard2.detach(s);
    s.detachServoController(ard2);
	  assertFalse(s.isAttached());	  

	  //
	  s.attach(ard1, 10, 1);
	  assertTrue(s.isEnabled());
	  s.disable();
	  assertFalse(s.isEnabled());
	  
	  s.detachServoController(ard1);
	  assertFalse(s.isAttached());
	  
	}

	// @Test
	public void testAttach() throws Exception {
		// FIXME - test state change - mrl gets restarted arduino doesn't what happens - how to handle gracefully
		// FIXME - test enabled Events
		// FIXME - make abstract class from interfaces to attempt to do Java 8 interfaces with default		
		// creation ...
		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
		Runtime.start("gui", "WebGui");
		Adafruit16CServoDriver afdriver = (Adafruit16CServoDriver) Runtime.start("afdriver", "Adafruit16CServoDriver");
		Servo servo01 = (Servo) Runtime.start("servo01", "Servo");
		Servo servo02 = (Servo) Runtime.start("servo02", "Servo");

		Serial serial = arduino.getSerial();
		// really I have to call refresh first ? :P
		serial.getPortNames();
		List<String> ports = serial.getPortNames();
//		for (String port : ports) {
//			log.info(port);
//		}

		// User code begin ...
		// should be clear & easy !!

		// microcontroller connect ...
		arduino.connect("COM15");
		// arduino.setDebug(true);

		// ServoControl Methods begin --------------
		// are both these valid ?
		// gut feeling says no - they should not be
		// servo01.attach(arduino, 8);
		servo01.moveTo(30);
		servo01.attach(arduino, 8, 40.0);
		servo01.attach(arduino, 8, 30);
		
		servo02.attach(arduino, 7, 40);
		servo01.eventsEnabled(true);
		// FIXME is attach re-entrant ???
		servo01.broadcastState();
		servo02.broadcastState();

		/*
		servo01.setSpeed(0.02);
		servo02.setSpeed(0.02);
		*/
		
		/*
		servo02.setSpeed(1.0);
		servo01.setSpeed(1.0);
		*/
		
		// sub speed single move
		servo01.moveTo(30);
		servo01.moveTo(31);
		servo01.moveTo(30);
		servo01.moveTo(31);
		servo01.moveTo(30);
		
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		
		arduino.setDebug(true);
		
		// detaching the device
		servo01.detachServoController(arduino); // test servo02.detach(arduino); error ?
		// servo02.detach(afdriver); // TEST CASE - THIS FAILED - THEN RE-ATTACHED DID SPLIT BRAIN FIXME
		servo02.detachServoController(arduino);

		// errors / boundary cases
		// servo01.attach(arduino, 8, 40);
		servo02.attach(arduino, 8, 40); // same pin?
		servo01.attach(arduino, 7, 40); // already attached ?
		
		
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		
		servo01.broadcastState();
		servo02.broadcastState();
		
		servo01.setSpeed(0.2);
		servo02.setSpeed(0.2);
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		servo01.moveTo(130);
		servo01.setSpeed(1.0);
		servo01.moveTo(30);
		servo01.moveTo(130);
		servo01.moveTo(30);
		servo01.moveTo(130);
		
		servo01.detach();

		// no move after detach test
		servo01.moveTo(30);
		servo01.moveTo(130);
		servo01.moveTo(30);
		servo01.moveTo(130);
		
		// move after detach/re-attach
		servo01.attach();
		servo01.moveTo(30);
		servo01.moveTo(130);
		servo01.moveTo(30);
		servo01.moveTo(130);
		
		

		//servo02.attach(afdriver, 8);

		// this is valid
		// FIXME --- THIS IS NOT RE-ENTRANT !!!
		// servo01.attach(arduino, 8, 40); // this attaches the device, calls
										// Servo.attach(8), then Servo.write(40)
		// FIXME --- THIS IS NOT RE-ENTRANT !!!
		//servo02.attach(afdriver, 8, 40);
		// IS IT Equivalent to this ?

		// energize to different pin
		// servo01.attach(7);
		arduino.setDebug(true);
		
		
		servo01.moveTo(130);
		servo01.moveTo(30);
		// servo02.attach(7);

		// servo move methods
		servo02.moveTo(30);
		servo02.moveTo(130);
		
		servo02.detach();
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);
	
		servo02.attach();
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);

		/*
		servo01.moveTo(30);
		servo02.moveTo(30);
		servo01.moveTo(130);
		servo02.moveTo(130);
		servo01.moveTo(30);
		servo02.moveTo(30);
		servo01.moveTo(130);
		servo02.moveTo(130);
		*/

		// servo detach
		servo01.detach();
		servo02.detach();

		// should re-attach
		// with the same pin & pos
		servo01.attach();
		servo02.attach();

	
		
		servo02.moveTo(30);
		servo02.moveTo(130);
		servo02.moveTo(30);
		servo02.moveTo(130);

	}

}
