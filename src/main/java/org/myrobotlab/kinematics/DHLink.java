package org.myrobotlab.kinematics;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.slf4j.Logger;

//import marytts.util.math.MathUtils;

/**
 * A link class to encapsulate the D-H parameters for a given link in a robotic
 * arm.
 * 
 * d - the "depth" along the previous joint's z axis theta - the rotation about
 * the previous z (the angle between the common normal and the previous x axis)
 * r - the radius of the new origin about the previous z (the length of the
 * common normal) alpha - the rotation about the new x axis (the common normal)
 * to align the old z to the new z.
 * 
 * @author kwatters
 *
 */
public class DHLink implements Serializable {

  private static final long serialVersionUID = 1L;
  private double d;
  private double theta;
  // rename this to a ?)
  private double r;
  private double alpha;
  private DHLinkType type;
  // -180 / +180 as min/max i guess?
  private double min = -Math.PI;
  private double max = Math.PI;
  private double initialTheta;

  // TODO: figure this out.
  private String name;

  public transient final static Logger log = LoggerFactory.getLogger(DHLink.class);

  private double velocity; // FIXME - is this set by IK being dp/dt ? .. it
  // should be
  // private int state = Servo.SERVO_EVENT_STOPPED; // FIXME - no servo info
  public double targetPos;
  public boolean hasServo = false; // FIXME - no servo info
  public double servoMin;
  public double servoMax;
  public double currentPos = 0.0;
  public double offset = 0.0;

  /**
   * Servo degrees per degree of {@link #theta}, normally +1 or −1. Together with
   * {@link #offset} this is the whole joint-space calibration:
   * {@code servoDeg = servoSlope * thetaDeg + offset}.
   *
   * <p>
   * When the link is built from a {@link JointFrame} this comes straight from the
   * simulator's node mapper, so the solver, the mesh and the physical servo
   * cannot disagree about direction.
   * </p>
   */
  private double servoSlope = 1.0;

  /**
   * Measured transform from the previous link's frame to this link's frame at
   * {@link #bindTheta}, replacing the Denavit-Hartenberg {@code d/r/alpha}
   * parameters when non-null.
   *
   * <p>
   * The frame's Z axis is the real rotation axis of the rig's joint and its
   * origin is a point on that axis, so {@code resolveMatrix()} becomes
   * {@code fixedTransform * Rz(theta - bindTheta)}. That reproduces a rigged
   * skeleton exactly, whereas DH parameters can only approximate an arbitrary
   * bone hierarchy.
   * </p>
   */
  private Matrix fixedTransform;

  /** The {@link #theta} at which {@link #fixedTransform} was measured. */
  private double bindTheta = 0.0;

  // private Matrix m;
  // TODO: add max/min angle
  public DHLink(String name, double d, double r, double theta, double alpha) {
    this(name, d, r, theta, alpha, 0);
  }

  /**
   * A revolute joint measured from a rig: the axis and origin come from
   * {@code fixedTransform}, and {@code theta} is in the same units and direction
   * as the simulator's mesh angle.
   */
  public DHLink(String name, Matrix fixedTransform, double bindTheta) {
    super();
    this.name = name;
    this.fixedTransform = fixedTransform;
    this.bindTheta = bindTheta;
    this.theta = bindTheta;
    this.initialTheta = bindTheta;
    this.type = DHLinkType.REVOLUTE;
  }

  public DHLink(String name, double d, double r, double theta, double alpha, double offset) {
    super();
    // The name of the servo that we are controlling.
    this.name = name;
    this.d = d;
    this.r = r;
    this.theta = theta;
    initialTheta = theta;
    this.alpha = alpha;
    this.offset = offset;
    //
    this.type = DHLinkType.REVOLUTE;
    // m = resolveMatrix();
  }

  public DHLink(DHLink copy) {
    super();
    this.d = copy.d;
    this.theta = copy.theta;
    this.r = copy.r;
    this.alpha = copy.alpha;
    this.type = copy.type;
    this.min = copy.min;
    this.max = copy.max;
    this.name = copy.name;
    this.initialTheta = copy.initialTheta;
    // this.state = copy.state;
    this.targetPos = copy.targetPos;
    this.velocity = copy.velocity;
    this.hasServo = copy.hasServo;
    this.servoMax = copy.servoMax;
    this.servoMin = copy.servoMin;
    this.currentPos = copy.currentPos;
    this.offset = copy.offset;
    this.servoSlope = copy.servoSlope;
    this.bindTheta = copy.bindTheta;
    this.fixedTransform = copy.fixedTransform != null ? new Matrix(copy.fixedTransform) : null;
  }

