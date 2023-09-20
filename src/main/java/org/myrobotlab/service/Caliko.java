package org.myrobotlab.service;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.caliko.Application;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.CalikoConfig;
import org.slf4j.Logger;

import au.edu.federation.caliko.FabrikBone3D;
import au.edu.federation.caliko.FabrikChain3D;
import au.edu.federation.caliko.FabrikChain3D.BaseboneConstraintType3D;
import au.edu.federation.caliko.FabrikJoint3D.JointType;
import au.edu.federation.caliko.FabrikStructure3D;
import au.edu.federation.caliko.visualisation.Camera;
import au.edu.federation.utils.Colour4f;
import au.edu.federation.utils.Utils;
import au.edu.federation.utils.Vec3f;

public class Caliko extends Service<CalikoConfig> {

  private class WindowWorker extends Thread {

    Application application;
    String name;
    Caliko service;

    public WindowWorker(Caliko service, String name) {
      super(String.format("%s.WindowWorker.%s", service.getName(), name));
      this.name = name;
      this.service = service;
    }

    public void run() {
      try {
        application = new Application(service);
      } catch (Exception e) {
        error(e);
        shutdown();
      }
    }

    public void shutdown() {
      application.running = false;
      windows.remove(name);
      application.window.cleanup();
    }

  }

  public final static Logger log = LoggerFactory.getLogger(Caliko.class);

  private static final long serialVersionUID = 1L;

//  public static final Vec3f X_AXIS = new Vec3f(1.0f, 0.0f, 0.0f);
//
//  public static final Vec3f Y_AXIS = new Vec3f(0.0f, 1.0f, 0.0f);
//
//  public static final Vec3f Z_AXIS = new Vec3f(0.0f, 0.0f, 1.0f);
//
//  public static final Vec3f NEG_X_AXIS = new Vec3f(-1.0f, -0.0f, -0.0f);
//
//  public static final Vec3f NEG_Y_AXIS = new Vec3f(-0.0f, -1.0f, -0.0f);
//
//  public static final Vec3f NEG_Z_AXIS = new Vec3f(-0.0f, -0.0f, -1.0f);

  
  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // identical to command line start
      // Runtime.startConfig("inmoov2");
      Runtime.main(new String[] { "--log-level", "info", "-s", "webgui", "WebGui", "intro", "Intro", "python", "Python" });

      Caliko caliko = (Caliko) Runtime.start("caliko", "Caliko");
      caliko.test();

      boolean done = true;
      if (done)
        return;

      Runtime.start("webgui", "WebGui");
      caliko.test();
      // Runtime.start("webgui", "WebGui");
      log.info("here");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  private transient Map<String, FabrikChain3D> chains = new HashMap<>();

  private transient FabrikStructure3D structure;

  protected int windowHeight = 600;

  transient private Map<String, WindowWorker> windows = new HashMap<>();

  protected int windowWidth = 800;

  private transient Camera camera = new Camera(new Vec3f(0.0f, 00.0f, 150.0f), new Vec3f(), windowWidth, windowHeight);

  public Caliko(String n, String id) {
    super(n, id);
    structure = new FabrikStructure3D(n);
  }

  public void addBaseBone(float startX, float startY, float startZ, float endX, float endY, float endZ) {
    addChain("default");
    addBone("default", startX, startY, startZ, endX, endY, endX, "-x", 0.0f, "red");
  }

  public void addBone(String chainName, float startX, float startY, float startZ, float endX, float endY, float endZ, String axis, float constraints, String color) {
    if (!chains.containsKey(chainName)) {
      error("%s chain does not exist", chainName);
    }
    FabrikBone3D basebone = new FabrikBone3D(new Vec3f(startX, startY, startZ), new Vec3f(endX, endY, endZ));
    basebone.setColour(getColor(color));
    FabrikChain3D chain = chains.get(chainName);
    chain.addBone(basebone);
    // TODO set type
    chain.setRotorBaseboneConstraint(BaseboneConstraintType3D.GLOBAL_ROTOR, getAxis(axis), constraints);
  }

  public void addChain(String name) {
    if (chains.containsKey(name)) {
      error("chain %s already exists", name);
      return;
    }
    FabrikChain3D chain = new FabrikChain3D();
    structure.addChain(chain);
    chains.put(name, chain);
  }

  public void addFreelyRotatingHingedBone(String directionUV, float length, String hingeRotationAxis, String color) {
    addFreelyRotatingHingedBone("default", directionUV, length, hingeRotationAxis, color);
  }

