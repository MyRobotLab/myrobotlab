package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.IntegratedMovement.ArmConfig;
import org.myrobotlab.IntegratedMovement.CollisionDectection;
import org.myrobotlab.IntegratedMovement.CollisionItem;
import org.myrobotlab.IntegratedMovement.GravityCenter;
import org.myrobotlab.IntegratedMovement.IMArm;
import org.myrobotlab.IntegratedMovement.IMBuild;
import org.myrobotlab.IntegratedMovement.IMCollisionShape;
import org.myrobotlab.IntegratedMovement.IMData;
import org.myrobotlab.IntegratedMovement.IMPart;
import org.myrobotlab.IntegratedMovement.JmeManager;
import org.myrobotlab.IntegratedMovement.Map3D;
import org.myrobotlab.IntegratedMovement.Map3DPoint;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.kinematics.DHLinkType;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoControlListener;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoDataListener;
import org.slf4j.Logger;
import com.jme3.math.Vector3f;

/**
 * 
 * IntegratedMovement - This class provides a 3D based inverse kinematics
 * implementation that allows you to specify the robot arm geometry based on DH
 * Parameters. The work is based on InversedKinematics3D by kwatters with
 * different computation and goal, including collision detection and
 * moveToObject
 *
 * @author Christian/Calamity
 * 
 */
