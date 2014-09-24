/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Wii.IRData;
import org.myrobotlab.service.WiiDAR.Point;
import org.myrobotlab.service.data.Pin;
import org.slf4j.Logger;

// TODO - BlockingQueue - + reference !

public class WiiBot extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(WiiBot.class.getCanonicalName());

	Arduino arduino = null;
	Wii wii = new Wii("wii");
	Servo servo = new Servo("servo");
	// OpenCV opencv = new OpenCV("opencv");
	WiiDAR wiidar = new WiiDAR("wiidar");
	GUIService gui = new GUIService("gui");

	public WiiBot(String n) {
		super(n);
	}



	public void startRobot() {
		arduino = new Arduino("arduino");

		// adding wiicom as an option
		/*
		 * Arduino.addPortName("wiicom", CommPortIdentifier.PORT_SERIAL,
		 * (CommDriver) new WiiDriver(wii));
		 */

		gui.startService();
		wiidar.servo = servo;
		// setting up servo
		// servo.attach(arduino.getName(), 9);

		// gui.start();
		// 

		// setting up wii
		wii.getWiimotes();
		wii.setSensorBarAboveScreen();
		wii.activateIRTRacking();
		wii.setIrSensitivity(5); // 1-5 (highest)
		wii.activateListening();

		arduino.startService();

		wiidar.startService();

		// starting services
		servo.startService();
		// opencv.start();
		wii.startService();

		// send data from the wii to wiidar
		wii.addListener(wiidar.getName(), "publishIR", IRData.class);
		// data from widar to the gui
		wiidar.addListener("publishArrayofPoints", gui.getName(), "displaySweepData", Point.class);

		// send the data from the wii to wiidar
		// wii.addListener("publishIR", wiidar.getName(), "computeDepth",
		// IRData.class);
		// send the computed depth & data to the gui
		// addListener("computeDepth", gui.getName(),"publishSinglePoint",
		// Point.class);
		wiidar.addListener("publishSinglePoint", gui.getName(), "publishSinglePoint", Point.class);
		// gui.addListener("processImage", opencv.getName(),"input",
		// BufferedImage.class);
		// wii.addListener("publishPin", wiidar.getName(), "publishPin",
		// IRData.class);
		arduino.addListener(wiidar.getName(), "publishPin", Pin.class);
		
	}

	int speedRight = 0;
	int speedLeft = 0;

	// TODO - seperate left and right direction

	public void keyPressed(Integer i) {
		log.warn("keyPressed " + i);
		if (i == 38) // up arrow
		{
			speedRight += 10;
			speedLeft += 10;

			log.warn("up speed" + speedLeft + " " + speedRight);

			if (speedRight > 0 || speedLeft > 0) {
				arduino.digitalWrite(13, 0);
			}

			arduino.analogWrite(5, speedRight);
			arduino.analogWrite(6, speedLeft);
		} else if (i == 32) // space
		{
			log.warn("space" + speedLeft + " " + speedRight);

			speedRight = 0;
			speedLeft = 0;

			arduino.digitalWrite(13, 1);

			arduino.analogWrite(5, speedRight);
			arduino.analogWrite(6, speedLeft);

		} else if (i == 40) // down
		{
			speedRight -= 10;
			speedLeft -= 10;

			log.warn("down speed" + speedLeft + " " + speedRight);

			if (speedRight < 0 || speedLeft < 0) {
				arduino.digitalWrite(13, 1);
			}

			arduino.analogWrite(5, Math.abs(speedRight));
			arduino.analogWrite(6, Math.abs(speedLeft));

		} else if (i == 39) // right arrow
		{
			speedLeft += 10;

			log.warn("right speed" + speedLeft + " " + speedRight);

			if (speedLeft > 0) {
				arduino.digitalWrite(13, 0);
			}

			arduino.analogWrite(6, speedLeft);
		} else if (i == 37) // left arrow
		{
			speedRight += 10;

			log.warn("left speed" + speedLeft + " " + speedRight);

			if (speedRight > 0) {
				arduino.digitalWrite(13, 0);
			}

			arduino.analogWrite(5, speedRight);

		} else if (i == 87) // w
		{
			wiidar.startSweep();
		} else if (i == 83) // s
		{
			wiidar.stopSweep();
		}
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		WiiBot wiibot = new WiiBot("wiibot");
		wiibot.startService();
		wiibot.startRobot();

	}

	@Override
	public String getDescription() {
		return "(not implemented) - robot utilizing the wii mote and wiidar";
	}

}
