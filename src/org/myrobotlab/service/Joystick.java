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

import java.util.TreeMap;

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Component.POV;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Joystick extends Service {

	private static final long serialVersionUID = 1L;

	public static final int NUM_BUTTONS = 12;

	public static final int BUTTON_OFF = 0;
	public static final int BUTTON_ON = 1;

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

	public final static Logger log = LoggerFactory.getLogger(Joystick.class.getCanonicalName());

	public final static String Z_AXIS = "Z_AXIS";
	public final static String Z_ROTATION = "Z_ROTATION";

	transient Controller[] controllers;
	TreeMap<String, Integer> controllerNames = new TreeMap<String, Integer>();

	InputPollingThread pollingThread = null;
	int myDeviceIndex = -1;
	transient Controller controller = null;
	double[] lastValues;

	public class InputPollingThread extends Thread {
		public boolean isPolling = false;

		public InputPollingThread(String name) {
			super(name);
		}

		public void run() {

			if (controller == null) {
				error("controller is null - can not poll");
			}

			/* Get all the axis and buttons */
			Component[] components = controller.getComponents();
			lastValues = new double[components.length];

			isPolling = true;
			while (isPolling) {

				/* Poll the controller */
				controller.poll();

				// StringBuffer buffer = new StringBuffer();

				// TODO - selectively publish ... publish Direction? Yes
				/* For each component, get it's name, and it's current value */

				// FIXME - switch statement (it's Java 7 !)
				for (int i = 0; i < components.length; i++) {


					Component component = components[i];
					Identifier id = component.getIdentifier();

					String n = components[i].getName();
					float data = components[i].getPollData();

					// buffer.append(n);
					// TODO - invoke based on
					// invoke(String.trim(component.getName()),
					// mapMultiplier(getName()), mapOffset(getName()) - REFACTOR
					// REFACTOR !!! use switch statement
					if (Identifier.Axis.Z.equals(id)) {
						if (lastValues[i] != data) {
							if (ZAxisTransform) {
								invoke("ZAxis", (int) (ZAxisMultiplier * data) + ZAxisOffset);
							} else {
								invoke("ZAxisRaw", data);
							}
						}

					} else if (Identifier.Axis.RZ.equals(id)) {
						if (lastValues[i] != data) {
							if (ZRotTransform) {
								invoke("ZRotation", (int) (ZRotMultiplier * data) + ZRotOffset);
							} else {
								invoke("ZRotationRaw", data);
							}
						}
					} else if (Identifier.Axis.X.equals(id)) {
						if (lastValues[i] != data) {
							if (XAxisTransform) {
								invoke("XAxis", (int) (XAxisMultiplier * data) + XAxisOffset);
							} else {
								invoke("XAxisRaw", data);
							}
						}
					} else if (Identifier.Axis.Y.equals(id)) {
						if (lastValues[i] != data) {
							if (YAxisTransform) {
								invoke("YAxis", (int) (YAxisMultiplier * data) + YAxisOffset);
							} else {
								invoke("YAxisRaw", data);
							}
						}
					} else if (Identifier.Axis.POV.equals(id)) {
						if (lastValues[i] != data) {
							if (hatTransform) {
								invoke("hatSwitch", (int) (hatMultiplier * data) + hatOffset);
							} else {
								invoke("hatSwitchRaw", data);
							}
						}
						// WTF ??? - A on Linux _0 on Windows, really? I mean
						// really? Why?
					} else if (Identifier.Button.A.equals(id) || Identifier.Button._0.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button1", pos);
						}
					} else if (Identifier.Button.B.equals(id) || Identifier.Button._1.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button2", pos);
						}
					} else if (Identifier.Button.C.equals(id) || Identifier.Button._2.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button3", pos);
						}
					} else if (Identifier.Button.X.equals(id) || Identifier.Button._3.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button4", pos);
						}
					} else if (Identifier.Button.Y.equals(id) || Identifier.Button._4.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button5", pos);
						}
					} else if (Identifier.Button.Z.equals(id) || Identifier.Button._5.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button6", pos);
						}
					} else if (Identifier.Button.LEFT_THUMB.equals(id) || Identifier.Button._6.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button7", pos);
						}
					} else if (Identifier.Button.RIGHT_THUMB.equals(id) || Identifier.Button._7.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button8", pos);
						}
					} else if (Identifier.Button.LEFT_THUMB2.equals(id) || Identifier.Button._8.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button9", pos);
						}
					} else if (Identifier.Button.RIGHT_THUMB2.equals(id) || Identifier.Button._9.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button10", pos);
						}
					} else if (Identifier.Button.SELECT.equals(id) || Identifier.Button._10.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button11", pos);
						}
					} else if (Identifier.Button.UNKNOWN.equals(id) || Identifier.Button._11.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button12", pos);
						}
					} else if (Identifier.Button._12.equals(id)) {
						int pos = (int) data;
						if (lastValues[i] != data) {
							invoke("button13", pos);
						}
					} else {
						log.debug(String.format("unknown identifier %s", id.toString()));
					}

					lastValues[i] = data;

					/*
					 * buffer.append(": "); if (components[i].isAnalog()) { //
					 * Get the value at the last poll of this component
					 * buffer.append(components[i].getPollData()); } else {
					 * buffer.append(components[i].getPollData()); if
					 * (components[i].getPollData() == 1.0f) {
					 * buffer.append("On"); } else { buffer.append("Off"); } }
					 */
				}

				// log.info(buffer.toString());
				/*
				 * Sleep for 20 millis, this is just so the example doesn't
				 * thrash the system. FIXME - can a polling system be avoided -
				 * could this block with the JNI code?
				 */
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public Joystick(String n) {
		super(n);

		log.info(String.format("%s getting controllers", n));
		controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
		info(String.format("found %d controllers", controllers.length));
		for (int i = 0; i < controllers.length; i++) {
			log.info(String.format("Found input device: %d %s", i, controllers[i].getName()));
			// if (controllers[i].getType() == Controller.Type.GAMEPAD ||
			// controllers[i].getType() == Controller.Type.STICK) {
			controllerNames.put(String.format("%d - %s", i, controllers[i].getName()), i);
			// }
			// search for gamepad or joystick
		}
	}

	public boolean attach(Servo servo, String axis) {
		if (Z_AXIS.equals(axis)) {
			servo.subscribe("ZAxis", getName(), "moveTo", Integer.class);
			return true;
		} else if (Z_ROTATION.equals(axis)) {
			servo.subscribe("ZRotation", getName(), "moveTo", Integer.class);
			return true;
		}

		error("unknown axis %s", axis);
		return false;
	}

	public boolean setController(int index) {
		log.info(String.format("attaching controller %d", index));
		if (index > -1 && index < controllers.length) {
			controller = controllers[index];
			// findRumblers(controller);
			log.info("here 1");
			findCompIndices(controller);
			log.info("here 2");
			return true;
		}
		log.info("here 3");
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

	public boolean isPolling()
	{
		return pollingThread != null;
	}


	@Override
	public String getDescription() {
		return "used for interfacing with a Joystick";
	}

	public TreeMap<String, Integer> getControllerNames() {
		return controllerNames;
	}

	// ///////////////////////////
	private Component[] comps; // holds the components
	private int xAxisIdx, yAxisIdx, zAxisIdx, rzAxisIdx;
	// indices for the analog sticks axes
	private int povIdx; // index for the POV hat
	private int buttonsIdx[]; // indices for the buttons

	private Rumbler[] rumblers;
	private int rumblerIdx; // index for the rumbler being used
	private boolean rumblerOn = false; // whether rumbler is on or off

	/**
	 * Search the array of controllers until a suitable game pad controller is
	 * found (either of type GAMEPAD or STICK).
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
	 * Search through comps[] for id, returning the corresponding array index,
	 * or -1
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

	private boolean isButton(Component c)
	/*
	 * Return true if the component is a digital/absolute button, and its
	 * identifier name ends with "Button" (i.e. the identifier class is
	 * Component.Identifier.Button).
	 */
	{
		if (!c.isAnalog() && !c.isRelative()) { // digital and absolute
			String className = c.getIdentifier().getClass().getName();
			// log.info(c.getName() + " identifier: " + className);
			if (className.endsWith("Button"))
				return true;
		}
		return false;
	} // end of isButton()

	private void findRumblers(Controller controller)
	/*
	 * Find the rumblers. Use the last rumbler for making vibrations, an
	 * arbitrary decision.
	 */
	{
		// get the game pad's rumblers
		rumblers = controller.getRumblers();
		if (rumblers.length == 0) {
			log.info("No Rumblers found");
			rumblerIdx = -1;
		} else {
			log.info("Rumblers found: " + rumblers.length);
			rumblerIdx = rumblers.length - 1; // use last rumbler
		}
	} // end of findRumblers()

	private void findCompIndices(Controller controller)
	/*
	 * Store the indices for the analog sticks axes (x,y) and (z,rz), POV hat,
	 * and button components of the controller.
	 */
	{
		comps = controller.getComponents();
		if (comps.length == 0) {
			log.info("No Components found");
			System.exit(0);
		} else
			log.info("Num. Components: " + comps.length);

		// get the indices for the axes of the analog sticks: (x,y) and (z,rz)
		xAxisIdx = findCompIndex(comps, Component.Identifier.Axis.X, "x-axis");
		yAxisIdx = findCompIndex(comps, Component.Identifier.Axis.Y, "y-axis");

		zAxisIdx = findCompIndex(comps, Component.Identifier.Axis.Z, "z-axis");
		rzAxisIdx = findCompIndex(comps, Component.Identifier.Axis.RZ, "rz-axis");

		// get POV hat index
		povIdx = findCompIndex(comps, Component.Identifier.Axis.POV, "POV hat");

		findButtons(comps);
	} // end of findCompIndices()

	public int getXYStickDir()
	// return the (x,y) analog stick compass direction
	{
		if ((xAxisIdx == -1) || (yAxisIdx == -1)) {
			log.info("(x,y) axis data unavailable");
			return NONE;
		} else
			return getCompassDir(xAxisIdx, yAxisIdx);
	} // end of getXYStickDir()

	public int getZRZStickDir()
	// return the (z,rz) analog stick compass direction
	{
		if ((zAxisIdx == -1) || (rzAxisIdx == -1)) {
			log.info("(z,rz) axis data unavailable");
			return NONE;
		} else
			return getCompassDir(zAxisIdx, rzAxisIdx);
	} // end of getXYStickDir()

	private int getCompassDir(int xA, int yA)
	// Return the axes as a single compass value
	{
		float xCoord = comps[xA].getPollData();
		float yCoord = comps[yA].getPollData();
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

	public int getHatDir()
	// Return the POV hat's direction as a compass direction
	{
		if (povIdx == -1) {
			log.info("POV hat data unavailable");
			return NONE;
		} else {
			float povDir = comps[povIdx].getPollData();
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

	public boolean[] getButtons()
	/*
	 * Return all the buttons in a single array. Each button value is a boolean.
	 */
	{
		boolean[] buttons = new boolean[NUM_BUTTONS];
		float value;
		for (int i = 0; i < NUM_BUTTONS; i++) {
			value = comps[buttonsIdx[i]].getPollData();
			buttons[i] = ((value == 0.0f) ? false : true);
		}
		return buttons;
	} // end of getButtons()

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

		float value = comps[buttonsIdx[pos - 1]].getPollData();
		// array range is 0-NUM_BUTTONS-1
		return ((value == 0.0f) ? false : true);
	} // end of isButtonPressed()

	// ------------------- trigger a rumbler -------------------

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

	public boolean isRumblerOn() {
		return rumblerOn;
	}

	// ----Component Publishing Begin ---------
	public Integer XAxis(Integer value) {
		return value;
	}

	public Float XAxisRaw(Float value) {
		log.debug(String.format("XAxisRaw %s", value));
		return value;
	}

	public Integer YAxis(Integer value) {
		return value;
	}

	public Float YAxisRaw(Float value) {
		return value;
	}

	public Integer ZAxis(Integer value) {
		return value;
	}

	public Float ZAxisRaw(Float value) {
		return value;
	}

	public Integer ZRotation(Integer value) {
		return value;
	}

	public Float ZRotationRaw(Float value) {
		return value;
	}

	public Integer hatSwitch(Integer value) {
		return value;
	}

	public Float hatSwitchRaw(Float value) {
		return value;
	}

	/*
	public Integer button0(Integer value) {
		return value;
	}
	*/

	public Integer button1(Integer value) {
		return value;
	}

	public Integer button2(Integer value) {
		return value;
	}

	public Integer button3(Integer value) {
		return value;
	}

	public Integer button4(Integer value) {
		return value;
	}

	public Integer button5(Integer value) {
		return value;
	}

	public Integer button6(Integer value) {
		return value;
	}

	public Integer button7(Integer value) {
		return value;
	}

	public Integer button8(Integer value) {
		return value;
	}

	public Integer button9(Integer value) {
		return value;
	}

	public Integer button10(Integer value) {
		return value;
	}

	public Integer button11(Integer value) {
		return value;
	}

	public Integer button12(Integer value) {
		return value;
	}
	
	public Integer button13(Integer value) {
		return value;
	}

	// ----Component Publishing End ---------
	boolean hatTransform = false;
	int hatMultiplier = 1;
	int hatOffset = 0;

	public void setHatTransform(Integer multiplier, Integer offset) {
		hatTransform = true;
		hatMultiplier = multiplier;
		hatOffset = offset;
	}

	public void resetHatTransform() {
		hatTransform = false;
		hatMultiplier = 1;
		hatOffset = 0;
	}

	boolean XAxisTransform = false;
	int XAxisMultiplier = 1;
	int XAxisOffset = 0;

	public void setXAxisTransform(Integer multiplier, Integer offset) {
		XAxisTransform = true;
		XAxisMultiplier = multiplier;
		XAxisOffset = offset;
	}

	public void resetXAxisTransform() {
		XAxisTransform = false;
		XAxisMultiplier = 1;
		XAxisOffset = 0;
	}

	boolean YAxisTransform = false;
	int YAxisMultiplier = 1;
	int YAxisOffset = 0;

	public void setYAxisTransform(Integer multiplier, Integer offset) {
		YAxisTransform = true;
		YAxisMultiplier = multiplier;
		YAxisOffset = offset;
	}

	public void resetYAxisTransform() {
		YAxisTransform = false;
		YAxisMultiplier = 1;
		YAxisOffset = 0;
	}

	boolean ZAxisTransform = false;
	int ZAxisMultiplier = 1;
	int ZAxisOffset = 0;

	public void setZAxisTransform(Integer multiplier, Integer offset) {
		ZAxisTransform = true;
		ZAxisMultiplier = multiplier;
		ZAxisOffset = offset;
	}

	public void resetZAxisTransform() {
		ZAxisTransform = false;
		ZAxisMultiplier = 1;
		ZAxisOffset = 0;
	}

	boolean ZRotTransform = false;
	int ZRotMultiplier = 1;
	int ZRotOffset = 0;

	public void setZRotTransform(Integer multiplier, Integer offset) {
		ZRotTransform = true;
		ZRotMultiplier = multiplier;
		ZRotOffset = offset;
	}

	public void resetZRotTransform() {
		ZRotTransform = false;
		ZRotMultiplier = 1;
		ZRotOffset = 0;
	}

	public static void main(String args[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		// First you need to create controller.
		// http://theuzo007.wordpress.com/2012/09/02/joystick-in-java-with-jinput/
		// JInputJoystick joystick = new JInputJoystick(Controller.Type.STICK,
		// Controller.Type.GAMEPAD);

		Joystick joy = new Joystick("joystick");
		joy.startService();
		// joy.setController(2);
		// joy.startPolling();
		Runtime.start("gui", "GUIService");		

	}

}
