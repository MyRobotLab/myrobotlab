package org.myrobotlab.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.openni.PImage;
import org.myrobotlab.openni.PVector;
import org.myrobotlab.openni.Skeleton;
import org.myrobotlab.service.interfaces.VideoSink;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

import SimpleOpenNI.SimpleOpenNI;

/**
 * @author GroG
 * 
 *         Service to expose the capabilities of kinect like sensors through a
 *         modified SimpleOpenNI interface
 * 
 *         References
 * 
 *         http://stackoverflow.com/questions/2676719/calculating-the-angle-
 *         between-the-line-defined-by-two-points
 *         http://stackoverflow.com/questions
 *         /9614109/how-to-calculate-an-angle-from-points
 *         http://nghiaho.com/?page_id=846
 *         https://www.youtube.com/watch?v=KKuiuctKGRQ Some snippets are taken
 *         from "Making Things See" a excellent book and I recommend buying it
 *         http://shop.oreilly.com/product/0636920020684.do
 * 
 */
public class OpenNI extends Service // implements
// UserTracker.NewFrameListener,
// HandTracker.NewFrameListener
{

	private static final long serialVersionUID = 1L;

	public static final float PI = (float) Math.PI;
	public static final float RAD_TO_DEG = 180.0f / PI;

	public final static Logger log = LoggerFactory.getLogger(OpenNI.class);
	SimpleOpenNI context;

	ArrayList<VideoSink> sinks = new ArrayList<VideoSink>();

	Graphics2D g2d;

	int cnt = 0;

	int handVecListSize = 20;
	HashMap<Integer, ArrayList<PVector>> handPathList = new HashMap<Integer, ArrayList<PVector>>();

	// user begin
	Color[] userClr = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.CYAN };
	PVector com = new PVector();
	PVector com2d = new PVector();

	BufferedImage frame;
	
	@Element
	public Skeleton skeleton = new Skeleton();

	private boolean recordRubySketchUp = false;
	private boolean initialized = false;
	transient Worker worker = null;

	public OpenNI(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public void startService() {
		super.startService();
		initContext(); // FIXME - manual or auto call to initContext ?
	}

	@Override
	public void stopService() {
		super.stopService();
		stopCapture();
		if (context != null) {
			context.close();
		}
	}

	public void initContext() {

		if (!initialized) {
			// String s = SimpleOpenNI.getLibraryPathWin();
			SimpleOpenNI.start();

			SimpleOpenNI.initContext();
			int cnt = SimpleOpenNI.deviceCount();
			info("initContext found %d devices", cnt);

			if (cnt < 1) {
				error("found 0 devices - Jenga software not initialized :P");
			}

			// fake = new PApplet(this);
			context = new SimpleOpenNI(this);
			initialized = true;
		}

	}

	/**
	 * FIXME - input needs to be OpenCVData THIS IS NOT USED ! VideoProcessor
	 * NOW DOES OpenCVData - this will return NULL REMOVE !!
	 */
	public final SerializableImage publishDisplay(SerializableImage img) {
		// lastDisplay = new SerializableImage(img, source);
		// return lastDisplay;
		return img;
	}

	public SerializableImage publishFrame(SerializableImage frame) {
		log.debug("publishing frame");
		return frame;
	}

	public void add(VideoSink vs) {
		sinks.add(vs);
	}

	public void remove(VideoSink vs) {
		sinks.remove(vs);
	}

	// USER BEGIN ---------------------------------------------

	public void startUserTracking() {
		if (context == null) {
			error("could not get context");
			return;
		}

		if (context.isInit() == false) {
			error("Can't init SimpleOpenNI, maybe the camera is not connected!");

			return;
		}

		// enable depthMap generation
		context.enableDepth();
		// context.en

		// enable skeleton generation for all joints
		context.enableUser();

		info("starting user worker");
		if (worker != null) {
			stopCapture();
		}
		worker = new Worker("user");
		worker.start();
	}

	public void startHandTracking() {

		if (context.isInit() == false) {
			error("Can't init SimpleOpenNI, maybe the camera is not connected!");
			return;
		}

		// enable depthMap generation
		context.enableDepth();

		// disable mirror
		context.setMirror(true);

		// enable hands + gesture generation
		// context.enableGesture();
		context.enableHand();
		// context.startGesture(SimpleOpenNI.GESTURE_WAVE);

		// set how smooth the hand capturing should be
		// context.setSmoothingHands(.5);

		worker = new Worker("hands");
		worker.start();
	}

	// shutdown worker
	public void stopCapture() {
		if (worker != null) {
			info(String.format("stopping worker %s", worker.type));
			worker.isRunning = false;
			worker = null;
		}
	}

	public class Worker extends Thread {
		public boolean isRunning = false;
		public String type = null;

		public Worker(String type) {
			super(String.format("%s.worker", type));
			this.type = type;
		}

		public void run() {
			try {
				isRunning = true;
				while (isRunning) {
					if ("user".equals(type)) {
						drawUser();
					} else if ("hands".equals(type)) {
						drawHand();
					} else {
						error("unknown worker %s", type);
						isRunning = false;
					}

				}

			} catch (Exception e) {
				Logging.logException(e);
			}
		}
	}

	void drawUser() {
		// update the cam
		context.update();

		// draw depthImageMap
		// image(context.depthImage(),0,0);

		// FIXME - THIS IS INCORRECT - DATA CAN BE BROADCAST BUT NO GRAPHICS !
		PImage p = context.depthImage();
		frame = p.getImage();
		++cnt;

		g2d = frame.createGraphics();
		g2d.setColor(Color.RED);

		// draw the skeleton if it's available
		int[] userList = context.getUsers();
		for (int i = 0; i < userList.length; i++) {
			if (context.isTrackingSkeleton(userList[i])) {
				// stroke(userClr[(userList[i] - 1) % userClr.length]);
				int userID = userList[i];
				if (userID == 1){
					drawSkeleton(userID);
				}
			}

			// draw the center of mass
			if (context.getCoM(userList[i], com)) {
				context.convertRealWorldToProjective(com, com2d);
				/*
				 * stroke(100, 255, 0); strokeWeight(1); beginShape(LINES);
				 * vertex(com2d.x, com2d.y - 5); vertex(com2d.x, com2d.y + 5);
				 * 
				 * vertex(com2d.x - 5, com2d.y); vertex(com2d.x + 5, com2d.y);
				 * endShape();
				 * 
				 * 
				 * fill(0, 255, 100); text(Integer.toString(userList[i]),
				 * com2d.x, com2d.y);
				 */

				Integer.toString(userList[i]);
			}
		}

		invoke("publishFrame", new SerializableImage(frame, getName()));

	}

	public boolean recordRubySketchUp(boolean b) {
		recordRubySketchUp = b;
		return recordRubySketchUp;
	}

	public Skeleton publish(Skeleton skeleton) {
		return skeleton;
	}

	// draw the skeleton with the selected joints
	void drawSkeleton(int userId) {
		
		// to get the 3d joint data
		/*
		 * PVector jointPos = new PVector();
		 * context.getJointPositionSkeleton(userId
		 * ,SimpleOpenNI.SKEL_NECK,jointPos); println(jointPos);
		 */

		// FIXME - shouldn't have to new it up each frame - is a waste
		// skeleton = new Skeleton();

		PVector jointPos = new PVector();
		context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_NECK, jointPos);
		// println(jointPos);
		// log.info("jointPos skeleton neck {} ", jointPos);

		// 3D matrix 4x4
		// context.getJointOrientationSkeleton(userId, joint, jointOrientation);

		// ------- skeleton data build begin-------
		float quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_HEAD, skeleton.head);
		skeleton.head.quality = quality;

		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_NECK, skeleton.neck);
		skeleton.neck.quality = quality;

		// left & right arms
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, skeleton.leftShoulder);
		skeleton.leftShoulder.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, skeleton.leftElbow);
		skeleton.leftElbow.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, skeleton.leftHand);
		skeleton.leftHand.quality = quality;

		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, skeleton.rightShoulder);
		skeleton.rightShoulder.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, skeleton.rightElbow);
		skeleton.rightElbow.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HAND, skeleton.rightHand);
		skeleton.rightHand.quality = quality;

		// torso
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_TORSO, skeleton.torso);
		skeleton.torso.quality = quality;

		// right and left leg
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HIP, skeleton.leftHip);
		skeleton.leftHip.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_KNEE, skeleton.leftKnee);
		skeleton.leftKnee.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_FOOT, skeleton.leftFoot);
		skeleton.leftFoot.quality = quality;

		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HIP, skeleton.rightHip);
		skeleton.rightHip.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, skeleton.rightKnee);
		skeleton.rightKnee.quality = quality;
		quality = context.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_FOOT, skeleton.rightFoot);
		skeleton.rightFoot.quality = quality;
		// ------- skeleton data build end -------

		// log.info(sb.toString());

		// float quality = getJointPositionSkeleton(userId,
		// SimpleOpenNI.SKEL_HEAD, joint2Pos);

		context.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);

		context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);

		context.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);

		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);

		context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
		context.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);

		context.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
		context.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);

		// begin angular decomposition

		/**
		 * Taken from "Making Things See" a excellent book and I recommend
		 * buying it http://shop.oreilly.com/product/0636920020684.do
		 */

		// reduce our joint vectors to two dimensions
		PVector rightHandXY = new PVector(skeleton.rightHand.x, skeleton.rightHand.y);
		PVector rightElbowXY = new PVector(skeleton.rightElbow.x, skeleton.rightElbow.y);
		PVector rightElbowYZ = new PVector(skeleton.rightElbow.y, skeleton.rightElbow.z);
		PVector rightShoulderXY = new PVector(skeleton.rightShoulder.x, skeleton.rightShoulder.y);
		PVector rightShoulderYZ = new PVector(skeleton.rightShoulder.y, skeleton.rightShoulder.z);
		PVector rightHipXY = new PVector(skeleton.rightHip.x, skeleton.rightHip.y);

		PVector leftHandXY = new PVector(skeleton.leftHand.x, skeleton.leftHand.y);
		PVector leftElbowXY = new PVector(skeleton.leftElbow.x, skeleton.leftElbow.y);
		PVector leftElbowYZ = new PVector(skeleton.leftElbow.y, skeleton.leftElbow.z);
		PVector leftShoulderXY = new PVector(skeleton.leftShoulder.x, skeleton.leftShoulder.y);
		PVector leftShoulderYZ = new PVector(skeleton.leftShoulder.y, skeleton.leftShoulder.z);
		PVector leftHipXY = new PVector(skeleton.leftHip.x, skeleton.leftHip.y);

		// calculate the axis against which we want to measure our angles
		// dunno if this needs all the defintion it has :P - normal of the
		// "person" is pretty much XY
		PVector rightTorsoOrientationXY = PVector.sub(rightShoulderXY, rightHipXY);
		PVector rightUpperArmOrientationXY = PVector.sub(rightElbowXY, rightShoulderXY);

		PVector leftTorsoOrientationXY = PVector.sub(leftShoulderXY, leftHipXY);
		PVector leftUpperArmOrientationXY = PVector.sub(leftElbowXY, leftShoulderXY);

		// FIXME !! - IS THIS CORRECT - CAN XY JUST BE RE-USED - SINCE THE
		// NORMAL OF THE BODY IS IN THE Z ?
		PVector leftTorsoOrientationYZ = PVector.sub(leftShoulderXY, leftHipXY);
		PVector rightTorsoOrientationYZ = PVector.sub(rightShoulderXY, rightHipXY);
		
		// calculate the angles between our joints
		float rightShoulderAngleXY = angleOf(rightElbowXY, rightShoulderXY, rightTorsoOrientationXY);
		float rightShoulderAngleYZ = angleOf(rightElbowYZ, rightShoulderYZ, rightTorsoOrientationYZ);
		float rightElbowAngleXY = angleOf(rightHandXY, rightElbowXY, rightUpperArmOrientationXY);

		float leftShoulderAngleXY = angleOf(leftElbowXY, leftShoulderXY, leftTorsoOrientationXY);
		float leftShoulderAngleYZ = angleOf(leftElbowYZ, leftShoulderYZ, leftTorsoOrientationYZ);
		float leftElbowAngleXY = angleOf(leftHandXY, leftElbowXY, leftUpperArmOrientationXY);

		skeleton.rightShoulder.setAngleXY(rightShoulderAngleXY);
		skeleton.rightElbow.setAngleXY(rightElbowAngleXY);
		skeleton.rightShoulder.setAngleYZ(rightShoulderAngleYZ);
		
		skeleton.leftShoulder.setAngleXY(leftShoulderAngleXY);
		skeleton.leftElbow.setAngleXY(leftElbowAngleXY);
		skeleton.leftShoulder.setAngleYZ(leftShoulderAngleYZ);

		g2d.drawString(String.format("shoulder %d %d", Math.round(leftShoulderAngleYZ), Math.round(rightShoulderAngleYZ)), 20, 30);
		g2d.drawString(String.format("omoplate %d %d", Math.round(rightShoulderAngleXY), Math.round(leftShoulderAngleXY)), 20, 40);
		g2d.drawString(String.format("bicep %d %d", Math.round(rightElbowAngleXY), Math.round(leftElbowAngleXY)), 20, 50);
		
		invoke("publish", skeleton);

		if (recordRubySketchUp) {
			addRubySketchUpFrame(skeleton);
		}

	}

	FileOutputStream rubySketchUpFile = null;

	public void openRubySketchUpFile() {
		try {
			if (rubySketchUpFile != null) {
				rubySketchUpFile.close();
			}
			String filename = String.format("skeleton_%d.rb", System.currentTimeMillis());
			rubySketchUpFile = new FileOutputStream(new File(filename));
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void addRubySketchUpFrame(Skeleton skeleton) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format(String.format("\n#-----------------------frame %d begin----------------------\n", cnt)));
		sb.append(String.format("head = [%f,%f,%f]\n", skeleton.head.x, skeleton.head.z, skeleton.head.y));
		sb.append(String.format("neck = [%f,%f,%f]\n", skeleton.neck.x, skeleton.neck.z, skeleton.neck.y));

		sb.append(String.format("leftShoulder = [%f,%f,%f]\n", skeleton.leftShoulder.x, skeleton.leftShoulder.z, skeleton.leftShoulder.y));
		sb.append(String.format("leftElbow = [%f,%f,%f]\n", skeleton.leftElbow.x, skeleton.leftElbow.z, skeleton.leftElbow.y));
		sb.append(String.format("leftHand = [%f,%f,%f]\n", skeleton.leftHand.x, skeleton.head.z, skeleton.head.y));

		sb.append(String.format("rightShoulder = [%f,%f,%f]\n", skeleton.rightShoulder.x, skeleton.rightShoulder.z, skeleton.rightShoulder.y));
		sb.append(String.format("rightElbow = [%f,%f,%f]\n", skeleton.rightElbow.x, skeleton.rightElbow.z, skeleton.rightElbow.y));
		sb.append(String.format("rightHand = [%f,%f,%f]\n", skeleton.rightHand.x, skeleton.head.z, skeleton.head.y));

		sb.append(String.format("torso = [%f,%f,%f]\n", skeleton.torso.x, skeleton.torso.z, skeleton.torso.y));

		sb.append(String.format("leftHip = [%f,%f,%f]\n", skeleton.leftHip.x, skeleton.leftHip.z, skeleton.leftHip.y));
		sb.append(String.format("leftKnee = [%f,%f,%f]\n", skeleton.leftKnee.x, skeleton.leftKnee.z, skeleton.leftKnee.y));
		sb.append(String.format("leftFoot = [%f,%f,%f]\n", skeleton.leftFoot.x, skeleton.leftFoot.z, skeleton.leftFoot.y));

		sb.append(String.format("rightHip = [%f,%f,%f]\n", skeleton.rightHip.x, skeleton.rightHip.z, skeleton.rightHip.y));
		sb.append(String.format("rightKnee = [%f,%f,%f]\n", skeleton.rightKnee.x, skeleton.rightKnee.z, skeleton.rightKnee.y));
		sb.append(String.format("rightFoot = [%f,%f,%f]\n", skeleton.rightFoot.x, skeleton.rightFoot.z, skeleton.rightFoot.y));

		sb.append("model = Sketchup.active_model\n");
		sb.append("model.entities.add_line(head, neck)\n");

		sb.append("model.entities.add_line(neck, leftShoulder)\n");
		sb.append("model.entities.add_line(leftShoulder, leftElbow)\n");
		sb.append("model.entities.add_line(leftElbow, leftHand)\n");

		sb.append("model.entities.add_line(neck, rightShoulder)\n");
		sb.append("model.entities.add_line(rightShoulder, rightElbow)\n");
		sb.append("model.entities.add_line(rightElbow, rightHand)\n");

		sb.append("model.entities.add_line(torso, leftShoulder)\n");
		sb.append("model.entities.add_line(torso, rightShoulder)\n");

		sb.append("model.entities.add_line(torso, leftHip)\n");
		sb.append("model.entities.add_line(leftHip, leftKnee)\n");
		sb.append("model.entities.add_line(leftKnee, leftFoot)\n");

		sb.append("model.entities.add_line(torso, rightHip)\n");
		sb.append("model.entities.add_line(rightHip, rightKnee)\n");
		sb.append("model.entities.add_line(rightKnee, rightFoot)\n");

		sb.append(String.format(String.format("\n#-----------------------frame %d begin----------------------\n", cnt)));

		if (rubySketchUpFile == null) {
			openRubySketchUpFile();
		}

		try {
			rubySketchUpFile.write(sb.toString().getBytes());
		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	public void closeRubySketchUpFile() {
		try {
			if (rubySketchUpFile != null) {
				rubySketchUpFile.close();
			}
		} catch (Exception e) {
			Logging.logException(e);
		} finally {
			rubySketchUpFile = null;
		}
	}

	/**
	 * Taken from "Making Things See" a excellent book and I recommend buying it
	 * http://shop.oreilly.com/product/0636920020684.do
	 * 
	 * @param one
	 * @param two
	 * @param axis
	 * @return the resultant angle in degrees
	 */
	float angleOf(PVector one, PVector two, PVector axis) {
		PVector limb = PVector.sub(two, one);
		return degrees(PVector.angleBetween(limb, axis));
	}

	static public final float degrees(float radians) {
		return radians * RAD_TO_DEG;
	}

	// ----- hand begin ---------------------

	public void drawHand() {
		// update the cam
		context.update();

		PImage image = context.depthImage();
		frame = image.getImage();
		// image(context.depthImage(),0,0);
		g2d = frame.createGraphics();
		g2d.setColor(Color.RED);

		// draw the tracked hands
		if (handPathList.size() > 0) {
			Iterator itr = handPathList.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry mapEntry = (Map.Entry) itr.next();
				int handId = (Integer) mapEntry.getKey();
				ArrayList<PVector> vecList = (ArrayList<PVector>) mapEntry.getValue();
				PVector p;
				PVector p2d = new PVector();

				// stroke(userClr[(handId - 1) % userClr.length]);
				// noFill();
				// strokeWeight(1);
				Iterator itrVec = vecList.iterator();
				// beginShape();
				PVector p1 = null;
				while (itrVec.hasNext()) {
					p = (PVector) itrVec.next();

					context.convertRealWorldToProjective(p, p2d);
					if (p1 != null) {
						g2d.drawLine(Math.round(p1.x), Math.round(p1.y), Math.round(p2d.x), Math.round(p2d.y));
					}

					p1 = p2d;
					// vertex(p2d.x, p2d.y);
				}
				// endShape();

				// stroke(userClr[(handId - 1) % userClr.length]);
				// strokeWeight(4);
				p = vecList.get(0);
				context.convertRealWorldToProjective(p, p2d);
				g2d.fillOval(Math.round(p2d.x), Math.round(p2d.y), 2, 2);
				// point(p2d.x, p2d.y);

			}
		}

		invoke("publishFrame", new SerializableImage(frame, getName()));
	}

	// -----------------------------------------------------------------
	// hand events

	public void onNewHand(SimpleOpenNI curContext, int handId, PVector pos) {
		log.info("onNewHand - handId: " + handId + ", pos: " + pos);

		ArrayList<PVector> vecList = new ArrayList<PVector>();
		vecList.add(pos);

		handPathList.put(handId, vecList);
	}

	public void onTrackedHand(SimpleOpenNI curContext, int handId, PVector pos) {
		// println("onTrackedHand - handId: " + handId + ", pos: " + pos );

		ArrayList<PVector> vecList = handPathList.get(handId);
		if (vecList != null) {
			vecList.add(0, pos);
			if (vecList.size() >= handVecListSize)
				// remove the last point
				vecList.remove(vecList.size() - 1);
		}
	}

	public void onLostHand(SimpleOpenNI curContext, int handId) {
		log.info("onLostHand - handId: " + handId);
		handPathList.remove(handId);
	}

	// -----------------------------------------------------------------
	// gesture events

	public void onCompletedGesture(SimpleOpenNI curContext, int gestureType, PVector pos) {
		log.info("onCompletedGesture - gestureType: " + gestureType + ", pos: " + pos);

		int handId = context.startTrackingHand(pos);
		log.info("hand stracked: " + handId);
	}

	// ----- hand end -----------------------

	// -----------------------------------------------------------------
	// SimpleOpenNI events

	public void onNewUser(SimpleOpenNI context, int userId) {
		info("onNewUser - userId: " + userId);
		info("\tstart tracking skeleton");

		context.startTrackingSkeleton(userId);
	}

	public void onLostUser(SimpleOpenNI curContext, int userId) {
		info("onLostUser - userId: " + userId);
	}

	public void onVisibleUser(SimpleOpenNI curContext, int userId) {
		log.info("onVisibleUser - userId: " + userId);
	}

	public void onOutOfSceneUser(SimpleOpenNI curContext, int userId) {
		log.info("onOutOfSceneUser - userId: " + userId);

	}

	// PApplet
	public void line(Float x, Float y, Float x2, Float y2) {
		// TODO Auto-generated method stub
		g2d.drawLine(Math.round(x), Math.round(y), Math.round(x2), Math.round(y2));
		g2d.drawString(String.format("head %d %d %d", Math.round(skeleton.head.x), Math.round(skeleton.head.y), Math.round(skeleton.head.z)), 20, 20);
		// g2d.drawString(str, Math.round(x), Math.round(y));
	}
	
	// USER END ---------------------------------------------

	public void registerDispose(SimpleOpenNI simpleOpenNI) {
		log.info("registerDispose");
	}

	public String dataPath(String recordPath) {
		log.info("dataPath");
		return null;
	}

	public void createPath(String path) {
		log.info("createPath");
	}

	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");

		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");

		OpenNI openni = (OpenNI)Runtime.createAndStart("openni", "OpenNI");
		openni.startUserTracking();
		openni.recordRubySketchUp(true);

		// openni.startHandTracking();
	}
}
