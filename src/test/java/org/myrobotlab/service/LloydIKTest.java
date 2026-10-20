package org.myrobotlab.service;

import org.junit.Ignore;
import org.junit.Test;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

import marytts.util.math.MathUtils;

@Ignore
public class LloydIKTest extends AbstractTest {

  public final static Logger log = LoggerFactory.getLogger(LloydIKTest.class);

  private void printArmDetails(DHRobotArm arm) {
    int numLinks = arm.getNumLinks();
    for (int i = 0; i < numLinks; i++) {
      DHLink link = arm.getLink(i);
      String name = link.getName();
      Point p = arm.getJointPosition(i);
      Double theta = MathUtils.radian2degrees(link.getTheta());
      // log.info("{}",link);
      log.info("Link : {} POS: {} THETA: {}", name, p, theta);
    }
  }

  @Test
  public void testIK() {

    // LoggingFactory.init("INFO");

    String partName = "myArm";

    // Ok.,., i want to start an IK service.. and play around with it.
    // and then compare a mock output from controller position / orientation to
    // how the arm reacts.
    InverseKinematics3D leftIK = (InverseKinematics3D) Runtime.start("leftIK", "InverseKinematics3D");
    leftIK.setCurrentArm(partName, InMoov2Arm.getDHRobotArm("i01", "left"));
    leftIK.centerAllJoints(partName);
    Point position = leftIK.currentPosition(partName);
    log.info("Left IK center position is : {}", position);

    int numLinks = leftIK.getCurrentArm(partName).getNumLinks();
    DHRobotArm arm = leftIK.getCurrentArm(partName);

    // centered ..
    for (DHLink l : arm.getLinks()) {
      System.out.println(l);
    }

    // theta is the mesh angle now, so servo rest is theta 0 on every joint
    arm.getLink(0).setFromServoDegrees(InMoov2Arm.OMOPLATE_SERVO_REST);
    arm.getLink(1).setFromServoDegrees(InMoov2Arm.SHOULDER_SERVO_REST);
    arm.getLink(2).setFromServoDegrees(InMoov2Arm.ROTATE_SERVO_REST);
    arm.getLink(3).setFromServoDegrees(InMoov2Arm.BICEP_SERVO_REST);
    position = arm.getPalmPosition();
    // What are all the current angles?
    printArmDetails(arm);
    log.info("Left IK rest position is : {}", position);

    // for iteration.. let's try to solve it from center position
    leftIK.centerAllJoints(partName);

    // (x=-0.272, y=-0.338, z=-0.274 (should be close to rest position +/-)
    // double x = -0.272;
    // double y = -0.338;
    // double z = -0.274;

    // solver is native meters in the simulator's world frame - no scaling
    Point goal = leftIK.currentPosition(partName);
    double x = goal.getX() + 0.02;
    double y = goal.getY() + 0.02;
    double z = goal.getZ();

    leftIK.moveTo(partName, x, y, z);

    log.info("After Move To Position: {}", leftIK.currentPosition(partName));

    // now we need to print the angles leftIK.g
    printArmDetails(arm);

    log.info("Done.");
  }
}