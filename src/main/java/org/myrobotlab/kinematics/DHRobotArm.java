package org.myrobotlab.kinematics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InverseKinematics3D;
import org.slf4j.Logger;

public class DHRobotArm implements Serializable {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(DHRobotArm.class);

  /**
   * Damped least squares with a line search converges in tens of iterations, so
   * this is a "something is wrong" ceiling rather than a working budget.
   */
  private int maxIterations = 600;

  /** Finite-difference step for the Jacobian, radians. */
  private static final double JACOBIAN_DELTA = 1e-5;

  /** Starting Levenberg damping factor. */
  private static final double DAMPING_INITIAL = 0.01;

  private static final double DAMPING_MIN = 1e-5;

  /** Above this the configuration is singular or the goal is unreachable. */
  private static final double DAMPING_MAX = 1e4;

  /** Largest Cartesian correction requested per iteration, meters. */
  private static final double MAX_CARTESIAN_STEP_M = 0.05;

  /** Largest joint change per iteration, radians (~11°). */
  private static final double MAX_JOINT_STEP_RAD = 0.2;

  /** How many times the line search halves a rejected step before giving up. */
  private static final int LINE_SEARCH_STEPS = 6;

  /**
   * Extra starting configurations tried when the descent gets stuck in a local
   * minimum. A 4-DOF arm with tight joint limits has plenty of those, and they
   * are what left the old solver a few millimeters short of reachable goals.
   */
  private static final int SEED_ATTEMPTS = 24;

  /** Coprime bases for the Halton seed sequence, one per joint. */
  private static final int[] HALTON_BASES = { 2, 3, 5, 7, 11, 13, 17, 19 };

  private ArrayList<DHLink> links;

  public String name;

  // for debugging .. hmmm
  public transient InverseKinematics3D ik3D = null;

  /**
   * Fixed end-effector offset in the last link frame (meters). Applied after the
   * DH chain so a wrist/palm node that is not the last joint origin still
   * rotates with the arm. Not an IK joint.
   */
  private Point toolOffset = null;

  /**
   * Affine transform from the DH origin (omoplate) into the world / JME root
   * frame. Identity leaves the chain at the omoplate. When set to the omoplate
   * world pose, {@link #getPalmPosition()} is in the same meters / origin as
   * JMonkeyEngine.
   */
  private Matrix baseTransform = identity4();

  /** IK convergence radius in the same units as link lengths (meters). */
  private double errorThreshold = 0.002;

  public DHRobotArm() {
    super();
    links = new ArrayList<DHLink>();
  }

  public DHRobotArm(DHRobotArm copy) {
    super();
    name = copy.name;
    links = new ArrayList<DHLink>();
    for (DHLink link : copy.links) {
      links.add(new DHLink(link));
    }
    if (copy.toolOffset != null) {
      toolOffset = new Point(copy.toolOffset);
    }
    if (copy.baseTransform != null) {
      baseTransform = new Matrix(copy.baseTransform);
    }
    errorThreshold = copy.errorThreshold;
  }

  public ArrayList<DHLink> addLink(DHLink link) {
    links.add(link);
    return links;
  }

  /**
   * Translational Jacobian, {@code 3 x numLinks}, by central difference.
   *
   * <p>
   * Each joint variable is saved and restored exactly rather than being nudged
   * with {@code incrRotate(+d)} / {@code incrRotate(-d)}, which is not a
   * round trip at a joint limit. The probe is allowed to step marginally outside
   * the limits because it only estimates a derivative.
   * </p>
   */
  public Matrix getJacobian() {
    int numLinks = getNumLinks();
    Matrix jacobian = new Matrix(3, numLinks);
    for (int j = 0; j < numLinks; j++) {
      DHLink link = getLink(j);
      double saved = link.getJointVariable();

      link.setJointVariableUnchecked(saved + JACOBIAN_DELTA);
      Point plus = getPalmPosition();
      link.setJointVariableUnchecked(saved - JACOBIAN_DELTA);
      Point minus = getPalmPosition();
      link.setJointVariableUnchecked(saved);

      double scale = 1.0 / (2.0 * JACOBIAN_DELTA);
      jacobian.elements[0][j] = (plus.getX() - minus.getX()) * scale;
      jacobian.elements[1][j] = (plus.getY() - minus.getY()) * scale;
      jacobian.elements[2][j] = (plus.getZ() - minus.getZ()) * scale;
      // TODO: get orientation roll/pitch/yaw
    }
    return jacobian;
  }

