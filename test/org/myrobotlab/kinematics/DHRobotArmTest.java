package org.myrobotlab.kinematics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DHRobotArmTest {
  public final static Logger log = LoggerFactory.getLogger(DHRobotArmTest.class);

  // @Test
  public void testDHArm() {

    // distance to common normal
    double d = 0.4;
    // comon normal lenght (sometimes called "a"
    double r = 0.2;
    // angle between X and X-1 axis
    double alpha = 90.0 * Math.PI / 180.0;
    // angle between Z and Z-1 axis
    double theta = 45.0 * Math.PI / 180.0;
    DHLink link1 = new DHLink(null, d, r, theta, alpha);

    // double d1=0;
    // double r1=0;
    // double theta1=0.0 * Math.PI / 180.0;
    // double alpha1=0.0 * Math.PI / 180.0;
    //
    // DHLink link2 = new DHLink(d1, r1, theta1, alpha1);

    DHRobotArm arm = new DHRobotArm();
    arm.addLink(link1);
    // arm.addLink(link2);

    // TODO: validate forward kinematcis
    Point coord = arm.getPalmPosition();
    // System.out.println("Theta = " + theta);
    // System.out.println(coord);
    //
    // double angle = 90;
    // link1.moveToAngle(angle);

    // you want to know where the hand is.
    // coord = arm.getPalmPosition();

    log.info("" + coord);
    // assertEquals(coord.toString(),"(2.0, 1.0, 1.0)" );

    assertEquals("(x=0.141421, y=0.141421, z=0.400000)", coord.toString());
  }

  @Test
  public void testJacobian() {
    log.info("testJacobian");
    DHRobotArm testArm = createInMoovLeftArm();
    Matrix jInverse = testArm.getJInverse();
    System.out.println(jInverse);
    // now, the deltaPosition array has the delta x,y,z coordinates
    // what's the instantaneous rate of change for each of those
    // compute the rate of change for this

    // ok.
    testArm.moveToGoal(new Point(50, 50, 50, 0, 0, 0));

    int i = 0;
    for (DHLink link : testArm.getLinks()) {
      i++;
      log.info("Link : " + i + " " + link.getThetaDegrees());
    }
  }

  public DHRobotArm createArm() {
    log.info("createArm");

    DHRobotArm arm = new DHRobotArm();
    // d , r, theta , alpha
    DHLink link1 = new DHLink(null, 0, 1, 45 * Math.PI / 180, 0);
    arm.addLink(link1);
    DHLink link2 = new DHLink(null, 0.0, 0.2, 45 * Math.PI / 180, 90 * Math.PI / 180);
    arm.addLink(link2);
    return arm;
  }

  public double degToRad(double degrees) {
    return degrees * Math.PI / 180.0;
  }

  public DHRobotArm createInMoovLeftArm() {
    log.info("createInMoovLeftArm");
    DHRobotArm arm = new DHRobotArm();
    // d , r, theta , alpha

    DHLink link1 = new DHLink(null, 200, 100, degToRad(0), degToRad(90));
    DHLink link2 = new DHLink(null, 0, 100, degToRad(-66), degToRad(-90));
    DHLink link3 = new DHLink(null, 50, 1, degToRad(47), degToRad(90));
    DHLink link4 = new DHLink(null, 100, 0, degToRad(-148), degToRad(90));
    DHLink link5 = new DHLink(null, 0, 100, degToRad(22), degToRad(180));

    arm.addLink(link1);
    arm.addLink(link2);
    arm.addLink(link3);
    arm.addLink(link4);
    arm.addLink(link5);

    return arm;
  }

}