  /**
   * @return a 4x4 homogenous transformation matrix for this link at its current
   *         {@link #getTheta()}
   */
  public Matrix resolveMatrix() {
    if (fixedTransform != null) {
      return fixedTransform.multiply(Matrix.rotationZ(theta - bindTheta));
    }
    Matrix m = new Matrix(4, 4);
    // elements we need
    double cosTheta = Math.cos(theta);
    double sinTheta = Math.sin(theta);
    double cosAlpha = Math.cos(alpha);
    double sinAlpha = Math.sin(alpha);

    // cosTheta = zeroQuantize(cosTheta);
    // sinTheta = zeroQuantize(sinTheta);
    // cosAlpha= zeroQuantize(cosAlpha);
    // sinAlpha = zeroQuantize(sinAlpha);

    // // first row of homogenous xform
    // m.elements[0][0] = cosTheta;
    // m.elements[0][1] = -1 * sinTheta;
    // m.elements[0][2] = 0;
    // m.elements[0][3] = r;
    //
    // // 2nd row of homogenous xform
    // m.elements[1][0] = sinTheta * cosAlpha;
    // m.elements[1][1] = cosTheta * cosAlpha;
    // m.elements[1][2] = -1 * sinAlpha;
    // m.elements[1][3] = -1 * d * sinAlpha;
    //
    // // 3rd row of homogenous xform
    // m.elements[2][0] = sinTheta * sinAlpha;
    // m.elements[2][1] = cosTheta * sinAlpha;
    // m.elements[2][2] = cosAlpha;
    // m.elements[2][3] = d * cosAlpha;
    //
    // // 4th row of homogenous xform
    // m.elements[3][0] = 0;
    // m.elements[3][1] = 0;
    // m.elements[3][2] = 0;
    // m.elements[3][3] = 1;

    // first row of homogenous xform
    m.elements[0][0] = cosTheta;
    m.elements[0][1] = -1 * cosAlpha * sinTheta;
    m.elements[0][2] = sinAlpha * sinTheta;
    m.elements[0][3] = r * cosTheta;

    // 2nd row of homogenous xform
    m.elements[1][0] = sinTheta;
    m.elements[1][1] = cosAlpha * cosTheta;
    m.elements[1][2] = -1 * sinAlpha * cosTheta;
    m.elements[1][3] = r * sinTheta;

    // 3rd row of homogenous xform
    m.elements[2][0] = 0;
    m.elements[2][1] = sinAlpha;
    m.elements[2][2] = cosAlpha;
    m.elements[2][3] = d;

    // 4th row of homogenous xform
    m.elements[3][0] = 0;
    m.elements[3][1] = 0;
    m.elements[3][2] = 0;
    m.elements[3][3] = 1;

    return m;

  }

  public double zeroQuantize(double value) {
    // TODO: move this to a math utils class.
    double resolution = 0.000001;
    if (value < resolution && value > -resolution) {
      value = 0;
    }
    return value;
  }

