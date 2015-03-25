// GamePadController.java
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

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.POV;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.slf4j.Logger;

public class Joystick extends Service {

	static public class Button implements Serializable {
		private static final long serialVersionUID = 1L;
		public String id;
		public String type;
		public Float value;

		public Button(String id, String type, Float value) {
			this.id = id;
			this.type = type;
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("%s %s %f", id, type, value);
		}
	}

	public class InputPollingThread extends Thread {
		public boolean isPolling = false;

		public InputPollingThread(String name) {
			super(name);
		}

		@Override
		public void run() {

			if (controller == null) {
				error("controller is null - can not poll");
				return;
			}

			/* Get all the axis and buttons */
			Component[] components = controller.getComponents();
			info("found %d components", components.length);

			isPolling = true;
			while (isPolling) {

				/* Poll the controller */
				controller.poll();

				for (int i = 0; i < components.length; i++) {

					Component component = components[i];
					Identifier identifier = component.getIdentifier();

					String id = identifier.toString();
					// FIXME - or something like that....
					if (mapIds.containsKey(id)) {
						id = mapIds.get(id);
					}
					String method = String.format("publish%s", id.toUpperCase());

					Float input = components[i].getPollData();

					// FIXME - pre-load with 0.0 then don't need to test for
					// null
					Float lastValue = null;
					if (lastValues.containsKey(id)) {
						lastValue = lastValues.get(id);
					}

					Float output = input;
					if (lastValue == null || Math.abs(input - lastValue) > 0.0001) {

						if (mappers.containsKey(id)) {
							output = (float) mappers.get(id).calc(input);
						}

						Type type = controller.getType();

						String ctype = TYPE_BUTTON;
						if (identifier.getClass() == Component.Identifier.Axis.class) {
							ctype = TYPE_AXIS;
						} else if (identifier.getClass() == Component.Identifier.Key.class) {
							ctype = TYPE_KEY;
						}

						// FIXME - change to "generalized" Component - not
						// Button
						// FIXME - configuration to turn this on or off
						invoke("publishButton", new Button(id.toString(), ctype, output));

						if ((type == Controller.Type.GAMEPAD) || (type == Controller.Type.STICK)) {
							invoke(method, output);
						} else if (type == Type.KEYBOARD) {
							// invoke("publishKey", output);
							invoke("publishKey", id);
						} else if (type == Type.MOUSE) {
							invoke(method, output);
						} else {
							// error("unsupported controller type");
						}

					} // if (lastValue == null || Math.abs(input - lastValue) >
						// 0.0001)

					lastValues.put(id, input);
				}

				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public final static Logger log = LoggerFactory.getLogger(Joystick.class.getCanonicalName());

	private static final long serialVersionUID = 1L;
	public static final int NUM_BUTTONS = 12;

	public static final int BUTTON_OFF = 0;

	public static final int BUTTON_ON = 1;
	// FIXME - don't really care remove all this
	// public stick and hat compass positions
	public static final int NUM_COMPASS_DIRS = 9;
	public static final int NW = 0;
	public static final int NORTH = 1;
	public static final int NE = 2;
	public static final int WEST = 3;
	public static final int NONE = 4; // default value
	public static final int EAST = 5;
	public static final int SW = 6;

	public static final int SOUTH = 7;
	public static final int SE = 8;
	public final static String TYPE_BUTTON = "button";

	// FIXME - needs refactoring - remove all below
	// need a serializable hashmap string & index

	public final static String TYPE_AXIS = "axis";
	public final static String TYPE_KEY = "key";
	// array of "real" hardware non-serializable controls
	transient Controller[] controllers;

	// array of "real" hardware non-serializable components
	transient Component[] components; // holds the components
	// array of "real" hardware non-serializable bumplers
	transient Rumbler[] rumblers;
	// these data structures are serializable
	HashMap<String, Integer> controllerNames = new HashMap<String, Integer>();

	HashMap<String, Integer> componentNames = new HashMap<String, Integer>();
	HashMap<String, Float> lastValues = new HashMap<String, Float>();

	transient InputPollingThread pollingThread = null;
	// current selected controller
	transient Controller controller = null;
	// TODO - remove - direction and any details (e.g. index) - should be
	// published
	private int xAxisIdx, yAxisIdx, zAxisIdx, rzAxisIdx;

	int rumblerIdx; // index for the rumbler being used
	boolean rumblerOn = false; // whether rumbler is on or off

	// TODO - define uber components - optional publishComponent method

	// indices for the analog sticks axes
	private int povIdx; // index for the POV hat
	private int buttonsIdx[]; // indices for the buttons

	private HashMap<String, Mapper> mappers = new HashMap<String, Mapper>();

	private HashMap<String, String> mapIds = new HashMap<String, String>();

	public static void main(String args[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// First you need to create controller.
		// http://theuzo007.wordpress.com/2012/09/02/joystick-in-java-with-jinput/
		// JInputJoystick joystick = new JInputJoystick(Controller.Type.STICK,
		// Controller.Type.GAMEPAD);

		// Runtime.setRuntimeName("joyrun");
		Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
		// joy.mapId("x", "rx");
		// joy.map("y", -1, 1, 0, 180);
		Runtime.start("gui", "GUIService");
		// joy.test();

		joy.setController(2);
		joy.startPolling();

	}

	public Joystick(String n) {
		super(n);
		getControllers();
	}

	// --pov end---
	// --buttons begin---
	public void add0Listener(String service, String method) {
		addListener("publish0", service, method);
	}

	public void add10Listener(String service, String method) {
		addListener("publish10", service, method);
	}

	public void add11Listener(String service, String method) {
		addListener("publish11", service, method);
	}

	public void add12Listener(String service, String method) {
		addListener("publish12", service, method);
	}

	public void add13Listener(String service, String method) {
		addListener("publish13", service, method);
	}

	public void add1Listener(String service, String method) {
		addListener("publish1", service, method);
	}

	public void add2Listener(String service, String method) {
		addListener("publish2", service, method);
	}

	public void add3Listener(String service, String method) {
		addListener("publish3", service, method);
	}

	public void add4Listener(String service, String method) {
		addListener("publish4", service, method);
	}

	public void add5Listener(String service, String method) {
		addListener("publish5", service, method);
	}

	public void add6Listener(String service, String method) {
		addListener("publish6", service, method);
	}

	public void add7Listener(String service, String method) {
		addListener("publish7", service, method);
	}

	public void add8Listener(String service, String method) {
		addListener("publish8", service, method);
	}

	public void add9Listener(String service, String method) {
		addListener("publish9", service, method);
	}

	public void addKeyListener(Service service) {
		addListener("publishKey", service.getName(), "onKey", String.class);
	}

	public void addKeyListener(String serviceName) {
		ServiceInterface s = Runtime.getService(serviceName);
		addKeyListener((Service) s);
	}

	// --axis end---
	// --pov begin---
	public void addPOVListener(String service, String method) {
		addListener("publishPOV", service, method);
	}

	public void addRXListener(ServoControl sc) {
		map("publishRX", -1.0f, 1.0f, 0.0f, 180f);
		addRXListener(sc.getName(), "moveTo");
	}

	public void addRXListener(String service, String method) {
		addListener("publishRX", service, method);
	}

	public void addRYListener(ServoControl sc) {
		map("publishRY", -1.0f, 1.0f, 0.0f, 180f);
		addRYListener(sc.getName(), "moveTo");
	}

	// ------------------- trigger a rumbler -------------------

	public void addRYListener(String service, String method) {
		addListener("publishRY", service, method);
	}

	public void addRZListener(ServoControl sc) {
		map("publishRZ", -1.0f, 1.0f, 0.0f, 180f);
		addRZListener(sc.getName(), "moveTo");
	}

	// ----Component Publishing Begin ---------

	public void addRZListener(String service, String method) {
		addListener("publishRZ", service, method);
	}

	// ---add listeners begin---
	// --axis begin---
	public void addXListener(ServoControl sc) {
		map("publishX", -1.0f, 1.0f, 0.0f, 180f);
		addXListener(sc.getName(), "moveTo");
	}

	public void addXListener(String service, String method) {
		addListener("publishX", service, method);
	}

	public void addYListener(ServoControl sc) {
		map("publishY", -1.0f, 1.0f, 0.0f, 180f);
		addYListener(sc.getName(), "moveTo");
	}

	public void addYListener(String service, String method) {
		addListener("publishY", service, method);
	}

	public void addZListener(ServoControl sc) {
		map("publishZ", -1.0f, 1.0f, 0.0f, 180f);
		addZListener(sc.getName(), "moveTo");
	}

	public void addZListener(String service, String method) {
		addListener("publishZ", service, method);
	}

	/**
	 * Search through comps[] for NUM_BUTTONS buttons, storing their indices in
	 * buttonsIdx[]. Ignore excessive buttons. If there aren't enough buttons,
	 * then fill the empty spots in buttonsIdx[] with -1's.
	 */
	private void findButtons(Component[] comps) {
		buttonsIdx = new int[NUM_BUTTONS];
		int numButtons = 0;
		Component c;

		for (int i = 0; i < comps.length; i++) {
			c = comps[i];
			if (isButton(c)) { // deal with a button
				if (numButtons == NUM_BUTTONS) // already enough buttons
					log.info("Found an extra button; index: " + i + ". Ignoring it");
				else {
					buttonsIdx[numButtons] = i; // store button index
					log.info("Found " + c.getName() + "; index: " + i);
					numButtons++;
				}
			}
		}

		// fill empty spots in buttonsIdx[] with -1's
		if (numButtons < NUM_BUTTONS) {
			log.info("Too few buttons (" + numButtons + "); expecting " + NUM_BUTTONS);
			while (numButtons < NUM_BUTTONS) {
				buttonsIdx[numButtons] = -1;
				numButtons++;
			}
		}
	} // end of findButtons()

	// xbox 360 specific - end --

	/**
	 * TODO - remove this - not needed Search through comps[] for id, returning
	 * the corresponding array index, or -1
	 */
	private int findCompIndex(Component[] comps, Component.Identifier id, String nm) {
		Component c;
		for (int i = 0; i < comps.length; i++) {
			c = comps[i];
			if ((c.getIdentifier() == id) && !c.isRelative()) {
				log.info("Found " + c.getName() + "; index: " + i);
				return i;
			}
		}

		log.info("No " + nm + " component found");
		return -1;
	} // end of findCompIndex()

	/**
	 * Possibly useful for Auto-Find controller Search the array of controllers
	 * until a suitable game pad controller is found (either of type GAMEPAD or
	 * STICK).
	 */
	private Controller findGamePad(Controller[] cs) {
		Controller.Type type;
		int i = 0;
		while (i < cs.length) {
			type = cs[i].getType();
			if ((type == Controller.Type.GAMEPAD) || (type == Controller.Type.STICK))
				break;
			i++;
		}

		if (i == cs.length) {
			log.warn("No game pad found");
		} else
			log.info("Game pad index: " + i);

		return cs[i];
	} // end of findGamePad()

	/**
	 * Find the rumblers. Use the last rumbler for making vibrations, an
	 * arbitrary decision.
	 */
	private void findRumblers(Controller controller) {
		// get the game pad's rumblers
		rumblers = controller.getRumblers();
		if (rumblers.length == 0) {
			log.info("No Rumblers found");
			rumblerIdx = -1;
		} else {
			log.info("Rumblers found: " + rumblers.length);
			rumblerIdx = rumblers.length - 1; // use last rumbler
		}
	}

	public boolean[] getButtons()
	/*
	 * Return all the buttons in a single array. Each button value is a boolean.
	 */
	{
		boolean[] buttons = new boolean[NUM_BUTTONS];
		float value;
		for (int i = 0; i < NUM_BUTTONS; i++) {
			value = components[buttonsIdx[i]].getPollData();
			buttons[i] = ((value == 0.0f) ? false : true);
		}
		return buttons;
	} // end of getButtons()

	@Override
	public String[] getCategories() {
		return new String[] { "control" };
	}

	/**
	 * Return the axes as a single compass value
	 * 
	 * @param xA
	 * @param yA
	 * @return
	 */
	private int getCompassDir(int xA, int yA) {
		float xCoord = components[xA].getPollData();
		float yCoord = components[yA].getPollData();
		// log.info("(x,y): (" + xCoord + "," + yCoord + ")");

		int xc = Math.round(xCoord);
		int yc = Math.round(yCoord);
		// log.info("Rounded (x,y): (" + xc + "," + yc + ")");

		if ((yc == -1) && (xc == -1)) // (y,x)
			return NW;
		else if ((yc == -1) && (xc == 0))
			return NORTH;
		else if ((yc == -1) && (xc == 1))
			return NE;
		else if ((yc == 0) && (xc == -1))
			return WEST;
		else if ((yc == 0) && (xc == 0))
			return NONE;
		else if ((yc == 0) && (xc == 1))
			return EAST;
		else if ((yc == 1) && (xc == -1))
			return SW;
		else if ((yc == 1) && (xc == 0))
			return SOUTH;
		else if ((yc == 1) && (xc == 1))
			return SE;
		else {
			log.info("Unknown (x,y): (" + xc + "," + yc + ")");
			return NONE;
		}
	} // end of getCompassDir()

	/**
	 * Store the indices for the analog sticks axes (x,y) and (z,rz), POV hat,
	 * and button components of the controller.
	 */

	public HashMap<String, Integer> getComponents() {
		HashMap<String, Integer> ret = new HashMap<String, Integer>();
		if (controller == null) {
			error("no controller set");
			return ret;
		}

		components = controller.getComponents();
		if (components.length == 0) {
			error("No Components found");
			return ret;
		} else {
			info("Num. Components: " + components.length);
		}

		Component c;
		for (int i = 0; i < components.length; i++) {
			c = components[i];
			Identifier id = c.getIdentifier();
			if (id instanceof Component.Identifier.Axis) {

			}
			ret.put(c.getIdentifier().toString(), i);
		}

		// substitutions
		for (Map.Entry<String, String> entry : mapIds.entrySet()) {
			String from = entry.getKey();
			String to = entry.getValue();
			if (ret.containsKey(from)) {
				Integer move = ret.get(from);
				ret.put(mapIds.get(from), move);
			}
		}

		// get the indices for the axes of the analog sticks: (x,y) and (z,rz)
		xAxisIdx = findCompIndex(components, Component.Identifier.Axis.X, "x-axis");
		yAxisIdx = findCompIndex(components, Component.Identifier.Axis.Y, "y-axis");

		zAxisIdx = findCompIndex(components, Component.Identifier.Axis.Z, "z-axis");
		rzAxisIdx = findCompIndex(components, Component.Identifier.Axis.RZ, "rz-axis");

		// get POV hat index
		povIdx = findCompIndex(components, Component.Identifier.Axis.POV, "POV hat");

		findButtons(components);
		return ret;
	}

	public HashMap<String, Integer> getControllers() {
		log.info(String.format("%s getting controllers", getName()));
		controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		info(String.format("found %d controllers", controllers.length));
		controllerNames.clear();
		for (int i = 0; i < controllers.length; i++) {
			log.info(String.format("Found input device: %d %s", i, controllers[i].getName()));
			controllerNames.put(String.format("%d - %s", i, controllers[i].getName()), i);
		}
		return controllerNames;
	}

	@Override
	public String getDescription() {
		return "used for interfacing with a Joystick";
	}

	public int getHatDir()
	// Return the POV hat's direction as a compass direction
	{
		if (povIdx == -1) {
			log.info("POV hat data unavailable");
			return NONE;
		} else {
			float povDir = components[povIdx].getPollData();
			if (povDir == POV.CENTER) // 0.0f
				return NONE;
			else if (povDir == POV.DOWN) // 0.75f
				return SOUTH;
			else if (povDir == POV.DOWN_LEFT) // 0.875f
				return SW;
			else if (povDir == POV.DOWN_RIGHT) // 0.625f
				return SE;
			else if (povDir == POV.LEFT) // 1.0f
				return WEST;
			else if (povDir == POV.RIGHT) // 0.5f
				return EAST;
			else if (povDir == POV.UP) // 0.25f
				return NORTH;
			else if (povDir == POV.UP_LEFT) // 0.125f
				return NW;
			else if (povDir == POV.UP_RIGHT) // 0.375f
				return NE;
			else { // assume center
				log.info("POV hat value out of range: " + povDir);
				return NONE;
			}
		}
	} // end of getHatDir()

	/**
	 * @return - return the (x,y) analog stick compass direction
	 */
	public int getXYStickDir() {
		if ((xAxisIdx == -1) || (yAxisIdx == -1)) {
			log.info("(x,y) axis data unavailable");
			return NONE;
		} else
			return getCompassDir(xAxisIdx, yAxisIdx);
	}

	/**
	 * @return the (z,rz) analog stick compass direction
	 */
	public int getZRZStickDir() {
		if ((zAxisIdx == -1) || (rzAxisIdx == -1)) {
			log.info("(z,rz) axis data unavailable");
			return NONE;
		} else
			return getCompassDir(zAxisIdx, rzAxisIdx);
	}

	/**
	 * Return true if the component is a digital/absolute button, and its
	 * identifier name ends with "Button" (i.e. the identifier class is
	 * Component.Identifier.Button).
	 */
	private boolean isButton(Component c) {
		if (!c.isAnalog() && !c.isRelative()) { // digital and absolute
			String className = c.getIdentifier().getClass().getName();
			// log.info(c.getName() + " identifier: " + className);
			if (className.endsWith("Button"))
				return true;
		}
		return false;
	}

	public boolean isButtonPressed(int pos)
	/*
	 * Return the button value (a boolean) for button number 'pos'. pos is in
	 * the range 1-NUM_BUTTONS to match the game pad button labels.
	 */
	{
		if ((pos < 1) || (pos > NUM_BUTTONS)) {
			log.info("Button position out of range (1-" + NUM_BUTTONS + "): " + pos);
			return false;
		}

		if (buttonsIdx[pos - 1] == -1) // no button found at that pos
			return false;

		float value = components[buttonsIdx[pos - 1]].getPollData();
		// array range is 0-NUM_BUTTONS-1
		return ((value == 0.0f) ? false : true);
	} // end of isButtonPressed()

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

	public void mapId(String from, String to) {
		mapIds.put(from, to);
		invoke("getComponents");
	}

	public Float publish0(Float value) {
		return value;
	}

	public Float publish1(Float value) {
		return value;
	}

	public Float publish10(Float value) {
		return value;
	}

	public Float publish11(Float value) {
		return value;
	}

	public Float publish12(Float value) {
		return value;
	}

	public Float publish13(Float value) {
		return value;
	}

	public Float publish2(Float value) {
		return value;
	}

	public Float publish3(Float value) {
		return value;
	}

	public Float publish4(Float value) {
		return value;
	}

	public Float publish5(Float value) {
		return value;
	}

	public Float publish6(Float value) {
		return value;
	}

	public Float publish7(Float value) {
		return value;
	}

	public Float publish8(Float value) {
		return value;
	}

	public Float publish9(Float value) {
		return value;
	}

	public Button publishButton(final Button button) {
		return button;
	}

	/**
	 * internal publishing point - private ?
	 * 
	 * @param key
	 */
	public String publishKey(String key) {
		return key;
	}

	public Float publishPOV(Float pov) {
		return pov;
	}

	// xbox 360 specific - begin --
	public Float publishRX(Float z) {
		// invoke("publishZ", z); possible solution ;p
		return z;
	}

	//
	public Float publishRY(Float z) {
		// invoke("publishRZ", z); possible solition :P
		return z;
	}

	public Float publishRZ(Float rz) {
		return rz;
	}

	public Float publishX(Float x) {
		return x;
	}

	public Float publishY(Float y) {
		return y;
	}

	// --buttons end---

	public Float publishZ(Float z) {
		return z;
	}

	public boolean setController(int index) {
		log.info(String.format("attaching controller %d", index));
		if (index > -1 && index < controllers.length) {
			controller = controllers[index];
			findRumblers(controller);
			invoke("getComponents");
			// broadcastState();
			return true;
		}
		error("bad index");
		return false;
	}

	public boolean setController(String s) {
		if (controllerNames.containsKey(s)) {
			setController(controllerNames.get(s));
			return true;
		}
		error("cant find %s", s);
		return false;
	}

	// ---add listeners end---

	// ----Component Publishing End ---------

	public void setRumbler(boolean switchOn) {
		if (rumblerIdx != -1) {
			if (switchOn)
				rumblers[rumblerIdx].rumble(0.8f); // almost full on for last
													// rumbler
			else
				// switch off
				rumblers[rumblerIdx].rumble(0.0f);
			rumblerOn = switchOn; // record rumbler's new status
		}
	} // end of setRumbler()

	public void startPolling() {
		log.info(String.format("startPolling - starting new polling thread %s_polling", getName()));
		if (pollingThread != null) {
			stopPolling();
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

	@Override
	public Status test() {
		Status status = super.test();

		try {

			Runtime.start("gui", "GUIService");
			Joystick joy = (Joystick) Runtime.start("joy", "Joystick");
			// Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			Servo servo = (Servo) Runtime.start("servo", "Servo");

			// arduino.connect("COM15");
			// servo.attach(arduino, 4);

			// joy.addZListener(servo);

			// joy.setController(2);

			/*
			 * RemoteAdapter remote = (RemoteAdapter) Runtime.start("remote",
			 * "RemoteAdapter"); Runtime.start("python", "Python");
			 * 
			 * 
			 * sleep(1000);
			 * 
			 * // from java.net import URI // TODO - PREFIX (CHOOSE PREFIX SO NO
			 * NAME COLLISION) // SIMPLE CONNECT URI Message msg =
			 * remote.createMessage("", "register", joy); remote.sendRemote(new
			 * URI("tcp://127.0.0.1:6868"), msg);
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}

		return status;
	}

}
