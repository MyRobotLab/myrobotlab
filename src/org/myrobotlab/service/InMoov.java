package org.myrobotlab.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.openni.OpenNIData;
import org.myrobotlab.openni.Skeleton;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class InMoov extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoov.class);

	// FIXME - startPIR - all other starts of complex composite service need to
	// try to get their Arduino from the Arduino MAP !!! FIRST
	// BEFORE CREATING ONE !!!

	// FIXME - EVERYTHING .. ya EVERYTHING a local top level reference !

	// OBJECTIVE - try only have complex composite interaction here - everything
	// else should be done directly to targeted services !!!
	// OBJECTIVE - always return a service !!!

	// port map NOT SURE ????
	// will no right & left and com ports
	// 3 definitions at the top left right and head
	// port index, local references

	// this is good, because arduino's ultimately are identified by port keys
	HashMap<String, Arduino> arduinos = new HashMap<String, Arduino>();

	// services which do not require a body part
	// or can influence multiple body parts

	// Dynamic reflective services such as WebGui & XMPP are to be left out of
	// Peer definitions

	String defaultLeftPort; // FIXME - THIS IS A BUG GET RID OF IT - ALL ACCESS
	// THROUGH MAP !!!
	String defaultRightPort; // FIXME - THIS IS A BUG GET RID OF IT - ALL ACCESS

	// THROUGH MAP !!!
	// FIXME ALL PEERS NEED TO BE PRIVATE - ACCESS THROUGH GETTERS WHICH DO A
	// Runtime.create !
	// hands and arms
	transient public InMoovHead head;
	transient public InMoovTorso torso;
								transient public InMoovArm leftArm;
	transient public InMoovHand leftHand;
	transient public InMoovArm rightArm;
	transient public InMoovHand rightHand;

	transient private HashMap<String, InMoovArm> arms = new HashMap<String, InMoovArm>();
	transient private HashMap<String, InMoovHand> hands = new HashMap<String, InMoovHand>();

	// peers
	transient public Sphinx ear;
	transient public Speech mouth;
	transient public Tracking eyesTracking;
	transient public Tracking headTracking;
	transient public OpenCV opencv;
	transient public MouthControl mouthControl;
	transient public Python python;

	transient public final static String LEFT = "left";
	transient public final static String RIGHT = "right";

	transient public OpenNI openni;

	transient public PID pid;

	boolean copyGesture = false;
	boolean firstSkeleton = true;
	boolean saveSkeletonFrame = false;

	// reflective or non-interactive peers
	// transient public WebGUI webgui;
	// transient public XMPP xmpp;
	// transient public Security security;

	boolean speakErrors = false;
	String lastInMoovError = "";

	// long lastActivityTime;

	int maxInactivityTimeSeconds = 120;

	//
	private boolean mute = false;

	public static int attachPauseMs = 100;

	public Integer pirPin = null;

	// ---------- new getter interface begin ---------------------------

	Long startSleep = null;

	// ---------- new getter interface begin ---------------------------

	Long lastPIRActivityTime = null;

	boolean useHeadForTracking = true;

	boolean useEyesForTracking = false;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// SHARING !!!
		peers.suggestAs("head.arduino", "left", "Arduino", "shared left arduino");
		peers.suggestAs("torso.arduino", "left", "Arduino", "shared left arduino");

		peers.suggestAs("leftArm.arduino", "left", "Arduino", "shared left arduino");
		peers.suggestAs("leftHand.arduino", "left", "Arduino", "shared left arduino");

		peers.suggestAs("rightArm.arduino", "right", "Arduino", "shared right arduino");
		peers.suggestAs("rightHand.arduino", "right", "Arduino", "shared right arduino");

		peers.suggestAs("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
		peers.suggestAs("eyesTracking.arduino", "left", "Arduino", "shared head Arduino");
		peers.suggestAs("eyesTracking.x", "head.eyeX", "Servo", "shared servo");
		peers.suggestAs("eyesTracking.y", "head.eyeY", "Servo", "shared servo");

		peers.suggestAs("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");
		peers.suggestAs("headTracking.arduino", "left", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.x", "head.rothead", "Servo", "shared servo");
		peers.suggestAs("headTracking.y", "head.neck", "Servo", "shared servo");

		peers.suggestAs("mouthControl.arduino", "left", "Arduino", "shared head Arduino");
		peers.suggestAs("mouthControl.mouth", "mouth", "Speech", "shared Speech");
		peers.suggestAs("mouthControl.jaw", "head.jaw", "Servo", "shared servo");

		peers.suggestRootAs("python", "python", "Python", "shared Python service");

		// put peer definitions in
		peers.put("torso", "InMoovTorso", "torso");
		peers.put("leftArm", "InMoovArm", "left arm");
		peers.put("leftHand", "InMoovHand", "left hand");
		peers.put("rightArm", "InMoovArm", "right arm");
		peers.put("rightHand", "InMoovHand", "right hand");

		peers.put("ear", "Sphinx", "InMoov spech recognition service");
		peers.put("eyesTracking", "Tracking", "Tracking for the eyes");
		peers.put("head", "InMoovHead", "the head");
		peers.put("headTracking", "Tracking", "Head tracking system");
		peers.put("mouth", "Speech", "InMoov speech service");
		peers.put("mouthControl", "MouthControl", "MouthControl");
		peers.put("opencv", "OpenCV", "InMoov OpenCV service");
		peers.put("openni", "OpenNI", "Kinect service");
		peers.put("pid", "PID", "PID service");

		return peers;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Runtime.start("gui", "GUIService");

		InMoov i01 = (InMoov) Runtime.start("i01", "InMoov");
		i01.test();

		// i01.copyGesture(true);

		// i01.test();

		/*
		 * 
		 * Runtime.createAndStart("gui", "GUIService");
		 * Runtime.createAndStart("python", "Python");
		 * 
		 * InMoov i01 = (InMoov)Runtime.createAndStart("i01","InMoov");
		 * 
		 * i01.startOpenNI();
		 */

		// InMoovTorso torso = (InMoovTorso)i01.startTorso("COM4");

		// i01.startMouth();
		// i01.startLeftArm("COM4");
		// i01.copyGesture(true);

		// Create two virtual ports for UART and user and null them together:
		// create 2 virtual ports
		/*
		 * VirtualSerialPort vp0 = new VirtualSerialPort("UART15");
		 * VirtualSerialPort vp1 = new VirtualSerialPort("COM15");
		 * 
		 * // make null modem cable ;) VirtualSerialPort.makeNullModem(vp0,
		 * vp1);
		 * 
		 * 
		 * // add virtual ports to the serial device factory
		 * SerialDeviceFactory.add(vp0); SerialDeviceFactory.add(vp1);
		 */

		// create the UART serial service
		// log.info("Creating a LIDAR UART Serial service named: " + getName() +
		// "SerialService");
		// String serialName = getName() + "SerialService";
		/*
		 * Serial serial0 = new Serial("UART15"); serial0.startService();
		 * serial0.connect("UART15");
		 * 
		 * Runtime.createAndStart("gui", "GUIService");
		 * Runtime.createAndStart("python", "Python");
		 * 
		 * InMoov i01 = (InMoov) Runtime.createAndStart("i01", "InMoov");
		 * i01.startMouth(); // i01.power(120); InMoovHand lefthand =
		 * i01.startLeftHand("COM15"); i01.leftHand.setRest(10, 10, 10, 10, 10);
		 * i01.beginCheckingOnInactivity(10);
		 */

		/*
		 * log.info("inactivity {}", i01.checkInactivity());
		 * 
		 * lefthand.moveTo(5, 10, 30, 40, 50);
		 * 
		 * log.info("inactivity {}", i01.checkInactivity());
		 * 
		 * lefthand.rest();
		 * 
		 * log.info("inactivity {}", i01.checkInactivity());
		 */

		/*
		 * 
		 * Tracking neck = i01.startHeadTracking(leftPort); i01.detach();
		 */

	}

	public InMoov(String n) {
		super(n);
		// addRoutes();
		// FIXME - mebbe starts with same error - don't say it again unless a
		// certain time has passed

	}

	public void addRoutes() {
		// register with runtime for any new services
		// their errors are routed to mouth
		subscribe(this.getName(), "publishError", "handleError");

		Runtime r = Runtime.getInstance();
		r.addListener(getName(), "registered");
	}

	public void armsFront() {
		moveHead(99, 82);
		moveArm("left", 9, 115, 96, 51);
		moveArm("right", 13, 104, 101, 49);
		moveHand("left", 61, 0, 14, 38, 15, 0);
		moveHand("right", 0, 24, 54, 50, 82, 180);
	}

	public void armsUp() {
		moveHead(160, 97);
		moveArm("left", 9, 85, 168, 18);
		moveArm("right", 0, 68, 180, 10);
		moveHand("left", 61, 38, 14, 38, 15, 64);
		moveHand("right", 0, 0, 0, 50, 82, 180);
	}

	public void atEase() {
		if (head != null) {
			head.rest();
		}
		if (rightHand != null) {
			rightHand.rest();
			rightHand.detach();
		}
		if (leftHand != null) {
			leftHand.rest();
			leftHand.detach();
		}
		if (rightArm != null) {
			rightArm.rest();
			rightArm.detach();
		}
		if (leftArm != null) {
			leftArm.rest();
			leftArm.detach();
		}
		if (torso != null) {
			torso.rest();
			torso.detach();
		}
	}

	public void attach() {
		if (head != null) {
			head.attach();
		}
		if (rightHand != null) {
			rightHand.attach();
		}
		if (leftHand != null) {
			sleep(attachPauseMs);
			leftHand.attach();
		}
		if (rightArm != null) {
			sleep(attachPauseMs);
			rightArm.attach();
		}
		if (leftArm != null) {
			sleep(100);
			leftArm.attach();
		}
		if (torso != null) {
			torso.attach();
		}
	}

	public void beginCheckingOnInactivity() {
		beginCheckingOnInactivity(maxInactivityTimeSeconds);
	}

	public void beginCheckingOnInactivity(int maxInactivityTimeSeconds) {
		this.maxInactivityTimeSeconds = maxInactivityTimeSeconds;
		// speakBlocking("power down after %s seconds inactivity is on",
		// this.maxInactivityTimeSeconds);
		log.info("power down after %s seconds inactivity is on", this.maxInactivityTimeSeconds);
		addLocalTask(5 * 1000, "checkInactivity");
	}

	@Override
	public void broadcastState() {
		if (leftHand != null) {
			leftHand.broadcastState();
		}

		if (rightHand != null) {
			rightHand.broadcastState();
		}

		if (leftArm != null) {
			leftArm.broadcastState();
		}

		if (rightArm != null) {
			rightArm.broadcastState();
		}

		if (head != null) {
			head.broadcastState();
		}

		if (torso != null) {
			torso.broadcastState();
		}

		if (headTracking != null) {
			headTracking.broadcastState();
		}

		if (eyesTracking != null) {
			eyesTracking.broadcastState();
		}
	}

	public void cameraOff() {
		if (opencv != null) {
			opencv.stopCapture();
		}
	}

	public void cameraOn() throws Exception {
		startOpenCV();
		opencv.capture();
	}

	public String captureGesture() {
		return captureGesture(null);
	}

	public String captureGesture(String gestureName) {
		StringBuffer script = new StringBuffer();

		String indentSpace = "";

		if (gestureName != null) {
			indentSpace = "  ";
			script.append(String.format("def %s():\n", gestureName));
		}

		if (head != null) {
			script.append(indentSpace);
			script.append(head.getScript(getName()));
		}

		if (leftArm != null) {
			script.append(indentSpace);
			script.append(leftArm.getScript(getName()));
		}
		if (rightArm != null) {
			script.append(indentSpace);
			script.append(rightArm.getScript(getName()));
		}

		if (leftHand != null) {
			script.append(indentSpace);
			script.append(leftHand.getScript(getName()));
		}
		if (rightHand != null) {
			script.append(indentSpace);
			script.append(rightHand.getScript(getName()));
		}

		if (torso != null) {
			script.append(indentSpace);
			script.append(torso.getScript(getName()));
		}

		send("python", "appendScript", script.toString());

		return script.toString();
	}

	public long checkInactivity() {
		// speakBlocking("checking");
		long lastActivityTime = getLastActivityTime();
		long now = System.currentTimeMillis();
		long inactivitySeconds = (now - lastActivityTime) / 1000;
		if (inactivitySeconds > maxInactivityTimeSeconds && isAttached()) {
			// speakBlocking("%d seconds have passed without activity",
			// inactivitySeconds);
			powerDown();
		} else {
			// speakBlocking("%d seconds have passed without activity",
			// inactivitySeconds);
			info("checking checkInactivity - %d seconds have passed without activity", inactivitySeconds);
		}
		return lastActivityTime;
	}

	public void clearTrackingPoints() {
		if (headTracking == null) {
			error("attach head before tracking");
		} else {
			headTracking.clearTrackingPoints();
		}
	}

	public void closePinch(String which) {
		moveHand(which, 130, 140, 180, 180, 180);
	}

	public boolean copyGesture(boolean b) throws Exception {
		log.info("copyGesture {}", b);
		if (b) {
			if (openni == null) {
				openni = startOpenNI();
			}
			speakBlocking("copying gestures");
			openni.startUserTracking();
		} else {
			speakBlocking("stop copying gestures");
			if (openni != null) {
				openni.stopCapture();
				firstSkeleton = true;
			}
		}

		copyGesture = b;
		return b;
	}

	public void daVinci() {
		moveHead(75, 79);
		moveArm("left", 9, 115, 28, 80);
		moveArm("right", 13, 118, 26, 80);
		moveHand("left", 61, 49, 14, 38, 15, 64);
		moveHand("right", 0, 24, 54, 50, 82, 180);
	}

	public void detach() {
		if (head != null) {
			head.detach();
		}
		if (rightHand != null) {
			rightHand.detach();
		}
		if (leftHand != null) {
			leftHand.detach();
		}
		if (rightArm != null) {
			rightArm.detach();
		}
		if (leftArm != null) {
			leftArm.detach();
		}
		if (torso != null) {
			torso.detach();
		}
	}

	public void fighter() {
		moveHead(160, 87);
		moveArm("left", 31, 75, 152, 10);
		moveArm("right", 3, 94, 33, 16);
		moveHand("left", 161, 151, 133, 127, 107, 83);
		moveHand("right", 99, 130, 152, 154, 145, 180);
	}

	public void fistHips() {
		moveHead(138, 80);
		moveArm("left", 71, 41, 20, 39);
		moveArm("right", 71, 40, 14, 39);
		moveHand("left", 161, 151, 133, 127, 107, 83);
		moveHand("right", 99, 130, 152, 154, 145, 180);
	}

	public void fullSpeed() {
		if (head != null) {
			head.setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		}
		if (rightHand != null) {
			rightHand.setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		}
		if (leftHand != null) {
			leftHand.setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		}
		if (rightArm != null) {
			rightArm.setSpeed(1.0f, 1.0f, 1.0f, 1.0f);
		}
		if (leftArm != null) {
			leftArm.setSpeed(1.0f, 1.0f, 1.0f, 1.0f);
		}
		if (torso != null) {
			torso.setSpeed(1.0f, 1.0f, 1.0f);
		}
	}

	String getBoardType(String side, String type) {
		if (type != null) {
			return type;
		}

		if (RIGHT.equals(side)) {
			return Arduino.BOARD_TYPE_UNO;
		}

		return Arduino.BOARD_TYPE_ATMEGA2560;
	}

	@Override
	public String[] getCategories() {
		return new String[] { "robot" };
	}

	@Override
	public String getDescription() {
		return "The InMoov service";
	}

	// ------ starts end ---------
	// ------ composites begin ---------

	/**
	 * finds most recent activity
	 * 
	 * @return
	 */
	public long getLastActivityTime() {

		long lastActivityTime = 0;

		if (leftHand != null) {
			lastActivityTime = Math.max(lastActivityTime, leftHand.getLastActivityTime());
		}

		if (leftArm != null) {
			lastActivityTime = Math.max(lastActivityTime, leftArm.getLastActivityTime());
		}

		if (rightHand != null) {
			lastActivityTime = Math.max(lastActivityTime, rightHand.getLastActivityTime());
		}

		if (rightArm != null) {
			lastActivityTime = Math.max(lastActivityTime, rightArm.getLastActivityTime());
		}

		if (head != null) {
			lastActivityTime = Math.max(lastActivityTime, head.getLastActivityTime());
		}

		if (torso != null) {
			lastActivityTime = Math.max(lastActivityTime, torso.getLastActivityTime());
		}

		if (lastPIRActivityTime != null) {
			lastActivityTime = Math.max(lastActivityTime, lastPIRActivityTime);
		}

		if (lastActivityTime == 0) {
			error("invalid activity time - anything connected?");
			lastActivityTime = System.currentTimeMillis();
		}

		return lastActivityTime;
	}

	public Python getPython() {
		try {
			if (python == null) {
				python = (Python) startPeer("python");
			}

		} catch (Exception e) {
			error(e);
		}
		return python;
	}

	public void giving() {
		moveHead(44, 82);
		moveArm("left", 15, 55, 68, 10);
		moveArm("right", 13, 40, 74, 13);
		moveHand("left", 61, 0, 14, 0, 0, 180);
		moveHand("right", 0, 24, 24, 19, 21, 25);
	}

	public void handClose(String which) {
		moveHand(which, 130, 180, 180, 180, 180);
	}

	public void handleError(String msg) {
		// lets try not to nag
		if (!lastInMoovError.equals(msg) && speakErrors) {
			speakBlocking(msg);
		}
		lastInMoovError = msg;
	}

	public void handOpen(String which) {
		moveHand(which, 0, 0, 0, 0, 0);
	}

	public void handRest(String which) {
		moveHand(which, 60, 40, 30, 40, 40);
	}

	// ------ composites end

	// ------ composites servos begin -----------

	public void hello() {
		setHeadSpeed(1.0f, 1.0f);
		setArmSpeed("left", 1.0f, 1.0f, 1.0f, 1.0f);
		setArmSpeed("right", 1.0f, 1.0f, 1.0f, 1.0f);
		setHandSpeed("left", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		setHandSpeed("right", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		moveHead(105, 78);
		moveArm("left", 78, 48, 37, 10);
		moveArm("right", 90, 144, 60, 75);
		moveHand("left", 112, 111, 105, 102, 81, 10);
		moveHand("right", 0, 0, 0, 50, 82, 180);
	}

	public boolean isAttached() {
		boolean attached = false;
		if (leftHand != null) {
			attached |= leftHand.isAttached();
		}

		if (leftArm != null) {
			attached |= leftArm.isAttached();
		}

		if (rightHand != null) {
			attached |= rightHand.isAttached();
		}

		if (rightArm != null) {
			attached |= rightArm.isAttached();
		}

		if (head != null) {
			attached |= head.isAttached();
		}

		if (torso != null) {
			attached |= torso.isAttached();
		}
		return attached;
	}

	public boolean isMute() {
		return mute;
	}

	public void lookAtThis() {
		moveHead(66, 79);
		moveArm("left", 89, 75, 78, 19);
		moveArm("right", 90, 91, 72, 26);
		moveHand("left", 92, 106, 133, 127, 107, 29);
		moveHand("right", 86, 51, 133, 162, 153, 180);
	}

	public void moveArm(String which, Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (!arms.containsKey(which)) {
			error("setArmSpeed %s does not exist", which);
		} else {
			arms.get(which).moveTo(bicep, rotate, shoulder, omoplate);
		}
	}

	public void moveEyes(Integer eyeX, Integer eyeY) {
		if (head != null) {
			head.moveTo(null, null, eyeX, eyeY, null);
		} else {
			log.error("moveEyes - I have a null head");
		}
	}

	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky) {
		moveHand(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
		if (!hands.containsKey(which)) {
			error("moveHand %s does not exist", which);
		} else {
			hands.get(which).moveTo(thumb, index, majeure, ringFinger, pinky, wrist);
		}
	}

	// ------ composites servos end -----------

	public void moveHead(Integer neck, Integer rothead) {
		if (head != null) {
			head.moveTo(neck, rothead);
		} else {
			log.error("moveHead - I have a null head");
		}
	}

	public void moveHead(Integer neck, Integer rothead, Integer eyeX, Integer eyeY, Integer jaw) {
		if (head != null) {
			head.moveTo(neck, rothead, eyeX, eyeY, jaw);
		} else {
			log.error("I have a null head");
		}
	}

	// ---------- canned gestures end ---------

	public void moveTorso(Integer topStom, Integer midStom, Integer lowStom) {
		if (torso != null) {
			torso.moveTo(topStom, midStom, lowStom);
		} else {
			log.error("moveTorso - I have a null torso");
		}
	}

	public void onOpenNIData(OpenNIData data) {

		Skeleton skeleton = data.skeleton;

		if (firstSkeleton) {
			speakBlocking("i see you");
			firstSkeleton = false;
		}

		if (copyGesture) {
			if (leftArm != null) {
				leftArm.bicep.moveTo(Math.round(skeleton.leftElbow.getAngleXY()));
				leftArm.omoplate.moveTo(Math.round(skeleton.leftShoulder.getAngleXY()));
				leftArm.shoulder.moveTo(Math.round(skeleton.leftShoulder.getAngleYZ()));
			}
			if (rightArm != null) {
				rightArm.bicep.moveTo(Math.round(skeleton.rightElbow.getAngleXY()));
				rightArm.omoplate.moveTo(Math.round(skeleton.rightShoulder.getAngleXY()));
				rightArm.shoulder.moveTo(Math.round(skeleton.rightShoulder.getAngleYZ()));
			}
		}

		// TODO - route data appropriately
		// rgb & depth image to OpenCV
		// servos & depth image to gui (entire InMoov + references to servos)

	}

	// ---------- movement commands begin ---------

	public void openlefthand() {
		moveHand("left", 0, 0, 0, 0, 0, 0);
	}

	public void openPinch(String which) {
		moveHand(which, 0, 0, 180, 180, 180);
	}

	public void openrighthand() {
		moveHand("right", 0, 0, 0, 0, 0, 0);
	}

	public void powerDown() {

		rest();
		purgeAllTasks();
		detach();

		// TODO standard relay line ?
		// right
		// rightSerialPort.digitalWrite(53, Arduino.LOW);
		// leftSerialPort.digitalWrite(53, Arduino.LOW);
		if (ear != null) {
			ear.lockOutAllGrammarExcept("power up");
		}

		startSleep = System.currentTimeMillis();
		python.execMethod("power_down");
	}

	public void powerUp() {
		startSleep = null;
		attach();
		rest();
		if (ear != null) {
			ear.clearLock();
		}

		beginCheckingOnInactivity();

		python.execMethod("power_up");
	}

	public void publishPin(Pin pin) {
		if (pin.value == 1) {
			lastPIRActivityTime = System.currentTimeMillis();
		}
		// if its PIR & PIR is active & was sleeping - then wake up !
		if (pirPin == pin.pin && startSleep != null && pin.value == 1) {
			// attach(); // good morning / evening / night... asleep for % hours
			powerUp();
			Calendar now = Calendar.getInstance();

			/*
			 * FIXME - make a getSalutation String salutation = "hello "; if
			 * (now.get(Calendar.HOUR_OF_DAY) < 12) { salutation =
			 * "good morning "; } else if (now.get(Calendar.HOUR_OF_DAY) < 16) {
			 * salutation = "good afternoon "; } else { salutation =
			 * "good evening "; }
			 * 
			 * 
			 * speakBlocking(String.format("%s. i was sleeping but now i am awake"
			 * , salutation));
			 */
		}
	}

	@Override
	public void purgeAllTasks() {
		speakBlocking("purging all tasks");
		super.purgeAllTasks();
	}

	/**
	 * Service registration event. On newly registered service the InMoov
	 * service will set up various routing.
	 * 
	 * Routing of errors back to the InMoov service. This will allow the mouth
	 * to announce errors
	 * 
	 * @param sw
	 */
	public void registered(ServiceInterface sw) {
		// FIXME FIXME FIXME !!! - this right idea - but expanded methods have
		// incorrect parameter placement !!
		// addListener & suscribe the same !!!!
		subscribe(sw.getName(), "publishError", "handleError");
	}

	public void rest() {
		if (head != null) {
			head.rest();
		}
		if (rightHand != null) {
			rightHand.rest();
		}
		if (leftHand != null) {
			leftHand.rest();
		}
		if (rightArm != null) {
			rightArm.rest();
		}
		if (leftArm != null) {
			leftArm.rest();
		}
		if (torso != null) {
			torso.rest();
		}
	}

	@Override
	public boolean save() {
		super.save();
		if (leftHand != null) {
			leftHand.save();
		}

		if (rightHand != null) {
			rightHand.save();
		}

		if (rightArm != null) {
			rightArm.save();
		}

		if (leftArm != null) {
			leftArm.save();
		}

		if (head != null) {
			head.save();
		}

		if (openni != null) {
			openni.save();
		}

		return true;
	}

	public void setArmSpeed(String which, Float bicep, Float rotate, Float shoulder, Float omoplate) {
		if (!arms.containsKey(which)) {
			error("setArmSpeed %s does not exist", which);
		} else {
			arms.get(which).setSpeed(bicep, rotate, shoulder, omoplate);
		}
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky) {
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		if (!hands.containsKey(which)) {
			error("setHandSpeed %s does not exist", which);
		} else {
			hands.get(which).setSpeed(thumb, index, majeure, ringFinger, pinky, wrist);
		}
	}

	public void setHeadSpeed(Float rothead, Float neck) {
		setHeadSpeed(rothead, neck, null, null, null);
	}

	public void setHeadSpeed(Float rothead, Float neck, Float eyeXSpeed, Float eyeYSpeed, Float jawSpeed) {
		if (head != null) {
			head.setSpeed(rothead, neck, eyeXSpeed, eyeYSpeed, jawSpeed);
		} else {
			log.warn("setHeadSpeed - I have no head");
		}
	}

	public void setMute(boolean mute) {
		this.mute = mute;
	}

	public void setTorsoSpeed(Float topStom, Float midStom, Float lowStom) {
		if (torso != null) {
			torso.setSpeed(topStom, midStom, lowStom);
		} else {
			log.warn("setTorsoSpeed - I have no torso");
		}
	}

	public boolean speakBlocking(Status test) {
		if (test != null && !mute) {
			return speakBlocking(test.toString());
		}
		return false;
	}

	public boolean speakBlocking(String toSpeak) {
		if (mouth != null && !mute) {
			return mouth.speakBlocking(toSpeak);
		}
		return false;
	}

	boolean speakBlocking(String speak, Object... fdata) {
		if (mouth != null && !mute) {
			return mouth.speakBlocking(speak, fdata);
		}

		return false;
	}

	public boolean speakErrors(boolean b) {
		speakErrors = b;
		return b;
	}

	/*************
	 * STARTS BEGIN
	 * 
	 * @throws Exception
	 ************************/

	public void startAll(String leftPort, String rightPort) throws Exception {
		// TODO add vision
		startMouth();
		startHead(leftPort);
		startEar();

		startMouthControl(leftPort);

		startLeftHand(leftPort);
		startRightHand(rightPort);
		startLeftArm(leftPort);
		startRightArm(rightPort);

		startHeadTracking(leftPort);
		startEyesTracking(leftPort);

		speakBlocking("startup sequence completed");
	}

	public InMoovArm startArm(String side, String port, String boardType) throws Exception {
		speakBlocking("starting %s arm", side);

		InMoovArm arm = (InMoovArm) startPeer(String.format("%sArm", side));
		arms.put(side, arm);
		arm.setSide(side);// FIXME WHO USES SIDE - THIS SHOULD BE NAME !!!
		arm.arduino.setBoard(getBoardType(side, boardType));
		arm.connect(port);
		arduinos.put(port, arm.arduino);

		return arm;
	}

	// TODO TODO TODO - context & status report -
	// "current context is right hand"
	// FIXME - voice control for all levels (ie just a hand or head !!!!)
	public Sphinx startEar() throws Exception {
		speakBlocking("starting ear");
		ear = (Sphinx) startPeer("ear");
		if (mouth != null) {
			ear.attach(mouth);
		}
		return ear;
	}

	public Tracking startEyesTracking(String port) throws Exception {
		speakBlocking("starting eyes tracking");

		if (head == null) {
			startHead(port);
		}
		eyesTracking = (Tracking) startPeer("eyesTracking");
		eyesTracking.connect(port);
		arduinos.put(port, eyesTracking.arduino);
		return eyesTracking;
	}

	public InMoovHand startHand(String side, String port, String boardType) throws Exception {
		speakBlocking("starting %s hand", side);

		InMoovHand hand = (InMoovHand) startPeer(String.format("%sHand", side));
		hand.setSide(side);
		hands.put(side, hand);
		hand.arduino.setBoard(getBoardType(side, boardType));
		hand.connect(port);
		arduinos.put(port, hand.arduino);
		return hand;
	}

	public InMoovHead startHead(String port) throws Exception {
		return startHead(port, null);
	}

	public InMoovHead startHead(String port, String type) throws Exception {
		// log.warn(InMoov.buildDNA(myKey, serviceClass))
		speakBlocking("starting head on %s", port);

		opencv = (OpenCV) startPeer("opencv");
		head = (InMoovHead) startPeer("head");

		if (type == null) {
			type = Arduino.BOARD_TYPE_ATMEGA2560;
		}

		head.arduino.setBoard(type);
		head.connect(port);
		arduinos.put(port, head.arduino);

		return head;
	}

	// NOTE - BEST Services are one which are reflective on startService
	// like xmpp which exposes a the reflective REST API are startService
	public Tracking startHeadTracking(String port) throws Exception {
		speakBlocking("starting head tracking");

		if (head == null) {
			startHead(port);
		}
		headTracking = (Tracking) startPeer("headTracking");
		headTracking.connect(port);
		arduinos.put(port, headTracking.arduino);
		return headTracking;
	}

	public InMoovArm startLeftArm(String port) throws Exception {
		return startLeftArm(port, null);
	}

	public InMoovArm startLeftArm(String port, String type) throws Exception {
		leftArm = startArm(LEFT, port, type);
		return leftArm;
	}

	public InMoovHand startLeftHand(String port) throws Exception {
		return startLeftHand(port, null);
	}

	public InMoovHand startLeftHand(String port, String type) throws Exception {
		leftHand = startHand(LEFT, port, type);
		return leftHand;
	}

	// gestures begin ---------------

	public Speech startMouth() throws Exception {
		mouth = (Speech) startPeer("mouth");
		speakBlocking("starting mouth");

		if (ear != null) {
			ear.attach(mouth);
		}
		return mouth;
	}

	public MouthControl startMouthControl(String port) throws Exception {
		speakBlocking("starting mouth control");
		if (mouthControl == null) {

			if (head == null) {
				startHead(port);
			}

			mouthControl = (MouthControl) startPeer("mouthControl");
			mouthControl.jaw.setPin(26);
			mouthControl.arduino.connect(port);
			arduinos.put(port, mouthControl.arduino);
			String p = mouthControl.getArduino().getSerial().getPortName();
			if (p != null) {
				arduinos.put(p, mouthControl.arduino);
			}
			mouthControl.setmouth(10, 50);
		}
		return mouthControl;
	}

	// starting routines need to be fully re-entrant
	// they can be used to get a reference and start a very limited sub-system
	// of inmoov
	// very useful in the fact a head subsystem can be tested without starting
	// all of the peer services of the head
	public OpenCV startOpenCV() throws Exception {
		if (opencv != null) {
			opencv = (OpenCV) startPeer("opencv");
		}
		return opencv;
	}

	public OpenNI startOpenNI() throws Exception {
		if (openni == null) {
			speakBlocking("starting kinect");
			openni = (OpenNI) startPeer("openni");
			pid = (PID) startPeer("pid");

			pid.setMode(PID.MODE_AUTOMATIC);
			pid.setOutputRange(-1, 1);
			pid.setPID(10.0, 0.0, 1.0);
			pid.setControllerDirection(0);

			// re-mapping of skeleton !
			openni.skeleton.leftElbow.mapXY(0, 180, 180, 0);
			openni.skeleton.rightElbow.mapXY(0, 180, 180, 0);

			openni.skeleton.leftShoulder.mapYZ(0, 180, 180, 0);
			openni.skeleton.rightShoulder.mapYZ(0, 180, 180, 0);

			// openni.skeleton.leftShoulder

			// openni.addListener("publishOpenNIData", this.getName(),
			// "getSkeleton");
			// openni.addOpenNIData(this);
		}
		return openni;
	}

	public void startPIR(String port, int pin) throws IOException {
		speakBlocking("starting pee. eye. are. sensor on port %s pin %d", port, pin);
		if (arduinos.containsKey(port)) {
			Arduino arduino = arduinos.get(port);
			arduino.connect(port);
			arduino.setSampleRate(8000);
			arduino.digitalReadPollingStart(pin);
			pirPin = pin;
			arduino.addListener("publishPin", this.getName(), "publishPin");

		} else {
			// FIXME - SHOULD ALLOW STARTUP AND LATER ACCESS VIA PORT ONCE OTHER
			// STARTS CHECK MAP FIRST
			log.error(String.format("%s arduino not found - start some other system first (head, arm, hand)", port));
		}

	}

	public InMoovArm startRightArm(String port) throws Exception {
		return startRightArm(port, null);
	}

	public InMoovArm startRightArm(String port, String type) throws Exception {
		if (rightArm != null) {
			info("right arm already started");
			return rightArm;
		}
		rightArm = startArm(RIGHT, port, type);
		return rightArm;
	}

	public InMoovHand startRightHand(String port) throws Exception {
		return startRightHand(port, null);
	}

	public InMoovHand startRightHand(String port, String type) throws Exception {
		rightHand = startHand(RIGHT, port, type);
		return rightHand;
	}

	@Override
	public void startService() {
		python = getPython();
	}

	public InMoovTorso startTorso(String port) throws Exception {
		return startTorso(port, null);
	}

	public InMoovTorso startTorso(String port, String type) throws Exception {
		// log.warn(InMoov.buildDNA(myKey, serviceClass))
		speakBlocking("starting torso on %s", port);

		torso = (InMoovTorso) startPeer("torso");

		if (type == null) {
			type = Arduino.BOARD_TYPE_ATMEGA2560;
		}

		torso.arduino.setBoard(type);
		torso.connect(port);
		arduinos.put(port, torso.arduino);

		return torso;
	}

	public void stopPIR() {
		/*
		 * if (arduinos.containsKey(port)) { Arduino arduino =
		 * arduinos.get(port); arduino.connect(port);
		 * arduino.setSampleRate(8000); arduino.digitalReadPollStart(pin);
		 * pirPin = pin; arduino.addListener("publishPin", this.getName(),
		 * "publishPin"); }
		 */

	}

	public void stopTracking() {
		if (eyesTracking != null) {
			eyesTracking.stopTracking();
		}

		if (headTracking != null) {
			headTracking.stopTracking();
		}
	}

	// This is an in-flight check vs power up or power down
	public void systemCheck() {
		speakBlocking("starting system check");
		speakBlocking("testing");

		rest();
		sleep(500);

		if (rightHand != null) {
			speakBlocking("testing right hand");
			rightHand.test();
		}

		if (rightArm != null) {
			speakBlocking("testing right arm");
			rightArm.test();
		}

		if (leftHand != null) {
			speakBlocking("testing left hand");
			leftHand.test();
		}

		if (leftArm != null) {
			speakBlocking("testing left arm");
			leftArm.test();
		}

		if (head != null) {
			speakBlocking("testing head");
			head.test();
		}

		if (torso != null) {
			speakBlocking("testing torso");
			torso.test();
		}

		sleep(500);
		rest();
		broadcastState();
		speakBlocking("system check completed");
	}

	/*
	 * public boolean load(){ super.load(); if (leftHand != null){
	 * leftHand.load(); }
	 * 
	 * if (rightHand != null){ rightHand.load(); }
	 * 
	 * if (rightArm != null){ rightArm.load(); }
	 * 
	 * if (leftArm != null){ leftArm.load(); }
	 * 
	 * if (head != null){ head.load(); }
	 * 
	 * if (openni != null){ openni.load(); }
	 * 
	 * return true; }
	 */

	@Override
	public Status test() {
		Status status = Status.info("starting InMoov test");
		try {
			String rightPort = "COM7";
			String leftPort = "COM20";
			String rightUART = "COM7_UART";
			String leftUART = "COM20_UART";

			Serial luart = (Serial) Runtime.start(leftUART, "Serial");
			Serial ruart = (Serial) Runtime.start(rightUART, "Serial");

			luart.record();
			ruart.record();

			luart.connect(leftUART);
			ruart.connect(rightUART);

			GUIService gui = (GUIService) Runtime.start("gui", "GUIService");

			// get online script
			String script = new String(HTTPClient.get("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/master/home/hairygael/InMoov2.full3.byGael.Langevin.1.py"));
			log.info(script);
			python = (Python) Runtime.start("python", "Python");
			python.exec(script);

			log.info("done");
			// i01.startHead(leftPort);
			// i01.systemCheck();

			luart.releaseService();
			ruart.releaseService();

			Runtime.releaseAll();
		} catch (Exception e) {
			status.addError(e);
		}

		return status;

	}

	public void track() {
		if (headTracking == null) {
			error("attach head before tracking");
		} else {
			headTracking.trackPoint(0.5f, 0.5f);
		}
	}

	public void trackHumans() {
		if (eyesTracking != null) {
			eyesTracking.faceDetect();
		}

		if (headTracking != null) {
			headTracking.faceDetect();
		}
	}

	public void trackPoint() {

		if (eyesTracking != null) {
			eyesTracking.startLKTracking();
			eyesTracking.trackPoint(0.5f, 0.5f);
		}

		if (headTracking != null) {
			headTracking.startLKTracking();
			headTracking.trackPoint(0.5f, 0.5f);
		}
	}

	public void victory() {
		moveHead(114, 90);
		moveArm("left", 90, 91, 106, 10);
		moveArm("right", 0, 73, 30, 17);
		moveHand("left", 170, 0, 0, 168, 167, 0);
		moveHand("right", 98, 37, 34, 67, 118, 166);
	}

}