  // move to an angle
  public void rotate(double angle) {
    // TODO: which parameter?
    if (DHLinkType.REVOLUTE.equals(this.type)) {
      if (angle <= max && angle >= min) {
        this.theta = angle;
      } else {
        // TODO: it's out of range!
        log.info("Rotation out of range for link {}", angle);
      }
    }
    if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      if (angle <= max && angle >= min) {
        alpha = angle;
      } else {
        // TODO: it's out of range!
        log.info("Rotation out of range for link {}", angle);
      }
    } else {
      // TODO: You can't rotate a prismatic joint!
      // TODO Throw something?
    }
  }

  public void translate(double d) {
    // TODO: which parameter?
    if (DHLinkType.PRISMATIC.equals(this.type)) {
      this.d = d;
    } else {
      // TODO: You can't translate a revolute joint!
      // TODO Throw something?
    }
  }

  public double getD() {
    return d;
  }

  public void setD(double d) {
    this.d = d;
  }

  public double getA() {
    return r;
  }

  public void setA(double a) {
    this.r = a;
  }

  public double getTheta() {
    return theta;
  }

  public void setTheta(double theta) {
    this.theta = theta;
  }

  public double getAlpha() {
    return alpha;
  }

  public void setAlpha(double alpha) {
    this.alpha = alpha;
  }

  @Override
  public String toString() {
    // print in degrees

    return "DHLink [d=" + d + ", theta=" + MathUtils.radToDeg(theta) + ", r=" + r + ", alpha=" + MathUtils.radToDeg(alpha) + " min=" + MathUtils.radToDeg(min) + " max="
        + MathUtils.radToDeg(max) + "]";
  }

  /**
   * Move the joint variable by {@code delta}, clamping at the limits.
   *
   * <p>
   * This clamps rather than rejecting. Rejecting made
   * {@code incrRotate(+d); incrRotate(-d)} asymmetric at the upper limit — the
   * {@code +d} was dropped but the {@code -d} applied — so every Jacobian probe
   * silently dragged a joint that sat at its maximum away from it. Over thousands
   * of solver iterations that drifted the model tens of degrees away from the
   * pose it was reporting.
   * </p>
   */
  public void incrRotate(double delta) {
    if (DHLinkType.REVOLUTE.equals(type)) {
      this.theta = clampToLimits(this.theta + delta);
    } else if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      alpha = clampToLimits(alpha + delta);
    }
  }

  /** Clamp a joint variable (radians) into {@code [min, max]}. */
  public double clampToLimits(double value) {
    if (value > max) {
      return max;
    }
    if (value < min) {
      return min;
    }
    return value;
  }

  /** @return true if the joint variable sits on one of its limits. */
  public boolean isAtLimit() {
    double v = DHLinkType.REVOLUTE_ALPHA.equals(type) ? alpha : theta;
    double eps = 1e-9;
    return v >= max - eps || v <= min + eps;
  }

  /**
   * Set the joint variable directly with no limit checking. Used by the Jacobian
   * probe, which must be able to restore the exact previous value and may step
   * marginally outside the range to estimate a derivative.
   */
  public void setJointVariableUnchecked(double value) {
    if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      alpha = value;
    } else {
      theta = value;
    }
  }

  /** @return the joint variable ({@link #theta}, or {@link #alpha} for alpha links). */
  public double getJointVariable() {
    return DHLinkType.REVOLUTE_ALPHA.equals(type) ? alpha : theta;
  }

  public double getServoSlope() {
    return servoSlope;
  }

  /**
   * @param servoSlope
   *          servo degrees per degree of theta — ±1 for a rig whose mesh turns
   *          one degree per servo degree. Zero is ignored.
   */
  public void setServoSlope(double servoSlope) {
    if (servoSlope == 0.0 || Double.isNaN(servoSlope)) {
      log.warn("ignoring servoSlope {} for link {}", servoSlope, name);
      return;
    }
    this.servoSlope = servoSlope;
  }

  public Matrix getFixedTransform() {
    return fixedTransform;
  }

  public void setFixedTransform(Matrix fixedTransform) {
    this.fixedTransform = fixedTransform != null ? new Matrix(fixedTransform) : null;
  }

  public double getBindTheta() {
    return bindTheta;
  }

  public void setBindTheta(double bindTheta) {
    this.bindTheta = bindTheta;
  }

  /** @return true when the link geometry was measured rather than hand-tuned. */
  public boolean isMeasured() {
    return fixedTransform != null;
  }

  /**
   * @return the servo command for the current theta:
   *         {@code servoSlope * thetaDeg + offset}, clamped to the servo range
   *         implied by the joint limits so a solved pose can never ask a servo
   *         for something it will silently clip.
   */
  public double toServoDegrees() {
    double raw = servoSlope * getThetaDegrees() + offset;
    double a = servoSlope * Math.toDegrees(min) + offset;
    double b = servoSlope * Math.toDegrees(max) + offset;
    double lo = Math.min(a, b);
    double hi = Math.max(a, b);
    return Math.max(lo, Math.min(hi, raw));
  }

  /** Inverse of {@link #toServoDegrees()} — read a servo position into the model. */
  public void setFromServoDegrees(double servoDeg) {
    this.theta = Math.toRadians((servoDeg - offset) / servoSlope);
  }

  /**
   * Set the joint limits from the servo's input range, mapped through
   * {@link #getServoSlope()} / {@link #getOffset()}.
   */
  public void setServoLimits(double servoMinDeg, double servoMaxDeg) {
    double a = (servoMinDeg - offset) / servoSlope;
    double b = (servoMaxDeg - offset) / servoSlope;
    this.min = Math.toRadians(Math.min(a, b));
    this.max = Math.toRadians(Math.max(a, b));
    this.servoMin = Math.min(servoMinDeg, servoMaxDeg);
    this.servoMax = Math.max(servoMinDeg, servoMaxDeg);
  }

  public double getThetaDegrees() {
    return this.theta * 180 / Math.PI;
  }

  public double getMin() {
    return min;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public double getMax() {
    return max;
  }

  public void setMax(double max) {
    this.max = max;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addPositionValue(double positionDeg) {
    if (DHLinkType.REVOLUTE.equals(type)) {
      theta = initialTheta + MathUtils.degToRad(positionDeg);
    } else if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      alpha = initialTheta + MathUtils.degToRad(positionDeg);
    }
  }

  public double getInitialTheta() {
    return initialTheta;
  }

  public Double getPositionValueDeg() {
    if (DHLinkType.REVOLUTE.equals(type)) {
      return (theta * 180 / Math.PI) - (initialTheta * 180 / Math.PI);
    } else if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      return (alpha * 180 / Math.PI) - (initialTheta * 180 / Math.PI);
    }
    return 0.0;
  }

  public double getVelocity() {
    return velocity;
  }

  public void setVelocity(double velocity) {
    this.velocity = velocity;
  }

  /**
   * @return the targetPos
   */
  public Double getTargetPos() {
    return targetPos;
  }

  /**
   * @param targetPos2
   *          the targetPos to set
   */
  public void setTargetPos(Double targetPos2) {
    this.targetPos = targetPos2;
  }

  public void setCurrentPos(double pos) {
    currentPos = pos;

  }

  public Double getCurrentPos() {
    return currentPos;
  }

  public DHLinkType getType() {
    return type;
  }

  public void setType(DHLinkType type) {
    this.type = type;
    if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      initialTheta = alpha;
    }
  }

  public void setOffset(double offset) {
    this.offset = offset;
  }

  /**
   * @return This represents the difference in angles between the DH model and
   *         the real world encoder/joint angle for the link. This value will be
   *         added to the IK solved angles prior to invoking publishJointAngle.
   * 
   */
  public double getOffset() {
    return offset;
  }
}
