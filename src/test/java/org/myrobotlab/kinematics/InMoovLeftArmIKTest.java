package org.myrobotlab.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov2Arm;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class InMoovLeftArmIKTest extends AbstractTest {
  public final static Logger log = LoggerFactory.getLogger(InMoovLeftArmIKTest.class);

  /** Joints are commanded in servo degrees now — theta is the mesh angle. */
  private void moveArm(DHRobotArm arm, double omoplate, double shoulder, double rotate, double bicep) {
    arm.getLink(0).setFromServoDegrees(omoplate);
    arm.getLink(1).setFromServoDegrees(shoulder);
    arm.getLink(2).setFromServoDegrees(rotate);
    arm.getLink(3).setFromServoDegrees(bicep);
  }

  @Test
  public void testDHArm() {

    DHRobotArm arm = InMoov2Arm.getDHRobotArm("i01", "left");

    // well known configurations, in servo degrees
    moveArm(arm, InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST);
    Point rest = arm.getPalmPosition();
    log.info("Rest : {}", rest);

    moveArm(arm, 70, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST);
    Point omoplateRaised = arm.getPalmPosition();
    log.info("Omoplate raised : {}", omoplateRaised);

    moveArm(arm, InMoov2Arm.OMOPLATE_SERVO_REST, 90, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST);
    Point shoulderMid = arm.getPalmPosition();
    log.info("Shoulder mid : {}", shoulderMid);

    moveArm(arm, InMoov2Arm.OMOPLATE_SERVO_REST, 140, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST);
    Point shoulderHigh = arm.getPalmPosition();
    log.info("Shoulder high : {}", shoulderHigh);

    moveArm(arm, InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, 70);
    Point bicepBent = arm.getPalmPosition();
    log.info("Bicep bent : {}", bicepBent);

    moveArm(arm, InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, 50, 70);
    Point rotateBicep = arm.getPalmPosition();
    log.info("Rotate min bicep bent : {}", rotateBicep);

    Point[] goals = { rest, omoplateRaised, shoulderMid, shoulderHigh, bicepBent, rotateBicep };
    String[] labels = { "rest", "omoplate raised", "shoulder mid", "shoulder high", "bicep bent", "rotate + bicep" };
    for (int i = 0; i < goals.length; i++) {
      arm.centerAllJoints();
      assertTrue("IK " + labels[i] + " (missed by " + arm.distanceToGoal(goals[i]) + " m)", arm.moveToGoal(goals[i]));
      assertEquals("IK " + labels[i] + " residual", 0.0, arm.distanceToGoal(goals[i]), arm.getErrorThreshold());
    }
  }

  /**
   * Raising the omoplate must swing the hand out to the side while leaving the
   * forward axis alone — the joint really does turn about the forward axis.
   */
  @Test
  public void testOmoplateSwingsLaterally() {
    DHRobotArm arm = InMoov2Arm.getDHRobotArm("i01", "left");
    Point rest = arm.getPalmPosition();
    arm.getLink(0).setFromServoDegrees(InMoov2Arm.OMOPLATE_SERVO_MAX);
    Point raised = arm.getPalmPosition();
    log.info("omoplate rest {} raised {}", rest, raised);
    assertEquals("omoplate rotation keeps Z", rest.getZ(), raised.getZ(), 1e-9);
    assertTrue("omoplate moves the hand outward in X", raised.getX() > rest.getX() + 0.05);
    assertTrue("omoplate raises the hand", raised.getY() > rest.getY() + 0.01);
  }

  /** Increasing the shoulder servo brings the hand forward, on both arms. */
  @Test
  public void testShoulderSwingsForward() {
    for (String side : new String[] { "left", "right" }) {
      DHRobotArm arm = InMoov2Arm.getDHRobotArm("i01", side);
      Point rest = arm.getPalmPosition();
      arm.getLink(1).setFromServoDegrees(InMoov2Arm.SHOULDER_SERVO_REST + 60);
      Point forward = arm.getPalmPosition();
      log.info("{} shoulder rest {} forward {}", side, rest, forward);
      assertTrue(side + " shoulder moves the hand forward (+Z)", forward.getZ() > rest.getZ() + 0.10);
    }
  }
}
