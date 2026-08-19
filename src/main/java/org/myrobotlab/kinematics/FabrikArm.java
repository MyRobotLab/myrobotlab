package org.myrobotlab.kinematics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * A revolute kinematic chain solved with FABRIK (Forward And Backward Reaching
 * Inverse Kinematics — Aristidou &amp; Lasenby).
 *
 * <p>
 * Geometric FABRIK moves joint <em>positions</em>. InMoov's shoulder and rotate
 * joints are axial twists: they do not move the next joint, so a bone-direction
 * hinge constraint is degenerate. After each FABRIK position pass this class
 * projects the skeleton back onto the measured hinge axes (including twist,
 * using a distal landmark), polishes with cyclic coordinate descent around
 * those axes (line-searched so a large hinge step cannot overshoot), then
 * tightens with damped least squares until the palm is a few millimeters from
 * the goal. Joint limits are the servo ranges from {@link JointFrame}.
 * </p>
 *
 * <p>
 * Everything is meters, Y-up, matching JMonkeyEngine and
 * {@link org.myrobotlab.service.InverseKinematics3D}.
 * </p>
 */
public class FabrikArm implements Serializable {

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(FabrikArm.class);

  private static final double AXIS_PARALLEL = 0.92;

  private static final double TINY = 1e-9;

  /** Finite-difference step for the Jacobian, radians. */
  private static final double JACOBIAN_DELTA_RAD = 1e-5;

  private static final double DAMPING_INITIAL = 0.01;

  private static final double DAMPING_MIN = 1e-5;

  private static final double DAMPING_MAX = 1e4;

  /** Largest Cartesian correction requested per DLS iteration, meters. */
  private static final double MAX_CARTESIAN_STEP_M = 0.05;

  /** Largest joint change per DLS iteration, radians (~11°). */
  private static final double MAX_JOINT_STEP_RAD = 0.2;

  private static final int LINE_SEARCH_STEPS = 8;

  private static final int CCD_MAX_SWEEPS = 40;

  /**
   * Extra starting configurations when descent from the current pose stalls. A
   * 4-DOF InMoov arm has local minima; seeds are what close the last few
   * millimeters on reachable goals.
   */
  public static final int DEFAULT_SEED_ATTEMPTS = 24;

  private static final int[] HALTON_BASES = { 2, 3, 5, 7, 11, 13, 17, 19 };

  public String name;

  private final List<JointFrame> frames = new ArrayList<>();

  /** Bind-pose joint origins. */
  private Point[] bindOrigin = new Point[0];

  /** Bind-pose unit axes. */
  private Point[] bindAxis = new Point[0];

  private Point bindEndEffector = new Point(0, 0, 0);

  /** Mesh degrees at the bind pose ({@link JointFrame#angleDeg}). */
  private double[] bindThetaDeg = new double[0];

  /** Current mesh degrees. */
  private double[] thetaDeg = new double[0];

  /** Working FK joint origins + end effector. */
  private Point[] positions = new Point[0];

  /** Working FK axes (parent joints already applied). */
  private Point[] axes = new Point[0];

  private double errorThreshold = 0.002;

  private int maxIterations = 200;

  private double lastError = Double.NaN;

  private int lastIterations = 0;

  public FabrikArm() {
  }

  public FabrikArm(String name) {
    this.name = name;
  }

  public int getNumJoints() {
    return frames.size();
  }

  public List<JointFrame> getFrames() {
    return frames;
  }

  public JointFrame getFrame(int i) {
    return frames.get(i);
  }

  public Point getBaseOrigin() {
    return bindOrigin.length == 0 ? new Point(0, 0, 0) : new Point(bindOrigin[0]);
  }

  public Point getPalmPosition() {
    if (positions.length == 0) {
      return new Point(0, 0, 0);
    }
    return new Point(positions[positions.length - 1]);
  }

  public Point getJointPosition(int index) {
    if (index < 0 || index >= bindOrigin.length) {
      return null;
    }
    return new Point(positions[index]);
  }

  /**
   * Skeleton for the WebGui stick figure: each joint origin followed by the end
   * effector.
   */
  public double[][] getSkeleton() {
    double[][] map = new double[positions.length][3];
    for (int i = 0; i < positions.length; i++) {
      map[i][0] = positions[i].getX();
      map[i][1] = positions[i].getY();
      map[i][2] = positions[i].getZ();
    }
    return map;
  }

  public double getThetaDegrees(int i) {
    return thetaDeg[i];
  }

  public double getLastError() {
    return lastError;
  }

