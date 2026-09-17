package org.myrobotlab.kinematics;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.InMoov2Arm;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class DHRobotArmTest extends AbstractTest {
  public final static Logger log = LoggerFactory.getLogger(DHRobotArmTest.class);
  
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

  public double degToRad(double degrees) {
    return degrees * Math.PI / 180.0;
  }

  @Before
  public void setUp() throws Exception {
    // LoggingFactory.init("WARN");
  }

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
    testArm.setErrorThreshold(2.0);
    Matrix jacobian = testArm.getJacobian();
    Assert.assertEquals(3, jacobian.getNumRows());
    Assert.assertEquals(testArm.getNumLinks(), jacobian.getNumCols());
    // the pseudo inverse maps a Cartesian delta to a joint delta, so it must be
    // numLinks x 3 - not the other way round
    Matrix jInverse = testArm.getJInverse();
    Assert.assertEquals(testArm.getNumLinks(), jInverse.getNumRows());
    Assert.assertEquals(3, jInverse.getNumCols());
    log.info("{}", jInverse);

    Point goal = new Point(50, 50, 50, 0, 0, 0);
    testArm.moveToGoal(goal);
    log.info("residual {}", testArm.distanceToGoal(goal));

    int i = 0;
    for (DHLink link : testArm.getLinks()) {
      i++;
      log.info("Link : " + i + " " + link.getThetaDegrees());
    }
  }

  /** Damped least squares must not explode when the chain is singular. */
  @Test
  public void testSolverSurvivesUnreachableGoal() {
    DHRobotArm arm = InMoov2Arm.getDHRobotArm("i01", "left");
    Point unreachable = new Point(50, 50, 50);
    long start = System.currentTimeMillis();
    Assert.assertFalse("an unreachable goal must report failure", arm.moveToGoal(unreachable));
    long elapsed = System.currentTimeMillis() - start;
    log.info("gave up on an unreachable goal in {} ms", elapsed);
    Assert.assertTrue("must give up quickly, not grind through the iteration budget: " + elapsed + " ms", elapsed < 4000);
    for (DHLink link : arm.getLinks()) {
      Assert.assertFalse("theta must stay finite", Double.isNaN(link.getTheta()));
      Assert.assertTrue("theta must stay inside its limits", link.getTheta() <= link.getMax() + 1e-9 && link.getTheta() >= link.getMin() - 1e-9);
    }
  }

  @Test
  public void testDHLinkCopyOffset() {
    DHLink src = new DHLink("omoplate", 0, 40, 0, 0);
    src.setOffset(90.0);
    DHLink copy = new DHLink(src);
    assertEquals(90.0, copy.getOffset(), 1e-9);
    assertEquals("omoplate", copy.getName());
  }

  @Test
  public void testToolOffsetCopy() {
    DHRobotArm arm = createArm();
    arm.setToolOffset(1, 2, 3);
    DHRobotArm copy = new DHRobotArm(arm);
    assertEquals(1.0, copy.getToolOffset().getX(), 1e-9);
    assertEquals(2.0, copy.getToolOffset().getY(), 1e-9);
    assertEquals(3.0, copy.getToolOffset().getZ(), 1e-9);
  }

  @Test
  public void testBaseTransformAndAffineInverse() {
    DHRobotArm arm = InMoov2Arm.getDHRobotArm("i01", "left");
    Point local = arm.getPalmPositionLocal();
    arm.setBaseOrigin(0.2, 1.3, 0.05);
    Point world = arm.getPalmPosition();
    assertEquals(0.2 + local.getX(), world.getX(), 1e-9);
    assertEquals(1.3 + local.getY(), world.getY(), 1e-9);
    assertEquals(0.05 + local.getZ(), world.getZ(), 1e-9);
    Point back = arm.toLocalFrame(world);
    assertEquals(local.getX(), back.getX(), 1e-9);
    assertEquals(local.getY(), back.getY(), 1e-9);
    assertEquals(local.getZ(), back.getZ(), 1e-9);
  }

  /**
   * {@code rigid} and {@code toRollPitchYaw} must be inverses, so a calibration
   * saved with a rotated base reloads as the same base.
   */
  @Test
  public void testRigidRollPitchYawRoundTrips() {
    double roll = Math.toRadians(23);
    double pitch = Math.toRadians(-14);
    double yaw = Math.toRadians(37);
    Matrix m = Matrix.rigid(0.1, 0.2, 0.3, roll, pitch, yaw);
    double[] rpy = m.toRollPitchYaw();
    assertEquals(roll, rpy[0], 1e-9);
    assertEquals(pitch, rpy[1], 1e-9);
    assertEquals(yaw, rpy[2], 1e-9);
    Matrix again = Matrix.rigid(0.1, 0.2, 0.3, rpy[0], rpy[1], rpy[2]);
    for (int r = 0; r < 4; r++) {
      for (int c = 0; c < 4; c++) {
        assertEquals(m.elements[r][c], again.elements[r][c], 1e-9);
      }
    }
  }

  /** Right-handed rotations, unlike the deprecated {@code zRotation} family. */
  @Test
  public void testRotationsAreRightHanded() {
    Point spun = Matrix.rotationZ(Math.PI / 2).transformPoint(new Point(1, 0, 0));
    assertEquals("Rz(90) takes +X to +Y", 0.0, spun.getX(), 1e-9);
    assertEquals(1.0, spun.getY(), 1e-9);
    Point aboutAxis = Matrix.rotationAboutAxis(0, 0, 1, Math.PI / 2).transformPoint(new Point(1, 0, 0));
    assertEquals(0.0, aboutAxis.getX(), 1e-9);
    assertEquals(1.0, aboutAxis.getY(), 1e-9);
  }

  /** A frame built from an axis really has that axis as its Z column. */
  @Test
  public void testFrameFromZAxisIsOrthonormal() {
    double[] axis = { 0.3, -0.5, 0.81 };
    double len = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
    Matrix f = Matrix.frameFromZAxis(axis[0], axis[1], axis[2], 1, 2, 3);
    for (int r = 0; r < 3; r++) {
      assertEquals("z column is the axis", axis[r] / len, f.elements[r][2], 1e-9);
    }
    assertEquals(1.0, f.elements[0][3], 1e-9);
    // columns orthonormal
    for (int a = 0; a < 3; a++) {
      double dotSelf = 0;
      for (int r = 0; r < 3; r++) {
        dotSelf += f.elements[r][a] * f.elements[r][a];
      }
      assertEquals("column " + a + " is unit length", 1.0, dotSelf, 1e-9);
      for (int b = a + 1; b < 3; b++) {
        double dot = 0;
        for (int r = 0; r < 3; r++) {
          dot += f.elements[r][a] * f.elements[r][b];
        }
        assertEquals("columns " + a + "," + b + " orthogonal", 0.0, dot, 1e-9);
      }
    }
    Assert.assertNotNull(f.invertAffine());
  }

}
