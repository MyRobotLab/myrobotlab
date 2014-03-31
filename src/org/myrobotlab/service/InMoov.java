package org.myrobotlab.service;

import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
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

	@Element(required = false)
	String defaultLeftPort; // FIXME - THIS IS A BUG GET RID OF IT - ALL ACCESS
							// THROUGH MAP !!!

	@Element(required = false)
	String defaultRightPort; // FIXME - THIS IS A BUG GET RID OF IT - ALL ACCESS
								// THROUGH MAP !!!

	// hands and arms
	transient public InMoovHead head;
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

	transient public final static String LEFT = "left";
	transient public final static String RIGHT = "right";

	// reflective or non-interactive peers
	// transient public WebGUI webgui;
	// transient public XMPP xmpp;
	// transient public Security security;

	boolean speakErrors = true;
	String lastError = "";

	long lastActivityTime;

	int maxInactivityTimeSeconds = 120;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// SHARING !!!
		peers.suggestAs("head.arduino", "left", "Arduino", "shared left arduino");

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

		// put peer definitions in
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

		return peers;
	}

	public InMoov(String n) {
		super(n);
		// addRoutes();
		// FIXME - mebbe starts with same error - don't say it again unless a
		// certain time has passed
	}

	public boolean speakErrors(boolean b) {
		speakErrors = b;
		return b;
	}

	public void addRoutes() {
		// register with runtime for any new services
		// their errors are routed to mouth
		subscribe(this.getName(), "publishError", "handleError");

		Runtime r = Runtime.getInstance();
		r.addListener(getName(), "registered");
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

	public void handleError(String msg) {
		// lets try not to nag
		if (!lastError.equals(msg) && speakErrors) {
			speakBlocking(msg);
		}
		lastError = msg;
	}

	/************* STARTS BEGIN ************************/

	public void startAll(String leftPort, String rightPort) {
		// TODO add vision
		startMouth();
		startHead(leftPort);
		startEar();
		startLeftHand(leftPort);
		startRightHand(rightPort);
		startLeftArm(leftPort);
		startRightArm(rightPort);

		startHeadTracking(leftPort);
		startEyesTracking(leftPort);

		speakBlocking("startup sequence completed");
	}

	public Integer pirPin = null;
	Long startSleep = null;

	public void startPIR(String port, int pin) {
		speakBlocking("starting pee eye are sensor on port %s pin %d", port, pin);
		if (arduinos.containsKey(port)) {
			Arduino arduino = arduinos.get(port);
			arduino.connect(port);
			arduino.setSampleRate(8000);
			arduino.digitalReadPollStart(pin);
			pirPin = pin;
			arduino.addListener("publishPin", this.getName(), "publishPin");

		} else {
			// FIXME - SHOULD ALLOW STARTUP AND LATER ACCESS VIA PORT ONCE OTHER
			// STARTS CHECK MAP FIRST
			log.error(String.format("%s arduino not found - start some other system first (head, arm, hand)", port));
		}

	}

	public void publishPin(Pin pin) {
		if (pirPin == pin.pin) {
			if (startSleep != null) {
				attach(); // good morning / evening / night... asleep for % hours
				powerUp();
				speakBlocking("hello. i was sleeping but now i am awake");
				// TODO - add saying time asleep
			}
		}
	}

	// TODO TODO TODO - context & status report -
	// "current context is right hand"
	// FIXME - voice control for all levels (ie just a hand or head !!!!)
	public Sphinx startEar() {
		speakBlocking("starting ear");
		ear = (Sphinx) startPeer("ear");
		if (mouth != null) {
			ear.attach(mouth);
		}
		return ear;
	}

	public Speech startMouth() {
		mouth = (Speech) startPeer("mouth");
		speakBlocking("starting mouth");

		if (ear != null) {
			ear.attach(mouth);
		}
		return mouth;
	}

	// starting routines need to be fully re-entrant
	// they can be used to get a reference and start a very limited sub-system
	// of inmoov
	// very useful in the fact a head subsystem can be tested without starting
	// all of the peer services of the head
	public OpenCV startOpenCV() {
		if (opencv != null) {
			opencv = (OpenCV) startPeer("opencv");
		}
		return opencv;
	}

	// NOTE - BEST Services are one which are reflective on startService
	// like xmpp which exposes a the reflective REST API are startService
	public Tracking startHeadTracking(String port) {
		speakBlocking("starting head tracking");

		if (head == null) {
			startHead(port);
		}
		headTracking = (Tracking) startPeer("headTracking");
		headTracking.connect(port);
		arduinos.put(port, headTracking.arduino);
		return headTracking;
	}

	public Tracking startEyesTracking(String port) {
		speakBlocking("starting eyes tracking");

		if (head == null) {
			startHead(port);
		}
		eyesTracking = (Tracking) startPeer("eyesTracking");
		eyesTracking.connect(port);
		arduinos.put(port, eyesTracking.arduino);
		return eyesTracking;
	}

	public MouthControl startMouthControl(String port) {
		speakBlocking("starting mouth control");
		if (mouthControl == null) {

			if (head == null) {
				startHead(port);
			}

			mouthControl = (MouthControl) startPeer("mouthControl");
			mouthControl.jaw.setPin(26);
			mouthControl.arduino.connect(port);
			arduinos.put(port, mouthControl.arduino);
			String p = mouthControl.arduino.getPortName();
			if (p != null) {
				arduinos.put(p, mouthControl.arduino);
			}
			mouthControl.setmouth(10, 50);
		}
		return mouthControl;
	}

	/*
	 * 
	 * public void startPIR(String port, int pin){ Arduino
	 * arduino.setSampleRate(8000) arduino.digitalReadPollStart(12) }
	 */

	public InMoovHand startRightHand(String port) {
		return startRightHand(port, null);
	}

	public InMoovHand startRightHand(String port, String type) {
		rightHand = startHand(RIGHT, port, type);
		return rightHand;
	}

	public InMoovHand startLeftHand(String port) {
		return startLeftHand(port, null);
	}

	public InMoovHand startLeftHand(String port, String type) {
		leftHand = startHand(LEFT, port, type);
		return leftHand;
	}

	public InMoovHand startHand(String side, String port, String boardType) {
		speakBlocking("starting %s hand", side);

		InMoovHand hand = (InMoovHand) startPeer(String.format("%sHand", side));
		hand.setSide(side);
		hands.put(side, hand);
		hand.arduino.setBoard(getBoardType(side, boardType));
		hand.connect(port);
		arduinos.put(port, hand.arduino);
		return hand;
	}

	public InMoovArm startRightArm(String port) {
		return startRightArm(port, null);
	}

	public InMoovArm startRightArm(String port, String type) {
		rightArm = startArm(RIGHT, port, type);
		return rightArm;
	}

	public InMoovArm startLeftArm(String port) {
		return startLeftArm(port, null);
	}

	public InMoovArm startLeftArm(String port, String type) {
		leftArm = startArm(LEFT, port, type);
		return leftArm;
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

	public InMoovArm startArm(String side, String port, String boardType) {
		speakBlocking("starting %s arm", side);

		InMoovArm arm = (InMoovArm) startPeer(String.format("%sArm", side));
		arms.put(side, arm);
		arm.setSide(side);// FIXME WHO USES SIDE - THIS SHOULD BE NAME !!!
		arm.arduino.setBoard(getBoardType(side, boardType));
		arm.connect(port);
		arduinos.put(port, arm.arduino);

		return arm;
	}

	public InMoovHead startHead(String port) {
		return startHead(port, null);
	}

	public InMoovHead startHead(String port, String type) {
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

	// ------ starts end ---------
	// ------ composites begin ---------

	@Override
	public String getDescription() {
		return "The InMoov service";
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

	public void stopTracking() {
		if (eyesTracking != null) {
			eyesTracking.stopTracking();
		}

		if (headTracking != null) {
			headTracking.stopTracking();
		}
	}

	boolean speakBlocking(String speak, Object... fdata) {
		if (mouth != null) {
			return mouth.speakBlocking(speak, fdata);
		}

		return false;
	}

	public boolean speakBlocking(String toSpeak) {
		if (mouth != null) {
			return mouth.speakBlocking(toSpeak);
		}
		return false;
	}

	public boolean speakBlocking(Status test) {
		if (test != null) {
			return speakBlocking(test.toString());
		}
		return false;
	}

	// ------ composites end

	// ------ composites servos begin -----------

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
	}

	public static int attachPauseMs = 100;

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

		broadcastState();
		speakBlocking("system check completed");
	}

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

		if (headTracking != null) {
			headTracking.broadcastState();
		}

		if (eyesTracking != null) {
			eyesTracking.broadcastState();
		}
	}

	// ------ composites servos end -----------

	public void openlefthand() {
		moveHand("left", 0, 0, 0, 0, 0, 0);
	}

	public void openrighthand() {
		moveHand("right", 0, 0, 0, 0, 0, 0);
	}

	public void powerDown() {
		sleep(2);
		if (ear != null){
			ear.pauseListening();
		}
		rest();
		speakBlocking("I'm powering down");
		purgeAllTasks();
		sleep(2);
		moveHead(40, 85);
		sleep(4);
		detach();
		speakBlocking("for more interaction please request me to power up");
		speakBlocking("or activate the p eye are sensor. thank you. good bye.");
		
		// right
		// rightSerialPort.digitalWrite(53, Arduino.LOW);
		// leftSerialPort.digitalWrite(53, Arduino.LOW);
		if (ear != null){
			ear.lockOutAllGrammarExcept("power up");
			sleep(2);
			ear.resumeListening();
		}
		
		startSleep = System.currentTimeMillis();
	}

	public void powerUp() {
		startSleep = null;
		sleep(2);
		if (ear != null){
			ear.pauseListening();
		}
		// rightSerialPort.digitalWrite(53, Arduino.HIGH);
		// leftSerialPort.digitalWrite(53, Arduino.HIGH);
		speakBlocking("Im powered up");
		rest();
		if (ear != null){
			ear.clearLock();
			sleep(2);
			ear.resumeListening();
		}
		speakBlocking("ready");
		
	}

	// ---------- canned gestures end ---------

	public void cameraOn() {
		startOpenCV();
		opencv.capture();
	}

	public void cameraOff() {
		if (opencv != null) {
			opencv.stopCapture();
		}
	}

	// ---------- movement commands begin ---------

	public void handOpen(String which) {
		moveHand(which, 0, 0, 0, 0, 0);
	}

	public void handClose(String which) {
		moveHand(which, 130, 180, 180, 180, 180);
	}

	public void handRest(String which) {
		moveHand(which, 60, 40, 30, 40, 40);
	}

	public void openPinch(String which) {
		moveHand(which, 0, 0, 180, 180, 180);
	}

	public void closePinch(String which) {
		moveHand(which, 130, 140, 180, 180, 180);
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

	public void setArmSpeed(String which, Float bicep, Float rotate, Float shoulder, Float omoplate) {
		if (!arms.containsKey(which)) {
			error("setArmSpeed %s does not exist", which);
		} else {
			arms.get(which).setSpeed(bicep, rotate, shoulder, omoplate);
		}
	}

	public void moveArm(String which, Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (!arms.containsKey(which)) {
			error("setArmSpeed %s does not exist", which);
		} else {
			arms.get(which).moveTo(bicep, rotate, shoulder, omoplate);
		}
	}

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

	public void setHeadSpeed(Float rothead, Float neck) {
		if (head != null) {
			head.setSpeed(rothead, neck, null, null, null);
		} else {
			log.warn("setHeadSpeed - I have no head");
		}
	}

	public void moveEyes(Integer eyeX, Integer eyeY) {
		if (head != null) {
			head.moveTo(null, null, eyeX, eyeY, null);
		} else {
			log.error("moveEyes - I have a null head");
		}
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

		send("python", "appendScript", script.toString());

		return script.toString();
	}

	public void startAutoDetach(int intervalSeconds) {
		addLocalTask(intervalSeconds * 1000, "checkForInactivity");
	}

	/**
	 * finds most recent activity
	 * 
	 * @return
	 */
	public long getLastActivityTime() {

		long lastActivityTime = System.currentTimeMillis();
		long myLastActivity = 0;

		if (leftHand != null) {
			myLastActivity = leftHand.getLastActivityTime();
			lastActivityTime = (lastActivityTime > myLastActivity) ? myLastActivity : lastActivityTime;
		}

		if (leftArm != null) {
			myLastActivity = leftArm.getLastActivityTime();
			lastActivityTime = (lastActivityTime > myLastActivity) ? myLastActivity : lastActivityTime;
		}

		if (rightHand != null) {
			myLastActivity = rightHand.getLastActivityTime();
			lastActivityTime = (lastActivityTime > myLastActivity) ? myLastActivity : lastActivityTime;
		}

		if (rightArm != null) {
			myLastActivity = rightArm.getLastActivityTime();
			lastActivityTime = (lastActivityTime > myLastActivity) ? myLastActivity : lastActivityTime;
		}

		if (head != null) {
			myLastActivity = head.getLastActivityTime();
			lastActivityTime = (lastActivityTime > myLastActivity) ? myLastActivity : lastActivityTime;
		}

		return lastActivityTime;

	}

	public void autoPowerDownOnInactivity() {
		autoPowerDownOnInactivity(maxInactivityTimeSeconds);
	}

	public void autoPowerDownOnInactivity(int maxInactivityTimeSeconds) {
		this.maxInactivityTimeSeconds = maxInactivityTimeSeconds;
		speakBlocking("power down after %s seconds inactivity is on", this.maxInactivityTimeSeconds);
		addLocalTask(5 * 1000, "powerDownOnInactivity");
	}

	public long powerDownOnInactivity() {
		speakBlocking("checking");
		long lastActivityTime = getLastActivityTime();
		long now = System.currentTimeMillis();
		long inactivitySeconds = (now - lastActivityTime) / 1000;
		if (inactivitySeconds > maxInactivityTimeSeconds && isAttached()) {
			speakBlocking("%d seconds have passed without activity", inactivitySeconds);
			powerDown();
		} else {
			speakBlocking("%d seconds have passed without activity", inactivitySeconds);
		}
		return lastActivityTime;
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
		return attached;
	}

	// gestures begin ---------------

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

	public void giving() {
		moveHead(44, 82);
		moveArm("left", 15, 55, 68, 10);
		moveArm("right", 13, 40, 74, 13);
		moveHand("left", 61, 0, 14, 0, 0, 180);
		moveHand("right", 0, 24, 24, 19, 21, 25);
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

	public void lookAtThis() {
		moveHead(66, 79);
		moveArm("left", 89, 75, 78, 19);
		moveArm("right", 90, 91, 72, 26);
		moveHand("left", 92, 106, 133, 127, 107, 29);
		moveHand("right", 86, 51, 133, 162, 153, 180);
	}

	public void victory() {
		moveHead(114, 90);
		moveArm("left", 90, 91, 106, 10);
		moveArm("right", 0, 73, 30, 17);
		moveHand("left", 170, 0, 0, 168, 167, 0);
		moveHand("right", 98, 37, 34, 67, 118, 166);
	}

	public void armsUp() {
		moveHead(160, 97);
		moveArm("left", 9, 85, 168, 18);
		moveArm("right", 0, 68, 180, 10);
		moveHand("left", 61, 38, 14, 38, 15, 64);
		moveHand("right", 0, 0, 0, 50, 82, 180);
	}

	public void armsFront() {
		moveHead(99, 82);
		moveArm("left", 9, 115, 96, 51);
		moveArm("right", 13, 104, 101, 49);
		moveHand("left", 61, 0, 14, 38, 15, 0);
		moveHand("right", 0, 24, 54, 50, 82, 180);
	}

	public void daVinci() {
		moveHead(75, 79);
		moveArm("left", 9, 115, 28, 80);
		moveArm("right", 13, 118, 26, 80);
		moveHand("left", 61, 49, 14, 38, 15, 64);
		moveHand("right", 0, 24, 54, 50, 82, 180);
	}

	boolean useHeadForTracking = true;
	boolean useEyesForTracking = false;

	public void track() {
		if (headTracking == null) {
			error("attach head before tracking");
		} else {
			headTracking.trackPoint(0.5f, 0.5f);
		}
	}

	public void clearTrackingPoints() {
		if (headTracking == null) {
			error("attach head before tracking");
		} else {
			headTracking.clearTrackingPoints();
		}
	}
	
	public void purgeAllTasks()
	{
		speakBlocking("purging all tasks");
		super.purgeAllTasks();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// Create two virtual ports for UART and user and null them together:
		// create 2 virtual ports
/*
		VirtualSerialPort vp0 = new VirtualSerialPort("UART15");
		VirtualSerialPort vp1 = new VirtualSerialPort("COM15");

		// make null modem cable ;)
		VirtualSerialPort.makeNullModem(vp0, vp1);
		

		// add virtual ports to the serial device factory
		SerialDeviceFactory.add(vp0);
		SerialDeviceFactory.add(vp1);
*/		

		// create the UART serial service
		// log.info("Creating a LIDAR UART Serial service named: " + getName() +
		// "SerialService");
		// String serialName = getName() + "SerialService";
		Serial serial0 = new Serial("UART15");
		serial0.startService();
		serial0.connect("UART15");

		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");

		InMoov i01 = (InMoov) Runtime.createAndStart("i01", "InMoov");
		i01.startMouth();
		//i01.power(120);
		InMoovHand lefthand = i01.startLeftHand("COM15");
		i01.leftHand.setRest(10, 10, 10, 10, 10);
		i01.autoPowerDownOnInactivity(10);

		/*
		log.info("inactivity {}", i01.powerDownOnInactivity());

		lefthand.moveTo(5, 10, 30, 40, 50);

		log.info("inactivity {}", i01.powerDownOnInactivity());

		lefthand.rest();

		log.info("inactivity {}", i01.powerDownOnInactivity());
		*/

		/*
		 * 
		 * Tracking neck = i01.startHeadTracking(leftPort); i01.detach();
		 */

	}

}
