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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.DeviceController;
import org.myrobotlab.service.interfaces.NeoPixelControl;
import org.myrobotlab.service.interfaces.NeoPixelController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class NeoPixel extends Service implements NeoPixelControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(NeoPixel.class);

	transient NeoPixelController controller;

	public static class PixelColor {
		public int address;
		public int red;
		public int blue;
		public int green;

		PixelColor(int address, int red, int green, int blue) {
			this.address = address;
			this.red = red;
			this.blue = blue;
			this.green = green;
		}

		PixelColor() {
			address = 0;
			red = 0;
			blue = 0;
			green = 0;
		}
	}

	HashMap<Integer, PixelColor> pixelMatrix = new HashMap<Integer, PixelColor>();
	ArrayList<PixelColor> savedPixelMatrix = new ArrayList<PixelColor>();

	int numPixel = 0;

	/**
	 * list of names of possible controllers
	 */
	ArrayList<String> controllers;
	public String controllerName;

	public Integer pin;
	public boolean off = false;

	public NeoPixel(String n) {
		super(n);
		subscribe(Runtime.getInstance().getName(), "registered", this.getName(), "onRegistered");
	}

	public void onRegistered(ServiceInterface s) {
		refreshControllers();
		broadcastState();
	}

	public ArrayList<String> refreshControllers() {
		controllers = Runtime.getServiceNamesFromInterface(NeoPixelController.class);
		return controllers;
	}

	@Override
	public NeoPixelController getController() {
		return controller;
	}

	public String getControllerName() {
		String controlerName = null;
		if (controller != null) {
			controlerName = controller.getName();
		}
		return controlerName;
	}

	public boolean isAttached() {
		return controller != null;
	}

	public void setPixel(int address, int red, int green, int blue) {
		PixelColor pixel = new PixelColor(address, red, green, blue);
		setPixel(pixel);
	}

	public void setPixel(PixelColor pixel) {
		if (off)
			return;
		if (pixel.address <= getNumPixel()) {
			pixelMatrix.put(pixel.address, pixel);
		} else {
			log.info("Pixel address over the number of pixel");
		}
	}

	public void sendPixel(PixelColor pixel) {
		if (off)
			return;
		List<Integer> msg = new ArrayList<Integer>();
		msg.add(pixel.address);
		msg.add(pixel.red);
		msg.add(pixel.green);
		msg.add(pixel.blue);
		controller.neoPixelWriteMatrix(this, msg);
		savedPixelMatrix.clear();
		savedPixelMatrix.add(pixel);
	}

	public void sendPixel(int address, int red, int green, int blue) {
		PixelColor pixel = new PixelColor(address, red, green, blue);
		sendPixel(pixel);
	}

	public void writeMatrix() {
		savedPixelMatrix.clear();
		Set<Entry<Integer, PixelColor>> set = pixelMatrix.entrySet();
		Iterator<Entry<Integer, PixelColor>> i = set.iterator();
		List<Integer> msg = new ArrayList<Integer>();
		while (i.hasNext()) {
			Map.Entry<Integer, PixelColor> me = (Map.Entry<Integer, PixelColor>) i.next();
			msg.add(me.getValue().address);
			msg.add(me.getValue().red);
			msg.add(me.getValue().green);
			msg.add(me.getValue().blue);
			savedPixelMatrix.add(me.getValue());
			if (msg.size() > 32) {
				if (!off)
					controller.neoPixelWriteMatrix(this, msg);
				msg.clear();
			}
		}
		if (!off)
			controller.neoPixelWriteMatrix(this, msg);
		broadcastState();
	}

	public Integer getPin() {
		return pin;
	}

	public int getNumPixel() {
		return numPixel;
	}

	public void turnOff() {
		for (int i = 1; i <= numPixel; i++) {
			PixelColor pixel = new PixelColor(i, 0, 0, 0);
			setPixel(pixel);
		}
		writeMatrix();
		off = true;
	}

	public void turnOn() {
		off = false;
		broadcastState();
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

		ServiceType meta = new ServiceType(NeoPixel.class.getCanonicalName());
		meta.addDescription("Control a Neopixel hardware");
		meta.setAvailable(true); // false if you do not want it viewable in a
									// gui
		// add dependency if necessary
		// meta.addDependency("org.coolproject", "1.0.0");
		meta.addCategory("Neopixel, Control");
		return meta;
	}

	@Override
	public void attach(NeoPixelController controller, int pin, int numPixel) throws Exception {
		this.pin = pin;
		this.numPixel = numPixel;

		// clear the old matrix
		pixelMatrix.clear();

		// create a new matrix
		for (int i = 1; i < numPixel + 1; i++) {
			setPixel(new PixelColor(i, 0, 0, 0));
		}

		setController(controller);

		controller.deviceAttach(this, pin, numPixel);
		broadcastState();
	}

	@Override
	public void setController(DeviceController controller) {
		if (controller == null) {
			error("setting null as controller");
			return;
		}
		log.info(String.format("%s setController %s", getName(), controller.getName()));
		this.controller = (NeoPixelController) controller;
		controllerName = this.controller.getName();
	}

	@Override
	public void detach(NeoPixelController controller) {
		// let the controller you want to detach this device
		controller.deviceDetach(this);
		// setting controller reference to null
		controller = null;
		broadcastState();
	}

	public static void main(String[] args) throws InterruptedException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
			webgui.autoStartBrowser(false);
			webgui.startService();
			Runtime.start("gui", "GUIService");
			Runtime.start("python", "Python");
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			arduino.connect("COM15");
			// arduino.setDebug(true);
			NeoPixel neopixel = (NeoPixel) Runtime.start("neopixel", "Neopixel");
			webgui.startBrowser("http://localhost:8888/#/service/neopixel");
			// arduino.setLoadTimingEnabled(true);
			neopixel.attach(arduino, 31, 16);
			PixelColor pix = new NeoPixel.PixelColor(1, 255, 0, 0);
			neopixel.setPixel(pix);
			neopixel.writeMatrix();
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

}
