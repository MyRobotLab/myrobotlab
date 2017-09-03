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
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.openni.PImage;
import org.myrobotlab.openni.PVector;
import org.myrobotlab.openni.Skeleton;
import org.myrobotlab.service.interfaces.VideoSink;
import org.slf4j.Logger;

import SimpleOpenNI.ContextWrapper;
import SimpleOpenNI.SimpleOpenNI;
import SimpleOpenNI.SimpleOpenNIConstants;

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
public class OpenNi extends Service // implements
// UserTracker.NewFrameListener,
// HandTracker.NewFrameListener
{

	public boolean capturing = false;
	public class Worker extends Thread {
    public boolean isRunning = false;
    public String type = null;

    public Worker(String type) {
      super(String.format("%s.worker", type));
      this.type = type;
    }

    @Override
    public void run() {
      try {
        isRunning = true;
        while (isRunning) {
          if ("user".equals(type)) {
            getData();
          } else if ("hands".equals(type)) {
            drawHand();
            
          } 
          else if ("map3D".equals(type)) {
          	get3DData();
          }
          else {
            error("unknown worker %s", type);
            isRunning = false;
          }

        }

      } catch (Exception e) {
        Logging.logError(e);
      }
    }
  }

  private static final long serialVersionUID = 1L;

  public static final float PI = (float) Math.PI;

  public static final float RAD_TO_DEG = 180.0f / PI;
  boolean enableDepth = true;
  boolean enableRGB = true;

  // min max vars
  /*
   * float leftShoulderAngleYZmin = 361; float leftShoulderAngleYZmax = -361;
   * 
   * float leftShoulderAngleXYmin = 361; float leftShoulderAngleXYmax = -361;
   * 
   * float leftElbowAngleXYmin = 361; float leftElbowAngleXYmax = -361;
   */

  boolean enableIR = true;
  public final static Logger log = LoggerFactory.getLogger(OpenNi.class);

  transient SimpleOpenNI context;

  ArrayList<VideoSink> sinks = new ArrayList<VideoSink>();

  transient Graphics2D g2d;

  int frameNumber = 0;
  int handVecListSize = 20;

  HashMap<Integer, ArrayList<PVector>> handPathList = new HashMap<Integer, ArrayList<PVector>>();
  // user begin
  Color[] userClr = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.CYAN };
  PVector com = new PVector();

  PVector com2d = new PVector();

  transient BufferedImage frame;
  transient FileOutputStream csvFile = null;

  // IMPORTANT - this single skeleton contains mapping information !

  transient FileOutputStream rubySketchUpFile = null;

  public Skeleton skeleton = new Skeleton();
  private boolean initialized = false;

  transient Worker worker = null;
  private boolean recordSingleFrame = false;

  private boolean createHeader = true;

  int x1, y1, x2, y2;

  PVector joint1Pos2d = new PVector();

  PVector joint2Pos2d = new PVector();

  boolean drawSkeleton = true;

  static public final float degrees(float radians) {
    return radians * RAD_TO_DEG;
  }

  public OpenNi(String n) {
    super(n);
  }

  // USER BEGIN ---------------------------------------------

  public void add(VideoSink vs) {
    sinks.add(vs);
  }

  public void addCSVDataFrame(Skeleton skeleton, boolean singleFrame) {
    try {

      if (csvFile == null) {
        csvFile = new FileOutputStream(new File(String.format("skeleton.%d.csv", frameNumber)));
      }

      StringBuffer sb = new StringBuffer();
      if (createHeader) {
        sb.append("frame,user,head,neck,ls,lsxy,lsyz,rs,rsxy,rsyz,le,lexy,leyz,re,rexy,reyz\n");
      }

      sb.append(frameNumber).append(",");
      sb.append(skeleton.userId).append(",");
      sb.append(format(skeleton.head)).append(",");
      sb.append(format(skeleton.neck)).append(",");

      sb.append(format(skeleton.leftShoulder)).append(",");
      sb.append(Math.round(skeleton.leftShoulder.getAngleXY())).append(",");
      sb.append(Math.round(skeleton.leftShoulder.getAngleYZ())).append(",");

      sb.append(format(skeleton.rightShoulder)).append(",");
      sb.append(Math.round(skeleton.rightShoulder.getAngleXY())).append(",");
      sb.append(Math.round(skeleton.rightShoulder.getAngleYZ())).append(",");

      sb.append(format(skeleton.leftElbow)).append(",");
      sb.append(Math.round(skeleton.leftElbow.getAngleXY())).append(",");
      sb.append(Math.round(skeleton.leftElbow.getAngleYZ())).append(",");

      sb.append(format(skeleton.rightElbow)).append(",");
      sb.append(Math.round(skeleton.rightElbow.getAngleXY())).append(",");
      sb.append(Math.round(skeleton.rightElbow.getAngleYZ())).append("\n");

      csvFile.write(sb.toString().getBytes());

      if (singleFrame) {
        csvFile.close();
        csvFile = null;
        // recordCSVData = false;
        createHeader = true;
      } else {
        createHeader = false;
      }

    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public void addOpenNIData(Service service) {
    addListener("publishOpenNIData", service.getName(), "onOpenNIData");
  }

  public void addRubySketchUpFrame(Skeleton skeleton, boolean singleFrame) {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append(String.format(String.format("\n#-----------------------frame %d begin----------------------\n", frameNumber)));
      sb.append(String.format("head = [%f,%f,%f]\n", skeleton.head.x, skeleton.head.z, skeleton.head.y));
      sb.append(String.format("neck = [%f,%f,%f]\n", skeleton.neck.x, skeleton.neck.z, skeleton.neck.y));

      sb.append(String.format("leftShoulder = [%f,%f,%f]\n", skeleton.leftShoulder.x, skeleton.leftShoulder.z, skeleton.leftShoulder.y));
      sb.append(String.format("leftElbow = [%f,%f,%f]\n", skeleton.leftElbow.x, skeleton.leftElbow.z, skeleton.leftElbow.y));
      sb.append(String.format("leftHand = [%f,%f,%f]\n", skeleton.leftHand.x, skeleton.leftHand.z, skeleton.leftHand.y));

      sb.append(String.format("rightShoulder = [%f,%f,%f]\n", skeleton.rightShoulder.x, skeleton.rightShoulder.z, skeleton.rightShoulder.y));
      sb.append(String.format("rightElbow = [%f,%f,%f]\n", skeleton.rightElbow.x, skeleton.rightElbow.z, skeleton.rightElbow.y));
      sb.append(String.format("rightHand = [%f,%f,%f]\n", skeleton.rightHand.x, skeleton.rightHand.z, skeleton.rightHand.y));

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

      sb.append(String.format(String.format("\n#-----------------------frame %d begin----------------------\n", frameNumber)));

      if (rubySketchUpFile == null) {
        String filename = String.format("skeleton.%d.rb", skeleton.frameNumber);
        rubySketchUpFile = new FileOutputStream(new File(filename));
      }

      rubySketchUpFile.write(sb.toString().getBytes());

      if (singleFrame) {
        rubySketchUpFile.close();
        rubySketchUpFile = null;
        // recordRubySketchUp = false;
      }
    } catch (Exception e) {
      Logging.logError(e);
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
    // return degrees(PVector.aSinangleBetween(limb, axis));
  }

  public void capture() {
    startUserTracking();
  }

  public void closeRubySketchUpFile() {
    try {
      if (rubySketchUpFile != null) {
        rubySketchUpFile.close();
      }
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      rubySketchUpFile = null;
    }
  }

  public void createPath(String path) {
    log.info("createPath");
  }

  public String dataPath(String recordPath) {
    log.info("dataPath");
    return null;
  }

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
        // int handId = (Integer) mapEntry.getKey();
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
  }

  public boolean enableDepth(boolean b) {
    enableDepth = b;
    if (enableDepth) {
      context.enableDepth();
    }
    return b;
  }

  public boolean enableIR(boolean b) {
    enableIR = b;
    if (enableIR) {
      context.enableIR();
    }
    return b;
  }

  public boolean enableRGB(boolean b) {
    enableRGB = b;
    if (enableRGB) {
      context.enableRGB();
    }
    return b;
  }

  // FIXME - divide into parts - computer skeleton
  // FIXME - "draw"/graphics should be in OpenNIGUI !!!
  // FIXME - remove drawSkeleton
  // draw the skeleton with the selected joints
  void extractSkeleton(int userId) {

    skeleton.userId = userId;

    PVector jointPos = new PVector();
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_NECK, jointPos);

    // 3D matrix 4x4
    // context.getJointOrientationSkeleton(userId, joint, jointOrientation);

    // ------- skeleton data build begin-------
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_HEAD, skeleton.head);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_NECK, skeleton.neck);

    // left & right arms
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_LEFT_SHOULDER, skeleton.leftShoulder);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_LEFT_ELBOW, skeleton.leftElbow);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_LEFT_HAND, skeleton.leftHand);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_RIGHT_SHOULDER, skeleton.rightShoulder);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_RIGHT_ELBOW, skeleton.rightElbow);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_RIGHT_HAND, skeleton.rightHand);

    // torso
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_TORSO, skeleton.torso);

    // right and left leg
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_LEFT_HIP, skeleton.leftHip);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_LEFT_KNEE, skeleton.leftKnee);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_LEFT_FOOT, skeleton.leftFoot);

    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_RIGHT_HIP, skeleton.rightHip);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_RIGHT_KNEE, skeleton.rightKnee);
    context.getJointPositionSkeleton(userId, SimpleOpenNIConstants.SKEL_RIGHT_FOOT, skeleton.rightFoot);
    // ------- skeleton data build end -------

    // begin angular decomposition & projections

    /**
     * initially started from "Making Things See" a excellent book and I
     * recommend buying it http://shop.oreilly.com/product/0636920020684.do
     */

    // reduce our joint vectors to two dimensions
    PVector rightHandXY = new PVector(skeleton.rightHand.x, skeleton.rightHand.y);
    PVector rightElbowXY = new PVector(skeleton.rightElbow.x, skeleton.rightElbow.y);
    PVector rightShoulderXY = new PVector(skeleton.rightShoulder.x, skeleton.rightShoulder.y);

    PVector rightElbowYZ = new PVector(skeleton.rightElbow.y, skeleton.rightElbow.z);
    PVector rightShoulderYZ = new PVector(skeleton.rightShoulder.y, skeleton.rightShoulder.z);

    PVector rightHipXY = new PVector(skeleton.rightHip.x, skeleton.rightHip.y);

    PVector leftHandXY = new PVector(skeleton.leftHand.x, skeleton.leftHand.y);
    PVector leftElbowXY = new PVector(skeleton.leftElbow.x, skeleton.leftElbow.y);
    PVector leftShoulderXY = new PVector(skeleton.leftShoulder.x, skeleton.leftShoulder.y);

    PVector leftElbowYZ = new PVector(skeleton.leftElbow.y, skeleton.leftElbow.z);
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
    // PVector leftTorsoOrientationYZ = PVector.sub(leftShoulderXY,
    // leftHipXY);
    // PVector rightTorsoOrientationYZ = PVector.sub(rightShoulderXY,
    // rightHipXY);

    // calculate the angles between our joints
    float rightShoulderAngleXY = angleOf(rightElbowXY, rightShoulderXY, rightTorsoOrientationXY);
    float rightElbowAngleXY = angleOf(rightHandXY, rightElbowXY, rightUpperArmOrientationXY);

    float leftShoulderAngleXY = angleOf(leftElbowXY, leftShoulderXY, leftTorsoOrientationXY);
    float leftElbowAngleXY = angleOf(leftHandXY, leftElbowXY, leftUpperArmOrientationXY);

    float rightShoulderAngleYZ = angleOf(rightElbowYZ, rightShoulderYZ, rightTorsoOrientationXY);
    float leftShoulderAngleYZ = angleOf(leftElbowYZ, leftShoulderYZ, leftTorsoOrientationXY);

    skeleton.rightShoulder.setAngleXY(rightShoulderAngleXY);
    skeleton.rightElbow.setAngleXY(rightElbowAngleXY);
    skeleton.rightShoulder.setAngleYZ(rightShoulderAngleYZ);

    skeleton.leftShoulder.setAngleXY(leftShoulderAngleXY);
    skeleton.leftElbow.setAngleXY(leftElbowAngleXY);
    skeleton.leftShoulder.setAngleYZ(leftShoulderAngleYZ);

    /*
     * leftShoulderAngleYZmin = (leftShoulderAngleYZ < leftShoulderAngleYZmin) ?
     * leftShoulderAngleYZ : leftShoulderAngleYZmin; leftShoulderAngleYZmax =
     * (leftShoulderAngleYZ > leftShoulderAngleYZmax) ? leftShoulderAngleYZ :
     * leftShoulderAngleYZmax;
     * 
     * leftShoulderAngleXYmin = (leftShoulderAngleXY < leftShoulderAngleXYmin) ?
     * leftShoulderAngleXY : leftShoulderAngleXYmin; leftShoulderAngleXYmax =
     * (leftShoulderAngleXY > leftShoulderAngleXYmax) ? leftShoulderAngleXY :
     * leftShoulderAngleXYmax;
     * 
     * leftElbowAngleXYmin = (leftElbowAngleXY < leftElbowAngleXYmin) ?
     * leftElbowAngleXY : leftElbowAngleXYmin; leftElbowAngleXYmax =
     * (leftElbowAngleXY > leftElbowAngleXYmax) ? leftElbowAngleXY :
     * leftElbowAngleXYmax;
     */

    /*
     * g2d.drawString(String.format("shoulder min %d max %d %d %d",
     * Math.round(rightShoulderAngleYZ), Math.round(leftShoulderAngleYZ)), 20,
     * 30); g2d.drawString(String.format("omoplate min %d max %d %d %d",
     * Math.round(rightShoulderAngleXY), Math.round(leftShoulderAngleXY)), 20,
     * 40); g2d.drawString(String.format("bicep min %d max %d %d %d",
     * Math.round(rightElbowAngleXY), Math.round(leftElbowAngleXY)), 20, 50);
     */
    /*
     * g2d.drawString(String.format("shoulder min %d max %d cur %d",
     * Math.round(leftShoulderAngleYZmin), Math.round(leftShoulderAngleYZmax),
     * Math.round(leftShoulderAngleYZ)), 20, 30); g2d.drawString(String.format(
     * "omoplate min %d max %d cur %d", Math.round(leftShoulderAngleXYmin),
     * Math.round(leftShoulderAngleXYmax), Math.round(leftShoulderAngleXY)), 20,
     * 40); g2d.drawString(String.format("bicep min %d max %d cur %d",
     * Math.round(leftElbowAngleXYmin), Math.round(leftElbowAngleXYmax),
     * Math.round(leftElbowAngleXY)), 20, 50);
     */

    // invoke("publish", skeleton);

    // context.drawLimb(userId, SimpleOpenNI.SKEL_HEAD,
    // SimpleOpenNI.SKEL_NECK);

    // context.drawLimb(userId, SimpleOpenNI.SKEL_NECK,
    // SimpleOpenNI.SKEL_LEFT_SHOULDER);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_LEFT_SHOULDER, SimpleOpenNIConstants.SKEL_LEFT_ELBOW);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_LEFT_ELBOW, SimpleOpenNIConstants.SKEL_LEFT_HAND);

    // context.drawLimb(userId, SimpleOpenNI.SKEL_NECK,
    // SimpleOpenNI.SKEL_RIGHT_SHOULDER);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_RIGHT_SHOULDER, SimpleOpenNIConstants.SKEL_RIGHT_ELBOW);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_RIGHT_ELBOW, SimpleOpenNIConstants.SKEL_RIGHT_HAND);

    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_LEFT_SHOULDER, SimpleOpenNIConstants.SKEL_TORSO);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_RIGHT_SHOULDER, SimpleOpenNIConstants.SKEL_TORSO);

    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_TORSO, SimpleOpenNIConstants.SKEL_LEFT_HIP);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_LEFT_HIP, SimpleOpenNIConstants.SKEL_LEFT_KNEE);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_LEFT_KNEE, SimpleOpenNIConstants.SKEL_LEFT_FOOT);

    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_TORSO, SimpleOpenNIConstants.SKEL_RIGHT_HIP);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_RIGHT_HIP, SimpleOpenNIConstants.SKEL_RIGHT_KNEE);
    context.drawLimb(userId, SimpleOpenNIConstants.SKEL_RIGHT_KNEE, SimpleOpenNIConstants.SKEL_RIGHT_FOOT);

    if (drawSkeleton) {

      // --------------------------------------------------------------
      context.convertRealWorldToProjective(skeleton.head, joint1Pos2d);
      context.convertRealWorldToProjective(skeleton.neck, joint2Pos2d);
      x1 = Math.round(joint1Pos2d.x);
      y1 = Math.round(joint1Pos2d.y);
      x2 = Math.round(joint2Pos2d.x);
      y2 = Math.round(joint2Pos2d.y);

      g2d.drawString("head", x1, y1);
      g2d.drawString("neck", x2, y2);
      g2d.drawLine(x1, y1, x2, y2);

      line(joint1Pos2d.x, joint1Pos2d.y, joint2Pos2d.x, joint2Pos2d.y);
      // --------------------------------------------------------------
      context.convertRealWorldToProjective(skeleton.neck, joint1Pos2d);
      context.convertRealWorldToProjective(skeleton.leftShoulder, joint2Pos2d);
      x1 = Math.round(joint1Pos2d.x);
      y1 = Math.round(joint1Pos2d.y);
      x2 = Math.round(joint2Pos2d.x);
      y2 = Math.round(joint2Pos2d.y);

      g2d.drawString(String.format("lsh xy %d yz %d", Math.round(skeleton.leftShoulder.getAngleXY()), Math.round(skeleton.leftShoulder.getAngleYZ())), x2, y2);
      g2d.drawLine(x1, y1, x2, y2);

      line(joint1Pos2d.x, joint1Pos2d.y, joint2Pos2d.x, joint2Pos2d.y);
      // --------------------------------------------------------------
      context.convertRealWorldToProjective(skeleton.neck, joint1Pos2d);
      context.convertRealWorldToProjective(skeleton.rightShoulder, joint2Pos2d);
      x1 = Math.round(joint1Pos2d.x);
      y1 = Math.round(joint1Pos2d.y);
      x2 = Math.round(joint2Pos2d.x);
      y2 = Math.round(joint2Pos2d.y);

      g2d.drawString(String.format("rsh xy %d yz %d", Math.round(skeleton.rightShoulder.getAngleXY()), Math.round(skeleton.rightShoulder.getAngleYZ())), x2, y2);
      g2d.drawLine(x1, y1, x2, y2);

      line(joint1Pos2d.x, joint1Pos2d.y, joint2Pos2d.x, joint2Pos2d.y);

      // --------------------------------------------------------------
      context.convertRealWorldToProjective(skeleton.rightShoulder, joint1Pos2d);
      context.convertRealWorldToProjective(skeleton.rightElbow, joint2Pos2d);
      x1 = Math.round(joint1Pos2d.x);
      y1 = Math.round(joint1Pos2d.y);
      x2 = Math.round(joint2Pos2d.x);
      y2 = Math.round(joint2Pos2d.y);

      g2d.drawString(String.format("re xy %d", Math.round(skeleton.rightElbow.getAngleXY())), x2, y2);
      g2d.drawLine(x1, y1, x2, y2);

      line(joint1Pos2d.x, joint1Pos2d.y, joint2Pos2d.x, joint2Pos2d.y);

      // --------------------------------------------------------------
      context.convertRealWorldToProjective(skeleton.leftShoulder, joint1Pos2d);
      context.convertRealWorldToProjective(skeleton.leftElbow, joint2Pos2d);
      x1 = Math.round(joint1Pos2d.x);
      y1 = Math.round(joint1Pos2d.y);
      x2 = Math.round(joint2Pos2d.x);
      y2 = Math.round(joint2Pos2d.y);

      g2d.drawString(String.format("le xy %d", Math.round(skeleton.leftElbow.getAngleXY())), x2, y2);
      g2d.drawLine(x1, y1, x2, y2);

      line(joint1Pos2d.x, joint1Pos2d.y, joint2Pos2d.x, joint2Pos2d.y);

      g2d.dispose();

    }

    if (recordSingleFrame) {
      addCSVDataFrame(skeleton, recordSingleFrame);
      addRubySketchUpFrame(skeleton, recordSingleFrame);
      SerializableImage.writeToFile(frame, String.format("skeleton.%d.png", frameNumber));
      recordSingleFrame = false;
    }

  }

  // FIXME - too many methods !!!
  public String format(PVector v) {
    return String.format("%d %d %d", Math.round(v.x), Math.round(v.y), Math.round(v.z));
  }
  
  public OpenNiData get3DData() {
  	OpenNiData data = new OpenNiData();
  	context.update();
  	data.depthPImage = context.depthImage();
  	data.depthMapRW = context.depthMapRealWorld();
    data.depth = data.depthPImage.getImage();
    frame = data.depth;
    ++frameNumber;
    g2d = frame.createGraphics();
    invoke("publishOpenNIData", data);
    return data;
  }

  void getData() {

    // a new container is used to preserved references in
    // a multi-threaded environment
    OpenNiData data = new OpenNiData();

    // update the camera
    context.update();
    // FIXME - is PImage a faster data mech to get into OpenCV?
    data.depthPImage = context.depthImage();
    // This is the full depth map as an array , containing millimeters.
    // we should be able to use this to compute the depth for each pixel in
    // the RGB image.
    data.depthMap = context.depthMap();
    //data.depthMapRW = context.depthMapRealWorld();

    if (enableRGB) {
      data.rbgPImage = context.rgbImage();
    }

    // FIXME REMOVE - and just like OpenCV - convert and cache only on a
    // getBufferedImage !!!
    data.depth = data.depthPImage.getImage();
    frame = data.depth;

    // can not be new skeleton - as it contains mapping data
    data.skeleton = skeleton;

    ++frameNumber;
    data.frameNumber = frameNumber;
    skeleton.frameNumber = frameNumber;

    // FIXME REMOVE
    g2d = frame.createGraphics();
    g2d.setColor(Color.RED);

    // draw the skeleton if it's available
    int[] userList = context.getUsers();
    for (int i = 0; i < userList.length; i++) {
      if (context.isTrackingSkeleton(userList[i])) {
        // stroke(userClr[(userList[i] - 1) % userClr.length]);
        int userID = userList[i];
        if (userID == 1) {
          extractSkeleton(userID);
        }
      }

      // draw the center of mass
      if (context.getCoM(userList[i], com)) {
        context.convertRealWorldToProjective(com, com2d);
        data.skeleton.centerOfMass = com;
        Integer.toString(userList[i]);
      }
    }

    invoke("publishOpenNIData", data);

  }

  public void initContext() {

    if (!initialized) {
      SimpleOpenNI.start();

      ContextWrapper.initContext();
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

  public void line(Float x, Float y, Float x2, Float y2) {
    g2d.drawLine(Math.round(x), Math.round(y), Math.round(x2), Math.round(y2));
  }

  public void line(Float x, Float y, Float x2, Float y2, int user, int joint1, int joint2) {
    if (joint1 == SimpleOpenNIConstants.SKEL_HEAD) {
      g2d.drawString("head", x, y);
    }
    g2d.drawLine(Math.round(x), Math.round(y), Math.round(x2), Math.round(y2));
  }

  public void onCompletedGesture(SimpleOpenNI curContext, int gestureType, PVector pos) {
    log.info("onCompletedGesture - gestureType: " + gestureType + ", pos: " + pos);

    int handId = context.startTrackingHand(pos);
    log.info("hand stracked: " + handId);
  }

  // ----- hand begin ---------------------

  public void onLostHand(SimpleOpenNI curContext, int handId) {
    log.info("onLostHand - handId: " + handId);
    handPathList.remove(handId);
  }

  // -----------------------------------------------------------------
  // hand events

  public void onLostUser(SimpleOpenNI curContext, int userId) {
    info("onLostUser - userId: " + userId);
  }

  public void onNewHand(SimpleOpenNI curContext, int handId, PVector pos) {
    log.info("onNewHand - handId: " + handId + ", pos: " + pos);

    ArrayList<PVector> vecList = new ArrayList<PVector>();
    vecList.add(pos);

    handPathList.put(handId, vecList);
  }

  public void onNewUser(SimpleOpenNI context, int userId) {
    info("onNewUser - userId: " + userId);
    info("\tstart tracking skeleton");

    context.startTrackingSkeleton(userId);
  }

  // -----------------------------------------------------------------
  // gesture events

  public void onOutOfSceneUser(SimpleOpenNI curContext, int userId) {
    log.info("onOutOfSceneUser - userId: " + userId);

  }

  // ----- hand end -----------------------

  // -----------------------------------------------------------------
  // SimpleOpenNI events

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

  public void onVisibleUser(SimpleOpenNI curContext, int userId) {
    log.info("onVisibleUser - userId: " + userId);
  }

  public Skeleton publish(Skeleton skeleton) {
    return skeleton;
  }

  // publishing the big kahuna <output>
  public final OpenNiData publishOpenNIData(OpenNiData data) {
	if (data!=null)
		{
		capturing = true;
		}
	else
		{
			capturing = false;	
		}
    return data;
  }

  // FIXME - doesnt currently work
  public void recordSingleFrame() {
    recordSingleFrame = true;
  }

  public void registerDispose(SimpleOpenNI simpleOpenNI) {
    log.info("registerDispose");
  }

  // USER END ---------------------------------------------

  public void remove(VideoSink vs) {
    sinks.remove(vs);
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

  @Override
  public void startService() {
    super.startService();
    initContext();
  }

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
    enableDepth(true);
    // enableRGB(true);
    // enableIR(true);

    // enable skeleton generation for all joints
    context.enableUser();

    info("starting user worker");
    if (worker != null) {
      stopCapture();
    }
    worker = new Worker("user");
    worker.start();
  }

  public void start3DData() {
    if (context == null) {
      error("could not get context");
      return;
    }

    if (context.isInit() == false) {
      error("Can't init SimpleOpenNI, maybe the camera is not connected!");
      return;
    }
    enableDepth(true);
    info("starting user worker");
    if (worker != null) {
      stopCapture();
    }
    worker = new Worker("map3D");
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

  @Override
  public void stopService() {
    super.stopService();
    stopCapture();
    if (context != null) {
      context.close();
    }
  }

  public static void main(String s[]) {
    LoggingFactory.init("INFO");

    Runtime.createAndStart("gui", "SwingGui");
    Runtime.createAndStart("python", "Python");

    OpenNi openni = (OpenNi) Runtime.createAndStart("openni", "OpenNi");
    openni.startUserTracking();
    // openni.recordSingleFrame();
    // openni.startHandTracking();
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

    ServiceType meta = new ServiceType(OpenNi.class.getCanonicalName());
    meta.addDescription("OpenNI Service - 3D sensor");
    meta.addCategory("video", "vision", "sensor", "telerobotics");
    meta.sharePeer("streamer", "streamer", "VideoStreamer", "video streaming service for webgui.");
    meta.addDependency("com.googlecode.simpleopenni", "1.96");
    return meta;
  }


}