public class IntegratedMovement extends Service
		implements ServoDataListener, ServoControlListener {

	public IntegratedMovement(String reservedKey) {
		super(reservedKey);
	}

	public enum Ai {
		AVOID_COLLISION(0x02, "Avoid Collision"), HOLD_POSITION(0x01, "Hold Position"), KEEP_BALANCE(0x04,
				"Keep Balance");
		public String text;
		public int value;

		private Ai(int value, String text) {
			this.value = value;
			this.text = text;
		}
	}
	public enum ObjectPointLocation {
		CENTER(0x07, "Center"), CENTER_SIDE(0x08,
				"Side Center"), CLOSEST_POINT(0x06, "Closest Point"), END_CENTER(
				0x05, "Center End"), END_SIDE(0x04, "Side End"), LEFT_SIDE(0x09, "Left Side"), ORIGIN_CENTER(0x01, "Center Origin"), ORIGIN_SIDE(0x02, "Side Origin"), RIGHT_SIDE(0x0A, "Right Side");
		public String location;
		public int value;

		private ObjectPointLocation(int value, String location) {
			this.value = value;
			this.location = location;
		}
	}

	// private HashMap<String, DHRobotArm> arms = new HashMap<String,
	// DHRobotArm>();

	public final static Logger log = LoggerFactory.getLogger(IntegratedMovement.class);

	private static final long serialVersionUID = 1L;

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(IntegratedMovement.class.getCanonicalName());
		meta.addDescription("a 3D kinematics service supporting D-H parameters");
		meta.addCategory("robot", "control");
		meta.addCategory("simulator");
		meta.addPeer("openni", "OpenNi", "Kinect service");
		meta.addDependency("inmoov.fr", "jm3-model", "1.0.0", "zip");
		String jmeVersion = "3.2.2-stable";
		meta.addDependency("org.jmonkeyengine", "jme3-core", jmeVersion);
		meta.addDependency("org.jmonkeyengine", "jme3-desktop", jmeVersion);
		meta.addDependency("org.jmonkeyengine", "jme3-lwjgl", jmeVersion);
		meta.addDependency("org.jmonkeyengine", "jme3-jogg", jmeVersion);
		// meta.addDependency("org.jmonkeyengine", "jme3-test-data",
		// jmeVersion);
		meta.addDependency("com.simsilica", "lemur", "1.11.0");
		meta.addDependency("com.simsilica", "lemur-proto", "1.10.0");

		meta.addDependency("org.jmonkeyengine", "jme3-bullet", jmeVersion);
		meta.addDependency("org.jmonkeyengine", "jme3-bullet-native", jmeVersion);

		// meta.addDependency("jme3utilities", "Minie", "0.6.2");

		// "new" physics - ik forward kinematics ...

		// not really supposed to use blender models - export to j3o
		meta.addDependency("org.jmonkeyengine", "jme3-blender", jmeVersion);

		// jbullet ==> org="net.sf.sociaal" name="jME3-jbullet"
		// rev="3.0.0.20130526"

		// audio dependencies
		meta.addDependency("de.jarnbjo", "j-ogg-all", "1.0.0");
		meta.setAvailable(true);
		return meta;
	}

	public static void main(String[] args) throws Exception {
		LoggingFactory.init(Level.INFO);

		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("gui", "SwingGui");
		IntegratedMovement ik = (IntegratedMovement) Runtime.start("ik", "IntegratedMovement");

		// Setup controller and servo
		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
		arduino.setBoardMega();
		 arduino.setVirtual(true);
		arduino.connect("COM3");
		arduino.setDebug(false);
		// define and attach servo
		// map is set so servo accept angle as input, output where
		// they need to go so that their part they where attach to
		// move by the input degree
		HobbyServo midStom = (HobbyServo) Runtime.start("midStom", "HobbyServo");
		midStom.map(-90.0, 90.0, 148.0, 38.0);
		midStom.attach(arduino.getName(), 26, 0.0, 13.0);
		ik.attach(midStom);
		
		HobbyServo topStom = (HobbyServo) Runtime.start("topStom", "HobbyServo");
		topStom.map(-10.0, 10.0, 92.0, 118.0);
		topStom.attach(arduino.getName(), 7, 0.0, 13.0);
		ik.attach(topStom);
		
		HobbyServo omoplate = (HobbyServo) Runtime.start("omoplate", "HobbyServo");
		omoplate.attach(arduino.getName(), 11, 10.0, 15.0);
		omoplate.map(10.0, 70.0, 10.0, 70.0);
		ik.attach(omoplate);
		
		HobbyServo Romoplate = (HobbyServo) Runtime.start("Romoplate", "HobbyServo");
		Romoplate.attach(arduino.getName(), 31, 10.0, 15.0);
		Romoplate.map(10.0, 70.0, 10.0, 70.0);
		ik.attach(Romoplate);
		
		HobbyServo shoulder = (HobbyServo) Runtime.start("shoulder", "HobbyServo");
		shoulder.attach(arduino.getName(), 26, 0.0, 14.0);
		shoulder.map(-30.0, 150.0, 0.0, 180.0);
		ik.attach(shoulder);
		
		HobbyServo Rshoulder = (HobbyServo) Runtime.start("Rshoulder", "HobbyServo");
		Rshoulder.attach(arduino.getName(), 6, 0.0, 14.0);
		Rshoulder.map(-30.0, 150.0, 0.0, 180.0);
		ik.attach(Rshoulder);
		
		HobbyServo rotate = (HobbyServo) Runtime.start("rotate", "HobbyServo");
		rotate.attach(arduino.getName(), 9, 0.0, 18.0);
		rotate.map(-44.0, 70.0, 46.0, 160.0);
		ik.attach(rotate);
		
		HobbyServo Rrotate = (HobbyServo) Runtime.start("Rrotate", "HobbyServo");
		Rrotate.attach(arduino.getName(), 29, 0.0, 18.0);
		Rrotate.map(-44.0, 70.0, 46.0, 160.0);
		ik.attach(Rrotate);
		
		HobbyServo bicep = (HobbyServo) Runtime.start("bicep", "HobbyServo");
		bicep.attach(arduino.getName(), 8, 10.0, 26.0);
		bicep.map(5.0, 60.0, 5.0, 80.0);
		ik.attach(bicep);
		
		HobbyServo Rbicep = (HobbyServo) Runtime.start("Rbicep", "HobbyServo");
		Rbicep.attach(arduino.getName(), 28, 10.0, 26.0);
		Rbicep.map(5.0, 60.0, 5.0, 80.0);
		ik.attach(Rbicep);
		
		HobbyServo wrist = (HobbyServo) Runtime.start("wrist", "HobbyServo");
		wrist.attach(arduino.getName(), 7, 90.0, 26.0);
		wrist.map(0.0, 180.0, 0.0, 180.0);
		ik.attach(wrist);
		
		HobbyServo Rwrist = (HobbyServo) Runtime.start("Rwrist", "HobbyServo");
		Rwrist.attach(arduino.getName(), 27, 90.0, 26.0);
		Rwrist.map(0.0, 180.0, 0.0, 180.0);
		ik.attach(Rwrist);
		
		HobbyServo leftHipY = (HobbyServo) Runtime.start("leftHipY", "HobbyServo");
		leftHipY.attach(arduino.getName(), 35, 0.0, 5.0);
		leftHipY.map(-20.0, 20.0, 0.0, 40.0);
		ik.attach(leftHipY);
		
		HobbyServo leftHipR = (HobbyServo) Runtime.start("leftHipR", "HobbyServo");
		leftHipR.attach(arduino.getName(), 36, 0.0, 5.0);
		leftHipR.map(-10.0, 10.0, 0.0, 20.0);
		ik.attach(leftHipR);
		
		HobbyServo leftHipP = (HobbyServo) Runtime.start("leftHipP", "HobbyServo");
		leftHipP.attach(arduino.getName(),37,0.0, 5.0);
		leftHipP.map(-46.0, 46.0, 0.0, 92.0);
		ik.attach(leftHipP);
		
		HobbyServo leftKnee = (HobbyServo) Runtime.start("leftKnee", "HobbyServo");
		leftKnee.attach(arduino.getName(), 38, 0.0, 5.0);
		leftKnee.map(0.0, 40.0, 0.0, 40.0);
		ik.attach(leftKnee);
		
		HobbyServo leftAnkleP = (HobbyServo) Runtime.start("leftAnkleP", "HobbyServo");
		leftAnkleP.attach(arduino.getName(), 39, 0.0, 5.0);
		leftAnkleP.map(-25.0, 45.0, 0.0, 70.0);
		ik.attach(leftAnkleP);
		
		HobbyServo leftAnkleR = (HobbyServo) Runtime.start("leftAnkleR", "HobbyServo");
		leftAnkleR.attach(arduino.getName(), 40, 0.0, 5.0);
		leftAnkleR.map(-27.0, 27.0, 0.0, 54.0);
		ik.attach(leftAnkleR);
		
		HobbyServo rightHipY = (HobbyServo) Runtime.start("rightHipY", "HobbyServo");
		rightHipY.attach(arduino.getName(), 41, 0.0, 5.0);
		rightHipY.map(-20.0, 20.0, 0.0, 40.0);
		ik.attach(rightHipY);
		
		HobbyServo rightHipR = (HobbyServo) Runtime.start("rightHipR", "HobbyServo");
		rightHipR.attach(arduino.getName(), 42, 0.0, 5.0);
		rightHipR.map(-10.0, 10.0, 0.0, 20.0);
		ik.attach(rightHipR);
		
		HobbyServo rightHipP = (HobbyServo) Runtime.start("rightHipP", "HobbyServo");
		rightHipP.attach(arduino.getName(), 43, 0.0, 5.0);
		rightHipP.map(-46.0, 46.0, 0.0, 92.0);
		ik.attach(rightHipP);
		
		HobbyServo rightKnee = (HobbyServo) Runtime.start("rightKnee", "HobbyServo");
		rightKnee.attach(arduino.getName(), 44, 0.0, 5.0);
		rightKnee.map(0.0, 40.0, 0.0, 40.0);
		ik.attach(rightKnee);
		
		HobbyServo rightAnkleP = (HobbyServo) Runtime.start("rightAnkleP", "HobbyServo");
		rightAnkleP.attach(arduino.getName(), 45, 0.0, 5.0);
		rightAnkleP.map(-25.0, 45.0, 0.0, 70.0);
		ik.attach(rightAnkleP);
		
		HobbyServo rightAnkleR = (HobbyServo) Runtime.start("rightAnkleR", "HobbyServo");
		rightAnkleR.attach(arduino.getName(), 46, 0.0, 5.0);
		rightAnkleR.map(-27.0, 27.0, 0.0, 54.0);
		ik.attach(rightAnkleR);
		
		// Servo finger = (Servo) Runtime.start("finger","Servo");
		// finger.attach(arduino,18,90);
		// finger.map(89.999,90.001,89.999,90.001);
		// finger.setVelocity(26);
		// //#bicep.setMinMax(5,90)
		// finger.moveTo(90);
		// Servo Rfinger = (Servo) Runtime.start("Rfinger","Servo");
		// Rfinger.attach(arduino,38,90);
		// Rfinger.map(89.999,90.001,89.999,90.001);
		// Rfinger.setVelocity(26);
		// //#bicep.setMinMax(5,90)
		// Rfinger.moveTo(90);

		
		// need to move a little so the position update
		midStom.moveTo(1.0);
		topStom.moveTo(1.0);
		Romoplate.moveTo(11.0);
		Rshoulder.moveTo(1.0);
		Rrotate.moveTo(1.0);
		Rbicep.moveTo(6.0);
		omoplate.moveTo(11.0);
		shoulder.moveTo(1.0);
		rotate.moveTo(1.0);
		bicep.moveTo(6.0);
		wrist.moveTo(91.0);
		Rwrist.moveTo(91.0);
		leftHipY.moveTo(1.0);
		leftHipR.moveTo(1.0);
		leftHipP.moveTo(1.0);
		leftKnee.moveTo(1.0);
		leftAnkleP.moveTo(1.0);
		leftAnkleR.moveTo(1.0);
		rightHipY.moveTo(1.0);
		rightHipR.moveTo(1.0);
		rightHipP.moveTo(1.0);
		rightKnee.moveTo(1.0);
		rightAnkleP.moveTo(1.0);
		rightAnkleR.moveTo(1.0);

		midStom.moveTo(0.0);
		topStom.moveTo(0.0);
		Romoplate.moveTo(10.0);
		Rshoulder.moveTo(0.0);
		Rrotate.moveTo(0.0);
		Rbicep.moveTo(5.0);
		omoplate.moveTo(10.0);
		shoulder.moveTo(0.0);
		rotate.moveTo(0.0);
		bicep.moveTo(5.0);
		wrist.moveTo(90.0);
		Rwrist.moveTo(90.0);
		leftHipY.moveTo(0.0);
		leftHipR.moveTo(0.0);
		leftHipP.moveTo(0.0);
		leftKnee.moveTo(0.0);
		leftAnkleP.moveTo(0.0);
		leftAnkleR.moveTo(0.0);
		rightHipY.moveTo(0.0);
		rightHipR.moveTo(0.0);
		rightHipP.moveTo(0.0);
		rightKnee.moveTo(0.0);
		rightAnkleP.moveTo(0.0);
		rightAnkleR.moveTo(0.0);

		sleep(1000);
		
		ik.setOrigin(new Point(0, 0, -0.1345, 0, 0, 0));
		/*
		 * defining each part of the robot TODO saved those setting to file
		 */

		float scale = 0.001f;
		IMPart partMidStom = ik.createPart("midStom", IMCollisionShape.CYLINDER, 0.085); // create a part
																// with his name
																// and radius
																// (used for
																// collision &
																// if no 3d
																// model)
		partMidStom.setControl(ArmConfig.DEFAULT, midStom.getName()); // set a servo to this
														// part, (String
														// configuration, part,
														// servo)
		partMidStom.setDHParameters(ArmConfig.DEFAULT, 0.108, 0, 0, 90, DHLinkType.REVOLUTE); // set
																					// the
																					// DH
																					// parameters
																					// for
																					// kinematic
		partMidStom.set3DModel("Models/mtorso.j3o", 0.001f, new Point(-0,0,0,0, 0, 0)); //set the 3d model, scale, and offset)
		partMidStom.setMass(2.832, 0.5);
		ik.attach(partMidStom); // add the part to the IntegratedMovement
								// service.

		IMPart partTopStom = ik.createPart("topStom", 0.150);
		partTopStom.setControl(ArmConfig.DEFAULT, topStom.getName());
		partTopStom.setDHParameters(ArmConfig.DEFAULT, 0, 90, 0.300, -90);
		partTopStom.setVisible(true);
		partTopStom.set3DModel("Models/ttorso1.j3o", .001f, new Point(0,0.015f, 0f , 90 , -90, 0));
		partTopStom.setMass(5.774, 0.95);
		ik.attach(partTopStom);
		

		IMPart partLeftArmAttach = ik.createPart("leftArmAttach", .001);
		partLeftArmAttach.setDHParameters(ArmConfig.DEFAULT, 0.143, 180, 0, 90);
		partLeftArmAttach.setVisible(true);
		partLeftArmAttach.noCollisionCheck(true);
		ik.attach(partLeftArmAttach);

		IMPart partLeftOmoplate = ik.createPart("leftOmoplate", .001);
		partLeftOmoplate.setDHParameters(ArmConfig.DEFAULT, .004, -5.6, 0.04, 90);
		partLeftOmoplate.setControl(ArmConfig.DEFAULT, omoplate.getName());
		partLeftOmoplate.set3DModel("Models/Lomoplate1.j3o", 0.001f, new Point(0.001,0.004,0,-90,-90,0));
		partLeftOmoplate.setMass(0.739, 0.5);
		partLeftOmoplate.noCollisionCheck(true);
		ik.attach(partLeftOmoplate);
		
		IMPart partLeftShoulder = ik.createPart("leftShoulder", IMCollisionShape.BOX, .055);
		partLeftShoulder.setControl(ArmConfig.DEFAULT, shoulder.getName());
		partLeftShoulder.set3DModel("Models/Lshoulder.j3o", 0.001f, new Point(-.0125,0,0,-90,0,-90));
		partLeftShoulder.setDHParameters(ArmConfig.DEFAULT, -0.077, 90, 0, -90);
		partLeftShoulder.setMass(0.513, 0.5);
		ik.attach(partLeftShoulder);
		
		IMPart partLeftRotate = ik.createPart("leftRotate", 0.085);
		partLeftRotate.setControl(ArmConfig.DEFAULT, rotate.getName());
		partLeftRotate.setDHParameters(ArmConfig.DEFAULT, -0.282, -90, 0, 90);
		partLeftRotate.set3DModel("Models/rotate1.j3o", .001f, new Point(0, 0, -0.0582, 0, 0, 0));
		partLeftRotate.setMass(0.715, 0.5754);
		partLeftRotate.noCollisionCheckWith("leftBicep");
		ik.attach(partLeftRotate);
		
		IMPart partLeftBicepAttach = ik.createPart("leftBicepAttach", 0.001);
		partLeftBicepAttach.setDHParameters(ArmConfig.DEFAULT, .03, 90, 0, 90);
		partLeftBicepAttach.noCollisionCheck(true);
		ik.attach(partLeftBicepAttach);

		IMPart partLeftBicep = ik.createPart("leftBicep", 0.05);
		partLeftBicep.setControl(ArmConfig.DEFAULT, bicep.getName());
		partLeftBicep.setDHParameters(ArmConfig.DEFAULT, 0, -7 + 24.4 + 180, .3, 0);
		partLeftBicep.set3DModel("Models/Lbicep.j3o", 0.001f, new Point(0.013,0.001,0,-90,0,0));
		partLeftBicep.setMass(0.940, 0.4559);
		partLeftBicep.noCollisionCheckWith("leftRotate");
		ik.attach(partLeftBicep);
		
		IMPart partRightArmAttach = ik.createPart("rightArmAttach", 0.001);
		partRightArmAttach.setDHParameters(ArmConfig.DEFAULT, -0.143, 0, 0, 90);
		partRightArmAttach.noCollisionCheck(true);
		ik.attach(partRightArmAttach);
		
		IMPart partRightOmoplate = ik.createPart("rightOmoplate", 0.001);
		partRightOmoplate.setDHParameters(ArmConfig.DEFAULT, -0.004, -5.6+180, 0.04, -90);
		partRightOmoplate.setControl(ArmConfig.DEFAULT, Romoplate.getName());
		partRightOmoplate.set3DModel("Models/Romoplate1.j3o", scale, new Point(-0.001,-0.002,0,-90,90,0));
		partRightOmoplate.setMass(0.739, 0.5);
		partRightOmoplate.noCollisionCheck(true);
		ik.attach(partRightOmoplate);
		
		IMPart partRightShoulder = ik.createPart("rightShoulder", IMCollisionShape.BOX, 0.055);
		partRightShoulder.setDHParameters(ArmConfig.DEFAULT, 0.077, 90, .0, 90);
		partRightShoulder.setControl(ArmConfig.DEFAULT, Rshoulder.getName());
		partRightShoulder.set3DModel("Models/Rshoulder1.j3o", scale, new Point(0.0225,-0.01,0,-90,0,-90));
		partRightShoulder.setMass(0.513, 0.5);
		ik.attach(partRightShoulder);
		
		IMPart partRightRotate = ik.createPart("rightRotate", 0.085);
		partRightRotate.setDHParameters(ArmConfig.DEFAULT, 0.282, -90, 0, 90);
		partRightRotate.setControl(ArmConfig.DEFAULT, Rrotate.getName());
		partRightRotate.set3DModel("Models/rotate1.j3o", scale, new Point(0,0,-0.056,180,0,0));
		partRightRotate.setMass(0.715, 0.5754);
		partRightRotate.noCollisionCheckWith("rightBicep");
		ik.attach(partRightRotate);
		
		IMPart partRightBicepAttach = ik.createPart("rightBicepAttach", 0.001);
		partRightBicepAttach.setDHParameters(ArmConfig.DEFAULT, .03, 90, 0, -90);
		partRightBicepAttach.noCollisionCheck(true);
		ik.attach(partRightBicepAttach);
		
		IMPart partRightBicep = ik.createPart("rightBicep", 0.05);
		partRightBicep.setDHParameters(ArmConfig.DEFAULT, 0, -7 + 24.4 , .3, 0);
		partRightBicep.setControl(ArmConfig.DEFAULT, Rbicep.getName());
		partRightBicep.set3DModel("Models/Rbicep1.j3o", scale, new Point(0.004,0,0,-90,0,0));
		partRightBicep.setMass(0.940, 0.4559);
		partRightBicep.noCollisionCheckWith("rightRotate");
		ik.attach(partRightBicep);
		
		IMPart partLowStom = ik.createPart("lowStom", IMCollisionShape.BOX, 0.115);
		partLowStom.setDHParameters(ArmConfig.DEFAULT, 0.071, 0, 0, 0);
		partLowStom.set3DModel("Models/ltorso.j3o", scale, new Point(0,-0.005,0.071,0,0,0));
		partLowStom.setMass(2.832, 0.5);
		ik.attach(partLowStom);
		
		IMPart partHarlHip = ik.createPart("harlHip", IMCollisionShape.BOX, 0.169);
		partHarlHip.setDHParameters(ArmConfig.DEFAULT, 0.0635, 0, 0, 0);
		partHarlHip.set3DModel("Models/harlLTorso1.j3o", 1, new Point(0,0,0,0,0,0));
		partHarlHip.setMass(1.814, 0.5);
		ik.attach(partHarlHip);
		
		IMPart partHarlHipLeftAttach = ik.createPart("harlHipLeftAttach", 0.001); //o= 0,0,0 right, up front
		partHarlHipLeftAttach.setDHParameters(ArmConfig.DEFAULT, 0, 180, 0.127, 180); //.127,0,0,left,down,front
		partHarlHipLeftAttach.setDHParameters(ArmConfig.REVERSE, 0.0415, 0, 0.127, 0); // 0,0,0,right up front
		partHarlHipLeftAttach.setControl(ArmConfig.REVERSE, "leftHipY");
		partHarlHipLeftAttach.noCollisionCheck(true);
		ik.attach(partHarlHipLeftAttach);
		
		IMPart partHarlLeftHipY = ik.createPart("harlLeftHipY", IMCollisionShape.BOX, 0.064);
		partHarlLeftHipY.setDHParameters(ArmConfig.DEFAULT, .0415, 0, 0, 90); //.127,-.0415,0, left, back, down
		partHarlLeftHipY.setDHParameters(ArmConfig.REVERSE, 0, 0, 0, -90); //.127, -0.0415, 0 right up front
		partHarlLeftHipY.set3DModel("Models/harlLhipY.j3o", scale, new Point(0,0,0,0,0,0));
		partHarlLeftHipY.setControl(ArmConfig.DEFAULT, "leftHipY");
		partHarlLeftHipY.setMass(0.3);
		partHarlLeftHipY.noCollisionCheckWith("harlLeftHipR");
		ik.attach(partHarlLeftHipY);
		
		IMPart partHarlLeftHipRAttach = ik.createPart("harlLeftHipRAttach", 0.001);
		partHarlLeftHipRAttach.setDHParameters(ArmConfig.DEFAULT, 0.01, 90, 0, 0); //.127,-.0415,-.01, down, back, right
		partHarlLeftHipRAttach.setDHParameters(ArmConfig.REVERSE, 0.01, 90, 0, 180); // 0.127, -0.0415, 0 right back up
		partHarlLeftHipRAttach.setControl(ArmConfig.REVERSE, "leftHipR");
		partHarlLeftHipRAttach.noCollisionCheck(true);
		ik.attach(partHarlLeftHipRAttach);
		
		IMPart partHarlLeftHipR = ik.createPart("harlLeftHipR", IMCollisionShape.BOX, 0.110);
		partHarlLeftHipR.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.0629, 90); //.127, -0.143, -0.1, down, left, back
		partHarlLeftHipR.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.0629, 90); //0.127, -0.0415, -0.01 up front right
		partHarlLeftHipR.setControl(ArmConfig.DEFAULT, "leftHipR");
		partHarlLeftHipR.set3DModel("Models/harlLhipR.j3o", scale, new Point(0.001,-0.011,0,-90,90,0));
		partHarlLeftHipR.setMass(0.3);
		partHarlLeftHipR.noCollisionCheckWith("harlLeftHipY", "harlLeftHipP");
		ik.attach(partHarlLeftHipR);
		
		IMPart partHarlLeftHipPAttach = ik.createPart("harlLeftHipPAttach", 0.001);
		partHarlLeftHipPAttach.setDHParameters(ArmConfig.DEFAULT, -0.006, 0, 0, 180); //.121, -0.143, -0.01, down, right, front
		partHarlLeftHipPAttach.setDHParameters(ArmConfig.REVERSE, 0.006, 0, 0, 180); //.127, -0.143, -0.01, up, right, back
		partHarlLeftHipPAttach.setControl(ArmConfig.REVERSE, "leftHipP");
		partHarlLeftHipPAttach.noCollisionCheck(true);
		ik.attach(partHarlLeftHipPAttach);
		
		IMPart partHarlLeftHipP = ik.createPart("harlLeftHipP", IMCollisionShape.BOX, 0.085);
		partHarlLeftHipP.setDHParameters(ArmConfig.DEFAULT, 0, 0, .3630, 180); //0.121, -0.506, -0.01, down, left, back
		partHarlLeftHipP.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.3630, 180); //0.121, -0.143, -0.01, up, left front
		partHarlLeftHipP.setControl(ArmConfig.DEFAULT, "leftHipP");
		partHarlLeftHipP.setControl(ArmConfig.REVERSE, "leftKnee");
		partHarlLeftHipP.set3DModel("Models/harlLhipP.j3o", scale, new Point(0,0,0,-90,0,0));
		partHarlLeftHipP.setMass(1.980);
		partHarlLeftHipP.noCollisionCheckWith("harlLeftHipR");
		ik.attach(partHarlLeftHipP);
		
		IMPart partHarlLeftKnee = ik.createPart("harlLeftKnee", IMCollisionShape.BOX, 0.085);
		partHarlLeftKnee.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.3668, 180); //0.121, -0.8728, -0.01, down right front
		partHarlLeftKnee.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.3668, 180); //0.121, -0.506, -0.01, up right back
		partHarlLeftKnee.setControl(ArmConfig.DEFAULT, "leftKnee");
		partHarlLeftKnee.setControl(ArmConfig.REVERSE, "leftAnkleP");
		partHarlLeftKnee.set3DModel("Models/harlLKnee.j3o", scale, new Point(0,0.003,0,-90,180,0));
		partHarlLeftKnee.setMass(2.195);
		ik.attach(partHarlLeftKnee);
		
		IMPart partHarlLAnkleP = ik.createPart("harlLAnkleP", IMCollisionShape.BOX, 0.110);
		partHarlLAnkleP.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.04, 90); //0.121, -0.9128, -0.01, down back right
		partHarlLAnkleP.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.04, 90); //0.121, -0.8728, -0.01, up left front
		partHarlLAnkleP.setControl(ArmConfig.DEFAULT, "leftAnkleP");
		partHarlLAnkleP.setControl(ArmConfig.REVERSE, "leftAnkleR");
		partHarlLAnkleP.set3DModel("Models/harlLAnkleP1.j3o", scale, new Point(0,0.003,0,90,0,0));
		partHarlLAnkleP.setMass(0.3);
		ik.attach(partHarlLAnkleP);
		
		IMPart partHarlLAnkleR = ik.createPart("harlLAnkleR", IMCollisionShape.BOX, 0.140);
		partHarlLAnkleR.setDHParameters(ArmConfig.DEFAULT, 0, 0, .04, 0); //0.121, -0.9528, -0.01, down back right
		partHarlLAnkleR.setDHParameters(ArmConfig.REVERSE, 0, 180, 0.04, 180); //0.121, -0.9128, 0.01, up front right
		partHarlLAnkleR.setControl(ArmConfig.DEFAULT, "leftAnkleR");
		partHarlLAnkleR.set3DModel("Models/harlLankleR.j3o", scale, new Point(0,0,0,-90,90,0));
		partHarlLAnkleR.setMass(1.402);
		ik.attach(partHarlLAnkleR);

		IMPart partHarlHipRightAttach = ik.createPart("harlHipRightAttach", 0.001); //o = x(0): right z(-.1345): up y(0): front
		partHarlHipRightAttach.setDHParameters(ArmConfig.DEFAULT, 0, 0, .127, 180); // x(.127): right, z(-.1345): down, y(0): back
		partHarlHipRightAttach.setDHParameters(ArmConfig.REVERSE, 0.0415, 180, -0.127, 0); //x(0)right, z(-.1345: up, y(0): front//something wrong from here
		partHarlHipRightAttach.setControl(ArmConfig.REVERSE, "rightHipY");
		partHarlHipRightAttach.noCollisionCheck(true);
		ik.attach(partHarlHipRightAttach);
		
		IMPart partHarlRightHipY = ik.createPart("harlRightHipY", IMCollisionShape.BOX, 0.064);
		partHarlRightHipY.setDHParameters(ArmConfig.DEFAULT, 0.0415, 0, 0, -90); // x(.127): right, z(-.176): back, y(0): up
		partHarlRightHipY.setDHParameters(ArmConfig.REVERSE, 0, 0, 0., 90); //x(.127)left, z(-.176)up, y(0)back
		partHarlRightHipY.setControl(ArmConfig.DEFAULT, "rightHipY");
		partHarlRightHipY.set3DModel("Models/harlRHipY.j3o", scale, new Point(0,0,0,0,0,0));
		partHarlRightHipY.setMass(0.3);
		partHarlRightHipY.noCollisionCheckWith("harlRightHipR");
		ik.attach(partHarlRightHipY);
		
		IMPart partHarlRightHipRAttach = ik.createPart("harlRightHipRAttach", 0.001);
		partHarlRightHipRAttach.setDHParameters(ArmConfig.DEFAULT, 0.01, -90, 0, 0); // x(.127):DOWN  , Z(-.176):back  y(-.01):right
		partHarlRightHipRAttach.setDHParameters(ArmConfig.REVERSE, 0.01, -90, 0, 180);  //x(.127)left, z(-.176)back, y(0)down
		partHarlRightHipRAttach.setControl(ArmConfig.REVERSE, "rightHipR");
		partHarlRightHipRAttach.noCollisionCheck(true);
		ik.attach(partHarlRightHipRAttach);
		
		IMPart partHarlRightHipR = ik.createPart("harlRightHipR", IMCollisionShape.BOX, 0.085);
		partHarlRightHipR.setDHParameters(ArmConfig.DEFAULT, 0., 0, 0.0629, 90); //x(.127):down z(-.2389):left, y(-0.1):back
		partHarlRightHipR.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.0629, 90); //x(.127)up, z(-.176)front, y(-.1)right 
		partHarlRightHipR.setControl(ArmConfig.DEFAULT, "rightHipR");
		partHarlRightHipR.set3DModel("Models/harlRHipR.j3o", scale, new Point(0.001,-0.011,0,90,-90,180));
		partHarlRightHipR.setMass(0.3);
		partHarlRightHipR.noCollisionCheckWith("harlrightHipY", "harlRightHipP");
		ik.attach(partHarlRightHipR);
		
		IMPart partHarlRightHipPAttach = ik.createPart("harlRightHipPAttach", 0.001);
		partHarlRightHipPAttach.setDHParameters(ArmConfig.DEFAULT, -0.006, 0, 0, 180); //x(.133):down , z(-.2389):right , y(-.1):front
		partHarlRightHipPAttach.setDHParameters(ArmConfig.REVERSE, 0.006, 0, 0.0, 180); //(x.127):up, z(-.2389):right, y(-.1)back   
		partHarlRightHipPAttach.setControl(ArmConfig.REVERSE, "rightHipP");
		partHarlRightHipPAttach.noCollisionCheck(true);
		ik.attach(partHarlRightHipPAttach);
		
		IMPart partHarlRightHipP = ik.createPart("harlRightHipP", IMCollisionShape.BOX, 0.110);
		partHarlRightHipP.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.3630, 180); //x(.133):down , z(-.6009):left , y(-.1):back
		partHarlRightHipP.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.3630, 180); //x(.133):up, z(-.2389):left , y(-.1):front
		partHarlRightHipP.setControl(ArmConfig.DEFAULT, "rightHipP");
		partHarlRightHipP.setControl(ArmConfig.REVERSE, "rightKnee");
		partHarlRightHipP.set3DModel("Models/harlRHip.j3o", scale, new Point(0,0,0,-90,0,0));
		partHarlRightHipP.setMass(1.980);
		partHarlRightHipP.noCollisionCheckWith("harlRightHipR");
		ik.attach(partHarlRightHipP);
		
		IMPart partHarlRightKnee = ik.createPart("harlRightKnee", IMCollisionShape.BOX, 0.085);
		partHarlRightKnee.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.3668, 180); //x(.133):down z(-.9677):right , y(-.1):front
		partHarlRightKnee.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.3668, 180); //x(.133):up, z(-.6009):right , y(-.1)::back 
		partHarlRightKnee.setControl(ArmConfig.DEFAULT, "rightKnee");
		partHarlRightKnee.setControl(ArmConfig.REVERSE, "rightAnkleP");
		partHarlRightKnee.set3DModel("Models/harlRKnee.j3o", scale, new Point(0.,0.003,0,-90,180,0));
		partHarlRightKnee.setMass(2.195);
		ik.attach(partHarlRightKnee);
		
		IMPart partHarlRAnkleP = ik.createPart("harlRAnkleP", IMCollisionShape.BOX, 0.110);
		partHarlRAnkleP.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.04, 90); //x(.133): down, z(-1.0077): back, y(-.1):right
		partHarlRAnkleP.setDHParameters(ArmConfig.REVERSE, 0, 0, 0.04, 90); //x(.133):up, z(-.9677) left, y(-.1)front 
		partHarlRAnkleP.setControl(ArmConfig.DEFAULT, "rightAnkleP");
		partHarlRAnkleP.setControl(ArmConfig.REVERSE, "rightAnkleR");
		partHarlRAnkleP.set3DModel("Models/harlRankleP.j3o", scale, new Point(0,0.003,0,-90,0,0));
		partHarlRAnkleP.setMass(0.3);
		ik.attach(partHarlRAnkleP);
		
		IMPart partHarlRAnkleR = ik.createPart("harlRAnkleR", IMCollisionShape.BOX, 0.140);
		partHarlRAnkleR.setDHParameters(ArmConfig.DEFAULT, 0, 0, 0.04, 0); //x(.133): down, z(-1.0477):back, y(-.1):righy 
		partHarlRAnkleR.setDHParameters(ArmConfig.REVERSE, 0, 180, 0.04, 180); //x(.133)up, z(-1.0077)front, y(-0.1)right
		partHarlRAnkleR.setControl(ArmConfig.DEFAULT, "rightAnkleR");//o= x(.133): down, z(-1.0479) back, y(-.1):right
		partHarlRAnkleR.set3DModel("Models/harlRankleR.j3o", scale, new Point(0, 0, 0, -90, 90, 0));
		partHarlRAnkleR.setMass(1.402);
		ik.attach(partHarlRAnkleR);
		
		IMArm armTorso = ik.createArm("torso");
		armTorso.add(partHarlHip);
		armTorso.add(partLowStom);
		armTorso.add(partMidStom);
		armTorso.add(partTopStom);
		ik.attach(armTorso);
		
		IMArm armLeftArm = ik.createArm("leftArm");
		armLeftArm.add(partLeftArmAttach);
		armLeftArm.add(partLeftOmoplate);
		armLeftArm.add(partLeftShoulder);
		armLeftArm.add(partLeftRotate);
		armLeftArm.add(partLeftBicepAttach);
		armLeftArm.add(partLeftBicep);
		ik.attach(armLeftArm);
		
		IMArm armRightArm = ik.createArm("rightArm");
		armRightArm.add(partRightArmAttach);
		armRightArm.add(partRightOmoplate);
		armRightArm.add(partRightShoulder);
		armRightArm.add(partRightRotate);
		armRightArm.add(partRightBicepAttach);
		armRightArm.add(partRightBicep);
		ik.attach(armRightArm);
		
		IMArm armLeftLeg = ik.createArm("leftLeg");
		armLeftLeg.add(partHarlHipLeftAttach);
		armLeftLeg.add(partHarlLeftHipY);
		armLeftLeg.add(partHarlLeftHipRAttach);
		armLeftLeg.add(partHarlLeftHipR);
		armLeftLeg.add(partHarlLeftHipPAttach);
		armLeftLeg.add(partHarlLeftHipP);
		armLeftLeg.add(partHarlLeftKnee);
		armLeftLeg.add(partHarlLAnkleP);
		armLeftLeg.add(partHarlLAnkleR);
		ik.attach(armLeftLeg);
		
		IMArm armRightLeg = ik.createArm("rightLeg");
		armRightLeg.add(partHarlHipRightAttach);
		armRightLeg.add(partHarlRightHipY);
		armRightLeg.add(partHarlRightHipRAttach);
		armRightLeg.add(partHarlRightHipR);
		armRightLeg.add(partHarlRightHipPAttach);
		armRightLeg.add(partHarlRightHipP);
		armRightLeg.add(partHarlRightKnee);
		armRightLeg.add(partHarlRAnkleP);
		armRightLeg.add(partHarlRAnkleR);
		ik.attach(armRightLeg);
		
		IMBuild inMoov = ik.createBuild("inMoov");
		inMoov.addArm(armRightLeg, ArmConfig.REVERSE);
		inMoov.addArm(armTorso, armRightLeg);
		inMoov.addArm(armLeftLeg, armRightLeg);
		inMoov.addArm(armRightArm, armTorso);
		inMoov.addArm(armLeftArm, armTorso);
		ik.attach(inMoov);