  public int getLastIterations() {
    return lastIterations;
  }

  public double getErrorThreshold() {
    return errorThreshold;
  }

  public void setErrorThreshold(double errorThreshold) {
    this.errorThreshold = errorThreshold;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = Math.max(1, maxIterations);
  }

  /**
   * Replace the chain with measured joints. Current pose is the bind pose.
   *
   * @return true if the chain was rebuilt
   */
  public boolean applyJointFrames(List<JointFrame> measured, Point endEffectorWorld) {
    if (measured == null || measured.isEmpty() || endEffectorWorld == null) {
      log.error("applyJointFrames - need joints and an end effector");
      return false;
    }
    for (JointFrame frame : measured) {
      if (frame == null || frame.origin == null || frame.axis == null) {
        log.error("applyJointFrames - incomplete frame {}", frame);
        return false;
      }
    }
    frames.clear();
    for (JointFrame frame : measured) {
      frames.add(frame);
    }
    int n = frames.size();
    bindOrigin = new Point[n];
    bindAxis = new Point[n];
    bindThetaDeg = new double[n];
    thetaDeg = new double[n];
    axes = new Point[n];
    positions = new Point[n + 1];
    bindEndEffector = new Point(endEffectorWorld);
    for (int i = 0; i < n; i++) {
      JointFrame frame = frames.get(i);
      bindOrigin[i] = new Point(frame.origin);
      bindAxis[i] = unit(frame.axis);
      bindThetaDeg[i] = frame.angleDeg;
      thetaDeg[i] = frame.angleDeg;
    }
    rebuild();
    log.info("{} built from {} joints, palm {}", name, n, getPalmPosition());
    return true;
  }

  public void setThetaDegrees(int i, double meshDeg) {
    thetaDeg[i] = clampMesh(i, meshDeg);
    rebuild();
  }

  public void setFromServoDegrees(int i, double servoDeg) {
    thetaDeg[i] = meshFromServo(i, servoDeg);
    rebuild();
  }

  public double toServoDegrees(int i) {
    return servoFromMesh(i, thetaDeg[i]);
  }

  public Map<String, Double> toServoMap() {
    Map<String, Double> map = new LinkedHashMap<>();
    for (int i = 0; i < frames.size(); i++) {
      String jointName = frames.get(i).name;
      if (jointName != null) {
        map.put(jointName, toServoDegrees(i));
      }
    }
    return map;
  }

  public void centerAllJoints() {
    for (int i = 0; i < frames.size(); i++) {
      JointFrame frame = frames.get(i);
      thetaDeg[i] = 0.5 * (frame.angleAtServoMin + frame.angleAtServoMax);
    }
    rebuild();
  }

  /**
   * Drive the palm to {@code goal}: FABRIK hint, hinge projection, line-search
   * CCD, then damped least squares until the residual is inside
   * {@link #errorThreshold} (2 mm by default).
   *
   * @return true if the palm is inside {@link #errorThreshold}
   */
  public boolean moveToGoal(Point goal) {
    return moveToGoal(goal, DEFAULT_SEED_ATTEMPTS);
  }

  /**
   * @param seedAttempts
   *          extra starting configurations if descent from the current pose
   *          stalls. Use 0 for cheap intermediate waypoints; the current pose
   *          is always tried first.
   */
  public boolean moveToGoal(Point goal, int seedAttempts) {
    if (goal == null || frames.isEmpty()) {
      return false;
    }
    lastIterations = 0;
    lastError = distanceToGoal(goal);
    if (lastError <= errorThreshold) {
      return true;
    }

    double[] original = snapshotThetas();

    Point[] hint = copyPositions();
    fabrikReach(hint, goal);
    projectToHinges(hint, goal);
    ccdUntil(goal);
    dlsDescend(goal);

    double[] best = snapshotThetas();
    double bestError = distanceToGoal(goal);
    if (bestError <= errorThreshold) {
      lastError = bestError;
      return true;
    }

    // FABRIK can leave a poor seed on twist joints — try DLS from the pose we
    // started at before giving up on this configuration.
    restoreThetas(original);
    dlsDescend(goal);
    double fromOriginal = distanceToGoal(goal);
    if (fromOriginal < bestError) {
      bestError = fromOriginal;
      best = snapshotThetas();
    }
    if (bestError <= errorThreshold) {
      lastError = bestError;
      return true;
    }

    for (int attempt = 0; attempt < seedAttempts; attempt++) {
      seedJoints(attempt);
      dlsDescend(goal);
      double error = distanceToGoal(goal);
      if (error < bestError) {
        bestError = error;
        best = snapshotThetas();
      }
      if (bestError <= errorThreshold) {
        lastError = bestError;
        return true;
      }
    }

    restoreThetas(best);
    lastError = bestError;
    return bestError <= errorThreshold;
  }

