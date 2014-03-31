/*
 *  RoombaComm Interface
 *
 *  Copyright (c) 2006 Tod E. Kurt, tod@todbot.com, ThingM
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General
 *  Public License along with this library; if not, write to the
 *  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307  USA
 *
 */

package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;

import org.myrobotlab.framework.Errors;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.roomba.RoombaComm;
import org.myrobotlab.roomba.RoombaCommSerialDevice;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

@Root
public class Roomba extends Service implements SerialDeviceService {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Roomba.class.getCanonicalName());

	transient RoombaCommSerialDevice roombacomm = null;

	@Element
	String portName = "";
	@Element
	int baudRate = 57600;
	@Element
	int dataBits = 8;
	@Element
	int parity = 0;
	@Element
	int stopBits = 1;

	public Roomba(String n) {
		super(n);
		roombacomm = new RoombaCommSerialDevice();
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public ArrayList<String> getPortNames() {
		return SerialDeviceFactory.getSerialDeviceNames();
	}

	@Override
	public SerialDevice getSerialDevice() {
		return roombacomm.port;
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Connect to a serial port specified by portid doesn't guarantee connection
	 * to Roomba, just to serial port
	 * 
	 * @param portid
	 *            name of port, e.g. "/dev/cu.KeySerial1" or "COM3"
	 * @return true if connect was successful, false otherwise
	 */
	public boolean connect(String portName) {
		if (roombacomm.connect(portName)) {
			this.portName = portName;
			save();
			return true;
		}
		return false;
	}

	// RoombaComm passthrough begin ----------------------

	/**
	 * Disconnect from serial portb
	 */

	public boolean disconnect() {
		roombacomm.disconnect();
		return true;
	}

	public boolean send(byte[] cmd) {
		return roombacomm.send(cmd);
	}

	public void playNote(int i, int j) {
		roombacomm.playNote(i, j);
	}

	public boolean updateSensors() {
		return roombacomm.updateSensors();
	}

	public void control() {
		roombacomm.control();
	}

	public void startup() {
		roombacomm.startup();
	}

	// RoombaComm passthrough end ----------------------

	public void setHardwareHandshake(boolean hardwareHandshake) {
		setWaitForDSR(hardwareHandshake);
	}

	public void setWaitForDSR(boolean waitForDSR) {
		roombacomm.setWaitForDSR(waitForDSR);
	}

	public String getProtocol() {
		return roombacomm.getProtocol();
	}

	public void setProtocol(String protocol) {
		roombacomm.setProtocol(protocol);
	}

	public boolean isWaitForDSR() {
		return roombacomm.waitForDSR;
	}

	/**
	 * This will handle both ints, bytes and chars transparently.
	 */
	public boolean send(int b) {
		return roombacomm.send(b);
	}

	/**
	 * toggles DD line via serial port DTR (if available)
	 */
	public void wakeup() {
		roombacomm.wakeup();
	}

	/**
	 * Update sensors. Block for up to 1000 ms waiting for update To use
	 * non-blocking, call sensors() and then poll sensorsValid()
	 */
	public boolean updateSensors(int packetcode) {
		return roombacomm.updateSensors(packetcode);
	}

	/**
	 * called by serialEvent when we have enough bytes to make sensors valid
	 */
	public void computeSensors() {
		roombacomm.computeSensors();
	}

	// --- from RoombaComm abstract class

	public boolean connected() {
		return roombacomm.connected();
	}

	/**
	 * Turn all vacuum motors on or off according to state
	 * 
	 * @param state
	 *            true to turn on vacuum function, false to turn it off
	 */
	public void vacuum(boolean state) {
		roombacomm.vacuum(state);
	}

	/**
	 * Turns on/off the various LEDs. Low-level command. FIXME: this is too
	 * complex
	 */
	public void setLEDs(boolean status_green, boolean status_red, boolean spot, boolean clean, boolean max, boolean dirt, int power_color, int power_intensity) {
		roombacomm.setLEDs(status_green, status_red, spot, clean, max, dirt, power_color, power_intensity);
	}

	public void setPortname(String p) {
		roombacomm.setPortname(p);
	}

	//
	// low-level movement and action
	//

	/**
	 * Move the Roomba via the low-level velocity + radius method. See the
	 * 'Drive' section of the Roomba ROI spec for more details. Low-level
	 * command.
	 * 
	 * @param velocity
	 *            speed in millimeters/second, positive forward, negative
	 *            backward
	 * @param radius
	 *            radius of turn in millimeters
	 */
	public void drive(int velocity, int radius) {
		roombacomm.drive(velocity, radius);
	}

	//
	// higher-level functions
	//

	/** Set speed for movement commands */
	public void setSpeed(int s) {
		roombacomm.speed = Math.abs(s);
	}

	/** Get speed for movement commands */
	public int getSpeed() {
		return roombacomm.speed;
	}

	/**
	 * Go straight at the current speed for a specified distance. Positive
	 * distance moves forward, negative distance moves backward. This method
	 * blocks until the action is finished.
	 * 
	 * @param distance
	 *            distance in millimeters, positive or negative
	 */
	public void goStraight(int distance) {
		roombacomm.goStraight(distance);
	}

	/**
	 * @param distance
	 *            distance in millimeters, positive
	 */
	public void goForward(int distance) {
		roombacomm.goForward(distance);
	}

	/**
	 * @param distance
	 *            distance in millimeters, positive
	 */
	public void goBackward(int distance) {
		roombacomm.goBackward(distance);
	}

	/**
 *
 */
	public void turnLeft() {
		roombacomm.turn(129);
	}

	public void turnRight() {
		roombacomm.turn(-129);
	}

	public void turn(int radius) {
		roombacomm.turn(radius);
	}

	/**
	 * Spin right or spin left a particular number of degrees
	 * 
	 * @param angle
	 *            angle in degrees, positive to spin left, negative to spin
	 *            right
	 */
	public void spin(int angle) {
		roombacomm.spin(angle);
	}

	/**
	 * Spin right the current speed for a specified angle
	 * 
	 * @param angle
	 *            angle in degrees, positive
	 */
	public void spinRight(int angle) {
		roombacomm.spinRight(angle);
	}

	/**
	 * Spin left a specified angle at a specified speed
	 * 
	 * @param angle
	 *            angle in degrees, positive
	 */
	public void spinLeft(int angle) {
		roombacomm.spinLeft(angle);
	}

	/**
	 * Spin in place anti-clockwise, at the current speed
	 */
	public void spinLeft() {
		roombacomm.spinLeft();
	}

	/**
	 * Spin in place clockwise, at the current speed
	 */
	public void spinRight() {
		roombacomm.spinRight();
	}

	/**
	 * Spin in place anti-clockwise, at the current speed.
	 * 
	 * @param aspeed
	 *            speed to spin at
	 */
	public void spinLeftAt(int aspeed) {
		roombacomm.spinLeftAt(aspeed);
	}

	/**
	 * Spin in place clockwise, at the current speed.
	 * 
	 * @param aspeed
	 *            speed to spin at, positive
	 */
	public void spinRightAt(int aspeed) {
		roombacomm.spinRightAt(aspeed);
	}

	//
	// mid-level movement, no blocking, parameterized by speed, not distance
	//

	/**
	 * Go straight at a specified speed. Positive is forward, negative is
	 * backward
	 * 
	 * @param velocity
	 *            velocity of motion in mm/sec
	 */
	public void goStraightAt(int velocity) {
		roombacomm.goStraightAt(velocity);
	}

	/**
	 * Go forward the current (positive) speed
	 */
	public void goForward() {
		roombacomm.goForward();
	}

	/**
	 * Go backward at the current (negative) speed
	 */
	public void goBackward() {
		roombacomm.goBackward();
	}

	/**
	 * Go forward at a specified speed
	 */
	public void goForwardAt(int aspeed) {
		roombacomm.goForwardAt(aspeed);
	}

	/**
	 * Go backward at a specified speed
	 */
	public void goBackwardAt(int aspeed) {
		roombacomm.goBackwardAt(aspeed);
	}

	/**
	 * Stop Rooomba's motion. Sends drive(0,0)
	 */
	public void stop() {
		roombacomm.stop();
	}

	public void powerOff() {
		roombacomm.powerOff();
	}

	public void clean() {
		roombacomm.clean();
	}

	public void spot() {
		roombacomm.spot();
	}

	public String sensorsAsString() {
		return roombacomm.sensorsAsString();
	}

	public String[] listPorts() {
		return roombacomm.listPorts();
	}

	public String getPortname() {
		return roombacomm.getPortname();
	}

	/**
	 * Make a square with a Roomba. Leaves Roomba in same place it began
	 * (theoretically)
	 * 
	 * @param rc
	 *            RoombaComm object connected to a Roomba
	 * @param size
	 *            size of square in mm
	 */
	public void square(RoombaComm rc, int size) {
		roombacomm.goForward(size);
		roombacomm.spinLeft(90);
		roombacomm.goForward(size);
		roombacomm.spinLeft(90);
		roombacomm.goForward(size);
		roombacomm.spinLeft(90);
		roombacomm.goForward(size);
		roombacomm.spinLeft(90);
	}

	/**
	 * Read sensors to detect bumps and turn away from them while driving
	 * <p>
	 * Run it with something like:
	 * 
	 * <pre>
	 *     java roombacomm.BumpTurn /dev/cu.KeySerial1<br>
	 *    Usage: 
	 *    roombacomm.Drive serialportname [protocol] [options]
	 *    where:
	 *    protocol (optional) is SCI or OI
	 *    [options] can be one or more of:
	 *    -debug       -- turn on debug output
	 *    -hwhandshake -- use hardware-handshaking, for Windows Bluetooth
	 *    -nohwhandshake -- don't use hardware-handshaking
	 * </pre>
	 */
	public void bumpTurn() {
		boolean done = false;
		while (!done) {
			if (roombacomm.bumpLeft()) {
				roombacomm.spinRight(90);
			} else if (roombacomm.bumpRight()) {
				roombacomm.spinLeft(90);
			} else if (roombacomm.wall()) {
				roombacomm.playNote(72, 10); // beep!
			}
			roombacomm.goForward();
			roombacomm.updateSensors();
		}
	}

	/**
	 * Reset Roomba after a fault. This takes it out of whatever mode it was in
	 * and puts it into safe mode. This command also syncs the object's sensor
	 * state with the Roomba's by calling updateSensors()
	 * 
	 * @see #startup()
	 * @see #updateSensors()
	 */
	public void reset() {
		roombacomm.reset();
	}

	/**
	 * A Spirograph-like example
	 * <p>
	 * Run it with something like:
	 * 
	 * <pre>
	 *    java roombacomm.Spiro1 /dev/cu.KeySerial1 velocity radius waittime<br>
	 *   Usage: \n"+
	 *     roombacomm.Spiro1 <serialportname> [protocol] <velocity> <radius> <waittime> [options]<br>
	 *   where: 
	 *   protocol (optional) is SCI or OI
	 *   velocity and radius in mm, waittime in milliseconds
	 *   [options] can be one or more of:
	 *    -debug       -- turn on debug output
	 *    -hwhandshake -- use hardware-handshaking, for Windows Bluetooth
	 *    -nohwhandshake -- don't use hardware-handshaking
	 * </pre>
	 */
	public void spiro1(int velocity, int radius, int waittime) {

		int v = velocity;
		int r = radius;
		int dr = -10;

		boolean done = false;
		while (!done) {
			roombacomm.drive(v, r);
			roombacomm.pause(waittime);
			roombacomm.drive(v, (int) r / Math.abs(dr));
			roombacomm.pause(waittime);
			r += -10;
			// done = keyIsPressed();
		}
	}

	public void spiro2(int velocity, int radius, int radius2, int waittime, int waittime2) {
		int w, dr;

		boolean done = false;
		while (!done) {
			roombacomm.drive(velocity, radius);
			// roombacomm.pause( waittime );

			// lets try some easing
			w = waittime / 10; // divide into 10 msec chucks
			dr = (radius2 - radius) / 10;
			System.out.println("easing " + w + " times at " + dr + " radius");
			for (int i = 0; i < w; i++) {
				roombacomm.drive(velocity, radius + dr);
				roombacomm.pause(10);
			}

			roombacomm.drive(velocity, radius2);
			// roombacomm.pause( waittime2 );

			// lets try some easing
			w = waittime2 / 10; // divide into 10 msec chucks
			dr = (radius - radius2) / 10;
			System.out.println("easing " + w + " times at " + dr + " radius");
			for (int i = 0; i < w; i++) {
				roombacomm.drive(velocity, radius2 + dr);
				roombacomm.pause(10);
			}

			// done = keyIsPressed();
		}
	}

	/**
	 * Spy on the Roomba as it goes about its normal business
	 * <p>
	 */

	public void spy() {
		int pausetime = 500;
		boolean running = true;
		while (running) {

			try {
				if (System.in.available() != 0) {
					System.out.println("key pressed");
					running = false;
				}
			} catch (IOException ioe) {
			}

			boolean rc = roombacomm.updateSensors();
			if (!rc) {
				System.out.println("No Roomba. :(  Is it turned on?");
				continue;
			}

			System.out.println(System.currentTimeMillis() + ":" + roombacomm.sensorsAsString());

			roombacomm.pause(pausetime);
		}
	}

	public void spyAuto() {
		boolean running = true;
		int pausetime = 500;

		while (running) {

			if (!roombacomm.sensorsValid) {
				System.out.println("No Roomba. :(  Is it turned on?");
				continue;
			}

			System.out.println(roombacomm.sensorsAsString());

			try {
				if (System.in.available() != 0) {
					System.out.println("key pressed");
					running = false;
				}
			} catch (IOException ioe) {
			}

			roombacomm.pause(pausetime);
		}
	}

	public void spySimple() {
		int pausetime = 500;
		boolean done = false;
		while (!done) {
			roombacomm.updateSensors();
			printSensors();
			roombacomm.pause(pausetime);
			// done = keyIsPressed();
		}
	}

	public void printSensors() {
		System.out.println(System.currentTimeMillis() + ":" + "bump:" + (roombacomm.bumpLeft() ? "l" : "_") + (roombacomm.bumpRight() ? "r" : "_") + " wheel:"
				+ (roombacomm.wheelDropLeft() ? "l" : "_") + (roombacomm.wheelDropCenter() ? "c" : "_") + (roombacomm.wheelDropLeft() ? "r" : "_"));

	}

	public boolean bump() {
		return roombacomm.bump();
	}

	public void test() {
		try {
			// must pause after every playNote to let to note sound
			System.out.println("Playing some notes");
			roombacomm.playNote(72, 10);
			roombacomm.pause(200);
			roombacomm.playNote(79, 10);
			roombacomm.pause(200);
			roombacomm.playNote(76, 10);
			roombacomm.pause(200);

			// test Logo-like functions (blocking)
			// speed is in mm/s, go* is in mm, spin is in degrees
			roombacomm.setSpeed(100); // can be positive or negative
			roombacomm.goStraight(100); // can be positive or negative
			roombacomm.goForward(100); // negative numbers not allowed
			roombacomm.goBackward(200); // negative numbers not allowed

			roombacomm.setSpeed(150);
			roombacomm.spin(-360); // can be positive or negative
			roombacomm.spinRight(360); // negative numbers not allowed
			roombacomm.spinLeft(360); // negative numbers not allowed

			// test non-blocking functions
			roombacomm.goStraightAt(200); // speed argument
			roombacomm.pause(1000);
			roombacomm.goForwardAt(200); // speed argument
			roombacomm.pause(1000);
			roombacomm.goBackwardAt(400); // speed argument
			roombacomm.pause(1000);

			roombacomm.spinLeftAt(-15); // mm/s or degs/sec ?
			roombacomm.pause(1000);
			roombacomm.spinRightAt(15);
			roombacomm.pause(1000);
		} catch (Exception e) {
			error(e);
			return;
		}

		info("test completed");
	}

	public void purr() {
		System.out.println("purr");
		roombacomm.playSong(5);
		for (int i = 0; i < 5; i++) {
			roombacomm.spinLeftAt(75);
			roombacomm.pause(100);
			roombacomm.spinRightAt(75);
			roombacomm.pause(100);
			roombacomm.stop();
		}
	}

	public void createTribblePurrSong() {
		int song[] = { 68, 4, 67, 4, 66, 4, 65, 4, 64, 4, 63, 4, 62, 4, 61, 4, 60, 4, 59, 4, 60, 4, 61, 4, };
		roombacomm.createSong(5, song);
	}

	public void bark() {
		System.out.println("bark");
		roombacomm.vacuum(true);
		roombacomm.playNote(50, 5);
		roombacomm.pause(150);
		roombacomm.vacuum(false);
	}

	public void tribble() {
		createTribblePurrSong();

		System.out.println("Press return to exit.");
		boolean done = false;
		while (!done) {

			purr();

			if (Math.random() < 0.1)
				bark();

			roombacomm.pause(1500 + (int) (Math.random() * 500));
			// done = keyIsPressed();
		}

	}

	public void waggle(int velocity, int radius, int waittime) {
		for (int i = 0; i < 5; i++) {
			roombacomm.drive(velocity, radius);
			roombacomm.pause(waittime);
			roombacomm.drive(velocity, -radius);
			roombacomm.pause(waittime);
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Roomba roomba = new Roomba("roomba");
		roomba.startService();

		/*
		 * roomba.connect("COM6");
		 * 
		 * roomba.startup(); roomba.control();
		 */
		// roomba.pause(30);
		/*
		 * System.out.println("Checking for Roomba... "); if(
		 * roomba.updateSensors() ) System.out.println("Roomba found!"); else
		 * System.out.println("No Roomba. :(  Is it turned on?");
		 * 
		 * //roomba.updateSensors();
		 * 
		 * System.out.println("Playing some notes"); roomba.playNote( 72, 10 );
		 * // C roomba.pause( 200 ); roomba.playNote( 79, 10 ); // G
		 * roomba.pause( 200 ); roomba.playNote( 76, 10 ); // E roomba.pause(
		 * 200 );
		 * 
		 * System.out.println("Spinning left, then right"); roomba.spinLeft();
		 * roomba.pause(1000); roomba.spinRight(); roomba.pause(1000);
		 * roomba.stop();
		 * 
		 * System.out.println("Going forward, then backward");
		 * roomba.goForward(); roomba.pause(1000); roomba.goBackward();
		 * roomba.pause(1000); roomba.stop();
		 * 
		 * 
		 * System.out.println("Moving via send()"); byte cmd[] =
		 * {(byte)RoombaComm.DRIVE, (byte)0x00,(byte)0xfa,
		 * (byte)0x00,(byte)0x00}; roomba.send( cmd ) ; roomba.pause(1000);
		 * roomba.stop(); cmd[1] = (byte)0xff; cmd[2] = (byte)0x05; roomba.send(
		 * cmd ) ; roomba.pause(1000); roomba.stop();
		 * 
		 * System.out.println("Disconnecting"); roomba.disconnect();
		 * 
		 * System.out.println("Done");
		 * 
		 * roomba.setWaitForDSR(false);
		 * 
		 * roomba.setHardwareHandshake(false);
		 */
		GUIService gui = new GUIService("gui");
		gui.startService();

	}

	@Override
	public void write(String data) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(byte[] data) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(char data) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(int data) throws IOException {
		// TODO Auto-generated method stub

	}

}