  /**
   * @return the Moore-Penrose pseudo inverse of {@link #getJacobian()},
   *         {@code numLinks x 3}
   */
  public Matrix getJInverse() {
    Matrix jInverse = getJacobian().pseudoInverse();
    if (jInverse == null) {
      // must be numLinks x 3 to multiply a 3x1 Cartesian delta
      jInverse = new Matrix(getNumLinks(), 3);
    }
    return jInverse;
  }

  /**
   * Damped least squares step: {@code dTheta = J^T (J J^T + lambda^2 I)^-1 dP}.
   *
   * <p>
   * The undamped pseudo inverse blows up near singularities — precisely where a
   * 4-DOF arm ends up when reaching for a distant goal — so the raw solution had
   * to be thrown away by the joint limits, stalling the solve. Damping trades a
   * little accuracy for a bounded, well-conditioned step.
   * </p>
   *
   * @return an {@code numLinks x 1} joint delta, or null if the system is
   *         degenerate
   */
  static Matrix solveDamped(Matrix jacobian, Matrix dP, double lambda) {
    Matrix jt = jacobian.transpose();
    Matrix jjt = jacobian.multiply(jt);
    double lambdaSq = lambda * lambda;
    for (int i = 0; i < 3; i++) {
      jjt.elements[i][i] += lambdaSq;
    }
    Matrix inv = invert3x3(jjt);
    if (inv == null) {
      return null;
    }
    return jt.multiply(inv.multiply(dP));
  }