//		IMBuild inMoov = ik.createBuild("inMoov");
//		inMoov.addArm(armRightLeg);
//		inMoov.addArm(armTorso);
//		inMoov.addArm(armLeftLeg);
//		inMoov.addArm(armRightArm, armTorso);
//		inMoov.addArm(armLeftArm, armTorso);
//		ik.attach(inMoov);

//		IMBuild inMoov = ik.createBuild("inMoov");
//		inMoov.setInputMatrix(ik.createInputMatrix(0, 0, -0.1345, 0, 0, 0));
//		inMoov.addArm(armLeftLeg, ArmConfig.REVERSE);
//		inMoov.addArm(armTorso, armLeftLeg);
//		inMoov.addArm(armRightLeg, armLeftLeg);
//		inMoov.addArm(armRightArm, armTorso);
//		inMoov.addArm(armLeftArm, armTorso);
//		ik.attach(inMoov);

		
		//rightKnee.moveTo(15.5);
		
		sleep(50);
		//Matrix m = ik.getArm("rightLeg").getInputMatrix();
		//Matrix im = new Matrix(4,4).loadIdentity().multiply(ik.getArm("rightLeg").parts.getLast().getEnd());
		//im = armRightLeg.getTransformMatrix(ArmConfig.REVERSE, im);
		
		
		
		//ik.getArm("rrightLeg").updatePosition(ik.getData().getControls());

		/*
		 * ik.addArm("leftArm");
		 * 
		 * //ik.setDHLink("leftArm", wrist, 0, -90, 0, 0);
		 * //ik.setDHLinkType("wrist", DHLinkType.REVOLUTE_ALPHA);
		 * //ik.setDHLink("leftArm", "wristup", 0, -5, 110, 0);
		 * //ik.setDHLink("leftArm", "wristdown", 0, 0, 105, 45);
		 * //ik.setDHLink("leftArm", "finger", 5, -90, 5, 0);
		 * ik.removeAi("leftArm", Ai.AVOID_COLLISION);
		 * ik.startEngine("leftArm");
		 * 
		 * ik.addArm("rightArm");
		 * //////////// //#ik.setDHLink(wrist,00,-90,200,0)
		 * //ik.setDHLink("rightArm", Rwrist, 00, -90, 0, 0);
		 * //ik.setDHLinkType("Rwrist", DHLinkType.REVOLUTE_ALPHA);
		 * //ik.setDHLink("rightArm", "Rwristup", 0, 5, 110, 0);
		 * //ik.setDHLink("rightArm", "Rwristdown", 0, 0, 105, -45);
		 * //ik.setDHLink("rightArm", "Rfinger", 5, 90, 5, 0);
		 * ik.removeAi("rightArm", Ai.AVOID_COLLISION);
		 * ik.startEngine("rightArm");
		 * 
		 * ik.addArm("kinect"); // ik.setDHLink("kinect", mtorso, 113, 90, 0,
		 * -90); // ik.setDHLink("kinect", ttorso, 0, 90 + 90, 110, -90);
		 * ik.setDHLink("kinect", "camera", 0, 90, 10, 90); //
		 * ik.startEngine("kinect"); ik.removeAi("kinect", Ai.AVOID_COLLISION);
		 * // #define object, each dh link are set as an object, but the //
		 * #start point and end point will be update by the ik service, but
		 * still // need // #a name and a radius // #static object need a start
		 * point, an end point, a name and a radius ik.clearObject();
		 * ik.addObject(0.0, 0.0, 0.0, 0.0, 0.0, -150.0, "base", 150.0, false);
		 * ik.addObject("mtorso", 150.0); ik.addObject("ttorso", 10.0);
		 * ik.addObject("omoplate", 10.0); ik.addObject("Romoplate", 10.0);
		 * ik.addObject("shoulder", 50.0); ik.addObject("Rshoulder", 50.0);
		 * ik.addObject("rotate", 50.0); ik.addObject("Rrotate", 50.0);
		 * ik.addObject("bicep", 60.0); ik.addObject("Rbicep", 60.0);
		 * ik.addObject("wrist", 10.0); ik.addObject("Rwrist", 70.0);
		 * ik.addObject("leftS", 10); ik.addObject("rightS", 10);
		 * ik.addObject("wristup", 70); ik.addObject("wristdown", 70);
		 * ik.objectAddIgnore("bicep", "wristup"); ik.addObject("Rwristup", 70);
		 * ik.addObject("Rwristdown", 70); ik.objectAddIgnore("Rbicep",
		 * "Rwristup"); ik.objectAddIgnore("leftS", "rightS");
		 * ik.objectAddIgnore("omoplate", "rotate");
		 * ik.objectAddIgnore("Romoplate", "Rrotate");
		 * ik.objectAddIgnore("rightS", "shoulder"); ik.objectAddIgnore("leftS",
		 * "Rshoulder"); //sleep(1000); // ik.addObject("Rfinger",10.0); //
		 * ik.addObject(-1000.0,400, 0, 1000, 425, 00, "obstacle",40, true); //
		 * #ik.addObject(360,540,117,360, 550,107,"cymbal",200) //
		 * #ik.addObject(90,530,-180,300,545,-181,"bell", 25) //
		 * ik.addObject(170,640,-70,170,720,-250,"tom",150,true);
		 * ik.addObject(0, 700, 300, 0, 700, 150, "beer", 30, true);
		 * 
		 * // ik.holdTarget("leftArm", true); ik.visualize(); 
		 * ((TestJmeIMModel) ik.jmeApp).addPart("Rwrist", "Models/RWristFinger.j3o", 1f, "Rbicep", new Vector3f(15, -290, -10), Vector3f.UNIT_Y.mult(-1), (float) Math.toRadians(180));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("wrist","Models/LWristFinger.j3o", 1f, "bicep", new Vector3f(0, -290, -20), Vector3f.UNIT_Y.mult(1), (float) Math.toRadians(180));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("neck", "Models/neck.j3o", 1f,"ttorso", new Vector3f(0, 452.5f, -45), Vector3f.UNIT_X.mult(-1),(float) Math.toRadians(0));
		 *  ((TestJmeIMModel)ik.jmeApp).addPart("neckroll", null, 1f, "neck", new Vector3f(0, 0, 0), Vector3f.UNIT_Z.mult(1), (float) Math.toRadians(2));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("head", "Models/head.j3o", 1f,"neckroll", new Vector3f(0, 10, 20), Vector3f.UNIT_Y.mult(-1),(float) Math.toRadians(0)); ((TestJmeIMModel)
		 * ik.jmeApp).addPart("jaw", "Models/jaw.j3o", 1f, "head", new Vector3f(-5, 63, -50), Vector3f.UNIT_X.mult(-1), (float) Math.toRadians(0)); 
		 * // ((TestJmeIMModel) ik.jmeApp).addPart("finger", null, 10f, "wrist", new // Vector3f(0,205,0), Vector3f.UNIT_X.mult(-1), // (float)Math.toRadians(0));
		 *  print
		 * ik.currentPosition("rightArm") print ik.currentPosition("leftArm")
		 * 
		 * // TODO add the object that can collide with the model //
		 * ik.jmeApp.addObject();
		 */
		
		// sleep(3000);
		// double[][] jp = ik.createJointPositionMap("leftArm");
		// Point x = new
		// Point(jp[jp.length-2][0],jp[jp.length-2][1],jp[jp.length-2][2],0,0,0);
		// Point y = new
		// Point(jp[jp.length-1][0],jp[jp.length-1][1],jp[jp.length-1][2],0,0,0);
		// ik.addObject(x,y,"finger",100.0, false);

		// ik.startOpenNI();
		// ik.processKinectData();

		/*
		 * ik.cog.setLinkMass("wrist", 0.176, 0.7474);
		 * ik.cog.setLinkMass("Rwrist", 0.176, 0.7474);
		 */
		// ik.setAi("rightArm", Ai.KEEP_BALANCE);
		// ik.setAi("leftArm", Ai.KEEP_BALANCE);
		ik.startSimulator();
		sleep(1000);
		// ik.getSimulatorManager().setAxesVisible(false);
		//inMoov.addMsg("reverseArm","rightLeg");
		//ik.moveTo("inMoov", "rightLeg", -0, -0.0, -.144);
