package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.myrobotlab.IntegratedMovement.CollisionDectection;
import org.myrobotlab.IntegratedMovement.CollisionItem;
import org.myrobotlab.IntegratedMovement.Util;
import org.myrobotlab.IntegratedMovement.GravityCenter;
import org.myrobotlab.IntegratedMovement.IMData;
import org.myrobotlab.IntegratedMovement.IMEngine;
import org.myrobotlab.IntegratedMovement.IMPart;
import org.myrobotlab.IntegratedMovement.JmeManager;
import org.myrobotlab.IntegratedMovement.Map3D;
import org.myrobotlab.IntegratedMovement.Map3DPoint;
import org.myrobotlab.IntegratedMovement.PositionData;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.genetic.GeneticParameters;
import org.myrobotlab.jme3.IntegratedMovementInterface;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHLinkType;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.service.interfaces.IKJointAnglePublisher;
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
 * Rotation and Orientation information is not currently supported. (but should
 * be easy to add)
 *
 * @author Christian/Calamity
 * 
 */
public class IntegratedMovement extends Service
		implements IKJointAnglePublisher, ServoDataListener, ServoControlListener {

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
		// arduino.setVirtual(true);
		arduino.connect("COM3");
		arduino.setDebug(false);
		// define and attach servo
		// map is set so servo accept angle as input, output where
		// they need to go so that their part they where attach to
		// move by the input degree
		HobbyServo midStom = (HobbyServo) Runtime.start("midStom", "HobbyServo");
		midStom.map(-90.0, 90.0, 148.0, 38.0);
		midStom.attach(arduino.getName(), 26, 0.0);
		midStom.setSpeed(13.0);
		HobbyServo topStom = (HobbyServo) Runtime.start("topStom", "HobbyServo");
		topStom.map(-10.0, 10.0, 92.0, 118.0);
		topStom.attach(arduino.getName(), 7, 0.0);
		topStom.setSpeed(13.0);
		HobbyServo omoplate = (HobbyServo) Runtime.start("omoplate", "HobbyServo");
		omoplate.attach(arduino.getName(), 11, 10.0);
		omoplate.map(10.0, 70.0, 10.0, 70.0);
		omoplate.setSpeed(15.0);
		HobbyServo Romoplate = (HobbyServo) Runtime.start("Romoplate", "HobbyServo");
		Romoplate.attach(arduino.getName(), 31, 10.0);
		Romoplate.map(10.0, 70.0, 10.0, 70.0);
		Romoplate.setSpeed(15.0);
		HobbyServo shoulder = (HobbyServo) Runtime.start("shoulder", "HobbyServo");
		shoulder.attach(arduino.getName(), 26, 0.0);
		shoulder.map(-30.0, 150.0, 0.0, 180.0);
		shoulder.setSpeed(14.0);
		HobbyServo Rshoulder = (HobbyServo) Runtime.start("Rshoulder", "HobbyServo");
		Rshoulder.attach(arduino.getName(), 6, 0.0);
		Rshoulder.map(-30.0, 150.0, 0.0, 180.0);
		Rshoulder.setSpeed(14.0);
		HobbyServo rotate = (HobbyServo) Runtime.start("rotate", "HobbyServo");
		rotate.attach(arduino.getName(), 9, 0.0);
		rotate.map(-44.0, 70.0, 46.0, 160.0);
		rotate.setSpeed(18.0);
		HobbyServo Rrotate = (HobbyServo) Runtime.start("Rrotate", "HobbyServo");
		Rrotate.attach(arduino.getName(), 29, 0.0);
		Rrotate.map(-44.0, 70.0, 46.0, 160.0);
		Rrotate.setSpeed(18.0);
		HobbyServo bicep = (HobbyServo) Runtime.start("bicep", "HobbyServo");
		bicep.attach(arduino.getName(), 8, 10.0);
		bicep.map(5.0, 60.0, 5.0, 80.0);
		bicep.setSpeed(26.0);
		HobbyServo Rbicep = (HobbyServo) Runtime.start("Rbicep", "HobbyServo");
		Rbicep.attach(arduino.getName(), 28, 10.0);
		Rbicep.map(5.0, 60.0, 5.0, 80.0);
		Rbicep.setSpeed(26.0);
		HobbyServo wrist = (HobbyServo) Runtime.start("wrist", "HobbyServo");
		wrist.attach(arduino.getName(), 7, 90.0);
		wrist.map(0.0, 180.0, 0.0, 180.0);
		wrist.setSpeed(26.0);
		HobbyServo Rwrist = (HobbyServo) Runtime.start("Rwrist", "HobbyServo");
		Rwrist.attach(arduino.getName(), 27, 90.0);
		wrist.map(0.0, 180.0, 0.0, 180.0);
		Rwrist.setSpeed(26.0);
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

		/*
		 * defining each part of the robot TODO saved those setting to file
		 */

		IMPart partMidStom = ik.createPart("midStom", 0.010); // create a part
																// with his name
																// and radius
																// (used for
																// collision &
																// if no 3d
																// model)
		partMidStom.setControl("torso", midStom.getName()); // set a servo to this
														// part, (String
														// configuration, part,
														// servo)
		partMidStom.setDHParameters("torso", 0.108, 0, 0, 90, DHLinkType.REVOLUTE); // set
																					// the
																					// DH
																					// parameters
																					// for
																					// kinematic
		partMidStom.set3DModel("Models/mtorso.j3o", 0.001f, new Point(-0,0,0,0, 0, 0)); //set the 3d model, scale, and offset)
		partMidStom.linkTo("torso", "topStom"); // set the next part in the
												// configuration
		ik.attach(partMidStom); // add the part to the IntegratedMovement
								// service.

		IMPart partTopStom = ik.createPart("topStom", 0.010);
		partTopStom.setControl("torso", topStom.getName());
		partTopStom.setDHParameters("torso", 0, 90, 0.300, -90);
		partTopStom.setVisible(true);
		partTopStom.set3DModel("Models/ttorso1.j3o", .001f, new Point(0,0.015f, 0f , 90 , -90, 0));
		//partTopStom.linkTo("torso", "leftArmAttach");
		ik.attach(partTopStom);

		IMPart partLeftArmAttach = ik.createPart("leftArmAttach", .010);
		partLeftArmAttach.setDHParameters("leftArm", 0.143, 180, 0, 90);
		partLeftArmAttach.setVisible(true);
		partLeftArmAttach.linkTo("leftArm", "leftOmoplate");
		ik.attach(partLeftArmAttach);

		IMPart partLeftOmoplate = ik.createPart("leftOmoplate", .010);
		partLeftOmoplate.setDHParameters("leftArm", .004, -5.6, 0.04, 90);
		partLeftOmoplate.setControl("leftArm", omoplate.getName());
		partLeftOmoplate.linkTo("leftArm", "leftShoulder");
		partLeftOmoplate.set3DModel("Models/Lomoplate1.j3o", 0.001f, new Point(0.001,0.004,0,-90,-90,0));
		ik.attach(partLeftOmoplate);
		
		IMPart partLeftShoulder = ik.createPart("leftShoulder", .01);
		partLeftShoulder.setControl("leftArm", shoulder.getName());
		partLeftShoulder.set3DModel("Models/Lshoulder.j3o", 0.001f, new Point(0,0,0,-90,0,-90));
		partLeftShoulder.setDHParameters("leftArm", -0.065, 90, 0, -90);
		partLeftShoulder.linkTo("leftArm", "leftRotate");
		ik.attach(partLeftShoulder);
		
		IMPart partLeftRotate = ik.createPart("leftRotate", 0.01);
		partLeftRotate.setControl("leftArm", rotate.getName());
		partLeftRotate.setDHParameters("leftArm", -0.282, -90, 0, 90);
		partLeftRotate.set3DModel("Models/rotate1.j3o", .001f, new Point(0, 0, -0.0582, 0, 0, 0));
		partLeftRotate.linkTo("leftArm", "leftBicepAttach");
		ik.attach(partLeftRotate);
		
		IMPart partLeftBicepAttach = ik.createPart("leftBicepAttach", 0.01);
		partLeftBicepAttach.setDHParameters("leftArm", .03, 90, 0, 90);
		partLeftBicepAttach.linkTo("leftArm", "leftBicep");
		ik.attach(partLeftBicepAttach);

		IMPart partLeftBicep = ik.createPart("leftBicep", 0.01);
		partLeftBicep.setControl("leftArm", bicep.getName());
		partLeftBicep.setDHParameters("leftArm", 0, -7 + 24.4 + 180, .3, 0);
		partLeftBicep.set3DModel("Models/Lbicep.j3o", 0.001f, new Point(0.013,0.001,0,-90,0,0));
		ik.attach(partLeftBicep);
		
		IMPart partRightArmAttach = ik.createPart("rightArmAttach", 0.01);
		partRightArmAttach.setDHParameters("rightArm", -0.143, 0, 0, 90);
		partRightArmAttach.linkTo("rightArm", "rightOmoplate");
		ik.attach(partRightArmAttach);
		
		IMPart partRightOmoplate = ik.createPart("rightOmoplate", 0.01);
		partRightOmoplate.setDHParameters("rightArm", 0, -5.6+180, 0.045, 90);
		partRightOmoplate.setControl("rightArm", Romoplate.getName());
		partRightOmoplate.linkTo("rightArm", "rightShoulder");
		ik.attach(partRightOmoplate);
		
		IMPart partRightShoulder = ik.createPart("rightShoulder", 0.01);
		partRightShoulder.setDHParameters("rightArm", -0.077, 90, 0, 90);
		partRightShoulder.setControl("rightArm", Rshoulder.getName());
		partRightShoulder.linkTo("rightArm", "rightRotate");
		ik.attach(partRightShoulder);
		
		IMPart partRightRotate = ik.createPart("rightRotate", 0.01);
		partRightRotate.setDHParameters("rightArm", 0.284, 90, 0, 90);
		partRightRotate.setControl("rightArm", Rrotate.getName());
		partRightRotate.linkTo("rightArm", "rightBicepAttach");
		ik.attach(partRightRotate);
		
		IMPart partRightBicepAttach = ik.createPart("rightBicepAttach", 0.01);
		partRightBicepAttach.setDHParameters("rightArm", .04, 90, 0, -90);
		partRightBicepAttach.linkTo("rightArm", "rightBicep");
		ik.attach(partRightBicepAttach);
		
		IMPart partRightBicep = ik.createPart("rightBicep", 0.01);
		partRightBicep.setDHParameters("rightArm", 0, -7 + 24.4 , .3, 0);
		partRightBicep.setControl("rightArm", Rbicep.getName());
		ik.attach(partRightBicep);
		
		// #define the DH parameters for the ik service
		ik.addArm("torso");
		ik.setInputMatrix("torso", ik.createInputMatrix(0, 0, 0, 0, 0, 0));
		ik.setFirstPart("torso", partMidStom.getName());

		ik.addArm("leftArm");
		ik.linkArmTo("leftArm", "torso");
		ik.setFirstPart("leftArm", "leftArmAttach");

		ik.addArm("rightArm");
		ik.linkArmTo("rightArm", "torso");
		ik.setFirstPart("rightArm", "rightArmAttach");
		/*
		 * ik.addArm("leftArm");
		 * 
		 * //ik.setDHLink("leftArm", mtorso, 113, 180, 0, -90); //
		 * ik.setDHLink("rightArm",ttorso,0,90+65.6,346,0);
		 * //ik.setDHLink("leftArm", ttorso, 0, -90, 292, 90);
		 * //ik.setDHLink("leftArm", "rightS", 143, 180, 0, 90);
		 * //ik.setDHLink("leftArm", omoplate, 0, -5.6, 45, -90);
		 * //ik.setDHLink("leftArm", shoulder, 77, -30 + 90, 0, 90);
		 * //ik.setDHLink("leftArm", rotate, 284, 90, 40, 90);
		 * //ik.setDHLink("leftArm", bicep, 0, -7 + 24.4 + 90, 300, 0);
		 * //ik.setDHLink("leftArm", wrist, 0, -90, 0, 0);
		 * //ik.setDHLinkType("wrist", DHLinkType.REVOLUTE_ALPHA);
		 * //ik.setDHLink("leftArm", "wristup", 0, -5, 110, 0);
		 * //ik.setDHLink("leftArm", "wristdown", 0, 0, 105, 45);
		 * //ik.setDHLink("leftArm", "finger", 5, -90, 5, 0);
		 * ik.removeAi("leftArm", Ai.AVOID_COLLISION);
		 * ik.startEngine("leftArm");
		 * 
		 * ik.addArm("rightArm"); // ik.setDHLink("rightArm", mtorso, 113, 180,
		 * 0, -90); // ik.setDHLink("rightArm", ttorso, 0, -90, 292, 90); //
		 * ik.setDHLink("leftArm",ttorso,0,180,297.5,90);
		 * //ik.setDHLink("rightArm", "leftS", -143, 180, 0, -90);
		 * //ik.setDHLink("rightArm", Romoplate, 0, -5.6, 45, 90);
		 * //ik.setDHLink("rightArm", Rshoulder, -77, -30 + 90, 0, -90);
		 * //ik.setDHLink("rightArm", Rrotate, -284, 90, 40, -90);
		 * //ik.setDHLink("rightArm", Rbicep, 0, -7 + 24.4 + 90, 300, 0);
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
		 * // print ik.currentPosition();
		 * 
		 * // #setting ik parameters for the computing
		 * 
		 * // #move to a position // ik.moveTo("leftArm",260,410,-120); //
		 * ik.moveTo(280,190,-345); //
		 * #ik.moveTo("cymbal",ik.ObjectPointLocation.ORIGIN_SIDE, 0,0,5) //
		 * #mtorso.moveTo(45)
		 * log.info(ik.currentPosition("leftArm").toString());
		 * log.info(ik.currentPosition("rightArm").toString()); //
		 * shoulder.moveTo(90); // sleep(1000); //
		 * log.info(ik.currentPosition("leftArm").toString());
		 * 
		 * // print "kinect Position" + str(ik.currentPosition("kinect"));
		 * 
		 * // ik.holdTarget("leftArm", true); ik.visualize(); 
		 * ((TestJmeIMModel)ik.jmeApp).addPart("ltorso", "Models/ltorso.j3o", 1, null, new Vector3f(0, 0, 0), Vector3f.UNIT_X.mult(1), (float)Math.toRadians(0));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("mtorso", "Models/mtorso.j3o", 1f, null, new Vector3f(0, 0, 0), Vector3f.UNIT_Y.mult(-1), (float) Math.toRadians(0));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("ttorso", "Models/ttorso1.j3o", 1f, "mtorso", new Vector3f(0, 105f, 10), Vector3f.UNIT_Z, (float) Math.toRadians(0));
		 *  ((TestJmeIMModel) ik.jmeApp).addPart("rightS", null, 1f, "ttorso", new Vector3f(0, 300f, 0), Vector3f.UNIT_Z, (float) Math.toRadians(0));
		 *  ((TestJmeIMModel)ik.jmeApp).addPart("Romoplate", "Models/Romoplate1.j3o", 1f,"rightS", new Vector3f(-143f, 0, -17), Vector3f.UNIT_Z.mult(-1),(float) Math.toRadians(-4));
		 *  ((TestJmeIMModel)ik.jmeApp).addPart("Rshoulder", "Models/Rshoulder1.j3o", 1f,"Romoplate", new Vector3f(-23, -45f, 0), Vector3f.UNIT_X.mult(-1),(float) Math.toRadians(-32));
		 *  ((TestJmeIMModel)ik.jmeApp).addPart("Rrotate", "Models/rotate1.j3o", 1f, "Rshoulder", new Vector3f(-57, -55, 8), Vector3f.UNIT_Y.mult(-1), (float)Math.toRadians(-90));
		 *  ((TestJmeIMModel) ik.jmeApp).addPart("Rbicep","Models/Rbicep1.j3o", 1f, "Rrotate", new Vector3f(5, -225, -32), Vector3f.UNIT_X.mult(-1), (float) Math.toRadians(20));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("leftS", null, 1f, "ttorso", new Vector3f(0, 300f, 0), Vector3f.UNIT_Z, (float) Math.toRadians(0));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("omoplate","Models/Lomoplate1.j3o", 1f, "leftS", new Vector3f(143f, 0, -15), Vector3f.UNIT_Z.mult(1), (float) Math.toRadians(-6));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("shoulder","Models/Lshoulder.j3o", 1f, "omoplate", new Vector3f(17, -45f, 5), Vector3f.UNIT_X.mult(-1), (float) Math.toRadians(-30));
		 * ((TestJmeIMModel) ik.jmeApp).addPart("rotate", "Models/rotate1.j3o", 1f, "shoulder", new Vector3f(65, -58, -3), Vector3f.UNIT_Y.mult(1),(float) Math.toRadians(-90)); 
		 * ((TestJmeIMModel)ik.jmeApp).addPart("bicep", "Models/Lbicep.j3o", 1f, "rotate", new Vector3f(-14, -223, -28), Vector3f.UNIT_X.mult(-1), (float)Math.toRadians(17)); 
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
		 * ik.cog = new GravityCenter(ik); ik.cog.setLinkMass("mtorso", 2.832,
		 * 0.5); ik.cog.setLinkMass("ttorso", 5.774, 0.5);
		 * ik.cog.setLinkMass("omoplate", 0.739, 0.5);
		 * ik.cog.setLinkMass("Romoplate", 0.739, 0.5);
		 * ik.cog.setLinkMass("rotate", 0.715, 0.5754);
		 * ik.cog.setLinkMass("Rrotate", 0.715, 0.5754);
		 * ik.cog.setLinkMass("shoulder", 0.513, 0.5);
		 * ik.cog.setLinkMass("Rshoulder", 0.513, 0.5);
		 * ik.cog.setLinkMass("bicep", 0.940, 0.4559);
		 * ik.cog.setLinkMass("Rbicep", 0.940, 0.4559);
		 * ik.cog.setLinkMass("wrist", 0.176, 0.7474);
		 * ik.cog.setLinkMass("Rwrist", 0.176, 0.7474);
		 */
		// ik.setAi("rightArm", Ai.KEEP_BALANCE);
		// ik.setAi("leftArm", Ai.KEEP_BALANCE);
		ik.startSimulator();
		// ik.getSimulatorManager().setAxesVisible(false);

	}

	public transient GravityCenter cog = new GravityCenter(this);

	public transient CollisionDectection collisionItems = new CollisionDectection();

	transient GeneticParameters geneticParameters = new GeneticParameters();
	private IMData imData = new IMData();
	private Matrix inputMatrix = null;

	private transient IntegratedMovementInterface jmeApp = null;

	private transient JmeManager jmeManager = null;

	private String kinectName = "kinect";
	private transient Map3D map3d = new Map3D();

	private transient OpenNi openni = null;

	private boolean ProcessKinectData = false;

	public IntegratedMovement(String n) {
		super(n);
	}

	public void addArm(String name) {
		IMEngine engine = new IMEngine(name, this);
		engine.setInputMatrix(inputMatrix);
		imData.addArm(engine);
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
		if (jmeApp != null) {
			jmeApp.addObject(item);
		}
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
		for (String srvName : part.getControls().values()){
			if (imData.getControl(srvName) != null){
				setControl(srvName);
			}
		}
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

	public double[][] createJointPositionMap(String armName) {
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			return arm.createJointPositionMap();
		}
		log.info("unknown arm {}", armName);
		return new double[0][0];
	}

	public IMPart createPart(String partName, double radius) {
		IMPart part = new IMPart(partName);
		part.setRadius(radius);
		return part;
	}

	public Point currentPosition(String arm) {
		return Util.getPosition(imData, arm, null);
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
		IMEngine arm = imData.getArm(armName);
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
		return 0.0;
		// return (double) va.angleBetween(vci);
	}

	public Collection<IMEngine> getArms() {
		return imData.getArms().values();
	}

	public ConcurrentHashMap<String, CollisionItem> getCollisionObject() {
		return collisionItems.getItems();
	}

	public IMData getData() {
		return imData;
	}

	public IMEngine getEngine(String armName) {
		return imData.getArm(armName);
	}

	public ObjectPointLocation[] getEnumLocationValue() {
		return ObjectPointLocation.values();
	}

	/**
	 * @return the jmeApp
	 */
	public IntegratedMovementInterface getJmeApp() {
		return jmeApp;
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

	public void holdTarget(String arm, boolean holdEnabled) {
		IMEngine engine = imData.getArm(arm);
		if (engine != null) {
			engine.holdTarget(holdEnabled);
		} else {
			log.info("unknown arm {} for hold target", arm);
		}
	}

	public void linkArmTo(String armName, String linkTo) {
		if (imData.getArm(armName) == null || imData.getArm(linkTo) == null) {
			log.info("no arm named {} or {} in linkArmTo)", armName, linkTo);
		}
		imData.linkArmTo(armName, linkTo);
	}

	public void moveTo(String arm, double x, double y, double z) {
		moveTo(arm, x, y, z, null);
	}

	public void moveTo(String arm, double x, double y, double z, String lastDHLink) {
		moveTo(arm, new Point(x, y, z, 0, 0, 0), lastDHLink);
	}

	public void moveTo(String arm, Point point) {
		moveTo(arm, point, null);
	}

	public void moveTo(String name, Point point, String lastDHLink) {
		IMEngine arm = imData.getArm(name);
		if (arm != null) {
			arm.moveTo(point, lastDHLink);
			jmeApp.addPoint(point);
		}
		log.info("unknow arm {}", arm);
	}

	public void moveTo(String armName, String objectName, ObjectPointLocation location) {
		moveTo(armName, objectName, location, null);
	}

	public void moveTo(String armName, String objectName, ObjectPointLocation location, String lastDHLink) {
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			arm.moveTo(collisionItems.getItem(objectName), location, lastDHLink);
		} else {
			log.info("unknown arm {}", armName);
		}
	}

	public void objectAddIgnore(String object1, String object2) {
		collisionItems.addIgnore(object1, object2);
	}

	public void objectRemoveIgnore(String object1, String object2) {
		collisionItems.removeIgnore(object1, object2);
	}

	public void onMoveTo(ServoControl data) {
		for (IMEngine e : imData.getArms().values()) {
			e.updateLinksPosition(data);
		}
		if (openni != null) {
			map3d.updateKinectPosition(currentPosition(kinectName));
		}
		if (jmeApp != null) {
		}
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
			if (jmeApp != null) {
				jmeApp.addObject(getCollisionObject());
			}
			long b = System.currentTimeMillis();
			log.info("end {} - {} - {}", b, b - a, this.inbox.size());
			broadcastState();
		}
	}

	public void onServoData(ServoData data) {
		for (IMEngine e : imData.getArms().values()) {
			e.updateLinksPosition(data);
		}
		if (openni != null) {
			map3d.updateKinectPosition(currentPosition(kinectName));
		}
		if (jmeApp != null) {
			// jmeApp.updatePosition(data);
		}
		imData.onServoData(data);
		// jmeManager.updatePosition(imData);
	}

	public void processKinectData() throws InterruptedException {
		ProcessKinectData = true;
		onOpenNiData(openni.get3DData());
	}

	public Object[] publishAngles(String name, double positionValueDeg) {
		Object[] retval = new Object[] { (Object) name, (Object) positionValueDeg };
		return retval;
	}

	@Override
	public Map<String, Double> publishJointAngles(HashMap<String, Double> angleMap) {
		return angleMap;
	}

	public double[][] publishJointPositions(double[][] jointPositionMap) {
		return jointPositionMap;
	}

	public PositionData publishPosition(PositionData position) {
		return position;
	}

	public void publishPosition(String armName) {
		invoke("publishPosition", new PositionData(armName, currentPosition(armName)));
	}

	public void removeAi(Ai ai) {
		for (IMEngine engine : imData.getArms().values()) {
			engine.removeAi(ai);
		}
	}

	public void removeAi(String armName, Ai ai) {
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			arm.removeAi(ai);
		} else {
			log.info("unknown arm {} for removeAi", armName);
		}
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
		for (IMEngine engine : imData.getArms().values()) {
			engine.setAi(ai);
		}
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
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			arm.setAi(ai);
		} else {
			log.info("unknown arm {} for setAI", armName);
		}
	}

