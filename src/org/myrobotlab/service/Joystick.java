// Andrew Davison, October 2006, ad@fivedots.coe.psu.ac.th

/* This controller supports a game pad with two
 analog sticks with axes (x,y) and (z,rz), 12 buttons, a 
 D-Pad acting as a point-of-view (POV) hat, and a 
 single rumbler.

 The sticks are assumed to be absolute and analog, while the 
 hat and buttons are absolute and digital.

 -----
 The sticks and hat data are accessed as compass directions
 (e.g. NW, NORTH). The compass constants are public so they can be 
 used in the rest of the application.

 The buttons values (booleans) can be accessed individually, or 
 together in an array. 

 The rumbler can be switched on/off, and its current status retrieved.

 created by Andrew Davison, appreciated & hacked up by GroG :)
 */

package org.myrobotlab.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.data.JoystickData;
import org.slf4j.Logger;

//import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;

/**
 * Joystick - The joystick service supports reading data from buttons and
 * joysticks. It supports many joysticks, though the button mapping may vary
 * from controller to controller. Component is a general descriptor for any form
 * of "Component" from JInput. Since Component is not serializable we need to
 * move the relevant descriptive data to InputDevice and send that information
 * to describe JInput's Components
 * 
 * To Test java -Djava.library.path="./" -cp "./*"
 * net.java.games.input.test.ControllerReadTest
 */
public class Joystick extends Service implements Runnable {

	public final static Logger log = LoggerFactory.getLogger(Joystick.class);
	private static final long serialVersionUID = 1L;

	/**
	 * array of "real" hardware non-serializable controls
	 */
	transient net.java.games.input.Controller[] hardwareControllers;
	/**
	 * current selected controller
	 */
	transient net.java.games.input.Controller hardwareController = null;
	/**
	 * array of "real" non-serializable hardware hwComponents
	 */
	transient net.java.games.input.Component[] hardwareComponents; // holds the
	transient Rumbler[] hardwareRumblers;
	transient InputPollingThread pollingThread = null;

	TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();

	// FIXME - lame not just last index :P
	int rumblerIdx; // index for the rumbler being used
	boolean rumblerOn = false; // whether rumbler is on or off

	/**
	 * non-transient serializable definition
	 */
	Map<String, Mapper> mappers = new HashMap<String, Mapper>();
	Map<String, Component> components = null;
	
	String controller;

	static public class Component implements Serializable {
		private static final long serialVersionUID = 1L;
		public String id;
		public boolean isRelative = false;
		public boolean isAnalog = false;
		public String type;
		public int index;
		public float value = 0;

		public Component(int index, net.java.games.input.Component c) {

			this.index = index;
			this.isRelative = c.isRelative();
			this.isAnalog = c.isAnalog();
			this.type = c.getIdentifier().getClass().getSimpleName();
			this.id = c.getIdentifier().toString();
		}

		@Override
		public String toString() {
			return String.format("%d %s [%s] relative %b analog %b", index, type, id, isRelative, isAnalog);
		}
	}

	public class InputPollingThread extends Thread {
		public boolean isPolling = false;

		public InputPollingThread(String name) {
			super(name);
		}

