import org.junit.Test;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.InMoovArm;
import org.myrobotlab.service.InverseKinematics3D;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

import marytts.util.math.MathUtils;

public class LloydIKTest {
  public final static Logger log = LoggerFactory.getLogger(InMoovArm.class);

  @Test
  public void testIK() {

    LoggingFactory.init("INFO");

    // Ok.,., i want to start an IK service.. and play around with it.
    // and then compare a mock output from controller position / orientation to
    // how the arm reacts.
    InverseKinematics3D leftIK = (InverseKinematics3D) Runtime.start("leftIK", "InverseKinematics3D");
    leftIK.setCurrentArm(InMoovArm.getDHRobotArm());
    leftIK.centerAllJoints();
    Point position = leftIK.currentPosition();
    log.info("Left IK center position is : {}", position);

    int numLinks = leftIK.getCurrentArm().getNumLinks();
    // let's iterate the joints. set the approproate angles.. and validate the
    // position to make sure our math is right.
    // test angles
    // IK "rest" input angles
    double omoplate = MathUtils.degrees2radian(-90);
    // arm straight down
    double shoulder = MathUtils.degrees2radian(90);
    // arm straight forward
    // double shoulder = MathUtils.degrees2radian(180);
    double rotate = MathUtils.degrees2radian(0);
    double bicep = MathUtils.degrees2radian(90);

    // // InMoov "servo rest" angles.
    // double omoplateIM = MathUtils.degrees2radian(0);
    // double shoulderIM = MathUtils.degrees2radian(90);
    // double rotateIM = MathUtils.degrees2radian(90);
    // double bicepIM = MathUtils.degrees2radian(0);
    //
    //
    // double omoplateDelta = -90;
    // double shoulderDelta = -90;
    // double rotateDelta = 90;
    // double bicepDelta = -90;

    DHRobotArm arm = leftIK.getCurrentArm();

    // centered ..
    for (DHLink l : arm.getLinks()) {
      System.out.println(l);
    }

    arm.getLink(0).setTheta(omoplate);
    arm.getLink(1).setTheta(shoulder);
    arm.getLink(2).setTheta(rotate);
    arm.getLink(3).setTheta(bicep);
    position = arm.getPalmPosition();
    // What are all the current angles?
    printArmDetails(arm);
    log.info("Left IK rest position is : {}", position);

    // for iteration.. let's try to solve it from center position
    leftIK.centerAllJoints();

    // (x=-0.272, y=-0.338, z=-0.274 (should be close to rest position +/-)
    // double x = -0.272;
    // double y = -0.338;
    // double z = -0.274;

    double x = -0.252;
    double y = -0.308;
    double z = -0.274;

    // scale from meters to mm.
    // TODO: z axis is reversed between the reference frames?! i'm confused.
    leftIK.createInputScale(1000.0, 1000.0, -1000.0);

    leftIK.moveTo(x, y, z);

    log.info("After Move To Position: {}", leftIK.currentPosition());

    // now we need to print the angles leftIK.g
    printArmDetails(arm);

    log.info("Done.");
  }

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
}