  /**
   * Add a consecutive hinge constrained bone to the end of this chain. The bone
   * may rotate freely about the hinge axis.
   * <p>
   * The bone will be drawn with a default colour of white.
   * <p>
   * This method can only be used when the IK chain contains a basebone, as
   * without it we do not have a start location for this bone (i.e. the end
   * location of the previous bone).
   * <p>
   * If this method is executed on a chain which does not contain a basebone
   * then a RuntimeException is thrown. If this method is provided with a
   * direction unit vector of zero, then an IllegalArgumentException is thrown.
   * If the joint type requested is not JointType.LOCAL_HINGE or
   * JointType.GLOBAL_HINGE then an IllegalArgumentException is thrown. If this
   * method is provided with a hinge rotation axis unit vector of zero, then an
   * IllegalArgumentException is thrown.
   * 
   * @param directionUV
   *          The initial direction of the new bone.
   * @param length
   *          The length of the new bone.
   * @param jointType
   *          The type of hinge joint to be used - either JointType.LOCAL or
   *          JointType.GLOBAL.
   * @param hingeRotationAxis
   *          The axis about which the hinge joint freely rotates.
   * @param colour
   *          The colour to draw the bone.
   */
  public void addFreelyRotatingHingedBone(String chainName, String directionUV, float length, String hingeRotationAxis, String color) {
    if (!chains.containsKey(chainName)) {
      error("%s chain does not exist", chainName);
    }
    FabrikChain3D chain = chains.get(chainName);
    chain.addConsecutiveFreelyRotatingHingedBone(getAxis(directionUV), length, JointType.LOCAL_HINGE, getAxis(hingeRotationAxis), getColor(color));
  }

  public void addHingeBone(String directionUV, float length, String hingeRotationAxis, float clockwiseDegs, float anticlockwiseDegs, String hingeReferenceAxis, String color) {
    addHingeBone("default", directionUV, length, hingeRotationAxis, clockwiseDegs, anticlockwiseDegs, hingeReferenceAxis, color);
  }

  /**
   * Add a consecutive hinge constrained bone to the end of this IK chain.
   * <p>
   * The hinge type may be a global hinge where the rotation axis is specified
   * in world-space, or a local hinge, where the rotation axis is relative to
   * the previous bone in the chain.
   * <p>
   * If this method is executed on a chain which does not contain a basebone
   * then a RuntimeException is thrown. If this method is provided with bone
   * direction or hinge constraint axis of zero then an IllegalArgumentException
   * is thrown. If the joint type requested is not LOCAL_HINGE or GLOBAL_HINGE
   * then an IllegalArgumentException is thrown.
   * 
   * @param chainName The name of chain
   * @param directionUV
   *          The initial direction of the new bone.
   * @param length
   *          The length of the new bone.
   * @param jointType
   *          The joint type of the new bone.
   * @param hingeRotationAxis
   *          The axis about which the hinge rotates.
   * @param clockwiseDegs
   *          The clockwise constraint angle in degrees.
   * @param anticlockwiseDegs
   *          The anticlockwise constraint angle in degrees.
   * @param hingeReferenceAxis
   *          The axis about which any clockwise/anticlockwise rotation
   *          constraints are enforced.
   * @param colour
   *          The colour to draw the bone.
   */
  public void addHingeBone(String chainName, String directionUV, float length, String hingeRotationAxis, float clockwiseDegs, float anticlockwiseDegs, String hingeReferenceAxis,
      String color) {
    if (!chains.containsKey(chainName)) {
      error("%s chain does not exist", chainName);
    }
    FabrikChain3D chain = chains.get(chainName);
    chain.addConsecutiveHingedBone(getAxis(directionUV), length, JointType.LOCAL_HINGE, getAxis(hingeRotationAxis), clockwiseDegs, anticlockwiseDegs, getAxis(hingeReferenceAxis),
        getColor(color));
  }

  Vec3f getAxis(String axis) {
    if (axis == null) {
      return null;
    }
    axis = axis.toLowerCase();
    if ("x".equals(axis)) {
      return new Vec3f(1.0f, 0.0f, 0.0f);
    } else if ("-x".equals(axis)) {
      return new Vec3f(-1.0f, -0.0f, -0.0f);
    } else if ("y".equals(axis)) {
      return new Vec3f(0.0f, 1.0f, 0.0f);
    } else if ("-y".equals(axis)) {
      return new Vec3f(-0.0f, -1.0f, -0.0f);
    } else if ("z".equals(axis)) {
      return new Vec3f(0.0f, 0.0f, 1.0f);
    } else if ("-z".equals(axis)) {
      return new Vec3f(-0.0f, -0.0f, -1.0f);
    } else {
      error("axis %s not found", axis);
      return null;
    }
  }

  public Camera getCamera() {
    return camera;
  }