		@Override
		public void run() {

			if (hardwareController == null) {
				error("controller is null - can not poll");
				return;
			}

			/* Get all the axis and buttons */
			net.java.games.input.Component[] hwComponents = hardwareController.getComponents();
			info("found %d hwComponents", hwComponents.length);

			isPolling = true;
			while (isPolling) {

				// get the data
				hardwareController.poll();

				// iterate through each component and compare last values
				for (int i = 0; i < hwComponents.length; i++) {

					net.java.games.input.Component hwComp = hwComponents[i];
					float input = hwComp.getPollData();
					String id = hwComp.getIdentifier().toString();
					Component component = components.get(id);
					if (component == null) {
						log.error("{} component is not valid", id);
						return;
					}

					// if delta enough
					if (Math.abs(input - component.value) > 0.0001) {

						if (mappers.containsKey(id)) {
							input = (float) mappers.get(id).calcOutput(input);
						}

						invoke("publishJoystickInput", new JoystickData(id, input));

					} // if (lastValue == null || Math.abs(input - lastValue) >
						// 0.0001)

					component.value = input;
				}

				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public Joystick(String n) {
		super(n);
	}

	public Map<String, Component> getComponents() {
		components = new HashMap<String, Component>();
		if (hardwareController == null) {
			info("getComponents no controller set");
			return components;
		}

		hardwareComponents = hardwareController.getComponents();
		if (hardwareComponents.length == 0) {
			error("getComponents no Components found");
			return components;
		}

		info("Num. Components: " + hardwareComponents.length);
		for (int i = 0; i < hardwareComponents.length; i++) {
			net.java.games.input.Component c = hardwareComponents[i];
			String id = c.getIdentifier().toString();
			Component component = new Component(i, c);
			log.info("found {}", component);
			components.put(id, component);
		}
		return components;
	}

	public Map<String, Integer> getControllers() {
		log.info(String.format("%s getting controllers", getName()));
		hardwareControllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		info(String.format("found %d controllers", hardwareControllers.length));
		controllerNames.clear();
		for (int i = 0; i < hardwareControllers.length; i++) {
			log.info(String.format("Found input device: %d %s", i, hardwareControllers[i].getName()));
			controllerNames.put(String.format("%d - %s", i, hardwareControllers[i].getName()), i);
		}
		return controllerNames;
	}

	public boolean isPolling() {
		return pollingThread != null;
	}

	public boolean isRumblerOn() {
		return rumblerOn;
	}

	public void map(String name, float x0, float x1, float y0, float y1) {
		Mapper mapper = new Mapper(x0, x1, y0, y1);
		mappers.put(name, mapper);
	}

	public void addInputListener(Service service) {
		service.subscribe(this.getName(), "publishJoystickInput");
	}

	public JoystickData publishJoystickInput(final JoystickData input) {
		log.info(String.format("publishJoystickInput %s", input));
		return input;
	}

	public boolean setController(int index) {
		log.info(String.format("attaching controller %d", index));

		stopPolling();

		if (index > -1 && index < hardwareControllers.length) {
			hardwareController = hardwareControllers[index];
			controller = String.format("%d - %s", index, hardwareController.getName());
			// invoke("getComponents");
			getComponents();
			startPolling();
			broadcastState();
			return true;
		}

		controller = null;
		error("setController %d bad index", index);
		return false;
	}

	public boolean setController(String s) {
		if (controllerNames.containsKey(s)) {
			setController(controllerNames.get(s));
			return true;
		}
		error("setController - can't find %s", s);
		return false;
	}

	public void setRumbler(boolean switchOn) {
		if (rumblerIdx != -1) {
			if (switchOn)
				hardwareRumblers[rumblerIdx].rumble(0.8f); // almost full on for
			// last
			// rumbler
			else
				// switch off
				hardwareRumblers[rumblerIdx].rumble(0.0f);
			rumblerOn = switchOn; // record rumbler's new status
		}
	} // end of setRumbler()

	synchronized public void startPolling() {
		log.info(String.format("startPolling - starting new polling thread %s_polling", getName()));
		if (pollingThread != null) {
			log.warn("already polling, stop polling first");
			return;
		}
		pollingThread = new InputPollingThread(String.format("%s_polling", getName()));
		pollingThread.start();
	}

	public void stopPolling() {
		if (pollingThread != null) {
			pollingThread.isPolling = false;
			pollingThread = null;
		}
	}

	public void startService() {
		super.startService();
		invoke("getControllers");
	}
	
	public String getController(){
	  return controller;
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

		ServiceType meta = new ServiceType(Joystick.class.getCanonicalName());
		meta.addDescription("service allows interfacing with a keyboard, joystick or gamepad");
		meta.addCategory("control");
		meta.addDependency("net.java.games.jinput", "20120914");
		return meta;
	}

	public static void main(String args[]) {
		LoggingFactory.init();
		try {

			// Runtime.setRuntimeName("joyrun");
			Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
			// joy.mapId("x", "rx");
			// joy.map("y", -1, 1, 0, 180);
			Runtime.start("cli", "Cli");
			Runtime.start("j", "SwingGui");
			RemoteAdapter remote = (RemoteAdapter)Runtime.start("rj", "RemoteAdapter");
			Service.sleep(3000);
			remote.connect("tcp://127.0.0.1:6767");
			
			/*
			 * RemoteAdapter remote = (RemoteAdapter) Runtime.create("remote",
			 * "RemoteAdapter"); remote.listenOnStartup(false);
			 * remote.connect("tcp://127.0.0.1:6767");
			 */

			// Runtime.start("webgui", "WebGui");
			// Runtime.start("python", "Python");

			// joy.setController(7);
			// joy.setController(5);
			// joy.startPolling();

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

}
