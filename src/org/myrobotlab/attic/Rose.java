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

package org.myrobotlab.attic;

import java.util.ArrayList;
import java.util.Random;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Sphinx;
import org.slf4j.Logger;

public class Rose extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Rose.class.getCanonicalName());

	OpenCV camera = null;
	GUIService gui = null;
	Arduino arduino = null;
	Servo pan = null;
	Servo tilt = null;
	AudioFile mouth = null;
	Sphinx ear = null;
	//ArrayList<Polygon> shapesISee = null;

	public Rose(String n) {
		super(n);
	}

	public void startServices() {
		camera = new OpenCV("camera");
		gui = new GUIService("gui");
		arduino = new Arduino("arduino");
		pan = new Servo("pan");
		tilt = new Servo("tilt");
		mouth = new AudioFile("mouth");
		ear = new Sphinx("ear");

		ear.startService();
		mouth.startService();
		pan.startService();
		tilt.startService();
		arduino.startService();
		camera.startService();
		gui.startService();
		this.startService();
	}

	public void setMessageRoutes() {
		/*
		 * 
		 * // tracking camera.addListener("publish", tracker.getName(),
		 * "center", CvPoint.class.getCanonicalName());
		 * tracker.addListener("correctX", pan.getName(), "move",
		 * Integer.class.getCanonicalName()); tracker.addListener("correctY",
		 * tilt.getName(), "move", Integer.class.getCanonicalName());
		 */

		// event set the polygons i see when there are new polygons
		camera.addListener("publish", this.getName(), "setPolygons", ArrayList.class);

		// suppress listening when talking
		// mouth.addListener("started", ear.getName(), "stopRecording", null);
		// mouth.addListener("stopped", ear.getName(), "startRecording", null);

		ear.addListener("recognized", this.getName(), "speechToAction", String.class);

	}

	public void center() {
		pan.invoke("moveTo", 90);
		tilt.invoke("moveTo", 107);

	}

	public void lookAtBoard() {
		pan.invoke("moveTo", 163);
		tilt.invoke("moveTo", 107);

	}

	public void moveRandomly(int amt) {
		int cnt = 0;
		while (cnt < amt) {
			++cnt;
			Random rand = new Random();
			pan.invoke("move", rand.nextInt(5) - 2);
			// tilt.invoke("move", rand.nextInt(5) - 2);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void applyFilter() {
		// camera.addFilter(name, newFilter)
	}

	public void speechToAction(String speech) {
		if (speech.compareTo("rose") == 0) {
			mouth.play("state/listening");
		} else if (speech.compareTo("board") == 0) {
			lookAtBoard();
			mouth.play("state/ready");
		} else if (speech.compareTo("center") == 0) {
			center();
			mouth.play("state/ready");
		} else if (speech.compareTo("relax") == 0) {
			mouth.play("other/laugh");
			moveRandomly(75);
		} else if (speech.compareTo("camera on") == 0) {
			cameraOn();
			mouth.play("state/looking");
		} else if (speech.compareTo("camera off") == 0) {
			cameraOff();
		} else if (speech.compareTo("test") == 0) {
			filterOn();
			mouth.play("state/looking");
		} else if (speech.compareTo("find") == 0) {
//			report();
		} else if (speech.compareTo("clear") == 0) {
			filterOff();
		} else {
			mouth.play("questions/what did you say");
		}
	}

	// center triangle

	public void filterOn() {
		camera.addFilter("PyramidDown", "PyramidDown");
		camera.addFilter("Dilate", "Dilate");
		camera.addFilter("Erode", "Erode");
		camera.addFilter("Threshold", "Threshold");
	}

	public void filterOff() {
		camera.removeFilter("PyramidDown");
		camera.removeFilter("Dilate");
		camera.removeFilter("Erode");
		camera.removeFilter("Threshold");
	}

	public void cameraOn() {
		camera.setInputSource("camera");
		camera.capture();
	}

	public void cameraOff() {
		camera.setInputSource("null");
		camera.capture();
		// camera.rel
	}

	public void Test1() {
		try {

			startServices();
			setMessageRoutes();

			Thread.sleep(2000);

			tilt.attach(arduino.getName(), 6); // TODO - should have
												// failed/thrown
												// !!! make bug Servo does not
												// have
												// a analogWrite fn! out
			pan.attach(arduino.getName(), 5); // TODO - allow gui to attach

			center();
			mouth.play("state/ready");

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	public void report() {
		// stop looking for objects
		camera.removeListener("publish", this.getName(), "setPolygons", ArrayList.class);

		try {

			if (shapesISee != null && shapesISee.size() > 0) {
				mouth.play("state/i found a");
				Thread.sleep(1000);

				for (int i = 0; i < shapesISee.size(); ++i) {
					Polygon p = (Polygon) shapesISee.get(i);
					mouth.play("colors/" + p.getColorWord());
					Thread.sleep(1000);
					mouth.play("size/" + p.getSizeWord());
					Thread.sleep(1000);
					mouth.play("shapes/" + p.getShapeWord());
					Thread.sleep(1000);
				}
			} else {
				mouth.play("state/i didnt find anything");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// event set the polygons i see when there are new polygons
		camera.addListener("publish", this.getName(), "setPolygons", ArrayList.class);

	}
*/
	/*
	public void setPolygons(ArrayList<Polygon> polygons) {
	//	shapesISee = polygons;
	}
*/
	public static void main(String[] args) {
		Rose rose = new Rose("rose");
		rose.Test1();
	}

	@Override
	public String getDescription() {
		return "behavioral experiment";
	}
}

/*
 * 
 * arduino.analogReadPollingStart(0); arduino.analogReadPollingStart(1);
 * arduino.analogReadPollingStart(2); arduino.analogReadPollingStart(3);
 */
