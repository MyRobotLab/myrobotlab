package org.myrobotlab.kinematics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.InMoov2Arm;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class InMoovLeftArmIKTest extends AbstractTest {
  public final static Logger log = LoggerFactory.getLogger(InMoovLeftArmIKTest.class);

  private void moveArm(DHRobotArm arm, double omoplate, double shoulder, double rotate, double bicep) {
    arm.getLink(0).setTheta(MathUtils.degToRad(omoplate));
    arm.getLink(1).setTheta(MathUtils.degToRad(shoulder));
    arm.getLink(2).setTheta(MathUtils.degToRad(rotate));
    arm.getLink(3).setTheta(MathUtils.degToRad(bicep));
  }

  @Test
  public void testDHArm() {

    // LoggingFactory.init("INFO");
    DHRobotArm arm = InMoov2Arm.getDHRobotArm("i01", "left");
    // Point palm = arm.getPalmPosition();
    // log.info(palm);
    // well known configurations.

    // rest (constructor pose)
    moveArm(arm, -80, 75, 90, 90);
    Point one = arm.getPalmPosition();

    log.info("Rest : " + one);

    // omoplate toward max
    moveArm(arm, -10, 75, 90, 90);
    Point two = arm.getPalmPosition();

    log.info("Omoplate raised : " + arm.getPalmPosition());

    // shoulder mid (servo 90)
    moveArm(arm, -80, 135, 90, 90);
    Point three = arm.getPalmPosition();

    log.info("Shoulder mid : " + arm.getPalmPosition());

    // shoulder max (servo 180)
    moveArm(arm, -80, 225, 90, 90);
    Point four = arm.getPalmPosition();

    log.info("Shoulder max : " + arm.getPalmPosition());

    // bicep bent
    moveArm(arm, -80, 75, 90, 180);
    Point five = arm.getPalmPosition();

    log.info("Bicep bent : " + arm.getPalmPosition());

    // rotate min + bicep bent
    moveArm(arm, -80, 75, 40, 180);
    Point six = arm.getPalmPosition();

    log.info("Rotate min bicep bent : " + arm.getPalmPosition());

    // InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d",
    // "InverseKinematics3D");

    // ik3d.setCurrentArm("i01.leftArm", arm);
    arm.centerAllJoints();
    assertTrue(arm.moveToGoal(one));
    arm.centerAllJoints();
    assertTrue(arm.moveToGoal(two));
    arm.centerAllJoints();
    assertTrue(arm.moveToGoal(three));
    arm.centerAllJoints();
    assertTrue(arm.moveToGoal(four));
    arm.centerAllJoints();
    assertTrue(arm.moveToGoal(five));
    arm.centerAllJoints();
    assertTrue(arm.moveToGoal(six));
    // now get where the palm is

  }

}