  Colour4f getColor(String color) {
    if (color == null) {
      return Utils.GREY;
    }
    color = color.toLowerCase();
    if ("red".equals(color)) {
      return Utils.RED;
    }
    if ("green".equals(color)) {
      return Utils.GREEN;
    }
    if ("blue".equals(color)) {
      return Utils.BLUE;
    }
    if ("black".equals(color)) {
      return Utils.BLACK;
    }
    if ("grey".equals(color)) {
      return Utils.GREY;
    }
    if ("white".equals(color)) {
      return Utils.WHITE;
    }
    if ("yellow".equals(color)) {
      return Utils.YELLOW;
    }
    if ("cyan".equals(color)) {
      return Utils.CYAN;
    }
    if ("magenta".equals(color)) {
      return Utils.MAGENTA;
    }
    return Utils.GREY;
  }

  public FabrikStructure3D getStructure() {
    return structure;
  }

  public void openWindow(String name) {
    if (windows.containsKey(name)) {
      info("%s already started", name);
      return;
    }
    WindowWorker worker = new WindowWorker(this, name);
    windows.put(name, worker);
    worker.start();
  }

  public void stopService() {
    super.stopService();
    for (WindowWorker worker : windows.values()) {
      worker.shutdown();
    }
  }

  public void test() {

    /* @param directionUV     The initial direction of the new bone.
    * @param hingeRotationAxis The axis about which the hinge rotates.
    * @param hingeReferenceAxis  The axis about which any clockwise/anticlockwise rotation constraints are enforced.
    */
    
    addBaseBone(0, 10, 0, -10, 10, 0);
    // addHingeBone("x", 5, "x", 70, 10, "z", "grey");
    addHingeBone("-x", 5, "x", 10, 60, "z", "grey");
    addHingeBone("-y", 20, "z", 90, 90, "-y", "red");
    addFreelyRotatingHingedBone("y", 18, "y", "grey");

    FabrikChain3D chain = chains.get("default");
    chain.solveForTarget(new Vec3f(-30, -300, -30));

    for (FabrikBone3D bone : chains.get("default").getChain()) {
      bone.getStartLocation().getGlobalPitchDegs();
      bone.getStartLocation().getGlobalYawDegs();
      Vec3f.getDirectionUV(bone.getStartLocation(), bone.getEndLocation());

      System.out.println("Bone X: " + bone.getStartLocation().toString());
    }

    openWindow("default");

    log.info("here");

    // FabrikBone3D base =new FabrikBone3D(new Vec3f(),new
    // Vec3f(0.0f,50.0f,0.0f));
    //
    // chain.addBone(base);
    // FabrikBone3D bone1 = new FabrikBone3D(new Vec3f(0.0f,50.0f,0.0f), new
    // Vec3f(0f, 0f, 100f));
    // FabrikBone3D bone2 = new FabrikBone3D(new Vec3f(0f, 0f, 100f), new
    // Vec3f(0f, 0f, 200f));

    // chain.setEff

    // for(intboneLoop =0;boneLoop
    // <5;++boneLoop){chain.addConsecutiveBone(newVec2f(0.0f,1.0f),50.0f);

    // Create a FabrikStructure3D
    // FabrikStructure3D structure = new FabrikStructure3D();
    //
    // // Create bones and joints
    // FabrikBone3D root = new FabrikBone3D(0, 0, 0);
    // FabrikBone3D bone1 = new FabrikBone3D(0, 0, 100);
    // FabrikBone3D bone2 = new FabrikBone3D(0, 0, 100);
    //
    // // Create joints (optional but can be used for constraints)
    // FabrikJoint3D joint1 = new FabrikJoint3D();
    // FabrikJoint3D joint2 = new FabrikJoint3D();
    //
    // // Add bones and joints to the structure
    // structure.addBone(root);
    // structure.addBone(bone1);
    // structure.addBone(bone2);
    //
    // // Set up the chain
    // FabrikChain3D chain = new FabrikChain3D("ChainName");
    // chain.addBone(root);
    // chain.addBone(bone1);
    // chain.addBone(bone2);
    //
    // // Set the target position for the end effector (bone2)
    // chain.setEffectorLocation(0, 0, 300);
    //
    // // Solve the IK problem
    // structure.solveForTarget(chain.getEffectorLocation());
    //
    // // Print the bone positions after solving
    // for (FabrikBone3D bone : structure.getChain("ChainName").getChain()) {
    // System.out.println("Bone X: " + bone.getStartLocation().getX() +
    // ", Y: " + bone.getStartLocation().getY() +
    // ", Z: " + bone.getStartLocation().getZ());
    // }
    //
  }

  public FabrikChain3D getChain(String name) {
    if (!chains.containsKey(name)) {
      error("no chain %s", name);
      return null;
    }
    return chains.get(name);
  }

}
