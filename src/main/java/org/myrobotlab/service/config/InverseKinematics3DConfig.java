package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Point;

/**
 * InverseKinematics3D configuration. The solver and public API are in
 * <em>meters</em>, Y-up, matching JMonkeyEngine.
 *
 * <p>
 * {@link #joints} is the real calibration: each entry is a joint's measured
 * rotation axis, a point on that axis, its angle when it was measured, and the
 * servo &rarr; mesh-degree map the simulator uses. Restoring those reproduces
 * the arm's kinematics without the simulator running.
 * </p>
 */
public class InverseKinematics3DConfig extends ServiceConfig {

  /**
   * One measured revolute joint. Flattened to primitives so the YAML stays
   * readable and diffable.
   */
  public static class JointCalibration {

    public String name;

    /** Simulator rotation mask ({@code x}, {@code y}, {@code z}) for reference. */
    public String rotationMask;

    /** A point on the rotation axis, world meters. */
    public double originX;

    public double originY;

    public double originZ;

    /** Unit rotation axis, world frame. */
    public double axisX;

    public double axisY;

    public double axisZ = 1.0;

    /** Joint angle in mesh degrees when the axis and origin were measured. */
    public double angleDeg;

    /** Servo input range, degrees. */
    public double servoMin = 0.0;

    public double servoMax = 180.0;

    /** Mesh degrees at {@link #servoMin} / {@link #servoMax}. */
    public double angleAtServoMin = 0.0;

    public double angleAtServoMax = 180.0;
  }

  /**
   * When true, {@code moveTo} / published palm are in world meters (JME root).
   * Always on for the InMoov / VinMoov path.
   */
  public boolean worldFrame = true;

  /** Measured joints, ordered parent to child. Empty means "use the defaults". */
  public List<JointCalibration> joints = new ArrayList<>();

  /** Measured end effector (wrist) in world meters at the calibration pose. */
  public double endEffectorX = 0.0;

  public double endEffectorY = 0.0;

  public double endEffectorZ = 0.0;

  public boolean endEffectorSet = false;

  /** World-frame base origin — the first measured joint. Reported, not input. */
  public double originX = 0.0;

  public double originY = 0.0;

  public double originZ = 0.0;

  /** Base rotation about Z, degrees. */
  public double originRoll = 0.0;

  /** Base rotation about X, degrees. */
  public double originPitch = 0.0;

  /** Base rotation about Y, degrees. */
  public double originYaw = 0.0;

  /**
   * @deprecated ignored. A negative axis scale reflected the chain and mirrored
   *             every solved joint angle; handedness now comes from the measured
   *             joint axes. Kept so older saved configs still load.
   */
  @Deprecated
  public double scaleX = 1.0;

  @Deprecated
  public double scaleY = 1.0;

  @Deprecated
  public double scaleZ = 1.0;

  /** Last-frame wrist / palm offset in the last link's frame (meters). */
  public double toolOffsetX = 0.0;

  public double toolOffsetY = 0.0;

  public double toolOffsetZ = 0.0;

  public boolean toolOffsetSet = false;

  /**
   * Measured omoplate&rarr;shoulder distance (meters). Reported for reference;
   * {@link #joints} is what the solver uses.
   */
  public Double omoplateA;

  /** Measured shoulder&rarr;rotate distance (meters). */
  public Double shoulderD;

  /** Measured rotate&rarr;bicep distance, i.e. the upper arm (meters). */
  public Double rotateD;

  /** Measured bicep&rarr;wrist distance, i.e. the forearm (meters). */
  public Double bicepA;

  /** Replace {@link #joints} with a freshly measured chain. */
  public void fromJointFrames(List<JointFrame> frames) {
    joints = new ArrayList<>();
    if (frames == null) {
      return;
    }
    for (JointFrame frame : frames) {
      if (frame == null || frame.origin == null || frame.axis == null) {
        continue;
      }
      JointCalibration jc = new JointCalibration();
      jc.name = frame.name;
      jc.rotationMask = frame.rotationMask;
      jc.originX = frame.origin.getX();
      jc.originY = frame.origin.getY();
      jc.originZ = frame.origin.getZ();
      jc.axisX = frame.axis.getX();
      jc.axisY = frame.axis.getY();
      jc.axisZ = frame.axis.getZ();
      jc.angleDeg = frame.angleDeg;
      jc.servoMin = frame.servoMin;
      jc.servoMax = frame.servoMax;
      jc.angleAtServoMin = frame.angleAtServoMin;
      jc.angleAtServoMax = frame.angleAtServoMax;
      joints.add(jc);
    }
  }

  /** @return the saved chain, or an empty list if nothing was calibrated */
  public List<JointFrame> toJointFrames() {
    List<JointFrame> frames = new ArrayList<>();
    if (joints == null) {
      return frames;
    }
    for (JointCalibration jc : joints) {
      if (jc == null) {
        continue;
      }
      JointFrame frame = new JointFrame(jc.name, new Point(jc.originX, jc.originY, jc.originZ), new Point(jc.axisX, jc.axisY, jc.axisZ));
      frame.rotationMask = jc.rotationMask;
      frame.angleDeg = jc.angleDeg;
      frame.withServoMap(jc.servoMin, jc.servoMax, jc.angleAtServoMin, jc.angleAtServoMax);
      frames.add(frame);
    }
    return frames;
  }
}
