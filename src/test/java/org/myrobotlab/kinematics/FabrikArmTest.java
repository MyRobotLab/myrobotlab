package org.myrobotlab.kinematics;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.service.InMoov2Arm;
import org.myrobotlab.test.AbstractTest;

public class FabrikArmTest extends AbstractTest {

  private static FabrikArm leftArm() {
    FabrikArm arm = new FabrikArm("i01.leftArm");
    Assert.assertTrue(arm.applyJointFrames(InMoov2Arm.getDefaultJointFrames("i01", "left"), InMoov2Arm.getDefaultEndEffector("left")));
    return arm;
  }

  @Test
  public void testRestPalmMatchesDh() {
    FabrikArm fabrik = leftArm();
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    Point fp = fabrik.getPalmPosition();
    Point dp = dh.getPalmPosition();
    Assert.assertEquals("rest X", dp.getX(), fp.getX(), 1e-6);
    Assert.assertEquals("rest Y", dp.getY(), fp.getY(), 1e-6);
    Assert.assertEquals("rest Z", dp.getZ(), fp.getZ(), 1e-6);
  }

  @Test
  public void testForwardKinematicsTracksDhAcrossJoints() {
    FabrikArm fabrik = leftArm();
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    double[] rests = { InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST };
    for (int j = 0; j < 4; j++) {
      for (double servo = dh.getLink(j).servoMin; servo <= dh.getLink(j).servoMax; servo += 20) {
        dh.getLink(j).setFromServoDegrees(servo);
        fabrik.setFromServoDegrees(j, servo);
        double err = fabrik.getPalmPosition().distanceTo(dh.getPalmPosition());
        Assert.assertEquals("joint " + j + " servo " + servo, 0.0, err, 1e-5);
      }
      dh.getLink(j).setFromServoDegrees(rests[j]);
      fabrik.setFromServoDegrees(j, rests[j]);
    }
  }

  @Test
  public void testServoRoundTrip() {
    FabrikArm arm = leftArm();
    for (int i = 0; i < 4; i++) {
      JointFrame frame = arm.getFrame(i);
      for (double servo = frame.servoMin; servo <= frame.servoMax; servo += 10) {
        arm.setFromServoDegrees(i, servo);
        Assert.assertEquals("joint " + i + " at " + servo, servo, arm.toServoDegrees(i), 1e-6);
      }
    }
  }

  @Test
  public void testMoveToNearRest() {
    FabrikArm arm = leftArm();
    arm.centerAllJoints();
    Point start = arm.getPalmPosition();
    Point goal = new Point(start.getX() + 0.03, start.getY(), start.getZ());
    boolean ok = arm.moveToGoal(goal);
    double err = arm.distanceToGoal(goal);
    Assert.assertTrue("FABRIK should get within 3 mm of a 3 cm X jog, err=" + err + " ok=" + ok, err < 0.003);
  }

  /**
   * Goals taken from forward kinematics are reachable. The old CCD ±25° cap
   * left ~0.15 m residual on these; DLS polish should land within 3 mm.
   */
  @Test
  public void testMoveToKnownReachablePoses() {
    FabrikArm arm = leftArm();
    double restO = InMoov2Arm.OMOPLATE_SERVO_REST;
    double restS = InMoov2Arm.SHOULDER_SERVO_REST;
    double restR = InMoov2Arm.ROTATE_SERVO_REST;
    double restB = InMoov2Arm.BICEP_SERVO_REST;
    double[][] poses = { { 70, restS, restR, restB }, { restO, 90, restR, restB }, { restO, 140, restR, restB }, { restO, restS, restR, 70 }, { restO, restS, 50, 70 } };
    String[] labels = { "omoplate raised", "shoulder mid", "shoulder high", "bicep bent", "rotate + bicep" };
    for (int p = 0; p < poses.length; p++) {
      arm.centerAllJoints();
      for (int j = 0; j < 4; j++) {
        arm.setFromServoDegrees(j, poses[p][j]);
      }
      Point goal = arm.getPalmPosition();
      arm.centerAllJoints();
      boolean ok = arm.moveToGoal(goal);
      double err = arm.distanceToGoal(goal);
      Assert.assertTrue(labels[p] + " err=" + err + " m ok=" + ok, err < 0.003);
    }
  }

  @Test
  public void testCalibrateResidualIsZero() {
    FabrikArm arm = new FabrikArm("shifted");
    java.util.List<JointFrame> frames = InMoov2Arm.getDefaultJointFrames("i01", "left");
    Point shift = new Point(0.30, 1.40, -0.02);
    for (JointFrame frame : frames) {
      frame.origin = frame.origin.add(shift);
    }
    Point wrist = InMoov2Arm.getDefaultEndEffector("left").add(shift);
    Assert.assertTrue(arm.applyJointFrames(frames, wrist));
    Assert.assertEquals(0.0, arm.getPalmPosition().distanceTo(wrist), 1e-9);
  }
}