  public double distanceToGoal(Point goal) {
    return getPalmPosition().distanceTo(goal);
  }

  /**
   * Unconstrained FABRIK on a copy of the current skeleton. Bone lengths stay
   * fixed; the result is generally off the hinge manifold until
   * {@link #projectToHinges} runs.
   */
  void fabrikReach(Point[] p, Point target) {
    int n = frames.size();
    double[] lengths = new double[n];
    for (int i = 0; i < n; i++) {
      lengths[i] = p[i].distanceTo(p[i + 1]);
      if (lengths[i] < TINY) {
        lengths[i] = TINY;
      }
    }
    Point base = new Point(p[0]);
    int passes = 12;
    for (int pass = 0; pass < passes; pass++) {
      p[n] = new Point(target);
      for (int i = n - 1; i >= 0; i--) {
        Point dir = p[i].subtract(p[i + 1]).unitVector(lengths[i]);
        p[i] = p[i + 1].add(dir);
      }
      p[0] = new Point(base);
      for (int i = 0; i < n; i++) {
        Point dir = p[i + 1].subtract(p[i]).unitVector(lengths[i]);
        p[i + 1] = p[i].add(dir);
      }
      if (p[n].distanceTo(target) <= errorThreshold) {
        break;
      }
    }
  }

  /**
   * Pull each hinge (including twist) toward the FABRIK skeleton, keeping the
   * step only if the palm gets closer to {@code goal}.
   */
  void projectToHinges(Point[] fabrikPos, Point goal) {
    for (int i = 0; i < frames.size(); i++) {
      Point axis = axes[i];
      Point from = positions[i];
      Point currentRef = landmark(positions, i);
      Point fabrikRef = landmark(fabrikPos, i);
      Point a = projectOnPlane(currentRef.subtract(from), axis);
      Point b = projectOnPlane(fabrikRef.subtract(from), axis);
      if (a.magnitude() < 1e-6 || b.magnitude() < 1e-6) {
        continue;
      }
      applyHingeDelta(i, signedAngleDeg(a, b, axis), goal);
    }
  }

  /** Kept for tests that exercise hinge projection without a Cartesian goal. */
  void projectToHinges(Point[] fabrikPos) {
    projectToHinges(fabrikPos, getPalmPosition());
  }

  private void ccdUntil(Point goal) {
    for (int sweep = 0; sweep < CCD_MAX_SWEEPS; sweep++) {
      lastIterations++;
      double before = distanceToGoal(goal);
      if (before <= errorThreshold) {
        lastError = before;
        return;
      }
      ccdSweep(goal);
      double after = distanceToGoal(goal);
      lastError = after;
      if (after >= before - 1e-9) {
        return;
      }
    }
  }

  private void ccdSweep(Point goal) {
    for (int i = frames.size() - 1; i >= 0; i--) {
      Point axis = axes[i];
      Point from = positions[i];
      Point a = projectOnPlane(positions[positions.length - 1].subtract(from), axis);
      Point b = projectOnPlane(goal.subtract(from), axis);
      if (a.magnitude() < 1e-6 || b.magnitude() < 1e-6) {
        continue;
      }
      applyHingeDelta(i, signedAngleDeg(a, b, axis), goal);
    }
  }

  /**
   * Apply a hinge rotation with backtracking. The old ±25° CCD cap overshot by
   * ~0.15 m on a forearm-length lever and the step was kept even when error
   * grew, so the polish oscillated instead of tightening.
   */
  private void applyHingeDelta(int i, double deltaDeg, Point goal) {
    if (Math.abs(deltaDeg) < 1e-9) {
      return;
    }
    double saved = thetaDeg[i];
    double bestError = distanceToGoal(goal);
    double scale = 1.0;
    boolean improved = false;
    for (int attempt = 0; attempt < LINE_SEARCH_STEPS; attempt++) {
      thetaDeg[i] = clampMesh(i, saved + deltaDeg * scale);
      rebuild();
      double error = distanceToGoal(goal);
      if (error < bestError - 1e-12) {
        improved = true;
        break;
      }
      scale *= 0.5;
    }
    if (!improved) {
      thetaDeg[i] = saved;
      rebuild();
    }
  }