//	public void setControl(String armName, IMPart part, ServoControl control) {
	public void setControl(String srvCtrlName){
		//part.setControl(armName, control.getName());
		subscribe(srvCtrlName, "publishMoveTo", getName(), "onMoveTo");
		subscribe(srvCtrlName, "publishServoData", getName(), "onServoData");
	}

	public void setDHLink(String armName, String linkName, double d, double theta, double r, double alpha) {
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			DHLink dhLink = new DHLink(linkName, d, r, MathUtils.degToRad(theta), MathUtils.degToRad(alpha));
			DHRobotArm dhArm = arm.getDHRobotArm();
			dhArm.addLink(dhLink);
			arm.setDHRobotArm(dhArm);
			imData.addArm(arm);
		}
		log.error("Unknow arm {}", armName);
	}

	public void setDHLinkType(String name, DHLinkType type) {
		for (IMEngine engine : imData.getArms().values()) {
			for (DHLink link : engine.getDHRobotArm().getLinks()) {
				if (link.getName().equals(name)) {
					link.setType(type);
				}
			}
		}
	}

	public void setFirstPart(String armName, String partName) {
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			IMPart part = imData.getPart(partName);
			if (part != null) {
				imData.setFirstPart(armName, partName);
			} else
				log.info("unknown part name {}", partName);
		} else
			log.info("unknown arm name {}", armName);

	}

	public void setInputMatrix(String armName, Matrix inputMatrix) {
		if (inputMatrix.getNumCols() != 4 || inputMatrix.getNumRows() != 4) {
			log.info("wrong dimention for setInputMatrix (must be 4 x 4)");
		} else {
			imData.addInputMatrix(armName, inputMatrix);
		}
	}

	public void setJmeApp(IntegratedMovementInterface jmeApp) {
		this.jmeApp = jmeApp;
	}

	public void setKinectName(String kinectName) {
		this.kinectName = kinectName;
	}

	public void setMinMaxAngles(String partName, double min, double max) {
		for (IMEngine engine : imData.getArms().values()) {
			for (DHLink link : engine.getDHRobotArm().getLinks()) {
				if (link.getName().equals(partName)) {
					link.setMin(link.getInitialTheta() + Math.toRadians(min));
					link.setMax(link.getInitialTheta() + Math.toRadians(max));
				}
			}
		}
	}

	/**
	 * @param openni
	 *            the openni to set
	 */
	public void setOpenni(OpenNi openni) {
		this.openni = openni;
	}

	public void startEngine(String armName) {
		IMEngine arm = imData.getArm(armName);
		if (arm != null) {
			arm.start();
			addTask("publishPosition-" + armName, 1000, 0, "publishPosition", armName);
		}
		log.info("unknown arm {}", armName);
	}

	public OpenNi startOpenNI() throws Exception {
		if (openni == null) {
			openni = (OpenNi) startPeer("openni");
			openni.start3DData();
			map3d.updateKinectPosition(currentPosition(kinectName));
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

	public void stopMoving() {
		for (IMEngine engine : imData.getArms().values()) {
			engine.target = null;
		}
	}

}
