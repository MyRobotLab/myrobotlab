package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Sweety extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Sweety.class);
	transient public Arduino arduino;
	transient public Sphinx ear;
	transient public Speech mouth;
	transient public Tracking eyesTracker;
	// transient public Tracking rightTracker;
	transient public ProgramAB chatBot;
	
	transient Servo leftForearm;
	transient Servo rightForearm;
	transient Servo rightShoulder;
	transient Servo leftShoulder;
	transient Servo rightArm;
	transient Servo neck;
	transient Servo leftEye;
	transient Servo leftArm;
	transient Servo rightEye;
	transient Servo rightHand;
	transient Servo rightWrist;
	transient Servo leftHand;
	transient Servo leftWrist;
	
	transient UltrasonicSensor USfront;
	transient UltrasonicSensor USfrontRight;
	transient UltrasonicSensor USfrontLeft;
	transient UltrasonicSensor USback;
	transient UltrasonicSensor USbackRight;
	transient UltrasonicSensor USbackLeft;
	
	int rightMotorBackwardPin = 2;
	int rightMotorForwardPin = 3;
	int leftMotorForwardPin = 4;
	int leftMotorBackwardPin = 5;

	int backUltrasonicTrig = 22;
	int backUltrasonicEcho = 23;
	int back_leftUltrasonicTrig = 24;
	int back_leftUltrasonicEcho = 25;
	int back_rightUltrasonicTrig = 26;
	int back_rightUltrasonicEcho = 27;
	int front_leftUltrasonicTrig = 28;
	int front_leftUltrasonicEcho = 29;
	int frontUltrasonicTrig = 30;
	int frontUltrasonicEcho = 31;
	int front_rightUltrasonicTrig = 32;
	int front_rightUltrasonicEcho = 33;

	int SHIFT = 47;
	int LATCH = 48;
	int DATA = 49;
	
	public int delaytime = 50;
	public int delaytimestop = 200;
	public int delaytimeletter = 50;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("arduino", "Arduino", "arduino");
		peers.put("mouth", "Speech", "sweetys mouth");
		peers.put("ear", "Sphinx", "ear");
		peers.put("chatBot", "ProgramAB", "chatBot");
		// peers.put("leftTracker", "Tracking", "leftTracker");
		peers.put("eyesTracker", "Tracking", "eyesTracker");
		
		peers.put("USfront", "UltrasonicSensor", "USfront");
		peers.put("USfrontRight", "UltrasonicSensor", "USfrontRight");
		peers.put("USfrontLeft", "UltrasonicSensor", "USfrontLeft");
		peers.put("USback", "UltrasonicSensor", "USback");
		peers.put("USbackRight", "UltrasonicSensor", "USbackRight");
		peers.put("USbackLeft", "UltrasonicSensor", "USbackLeft");
		
		peers.put("leftForearm", "Servo", "servo");
		peers.put("rightForearm", "Servo", "servo");
		peers.put("rightShoulder", "Servo", "servo");
		peers.put("leftShoulder", "Servo", "servo");
		peers.put("rightArm", "Servo", "servo");
		peers.put("neck", "Servo", "servo");
		peers.put("leftEye", "Servo", "servo");
		peers.put("leftArm", "Servo", "servo");
		peers.put("rightEye", "Servo", "servo");
		peers.put("rightHand", "Servo", "servo");
		peers.put("rightWrist", "Servo", "servo");
		peers.put("leftHand", "Servo", "servo");
		peers.put("leftWrist", "Servo", "servo");
		
		return peers;
	}

	public void startService(){
		super.startService();
		
		arduino = (Arduino) startPeer("arduino");
		
		// Share arduino service with others
		reserveRootAs("sweety.eyes.arduino", "sweety.arduino"); 
		//reserveRootAs("sweety.rightTracker.arduino", "sweety.arduino");
		
		chatBot = (ProgramAB) startPeer("chatBot");
		
		mouth = (Speech) startPeer("mouth");
		mouth.setLanguage("fr");
		mouth.setBackendType("GOOGLE");
		mouth.setGenderFemale();
		
		ear = (Sphinx) startPeer("ear");

		leftForearm = (Servo) startPeer("leftForearm");
		rightForearm = (Servo) startPeer("rightForearm");
		rightShoulder = (Servo) startPeer("rightShoulder");
		leftShoulder = (Servo) startPeer("leftShoulder");
		rightArm = (Servo) startPeer("rightArm");
		neck = (Servo) startPeer("neck");
		leftEye = (Servo) startPeer("leftEye");
		leftArm = (Servo) startPeer("leftArm");
		rightEye = (Servo) startPeer("rightEye");
		rightHand = (Servo) startPeer("rightHand");
		rightWrist = (Servo) startPeer("rightWrist");
		leftHand = (Servo) startPeer("leftHand");
		leftWrist = (Servo) startPeer("leftWrist");
		
		leftForearm.setMinMax(85,140);
		rightForearm.setMinMax(5,67);
		rightShoulder.setMinMax(0,155);
		leftShoulder.setMinMax(0,145);
		rightArm.setMinMax(25,130);
		neck.setMinMax(55,105);
		leftEye.setMinMax(25,125);
		leftArm.setMinMax(45,117);
		rightEye.setMinMax(80,180);
		rightHand.setMinMax(10,75);
		rightWrist.setMinMax(0,180);
		leftHand.setMinMax(80,150);
		leftWrist.setMinMax(0,180);
	}
	
	public void startUltraSonic(){
		/**
		 * Start the ultrasonic sensors services
		 */
		

				USfront = (UltrasonicSensor) startPeer("USfront");
				USfrontRight = (UltrasonicSensor) startPeer("USfrontRight");
				USfrontLeft = (UltrasonicSensor) startPeer("USfrontLeft");
				USback = (UltrasonicSensor) startPeer("USback");
				USbackRight = (UltrasonicSensor) startPeer("USbackRight");
				USbackLeft = (UltrasonicSensor) startPeer("USbackLeft");
				
				USfront.attach(arduino,arduino.getPortName(), frontUltrasonicTrig, frontUltrasonicEcho);
				USfrontRight.attach(arduino,arduino.getPortName(), front_rightUltrasonicTrig, front_rightUltrasonicEcho);
				USfrontLeft.attach(arduino,arduino.getPortName(), front_leftUltrasonicTrig, front_leftUltrasonicEcho);
				USback.attach(arduino,arduino.getPortName(), backUltrasonicTrig, backUltrasonicEcho);
				USbackRight.attach(arduino,arduino.getPortName(), back_rightUltrasonicTrig, back_rightUltrasonicEcho);
				USbackLeft.attach(arduino,arduino.getPortName(), back_leftUltrasonicTrig, back_leftUltrasonicEcho);
	}
	
	public void startTrack(){
		/**
		 * Start the tracking services
		 */
		// TODO left eye must do the same move than right eye
		eyesTracker = (Tracking) startPeer("eyesTracker");
		// rightTracker = (Tracking) startPeer("rightTracker");
		neck.detach();
		rightEye.detach();
		leftEye.detach();
		eyesTracker.y.setPin(39); // neck
		eyesTracker.x.setPin(42); // right eye
	}
	
	public void stopTrack(){
		eyesTracker.releaseService();
		neck.attach(arduino,39);
		rightEye.attach(arduino,42);
		leftEye.attach(arduino, 40);
	}
	
	// TODO protect against self collision with  -> servoName.getPos()
	public void leftArm(int shoulderAngle, int armAngle, int forearmAngle, int wristAngle, int handAngle){
		/**
		 * Move the left arm . Use : leftArm(shoulder angle, arm angle, forearm angle, wrist angle, hand angle) -1 mean "no change"
		 */
		if (shoulderAngle == -1){shoulderAngle = leftShoulder.getPos() ;}
		if (armAngle == -1){armAngle = leftArm.getPos() ;}
		if (forearmAngle == -1){forearmAngle = leftForearm.getPos() ;}
		if (wristAngle == -1){wristAngle = leftWrist.getPos() ;}
		if (handAngle == -1){handAngle = leftHand.getPos() ;}
		
		leftShoulder.moveTo(shoulderAngle);
		leftArm.moveTo(armAngle);
		leftForearm.moveTo(forearmAngle);
		leftWrist.moveTo(wristAngle);
		leftHand.moveTo(handAngle);
	}
	
	// TODO protect against self collision
	public void rightArm(int shoulderAngle, int armAngle, int forearmAngle, int wristAngle, int handAngle){
		/**
		 * Move the right arm . Use : leftArm(shoulder angle, arm angle, forearm angle, wrist angle, hand angle) -1 mean "no change"
		 */
		if (shoulderAngle == -1){shoulderAngle = rightShoulder.getPos() ;}
		if (armAngle == -1){armAngle = rightArm.getPos() ;}
		if (forearmAngle == -1){forearmAngle = rightForearm.getPos() ;}
		if (wristAngle == -1){wristAngle = rightWrist.getPos() ;}
		if (handAngle == -1){handAngle = rightHand.getPos() ;}
		
		rightShoulder.moveTo(shoulderAngle);
		rightArm.moveTo(armAngle);
		rightForearm.moveTo(forearmAngle);
		rightWrist.moveTo(wristAngle);
		rightHand.moveTo(handAngle);
	}

	public void startPosition(){
		/**
		 * Set the servo start
		 */
		
		leftForearm.moveTo(136);
		rightForearm.moveTo(5);
		rightShoulder.moveTo(4);
		leftShoulder.moveTo(145);
		rightArm.moveTo(30);
		leftArm.moveTo(108);
		neck.moveTo(75);
		leftEye.moveTo(75);
		rightEye.moveTo(127);
		rightHand.moveTo(10);
		rightWrist.moveTo(116);
		leftHand.moveTo(150);
		leftWrist.moveTo(85);
	}
	private void myShiftOut(String value){
		/**
		 * Used to manage a shift register
		 */
		
		arduino.digitalWrite(LATCH, 0);		// Stop the copy
		for (int i = 0; i < 8; i++){  // Store the data
			if (value.charAt(i) == '1') {
				arduino.digitalWrite(DATA, 1);
			}
			else {
				arduino.digitalWrite(DATA, 0);
			}
			arduino.digitalWrite(SHIFT, 1);
			arduino.digitalWrite(SHIFT, 0);
			}
		arduino.digitalWrite(LATCH, 1);	// copy   
		
	}
	
	public void mouthState(String value){
		/**
		 * Set the mouth attitude . choose : smile, notHappy, speechLess, empty.
		 */
		
		if (value == "smile") {
			myShiftOut("11011100");
		}
		else if (value == "notHappy"){
			myShiftOut("00111110");
		}
		else if (value == "speechLess"){
			myShiftOut("10111100");
		}
		else if (value == "empty"){
			myShiftOut("00000000");
		}

	}
	
	public void setdelays(Integer d1, Integer d2, Integer d3) {
		
		delaytime = d1;
		delaytimestop = d2;
		delaytimeletter = d3;
	}
	
	public synchronized void saying(String text) { // Adapt mouth leds to words
		/**
		 * Say text and move mouth leds
		 */
		
		log.info("Saying :" + text);
		mouth.speak(text);
		sleep(50);
			boolean ison = false;
			String testword;
			String[] a = text.split(" ");
			for (int w = 0; w < a.length; w++) {
				// String word = ;
				// log.info(String.valueOf(a[w].length()));

				if (a[w].endsWith("es")) {
					testword = a[w].substring(0, a[w].length() - 2);
				} 
				else if (a[w].endsWith("e")) {
					testword = a[w].substring(0, a[w].length() - 1);
					// log.info("e gone");
				}
				else {
					testword = a[w];
				}

				char[] c = testword.toCharArray();

				for (int x = 0; x < c.length; x++) {
					char s = c[x];

					if ((s == 'a' || s == 'e' || s == 'i' || s == 'o' || s == 'u' || s == 'y') && !ison) {

						myShiftOut("00011100"); 
						ison = true;
						sleep(delaytime);
						myShiftOut("00000100");					} 
					else if (s == '.') {
						ison = false;
						myShiftOut("00000000");
						sleep(delaytimestop);
					} 
					else {
						ison = false;
						sleep(delaytimeletter); // # sleep half a second
					}

				}
				myShiftOut("00000000");
				sleep(2);
			}
	}
	
	public Sweety publishState(){
		super.publishState();
		arduino.publishState();
		leftForearm.publishState();
		rightForearm.publishState();
		rightShoulder.publishState();
		leftShoulder.publishState();
		rightArm.publishState();
		neck.publishState();
		leftEye.publishState();
		leftArm.publishState();
		rightEye.publishState();
		rightHand.publishState();
		rightWrist.publishState();
		leftHand.publishState();
		leftWrist.publishState();
		return this;
	}
	
	
	public boolean connect(String port){
		/**
		 *  Connect the arduino to a COM port . Exemple : connect("COM8")
		 */
		
		return arduino.connect(port);
	}
	
	public void attach(){
		/**
		 * Attach the servos to arduino pins
		 */
		
		rightForearm.attach(arduino.getName(),34);
		leftForearm.attach(arduino.getName(),35);
		rightShoulder.attach(arduino.getName(),36);
		leftShoulder.attach(arduino.getName(),37);
		rightArm.attach(arduino.getName(),38);
		leftArm.attach(arduino.getName(),41);
		neck.attach(arduino.getName(),39);
		leftEye.attach(arduino.getName(),40);
		rightEye.attach(arduino.getName(),42);
		rightHand.attach(arduino.getName(),46);
		rightWrist.attach(arduino.getName(),44);
		leftHand.attach(arduino.getName(),43);
		leftWrist.attach(arduino.getName(),45);
	}
	
	public void detach(){
		/**
		 *  detach the servos to arduino pins
		 */
		
		rightForearm.detach();
		leftForearm.detach();
		rightShoulder.detach();
		leftShoulder.detach();
		rightArm.detach();
		leftArm.detach();
		neck.detach();
		leftEye.detach();
		rightEye.detach();
		rightHand.detach();
		rightWrist.detach();
		leftHand.detach();
		leftWrist.detach();
	}
	
	public Sweety(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		/**
		 * Return information about the service
		 */
		
		return "Service for the robot Sweety";
	}
	
	public Status test(){
		Status status = super.test();
		Runtime.start("gui", "GUIService");
		
		return status;
	}

	static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			Sweety sweety = (Sweety) Runtime.start("sweety", "Sweety");
			sweety.test();

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
