package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

/**
 * The check a single-pose calibration cannot do: sweep every joint across its
 * whole range and require the solver's forward kinematics to agree with the rig
 * everywhere, not just where it was sampled.
 *
 * <p>
 * The "rig" here is a synthetic nested transform chain with deliberately awkward
 * bind rotations and non-axis-aligned joint axes — the same thing a rigged
 * skeleton does when you set a node's local rotation. No JMonkeyEngine needed,
 * so this runs in milliseconds and catches axis, direction and ordering mistakes
 * that a rest-pose position match hides.
 * </p>
 */
public class MeasuredChainWorkspaceTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(MeasuredChainWorkspaceTest.class);

  /**
   * A joint of the synthetic rig: a fixed transform from its parent, plus a local
   * rotation axis it turns about.
   */
  private static class RigJoint {
    String name;
    Matrix bind;
    double[] localAxis;
    double angleDeg;
    double servoMin;
    double servoMax;
    double meshSlope;
    double servoRest;

    RigJoint(String name, Matrix bind, double[] localAxis, double servoMin, double servoMax, double servoRest, double meshSlope) {
      this.name = name;
      this.bind = bind;
      this.localAxis = localAxis;
      this.servoMin = servoMin;
      this.servoMax = servoMax;
      this.servoRest = servoRest;
      this.meshSlope = meshSlope;
      this.angleDeg = 0;
    }

    /** mesh degrees for a servo command, exactly like the simulator's mapper */
    double meshFor(double servoDeg) {
      return meshSlope * (servoDeg - servoRest);
    }
  }

  /** A four joint rig that looks nothing like a tidy DH chain. */
  private static List<RigJoint> buildRig() {
    List<RigJoint> rig = new ArrayList<>();
    // shoulder blade: offset up and out, twisted 37 degrees, turns about local Z
    rig.add(new RigJoint("omoplate", Matrix.rigid(0.30, 1.40, -0.02, Math.toRadians(37), Math.toRadians(-11), Math.toRadians(5)), new double[] { 0, 0, 1 }, 10, 80, 10, 1));
    // shoulder: 4 cm out along the blade, tipped, turns about local X
    rig.add(new RigJoint("shoulder", Matrix.rigid(0.04, 0.01, 0.0, Math.toRadians(-19), Math.toRadians(23), 0), new double[] { 1, 0, 0 }, 0, 180, 30, -1));
    // upper arm twist: turns about local Y (down the bone)
    rig.add(new RigJoint("rotate", Matrix.rigid(0.08, -0.01, 0.005, 0, Math.toRadians(8), Math.toRadians(-14)), new double[] { 0, -1, 0 }, 40, 180, 90, 1));
    // elbow: 28 cm down the upper arm, turns about local X
    rig.add(new RigJoint("bicep", Matrix.rigid(0.0, -0.28, 0.01, Math.toRadians(6), 0, 0), new double[] { 1, 0, 0 }, 0, 90, 0, -1));
    return rig;
  }

  /** wrist in the last joint's local frame - 28 cm of forearm, slightly offset */
  private static final double[] WRIST_LOCAL = { 0.01, -0.28, 0.02 };

  /** Cumulative world transform of joint {@code index}, inclusive. */
  private static Matrix rigWorld(List<RigJoint> rig, int index) {
    Matrix m = Matrix.identity(4);
    for (int i = 0; i <= index; i++) {
      RigJoint joint = rig.get(i);
      Matrix local = Matrix.rotationAboutAxis(joint.localAxis[0], joint.localAxis[1], joint.localAxis[2], Math.toRadians(joint.angleDeg));
      m = m.multiply(joint.bind).multiply(local);
    }
    return m;
  }

  private static Point rigWrist(List<RigJoint> rig) {
    Matrix last = rigWorld(rig, rig.size() - 1);
    return last.transformPoint(new Point(WRIST_LOCAL[0], WRIST_LOCAL[1], WRIST_LOCAL[2]));
  }

  /**
   * Measure the rig the same way {@code JMonkeyEngine.getJointFrames} measures
   * VinMoov: world axis origin, world axis direction, current angle and the
   * servo map.
   */
  private static List<JointFrame> measure(List<RigJoint> rig) {
    List<JointFrame> frames = new ArrayList<>();
    for (int i = 0; i < rig.size(); i++) {
      RigJoint joint = rig.get(i);
      Matrix world = rigWorld(rig, i);
      Point origin = new Point(world.elements[0][3], world.elements[1][3], world.elements[2][3]);
      Point axis = new Point(world.elements[0][0] * joint.localAxis[0] + world.elements[0][1] * joint.localAxis[1] + world.elements[0][2] * joint.localAxis[2],
          world.elements[1][0] * joint.localAxis[0] + world.elements[1][1] * joint.localAxis[1] + world.elements[1][2] * joint.localAxis[2],
          world.elements[2][0] * joint.localAxis[0] + world.elements[2][1] * joint.localAxis[1] + world.elements[2][2] * joint.localAxis[2]);
      JointFrame frame = new JointFrame(joint.name, origin, axis);
      frame.angleDeg = joint.angleDeg;
      frame.withServoMap(joint.servoMin, joint.servoMax, joint.meshFor(joint.servoMin), joint.meshFor(joint.servoMax));
      frames.add(frame);
    }
    return frames;
  }

  private static DHRobotArm calibrate(List<RigJoint> rig) {
    return DHRobotArm.fromJointFrames("synthetic", measure(rig), rigWrist(rig));
  }

  private static void setRigAndModel(List<RigJoint> rig, DHRobotArm arm, int joint, double servoDeg) {
    rig.get(joint).angleDeg = rig.get(joint).meshFor(servoDeg);
    arm.getLink(joint).setFromServoDegrees(servoDeg);
  }

  @Test
  public void testMeasuredChainMatchesRigAtCalibrationPose() {
    List<RigJoint> rig = buildRig();
    DHRobotArm arm = calibrate(rig);
    Assert.assertTrue("chain is measured, not hand-tuned DH", arm.isMeasured());
    Assert.assertEquals(0.0, arm.getPalmPosition().distanceTo(rigWrist(rig)), 1e-9);
  }

  @Test
  public void testMeasuredChainMatchesRigAcrossEveryJointRange() {
    List<RigJoint> rig = buildRig();
    DHRobotArm arm = calibrate(rig);

    double worst = 0;
    String worstAt = "";
    for (int j = 0; j < rig.size(); j++) {
      RigJoint joint = rig.get(j);
      int steps = 13;
      for (int s = 0; s < steps; s++) {
        double servo = joint.servoMin + (joint.servoMax - joint.servoMin) * s / (double) (steps - 1);
        setRigAndModel(rig, arm, j, servo);
        double err = arm.getPalmPosition().distanceTo(rigWrist(rig));
        if (err > worst) {
          worst = err;
          worstAt = joint.name + " @ " + servo;
        }
      }
      setRigAndModel(rig, arm, j, joint.servoRest);
    }
    log.info("worst single joint disagreement {} m at {}", worst, worstAt);
    Assert.assertEquals("forward kinematics must match the rig at every angle, not just at rest", 0.0, worst, 1e-9);
  }

  @Test
  public void testMeasuredChainMatchesRigForCombinedPoses() {
    List<RigJoint> rig = buildRig();
    DHRobotArm arm = calibrate(rig);

    double worst = 0;
    int steps = 4;
    for (int a = 0; a < steps; a++) {
      for (int b = 0; b < steps; b++) {
        for (int c = 0; c < steps; c++) {
          for (int d = 0; d < steps; d++) {
            int[] idx = { a, b, c, d };
            for (int j = 0; j < 4; j++) {
              RigJoint joint = rig.get(j);
              double servo = joint.servoMin + (joint.servoMax - joint.servoMin) * idx[j] / (double) (steps - 1);
              setRigAndModel(rig, arm, j, servo);
            }
            worst = Math.max(worst, arm.getPalmPosition().distanceTo(rigWrist(rig)));
          }
        }
      }
    }
    log.info("worst disagreement over {} combined poses: {} m", (int) Math.pow(steps, 4), worst);
    Assert.assertEquals("all four joints together must still match the rig", 0.0, worst, 1e-9);
  }

  /**
   * A model calibrated with a mirrored joint direction reproduces the rest pose
   * perfectly and then diverges — which is why position-only calibration is not
   * enough and this sweep exists.
   */
  @Test
  public void testMirroredJointDirectionIsCaughtBySweepButNotByRestPose() {
    List<RigJoint> rig = buildRig();
    List<JointFrame> frames = measure(rig);
    // flip the shoulder's rotation direction, as an inverted mapper slope would
    frames.get(1).withServoMap(frames.get(1).servoMin, frames.get(1).servoMax, frames.get(1).angleAtServoMax, frames.get(1).angleAtServoMin);
    DHRobotArm bad = DHRobotArm.fromJointFrames("mirrored", frames, rigWrist(rig));

    Assert.assertEquals("rest pose still matches, so it looks calibrated", 0.0, bad.getPalmPosition().distanceTo(rigWrist(rig)), 1e-9);

    setRigAndModel(rig, bad, 1, 120.0);
    double err = bad.getPalmPosition().distanceTo(rigWrist(rig));
    log.info("mirrored shoulder at servo 120 disagrees by {} m", err);
    Assert.assertTrue("a sweep must expose the mirrored joint: " + err, err > 0.05);
  }

  @Test
  public void testSolverReachesGoalsAcrossTheWorkspace() {
    List<RigJoint> rig = buildRig();
    DHRobotArm arm = calibrate(rig);
    arm.setErrorThreshold(0.002);

    // collect reachable goals by sampling the forward kinematics, then ask the
    // solver to find each one from a centered start
    List<Point> goals = new ArrayList<>();
    int steps = 3;
    for (int a = 0; a < steps; a++) {
      for (int b = 0; b < steps; b++) {
        for (int c = 0; c < steps; c++) {
          for (int j = 0; j < 3; j++) {
            int idx = j == 0 ? a : (j == 1 ? b : c);
            RigJoint joint = rig.get(j);
            double servo = joint.servoMin + (joint.servoMax - joint.servoMin) * idx / (double) (steps - 1);
            arm.getLink(j).setFromServoDegrees(servo);
          }
          goals.add(new Point(arm.getPalmPosition()));
        }
      }
    }

    int solved = 0;
    double worst = 0;
    for (Point goal : goals) {
      arm.centerAllJoints();
      if (arm.moveToGoal(goal)) {
        solved++;
      } else {
        log.warn("unsolved goal {} residual {} m", goal, arm.distanceToGoal(goal));
      }
      worst = Math.max(worst, arm.distanceToGoal(goal));
    }
    log.info("damped least squares solved {}/{} reachable goals, worst residual {} m", solved, goals.size(), worst);
    Assert.assertEquals("every sampled reachable pose should be solvable", goals.size(), solved);
  }

  @Test
  public void testJacobianProbeRestoresJointsAtTheirLimit() {
    List<RigJoint> rig = buildRig();
    DHRobotArm arm = calibrate(rig);
    for (int i = 0; i < arm.getNumLinks(); i++) {
      arm.getLink(i).setTheta(arm.getLink(i).getMax());
    }
    double[] before = new double[arm.getNumLinks()];
    for (int i = 0; i < arm.getNumLinks(); i++) {
      before[i] = arm.getLink(i).getTheta();
    }
    for (int repeat = 0; repeat < 5000; repeat++) {
      arm.getJacobian();
    }
    for (int i = 0; i < arm.getNumLinks(); i++) {
      Assert.assertEquals("link " + i + " must not drift when probed at its limit", before[i], arm.getLink(i).getTheta(), 1e-12);
    }
  }
}