//		ik.moveTo("inMoov", "leftArm", -0.1,.7,0.3);
//		ik.moveTo("inMoov", "rightArm", 0.6,.0,0.3);
//		ik.moveTo("inMoov", "rightLeg", .133, .5, -.91);
//		sleep(10000);
//		ik.moveTo("inMoov", "rightLeg", .133, 0, -.91);
//		sleep(5000);
//		ik.moveTo("inMoov", "leftArm", -0.6,.0,0.3);
//		ik.moveTo("inMoov", "rightArm", 0.120,.0,.7);
//		ik.moveTo("inMoov", "rightLeg", .5, .0, -.700);
		
	}


	private void attach(IMBuild build) {
		imData.addBuild(build);
		build.start();
	}

	public IMBuild createBuild(String buildName) {
		return new IMBuild(buildName, this, originMatrix);
	}

	public void attach(IMArm arm) {
		imData.addArm(arm);
		arm.updatePosition(imData.getControls());
	}

	public Matrix getOriginMatrix() {
		return originMatrix;
	}

	public void setOrigin(Point point) {
		originMatrix = createInputMatrix(point.getX(), point.getY(), point.getZ(), point.getRoll(), point.getPitch(), point.getYaw());
	}


	public transient GravityCenter cog = new GravityCenter();

	public transient CollisionDectection collisionItems = new CollisionDectection();

	transient private IMData imData = new IMData();
	transient private Matrix inputMatrix = new Matrix(4,4).loadIdentity();

	private transient JmeManager jmeManager = null;

	private String kinectName = "kinect";
	private transient Map3D map3d = new Map3D();

	private transient OpenNi openni = null;

	private boolean ProcessKinectData = false;
	
	private transient Matrix originMatrix = new Matrix(4,4).loadIdentity();


	public void attach(Attachable service) {
	    if (ServoControl.class.isAssignableFrom(service.getClass())) {
	        attachServoControl((ServoControl) service);
	        return;
	    }
	    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
	  }
	
	 public void attachServoControl(ServoControl service) {
	       // guard
        if (isAttached(service)){
        	return;
	    }
        setControl(service.getName());
	}
	 
	public boolean isAttached(ServoControl service){
		if(imData.getControl(service.getName())!=null){
			return true;
		}
		return false;
	}
	
	public IMArm createArm(String name){
		IMArm arm = new IMArm(name);
		return arm;
	}

	public String addObject(double oX, double oY, double oZ, double eX, double eY, double eZ, String name,
			double radius, boolean render) {
		return addObject(new Point(oX, oY, oZ, 0, 0, 0), new Point(eX, eY, eZ, 0, 0, 0), name, radius, render);
	}

	public String addObject(HashMap<Integer[], Map3DPoint> cloudMap) {
		CollisionItem item = new CollisionItem(cloudMap);
		collisionItems.addItem(item);
		return item.getName();
	}

	public String addObject(Point origin, Point end, String name, double radius) {
		return addObject(origin, end, name, radius, false);
	}

	public String addObject(Point origin, Point end, String name, double radius, boolean render) {
		CollisionItem item = new CollisionItem(origin, end, name, radius, render);
		collisionItems.addItem(item);
//		if (jmeApp != null) {
//			jmeApp.addObject(item);
//		}
		broadcastState();
		return item.getName();
	}

	public String addObject(String name, double radius) {
		return addObject(name, radius, false);
	}

	public String addObject(String name, double radius, boolean render) {
		return addObject(new Point(0, 0, 0, 0, 0, 0), new Point(0, 0, 0, 0, 0, 0), name, radius, render);
	}

	public void attach(IMPart part) {
		imData.addPart(part);
	}

	public void clearObject() {
		collisionItems.clearItem();
	}

	/**
	 * This create a rotation and translation matrix that will be applied on the
	 * "moveTo" call.
	 * 
	 * @param dx
	 *            - x axis translation
	 * @param dy
	 *            - y axis translation
	 * @param dz
	 *            - z axis translation
	 * @param roll
	 *            - rotation about z (in degrees)
	 * @param pitch
	 *            - rotation about x (in degrees)
	 * @param yaw
	 *            - rotation about y (in degrees)
	 * @return the matrix
	 */
	public Matrix createInputMatrix(double dx, double dy, double dz, double roll, double pitch, double yaw) {
		roll = MathUtils.degToRad(roll);
		pitch = MathUtils.degToRad(pitch);
		yaw = MathUtils.degToRad(yaw);
		Matrix trMatrix = Matrix.translation(dx, dy, dz);
		Matrix rotMatrix = Matrix.zRotation(roll).multiply(Matrix.yRotation(yaw).multiply(Matrix.xRotation(pitch)));
		inputMatrix = trMatrix.multiply(rotMatrix);
		return inputMatrix;
	}

	public IMPart createPart(String partName, double radius){
		return createPart(partName, IMCollisionShape.CYLINDER, radius);
	}
	
	public IMPart createPart(String partName, IMCollisionShape shape, double radius) {
		IMPart part = new IMPart(partName);
		part.setRadius(radius);
		return part;
	}
	
	public IMBuild getBuild(String buildName){
		return imData.getBuild(buildName);
	}
	
	public IMArm getArm(String armName){
		return imData.getArm(armName);
	}

	public Double getAngleWithAxis(String objectName, String axis) {
		CollisionItem ci = collisionItems.getItem(objectName);
		Vector3f vo = new Vector3f((float) ci.getOrigin().getX(), (float) ci.getOrigin().getY(),
				(float) ci.getOrigin().getZ());
		Vector3f ve = new Vector3f((float) ci.getEnd().getX(), (float) ci.getEnd().getY(), (float) ci.getEnd().getZ());
		Vector3f vci = vo.subtract(ve);
		Vector3f va = null;
		if (axis.equals("x")) {
			va = Vector3f.UNIT_X;
		} else if (axis.equals("y")) {
			va = Vector3f.UNIT_Y;
		} else if (axis.equals("z")) {
			va = Vector3f.UNIT_Z;
		}
		return MathUtils.radToDeg(va.angleBetween(vci));
	}

	public Double getAngleWithObject(String armName, String objectName) {
/*		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			CollisionItem ci = collisionItems.getItem(objectName);
			Vector3f vo = new Vector3f((float) ci.getOrigin().getX(), (float) ci.getOrigin().getY(),
					(float) ci.getOrigin().getZ());
			Vector3f ve = new Vector3f((float) ci.getEnd().getX(), (float) ci.getEnd().getY(),
					(float) ci.getEnd().getZ());
			Vector3f vci = vo.subtract(ve);
			Point armvector = arm.getDHRobotArm().getVector();
			Vector3f va = new Vector3f((float) armvector.getX(), (float) armvector.getY(), (float) armvector.getZ());
			float angle = va.dot(vci);
			double div = Math.sqrt(Math.pow(vci.x, 2) + Math.pow(vci.y, 2) + Math.pow(vci.z, 2))
					* Math.sqrt(Math.pow(va.x, 2) + Math.pow(va.y, 2) + Math.pow(va.z, 2));
			return MathUtils.radToDeg(Math.acos(angle / div));
		}
		log.info("unknown arm {} for getAngleWithObject", armName);
*/		return 0.0;
		// return (double) va.angleBetween(vci);
	}

	public Collection<IMArm> getArms() {
		return imData.getArms().values();
	}

	public ConcurrentHashMap<String, CollisionItem> getCollisionObject() {
		return collisionItems.getItems();
	}

	public IMData getData() {
		return imData;
	}

	public ObjectPointLocation[] getEnumLocationValue() {
		return ObjectPointLocation.values();
	}

	/**
	 * @return the openni
	 */
	public OpenNi getOpenni() {
		return openni;
	}

	public JmeManager getSimulatorManager() {
		return jmeManager;
	}


	public void moveTo(String build, String arm, String part, double x, double y, double z){
		moveTo(build, arm, part, new Point(x, y, z, 0, 0, 0));
	}
	
	public void moveTo(String build, String arm, double x, double y, double z, double roll, double pitch, double yaw){
		moveTo(build, arm, null, new Point(x, y, z, roll, pitch, yaw));
	}
	
	public void moveTo(String build, String arm, double x, double y, double z){
		moveTo(build, arm, null, x, y, z);
	}
	
	public void moveTo(String build, String arm, String part, Point point){
		imData.getBuild(build).addMsg("moveTo", arm, part, point);
	}

	public void moveTo(String build, String arm, Point point){
		moveTo(build, arm, null, point);
	}

	public void moveTo(String armName, String objectName, ObjectPointLocation location) {
		moveTo(armName, objectName, location, null);
	}

	public void moveTo(String armName, String objectName, ObjectPointLocation location, String lastDHLink) {
/*		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			arm.moveTo(collisionItems.getItem(objectName), location, lastDHLink);
		} else {
			log.info("unknown arm {}", armName);
		}
*/	}

	public void objectAddIgnore(String object1, String object2) {
		collisionItems.addIgnore(object1, object2);
	}

	public void objectRemoveIgnore(String object1, String object2) {
		collisionItems.removeIgnore(object1, object2);
	}

	public void onMoveTo(ServoControl data) {
//		if (openni != null) {
//			map3d.updateKinectPosition(currentPosition(kinectName));
//		}
		imData.onMoveTo(data);
	}

	public void onOpenNiData(OpenNiData data) throws InterruptedException {
		if (ProcessKinectData) {
			ProcessKinectData = false;
			long a = System.currentTimeMillis();
			log.info("start {}", a);
			map3d.processDepthMap(data);
			removeKinectObject();
			ArrayList<HashMap<Integer[], Map3DPoint>> object = map3d.getObject();
			for (int i = 0; i < object.size(); i++) {
				addObject(object.get(i));
			}
//			if (jmeApp != null) {
//				jmeApp.addObject(getCollisionObject());
//			}
			long b = System.currentTimeMillis();
			log.info("end {} - {} - {}", b, b - a, this.inbox.size());
			broadcastState();
		}
	}

	public void onServoData(ServoData data) {
//		if (openni != null) {
//			map3d.updateKinectPosition(currentPosition(kinectName));
//		}
		imData.onServoData(data);
	}

	public void processKinectData() throws InterruptedException {
		ProcessKinectData = true;
		onOpenNiData(openni.get3DData());
	}

	public void removeAi(Ai ai) {
/*		for (IMEngine engine : imData.getArms().values()) {
			engine.removeAi(ai);
		}
*/	}

	public void removeAi(String armName, Ai ai) {
//		IMEngine arm = imData.getArm(armName);
//		if (arm != null) {
//			arm.removeAi(ai);
//		} else {
//			log.info("unknown arm {} for removeAi", armName);
//		}
	}

	public void removeArm(String armName) {
		if (!imData.removeArm(armName))
			log.info("unknown arm {} for removeArm", armName);
		;
	}

	private void removeKinectObject() throws InterruptedException {
		collisionItems.removeKinectObject();

	}

	public void removeObject(String name) {
		collisionItems.removeObject(name);
	}

	public synchronized void sendAngles(String name, double positionValueDeg) {
		// invoke("publishAngles", name, pos);
		ServoControl srv = (ServoControl) Runtime.getService(name);
		if (srv != null)
			srv.moveTo(positionValueDeg);
	}

	public void setAi(Ai ai) {
//		for (IMEngine engine : imData.getArms().values()) {
//			engine.setAi(ai);
//		}
	}

	public void setAi(String ai) {
		for (Ai a : Ai.values()) {
			if (a.text.equals(ai)) {
				setAi(a);
				return;
			}
		}
		log.info("Ai {} not found", ai);
	}

	public void setAi(String armName, Ai ai) {
//		IMEngine arm = imData.getArm(armName);
//		if (arm != null) {
//			arm.setAi(ai);
//		} else {
//			log.info("unknown arm {} for setAI", armName);
//		}
	}

	public void setControl(String srvCtrlName){
		subscribe(srvCtrlName, "publishMoveTo", getName(), "onMoveTo");
		subscribe(srvCtrlName, "publishServoData", getName(), "onServoData");
		imData.addControl(srvCtrlName);
	}

	public void setInputMatrix(String armName, Matrix inputMatrix) {
		if (inputMatrix.getNumCols() != 4 || inputMatrix.getNumRows() != 4) {
			log.info("wrong dimention for setInputMatrix (must be 4 x 4)");
		} else {
			imData.addInputMatrix(armName, inputMatrix);
		}
	}

	public void setKinectName(String kinectName) {
		this.kinectName = kinectName;
	}

	/**
	 * @param openni
	 *            the openni to set
	 */
	public void setOpenni(OpenNi openni) {
		this.openni = openni;
	}

	public OpenNi startOpenNI() throws Exception {
		if (openni == null) {
			openni = (OpenNi) startPeer("openni");
			openni.start3DData();
			//map3d.updateKinectPosition(currentPosition(kinectName));
		}
		return openni;
	}

	public void startSimulator() {
		if (jmeManager != null) {
			log.info("JmeApp already started");
			return;
		}
		jmeManager = new JmeManager(this);
		jmeManager.start("JmeIMModel", "Jme3App");
		// jmeManager.loadParts(imData);

	}
	
	public void setAnchorArm(IMBuild build, IMArm arm){
		build.addMsg("setAnchorArm", arm.getName());
	}
	
	public void setAnchorArm(String buildName, String armName){
		IMBuild build = getData().getBuild(buildName);
		build.addMsg("setAnchorArm", armName);
	}
	
	public void stopMoving(String buildName, String armName){
		IMBuild build = getData().getBuild(buildName);
		build.addMsg("stopMoving", armName);
	}

}
