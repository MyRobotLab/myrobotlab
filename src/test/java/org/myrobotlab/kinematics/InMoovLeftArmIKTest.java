package org.myrobotlab.kinematics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.InMoovArm;
import org.myrobotlab.service.InverseKinematics3D;
import org.slf4j.Logger;

public class InMoovLeftArmIKTest {
  public final static Logger log = LoggerFactory.getLogger(InMoovLeftArmIKTest.class);

  @Test
  public void testDHArm() {

    // LoggingFactory.init("INFO");
    DHRobotArm arm = InMoovArm.getDHRobotArm("i01", "left");
    //  Point palm = arm.getPalmPosition();
    //  System.out.println(palm);
    // well known configurations.

    // arm down to the side
    moveArm(arm, -90, 90, 90, 90);
    Point one = arm.getPalmPosition();

    System.out.println("Down to the side : " + one);

    // this should be arm directly out to the side.
    moveArm(arm, 0, 90, 90, 90);
    Point two = arm.getPalmPosition();

    System.out.println("Out to the left side : " + arm.getPalmPosition());

    // this configuration is straight ahead.
    moveArm(arm, -90, 0, 90, 90);
    Point three = arm.getPalmPosition();

    System.out.println("straight in front : " + arm.getPalmPosition());
    
    // straight up configuration.
    moveArm(arm, -90, -90, 90, 90);
    Point four = arm.getPalmPosition();

    System.out.println("Straight up : " + arm.getPalmPosition());

    // arm down to the side, but bicep bent forward.
    moveArm(arm, -90, 90, 0 , 180);
    Point five = arm.getPalmPosition();

    System.out.println("Down to the side bicep bent forward : " + arm.getPalmPosition());

    // arm down to the side, bicep up, rotate directly away from inmoov..
    moveArm(arm, -90, 90, -90, 180);
    Point six = arm.getPalmPosition();

    System.out.println("Down to the side bicep up and pointing to the left : " + arm.getPalmPosition());

   // InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    
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

  private void moveArm(DHRobotArm arm, double omoplate, double shoulder, double rotate, double bicep) {
    arm.getLink(0).setTheta(MathUtils.degToRad(omoplate));
    arm.getLink(1).setTheta(MathUtils.degToRad(shoulder));
    arm.getLink(2).setTheta(MathUtils.degToRad(rotate));
    arm.getLink(3).setTheta(MathUtils.degToRad(bicep));
  }



}