  static Matrix invert3x3(Matrix m) {
    double[][] a = m.elements;
    double det = a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1]) - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0]) + a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0]);
    if (Math.abs(det) < 1e-18) {
      return null;
    }
    double invDet = 1.0 / det;
    Matrix out = new Matrix(3, 3);
    out.elements[0][0] = (a[1][1] * a[2][2] - a[1][2] * a[2][1]) * invDet;
    out.elements[0][1] = (a[0][2] * a[2][1] - a[0][1] * a[2][2]) * invDet;
    out.elements[0][2] = (a[0][1] * a[1][2] - a[0][2] * a[1][1]) * invDet;
    out.elements[1][0] = (a[1][2] * a[2][0] - a[1][0] * a[2][2]) * invDet;
    out.elements[1][1] = (a[0][0] * a[2][2] - a[0][2] * a[2][0]) * invDet;
    out.elements[1][2] = (a[0][2] * a[1][0] - a[0][0] * a[1][2]) * invDet;
    out.elements[2][0] = (a[1][0] * a[2][1] - a[1][1] * a[2][0]) * invDet;
    out.elements[2][1] = (a[0][1] * a[2][0] - a[0][0] * a[2][1]) * invDet;
    out.elements[2][2] = (a[0][0] * a[1][1] - a[0][1] * a[1][0]) * invDet;
    return out;
  }

  public DHLink getLink(int i) {
    if (links.size() >= i) {
      return links.get(i);
    } else {
      // TODO log a warning or something?
      return null;
    }
  }

  public ArrayList<DHLink> getLinks() {
    return links;
  }

  public int getNumLinks() {
    return links.size();
  }

  public synchronized Point getJointPosition(int index) {
    if (index >= this.links.size() || index < 0) {
      return null;
    }

    Matrix m = getHomogeneousMatrix(index);
    boolean includeTool = index == links.size() - 1;
    return pointFromMatrix(m, includeTool);
  }

  /**
   * @param lastDHLink
   *          the index of the link that you want the global position at.
   * @return the x,y,z of the palm. roll,pitc, and yaw are not returned/computed
   *         with this function
   */
  public Point getPalmPosition(String lastDHLink) {
    Matrix m = getHomogeneousMatrix(lastDHLink);
    boolean includeTool = lastDHLink == null || (links.size() > 0 && lastDHLink.equals(links.get(links.size() - 1).getName()));
    return pointFromMatrix(m, includeTool);
  }

  /**
   * Homogeneous transform from the DH base to the origin of {@code lastIndex}
   * (inclusive), without the tool offset. Includes {@link #baseTransform}.
   */
  public Matrix getHomogeneousMatrix(int lastIndex) {
    Matrix m = copyBase();
    int end = Math.min(lastIndex, links.size() - 1);
    for (int i = 0; i <= end; i++) {
      m = m.multiply(links.get(i).resolveMatrix());
    }
    return m;
  }

  /**
   * Homogeneous transform through {@code lastDHLink} (or the full chain if null).
   */
  public Matrix getHomogeneousMatrix(String lastDHLink) {
    Matrix m = copyBase();
    for (int i = 0; i < links.size(); i++) {
      m = m.multiply(links.get(i).resolveMatrix());
      if (links.get(i).getName() != null && links.get(i).getName().equals(lastDHLink)) {
        break;
      }
    }
    return m;
  }

  /**
   * Palm in the DH origin frame (omoplate), ignoring {@link #baseTransform}.
   */
  public Point getPalmPositionLocal() {
    Matrix saved = baseTransform;
    baseTransform = identity4();
    try {
      return getPalmPosition();
    } finally {
      baseTransform = saved;
    }
  }

  public Matrix getHomogeneousMatrix() {
    return getHomogeneousMatrix(links.size() - 1);
  }

  public Point getToolOffset() {
    return toolOffset;
  }

  public void setToolOffset(Point toolOffset) {
    this.toolOffset = toolOffset;
  }

  public void setToolOffset(double x, double y, double z) {
    this.toolOffset = new Point(x, y, z);
  }

  public Matrix getBaseTransform() {
    return baseTransform;
  }

  public void setBaseTransform(Matrix baseTransform) {
    this.baseTransform = baseTransform != null ? new Matrix(baseTransform) : identity4();
  }

  /**
   * Place the DH origin at a world translation with a right-handed Euler rotation
   * in degrees (roll=Z, pitch=X, yaw=Y).
   */
  public void setBaseTransform(double originX, double originY, double originZ, double rollDeg, double pitchDeg, double yawDeg) {
    setBaseTransform(Matrix.rigid(originX, originY, originZ, Math.toRadians(rollDeg), Math.toRadians(pitchDeg), Math.toRadians(yawDeg)));
  }

  /**
   * @deprecated axis scales of −1 reflect the chain and mirror every solved joint
   *             angle. Use
   *             {@link #setBaseTransform(double, double, double, double, double, double)}.
   */
  @Deprecated
  public void setBaseTransform(double originX, double originY, double originZ, double rollDeg, double pitchDeg, double yawDeg, double scaleX, double scaleY, double scaleZ) {
    if (scaleX < 0 || scaleY < 0 || scaleZ < 0) {
      log.warn("ignoring reflecting base scale ({}, {}, {}) - a mirrored chain solves to mirrored servo angles", scaleX, scaleY, scaleZ);
    }
    setBaseTransform(originX, originY, originZ, rollDeg, pitchDeg, yawDeg);
  }

  /**
   * Build a chain from joints measured on a rig (see {@link JointFrame}).
   *
   * <p>
   * The base becomes a pure translation to the first joint, and each link gets a
   * fixed transform whose Z axis is that joint's real rotation axis. The result
   * reproduces the rig's forward kinematics exactly for any joint angles, not
   * just the pose that was sampled — which is the whole point, because a
   * single-pose position fit cannot distinguish a correct model from one whose
   * axes are wrong.
   * </p>
   *
   * @param name
   *          arm name for logging
   * @param frames
   *          joints ordered parent to child
   * @param endEffectorWorld
   *          the point the solver should drive, e.g. the wrist node, sampled in
   *          the same pose as {@code frames}
   */
  public static DHRobotArm fromJointFrames(String name, List<JointFrame> frames, Point endEffectorWorld) {
    DHRobotArm arm = new DHRobotArm();
    arm.name = name;
    arm.applyJointFrames(frames, endEffectorWorld);
    return arm;
  }

  /**
   * Replace this arm's geometry with measured joint frames, preserving link order
   * and names. See {@link #fromJointFrames}.
   *
   * @return true if the chain was rebuilt
   */
  public boolean applyJointFrames(List<JointFrame> frames, Point endEffectorWorld) {
    if (frames == null || frames.isEmpty()) {
      log.error("applyJointFrames - no frames supplied");
      return false;
    }
    for (JointFrame frame : frames) {
      if (frame == null || frame.origin == null || frame.axis == null) {
        log.error("applyJointFrames - incomplete frame {}", frame);
        return false;
      }
    }

    Point base = frames.get(0).origin;
    // pure translation: no rotation, no reflection, so the solver's world axes
    // are the simulator's world axes
    setBaseTransform(Matrix.translation(base.getX(), base.getY(), base.getZ()));

    ArrayList<DHLink> rebuilt = new ArrayList<DHLink>();
    Matrix parentInverse = Matrix.identity(4);
    Matrix lastFrame = null;
    for (JointFrame frame : frames) {
      // express the joint in the base-local frame
      Matrix worldFrame = Matrix.frameFromZAxis(frame.axis.getX(), frame.axis.getY(), frame.axis.getZ(), frame.origin.getX() - base.getX(), frame.origin.getY() - base.getY(),
          frame.origin.getZ() - base.getZ());
      Matrix fixed = parentInverse.multiply(worldFrame);

      double bindTheta = Math.toRadians(frame.angleDeg);
      DHLink link = new DHLink(frame.name, fixed, bindTheta);
      link.setServoSlope(frame.getServoSlope());
      link.setOffset(frame.getServoOffset());
      link.setServoLimits(frame.servoMin, frame.servoMax);
      link.setTheta(bindTheta);
      rebuilt.add(link);

      parentInverse = worldFrame.invertAffine();
      if (parentInverse == null) {
        log.error("applyJointFrames - joint {} frame is not invertible", frame.name);
        return false;
      }
      lastFrame = worldFrame;
    }

    links = rebuilt;
    toolOffset = null;
    if (endEffectorWorld != null && lastFrame != null) {
      fitToolOffset(endEffectorWorld);
    }
    log.info("{} built from {} measured joints, tool offset {}, palm {}", name, links.size(), toolOffset, getPalmPosition());
    return true;
  }

  /** @return true when every link's geometry was measured from a rig. */
  public boolean isMeasured() {
    if (links.isEmpty()) {
      return false;
    }
    for (DHLink link : links) {
      if (!link.isMeasured()) {
        return false;
      }
    }
    return true;
  }

  public void setBaseOrigin(double originX, double originY, double originZ) {
    setBaseTransform(originX, originY, originZ, 0, 0, 0, 1, 1, 1);
  }

  public Point getBaseOrigin() {
    if (baseTransform == null) {
      return new Point(0, 0, 0);
    }
    return new Point(baseTransform.elements[0][3], baseTransform.elements[1][3], baseTransform.elements[2][3]);
  }

  public Point toWorldFrame(Point local) {
    if (local == null) {
      return null;
    }
    return copyBase().transformPoint(local);
  }

  public Point toLocalFrame(Point world) {
    if (world == null) {
      return null;
    }
    Matrix inv = copyBase().invertAffine();
    if (inv == null) {
      return world;
    }
    return inv.transformPoint(world);
  }

  public double getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(double errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  /**
   * Solve a last-frame tool offset so {@link #getPalmPosition()} equals
   * {@code targetPalm} at the current joint thetas. The offset rotates with the
   * arm; it is not a base-frame translation.
   */
  public Point fitToolOffset(Point targetPalm) {
    toolOffset = null;
    Matrix m = getHomogeneousMatrix();
    double dx = targetPalm.getX() - m.elements[0][3];
    double dy = targetPalm.getY() - m.elements[1][3];
    double dz = targetPalm.getZ() - m.elements[2][3];
    double tx = m.elements[0][0] * dx + m.elements[1][0] * dy + m.elements[2][0] * dz;
    double ty = m.elements[0][1] * dx + m.elements[1][1] * dy + m.elements[2][1] * dz;
    double tz = m.elements[0][2] * dx + m.elements[1][2] * dy + m.elements[2][2] * dz;
    toolOffset = new Point(tx, ty, tz);
    log.info("Fitted EE tool offset {} so palm matches {}", toolOffset, targetPalm);
    return toolOffset;
  }

  private Matrix copyBase() {
    return baseTransform != null ? new Matrix(baseTransform) : identity4();
  }

  private static Matrix identity4() {
    return Matrix.identity(4);
  }

  private Point pointFromMatrix(Matrix m, boolean includeTool) {
    double x = m.elements[0][3];
    double y = m.elements[1][3];
    double z = m.elements[2][3];
    if (includeTool && toolOffset != null) {
      x += m.elements[0][0] * toolOffset.getX() + m.elements[0][1] * toolOffset.getY() + m.elements[0][2] * toolOffset.getZ();
      y += m.elements[1][0] * toolOffset.getX() + m.elements[1][1] * toolOffset.getY() + m.elements[1][2] * toolOffset.getZ();
      z += m.elements[2][0] * toolOffset.getX() + m.elements[2][1] * toolOffset.getY() + m.elements[2][2] * toolOffset.getZ();
    }
    double pitch = Math.atan2(-1.0 * (m.elements[2][0]), Math.sqrt(m.elements[0][0] * m.elements[0][0] + m.elements[1][0] * m.elements[1][0]));
    double roll = 0;
    double yaw = 0;
    if (pitch == Math.PI / 2) {
      roll = Math.atan2(m.elements[0][1], m.elements[1][1]);
    } else if (pitch == -1 * Math.PI / 2) {
      roll = Math.atan2(m.elements[0][1], m.elements[1][1]) * -1;
    } else {
      roll = Math.atan2(m.elements[2][1] / Math.cos(pitch), m.elements[2][2]) / Math.cos(pitch);
      yaw = Math.atan2(m.elements[1][0] / Math.cos(pitch), m.elements[0][0] / Math.cos(pitch)) - Math.PI / 2;
    }
    return new Point(x, y, z, pitch * 180 / Math.PI, roll * 180 / Math.PI, yaw * 180 / Math.PI);
  }

  public void centerAllJoints() {
    for (DHLink link : links) {
      double center = (link.getMax() + link.getMin()) / 2.0;
      log.debug("Centering Servo {} to {} degrees", link.getName(), center);
      link.setTheta(center);
    }
  }

  /**
   * Drive the joints so {@link #getPalmPosition()} reaches {@code goal}, using
   * damped least squares with adaptive damping and a backtracking line search.
   *
   * <p>
   * A step is only kept if it reduces the distance to the goal; otherwise it is
   * halved a few times before the damping is raised and the iteration retried.
   * That combination is what makes this converge where the old fixed-gain
   * gradient descent stalled: the undamped pseudo inverse produced enormous joint
   * deltas near a singularity, the joint limits threw them away, and the loop
   * ground through its whole iteration budget without moving.
   * </p>
   *
   * @return true if the palm converged inside {@link #getErrorThreshold()}
   */
  public boolean moveToGoal(Point goal) {
    return moveToGoal(goal, SEED_ATTEMPTS);
  }

  /**
   * @param seedAttempts
   *          how many alternative starting configurations to try if descending
   *          from the current pose gets stuck. The current pose is always tried
   *          first so the arm keeps moving continuously when it can.
   */
  public boolean moveToGoal(Point goal, int seedAttempts) {
    if (goal == null || getNumLinks() == 0) {
      return false;
    }
    if (descendToGoal(goal)) {
      return true;
    }
    double[] best = jointSnapshot();
    double bestError = distanceToGoal(goal);
    for (int attempt = 0; attempt < seedAttempts; attempt++) {
      seedJoints(attempt);
      if (descendToGoal(goal)) {
        return true;
      }
      double error = distanceToGoal(goal);
      if (error < bestError) {
        bestError = error;
        best = jointSnapshot();
      }
    }
    restoreJoints(best);
    log.debug("no solution for {} after {} seeds - closest {} m{}", goal, seedAttempts, bestError, describeLimitedJoints());
    return false;
  }

  private double[] jointSnapshot() {
    double[] snapshot = new double[getNumLinks()];
    for (int i = 0; i < snapshot.length; i++) {
      snapshot[i] = getLink(i).getJointVariable();
    }
    return snapshot;
  }

  private void restoreJoints(double[] snapshot) {
    for (int i = 0; i < snapshot.length && i < getNumLinks(); i++) {
      getLink(i).setJointVariableUnchecked(snapshot[i]);
    }
  }

  /**
   * Deterministic starting configuration number {@code index}.
   *
   * <p>
   * The first {@code 2^numLinks} seeds are the corners of the joint box, because
   * poses at the edge of the reachable set need one or more joints pinned at a
   * limit and descent approaches a boundary very slowly from the interior. The
   * rest are a Halton sequence, which covers the interior with far fewer attempts
   * than random sampling. Both are deterministic, so a given goal always solves
   * to the same pose.
   * </p>
   */
  private void seedJoints(int index) {
    int numLinks = links.size();
    int corners = 1 << Math.min(numLinks, 4);
    for (int i = 0; i < numLinks; i++) {
      DHLink link = links.get(i);
      double lo = link.getMin();
      double hi = link.getMax();
      double fraction;
      if (index < corners) {
        fraction = ((index >> i) & 1) == 0 ? 0.0 : 1.0;
      } else {
        fraction = halton(index - corners + 1, HALTON_BASES[i % HALTON_BASES.length]);
      }
      link.setJointVariableUnchecked(lo + (hi - lo) * fraction);
    }
  }

  static double halton(int index, int base) {
    double result = 0;
    double f = 1.0 / base;
    int i = index;
    while (i > 0) {
      result += f * (i % base);
      i /= base;
      f /= base;
    }
    return result;
  }

  /** One damped-least-squares descent from the current configuration. */
  private boolean descendToGoal(Point goal) {
    int numLinks = getNumLinks();
    double lambda = DAMPING_INITIAL;
    double bestError = goal.distanceTo(getPalmPosition());
    double[] saved = new double[numLinks];
    int iterations = 0;

    while (iterations < maxIterations) {
      if (bestError <= errorThreshold) {
        log.debug("solved {} in {} iterations, error {} m", goal, iterations, bestError);
        return true;
      }
      iterations++;

      Point delta = goal.subtract(getPalmPosition());
      // asking for the whole remaining distance at once makes the linearization
      // invalid, so bound the requested correction
      double magnitude = delta.magnitude();
      double request = magnitude > MAX_CARTESIAN_STEP_M ? MAX_CARTESIAN_STEP_M / magnitude : 1.0;
      Matrix dP = new Matrix(3, 1);
      dP.elements[0][0] = delta.getX() * request;
      dP.elements[1][0] = delta.getY() * request;
      dP.elements[2][0] = delta.getZ() * request;

      Matrix dTheta = solveDamped(getJacobian(), dP, lambda);
      if (dTheta == null) {
        lambda *= 10.0;
        if (lambda > DAMPING_MAX) {
          break;
        }
        continue;
      }

      for (int i = 0; i < numLinks; i++) {
        saved[i] = getLink(i).getJointVariable();
      }

      boolean improved = false;
      double scale = 1.0;
      for (int attempt = 0; attempt < LINE_SEARCH_STEPS; attempt++) {
        for (int i = 0; i < numLinks; i++) {
          double step = 0;
          if (i < dTheta.getNumRows()) {
            step = dTheta.elements[i][0] * scale;
            step = Math.max(-MAX_JOINT_STEP_RAD, Math.min(MAX_JOINT_STEP_RAD, step));
          }
          getLink(i).setJointVariableUnchecked(getLink(i).clampToLimits(saved[i] + step));
        }
        double error = goal.distanceTo(getPalmPosition());
        if (error < bestError - 1e-12) {
          bestError = error;
          improved = true;
          break;
        }
        scale *= 0.5;
      }

      if (improved) {
        lambda = Math.max(DAMPING_MIN, lambda * 0.7);
      } else {
        for (int i = 0; i < numLinks; i++) {
          getLink(i).setJointVariableUnchecked(saved[i]);
        }
        lambda *= 6.0;
        if (lambda > DAMPING_MAX) {
          break;
        }
      }
    }

    return bestError <= errorThreshold;
  }

  private String describeLimitedJoints() {
    StringBuilder sb = new StringBuilder();
    for (DHLink link : links) {
      if (link.isAtLimit()) {
        sb.append(sb.length() == 0 ? " (at limit: " : ", ").append(link.getName());
      }
    }
    return sb.length() == 0 ? "" : sb.append(")").toString();
  }

  /**
   * @return the distance from the palm to {@code goal} in the current
   *         configuration, meters
   */
  public double distanceToGoal(Point goal) {
    return goal == null ? Double.NaN : goal.distanceTo(getPalmPosition());
  }

  public void setLinks(ArrayList<DHLink> links) {
    this.links = links;
  }

  public void setIk3D(InverseKinematics3D ik3d) {
    ik3D = ik3d;
  }

  public boolean armMovementEnds() {
    for (DHLink link : links) {
      // if (link.getState() != Servo.SERVO_EVENT_STOPPED) {
      // return false;
      // }
    }
    return true;
  }

  public double[][] createJointPositionMap() {

    double[][] jointPositionMap = new double[getNumLinks() + 1][3];

    // first position is the origin... second is the end of the first link
    jointPositionMap[0][0] = 0;
    jointPositionMap[0][1] = 0;
    jointPositionMap[0][2] = 0;

    for (int i = 1; i <= getNumLinks(); i++) {
      Point jp = getJointPosition(i - 1);
      jointPositionMap[i][0] = jp.getX();
      jointPositionMap[i][1] = jp.getY();
      jointPositionMap[i][2] = jp.getZ();
    }
    return jointPositionMap;
  }

  public Point getVector() {
    Point lastJoint = getJointPosition(links.size() - 1);
    Point previousJoint = getJointPosition(links.size() - 2);
    Point retval = lastJoint.subtract(previousJoint);

    return retval;
  }

  public Point getPalmPosition() {
    return getPalmPosition(null);
  }
}