  /** One damped-least-squares descent from the current configuration. */
  private boolean dlsDescend(Point goal) {
    int n = frames.size();
    double lambda = DAMPING_INITIAL;
    double bestError = distanceToGoal(goal);
    double[] saved = new double[n];
    int iterations = 0;

    while (iterations < maxIterations) {
      if (bestError <= errorThreshold) {
        lastError = bestError;
        return true;
      }
      iterations++;
      lastIterations++;

      Point delta = goal.subtract(getPalmPosition());
      double magnitude = delta.magnitude();
      double request = magnitude > MAX_CARTESIAN_STEP_M ? MAX_CARTESIAN_STEP_M / magnitude : 1.0;
      Matrix dP = new Matrix(3, 1);
      dP.elements[0][0] = delta.getX() * request;
      dP.elements[1][0] = delta.getY() * request;
      dP.elements[2][0] = delta.getZ() * request;

      Matrix dTheta = DHRobotArm.solveDamped(getJacobian(), dP, lambda);
      if (dTheta == null) {
        lambda *= 10.0;
        if (lambda > DAMPING_MAX) {
          break;
        }
        continue;
      }

      for (int i = 0; i < n; i++) {
        saved[i] = thetaDeg[i];
      }

      boolean improved = false;
      double scale = 1.0;
      for (int attempt = 0; attempt < LINE_SEARCH_STEPS; attempt++) {
        for (int i = 0; i < n; i++) {
          double stepRad = 0;
          if (i < dTheta.getNumRows()) {
            stepRad = dTheta.elements[i][0] * scale;
            stepRad = Math.max(-MAX_JOINT_STEP_RAD, Math.min(MAX_JOINT_STEP_RAD, stepRad));
          }
          thetaDeg[i] = clampMesh(i, saved[i] + Math.toDegrees(stepRad));
        }
        rebuild();
        double error = distanceToGoal(goal);
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
        restoreThetas(saved);
        lambda *= 6.0;
        if (lambda > DAMPING_MAX) {
          break;
        }
      }
    }

    lastError = bestError;
    return bestError <= errorThreshold;
  }

  /**
   * Numerical Jacobian {@code dP / d(theta_rad)} so DLS steps match
   * {@link DHRobotArm}.
   */
  Matrix getJacobian() {
    int n = frames.size();
    Matrix jacobian = new Matrix(3, n);
    double deltaDeg = Math.toDegrees(JACOBIAN_DELTA_RAD);
    double scale = 1.0 / (2.0 * JACOBIAN_DELTA_RAD);
    for (int j = 0; j < n; j++) {
      double saved = thetaDeg[j];
      thetaDeg[j] = saved + deltaDeg;
      rebuild();
      Point plus = getPalmPosition();
      thetaDeg[j] = saved - deltaDeg;
      rebuild();
      Point minus = getPalmPosition();
      thetaDeg[j] = saved;
      jacobian.elements[0][j] = (plus.getX() - minus.getX()) * scale;
      jacobian.elements[1][j] = (plus.getY() - minus.getY()) * scale;
      jacobian.elements[2][j] = (plus.getZ() - minus.getZ()) * scale;
    }
    rebuild();
    return jacobian;
  }

  private double[] snapshotThetas() {
    double[] snapshot = new double[thetaDeg.length];
    System.arraycopy(thetaDeg, 0, snapshot, 0, thetaDeg.length);
    return snapshot;
  }

  private void restoreThetas(double[] snapshot) {
    System.arraycopy(snapshot, 0, thetaDeg, 0, Math.min(snapshot.length, thetaDeg.length));
    rebuild();
  }

  private void seedJoints(int index) {
    int n = frames.size();
    int corners = 1 << Math.min(n, 4);
    for (int i = 0; i < n; i++) {
      JointFrame frame = frames.get(i);
      double lo = Math.min(frame.angleAtServoMin, frame.angleAtServoMax);
      double hi = Math.max(frame.angleAtServoMin, frame.angleAtServoMax);
      double fraction;
      if (index < corners) {
        fraction = ((index >> i) & 1) == 0 ? 0.0 : 1.0;
      } else {
        fraction = DHRobotArm.halton(index - corners + 1, HALTON_BASES[i % HALTON_BASES.length]);
      }
      thetaDeg[i] = clampMesh(i, lo + (hi - lo) * fraction);
    }
    rebuild();
  }

