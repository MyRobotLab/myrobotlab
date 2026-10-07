package org.myrobotlab.kinematics;

import java.io.Serializable;

/**
 * A single revolute joint measured from a rigged model (VinMoov / JMonkeyEngine)
 * or from a table of default dimensions.
 *
 * <p>
 * Everything is in the sampled world frame, meters, Y-up. {@link #origin} is a
 * point on the rotation axis and {@link #axis} is the unit rotation axis, both
 * taken <em>at the pose the sample was made in</em>. {@link #angleDeg} records
 * the joint angle at that pose so a solver can express later motion as a delta.
 * </p>
 *
 * <p>
 * A positive {@link #angleDeg} is a right-hand-rule rotation about
 * {@link #axis}, which is exactly what
 * {@code Quaternion.fromAngleAxis(theta, localAxis)} does on the mesh node.
 * That makes this record a lossless description of the rig's kinematics — no
 * Denavit-Hartenberg fitting, no per-axis sign guessing.
 * </p>
 *
 * <p>
 * The {@code servo*} / {@code angle*} pairs describe the affine map the
 * simulator applies between servo degrees and mesh degrees. Sharing that map
 * with the solver is what keeps joint space aligned:
 * {@code angleDeg = m * servoDeg + b}.
 * </p>
 */
public class JointFrame implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Node / servo name, e.g. {@code i01.leftArm.omoplate}. */
  public String name;

  /** Point on the rotation axis, world meters. */
  public Point origin;

  /** Unit rotation axis, world frame. */
  public Point axis;

  /** Which local axis the simulator rotates ({@code x}, {@code y}, {@code z}). */
  public String rotationMask;

  /** Joint angle (mesh degrees) at the moment the frame was sampled. */
  public double angleDeg;

  /** Servo input degrees at {@link #servoMin} end of the simulator mapper. */
  public double servoMin = 0.0;

  public double servoMax = 180.0;

  /** Mesh degrees corresponding to {@link #servoMin}. */
  public double angleAtServoMin = 0.0;

  /** Mesh degrees corresponding to {@link #servoMax}. */
  public double angleAtServoMax = 180.0;

  public JointFrame() {
  }

  public JointFrame(String name, Point origin, Point axis) {
    this.name = name;
    this.origin = origin;
    this.axis = axis;
  }

  /**
   * Declare the servo &rarr; mesh-degree map, normally copied straight from the
   * simulator's node mapper so both sides cannot drift apart.
   */
  public JointFrame withServoMap(double servoMin, double servoMax, double angleAtServoMin, double angleAtServoMax) {
    this.servoMin = servoMin;
    this.servoMax = servoMax;
    this.angleAtServoMin = angleAtServoMin;
    this.angleAtServoMax = angleAtServoMax;
    return this;
  }

  /** Mesh degrees per servo degree. Never zero. */
  public double getAngleSlope() {
    double dx = servoMax - servoMin;
    if (Math.abs(dx) < 1e-9) {
      return 1.0;
    }
    double slope = (angleAtServoMax - angleAtServoMin) / dx;
    return Math.abs(slope) < 1e-9 ? 1.0 : slope;
  }

  /** Mesh degrees at servo zero. */
  public double getAngleIntercept() {
    return angleAtServoMin - getAngleSlope() * servoMin;
  }

  /** Servo degrees per mesh degree — {@link DHLink#getServoSlope()}. */
  public double getServoSlope() {
    return 1.0 / getAngleSlope();
  }

  /** Servo degrees at mesh angle zero — {@link DHLink#getOffset()}. */
  public double getServoOffset() {
    return -getAngleIntercept() / getAngleSlope();
  }

  @Override
  public String toString() {
    return String.format("JointFrame[%s mask=%s origin=%s axis=%s angle=%.2f servo %.1f..%.1f -> %.1f..%.1f]", name, rotationMask, origin, axis, angleDeg, servoMin, servoMax,
        angleAtServoMin, angleAtServoMax);
  }
}