  /**
   * Distal point used to recover a hinge angle. For a twist joint the next bone
   * is parallel to the axis, so the end effector (or the next non-colinear
   * joint) is used instead.
   */
  private Point landmark(Point[] p, int joint) {
    Point axis = joint < axes.length ? axes[joint] : bindAxis[joint];
    for (int k = joint + 1; k < p.length; k++) {
      Point rel = p[k].subtract(p[joint]);
      double mag = rel.magnitude();
      if (mag < 1e-6) {
        continue;
      }
      double parallel = Math.abs(dot(rel, axis) / mag);
      if (parallel < AXIS_PARALLEL) {
        return p[k];
      }
    }
    return p[p.length - 1];
  }

  void rebuild() {
    int n = frames.size();
    if (n == 0) {
      return;
    }
    for (int i = 0; i < n; i++) {
      positions[i] = new Point(bindOrigin[i]);
      axes[i] = new Point(bindAxis[i]);
    }
    positions[n] = new Point(bindEndEffector);
    for (int i = 0; i < n; i++) {
      double delta = thetaDeg[i] - bindThetaDeg[i];
      if (Math.abs(delta) < 1e-12) {
        continue;
      }
      Point origin = positions[i];
      Point axis = axes[i];
      for (int j = i + 1; j <= n; j++) {
        positions[j] = rotateAround(positions[j], origin, axis, delta);
      }
      for (int j = i + 1; j < n; j++) {
        axes[j] = rotateVec(axes[j], axis, delta);
      }
    }
  }

  private Point[] copyPositions() {
    Point[] copy = new Point[positions.length];
    for (int i = 0; i < positions.length; i++) {
      copy[i] = new Point(positions[i]);
    }
    return copy;
  }

  private double clampMesh(int i, double meshDeg) {
    return meshFromServo(i, servoFromMesh(i, meshDeg));
  }

  private double servoFromMesh(int i, double meshDeg) {
    JointFrame frame = frames.get(i);
    double servo = frame.getServoSlope() * meshDeg + frame.getServoOffset();
    double lo = Math.min(frame.servoMin, frame.servoMax);
    double hi = Math.max(frame.servoMin, frame.servoMax);
    return Math.max(lo, Math.min(hi, servo));
  }

  private double meshFromServo(int i, double servoDeg) {
    JointFrame frame = frames.get(i);
    double lo = Math.min(frame.servoMin, frame.servoMax);
    double hi = Math.max(frame.servoMin, frame.servoMax);
    double servo = Math.max(lo, Math.min(hi, servoDeg));
    return frame.getAngleSlope() * servo + frame.getAngleIntercept();
  }

  static Point unit(Point p) {
    double m = p.magnitude();
    if (m < TINY) {
      return new Point(0, 0, 1);
    }
    return p.multiplyXYZ(1.0 / m);
  }

  static Point projectOnPlane(Point v, Point axis) {
    Point a = unit(axis);
    double d = dot(v, a);
    return new Point(v.getX() - d * a.getX(), v.getY() - d * a.getY(), v.getZ() - d * a.getZ());
  }

  static double signedAngleDeg(Point from, Point to, Point axis) {
    Point a = unit(from);
    Point b = unit(to);
    Point k = unit(axis);
    Point c = cross(a, b);
    double sin = dot(k, c);
    double cos = dot(a, b);
    return Math.toDegrees(Math.atan2(sin, cos));
  }

  static Point rotateAround(Point p, Point origin, Point axis, double deg) {
    Point rel = new Point(p.getX() - origin.getX(), p.getY() - origin.getY(), p.getZ() - origin.getZ());
    Point r = rotateVec(rel, axis, deg);
    return new Point(origin.getX() + r.getX(), origin.getY() + r.getY(), origin.getZ() + r.getZ());
  }

  static Point rotateVec(Point v, Point axis, double deg) {
    Point k = unit(axis);
    double rad = Math.toRadians(deg);
    double c = Math.cos(rad);
    double s = Math.sin(rad);
    Point kx = cross(k, v);
    double d = dot(k, v);
    double oc = 1.0 - c;
    return new Point(v.getX() * c + kx.getX() * s + k.getX() * d * oc, v.getY() * c + kx.getY() * s + k.getY() * d * oc, v.getZ() * c + kx.getZ() * s + k.getZ() * d * oc);
  }

  static double dot(Point a, Point b) {
    return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
  }

  static Point cross(Point a, Point b) {
    return new Point(a.getY() * b.getZ() - a.getZ() * b.getY(), a.getZ() * b.getX() - a.getX() * b.getZ(), a.getX() * b.getY() - a.getY() * b.getX());
  }
}
