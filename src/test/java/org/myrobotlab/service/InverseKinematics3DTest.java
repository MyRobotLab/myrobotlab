package org.myrobotlab.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.test.AbstractTest;
import org.slf4j.Logger;

public class InverseKinematics3DTest extends AbstractTest {

  static String arm = "myArm";

  public final static Logger log = LoggerFactory.getLogger(InverseKinematics3DTest.class);

  @Before
  public void setUp() {
    // LoggingFactory.init("WARN");
  }

  @Test
  public void testForwardKinematics() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InMoovArm ia = new InMoovArm("i01");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik3d.centerAllJoints(arm);
    log.info("{}", ik3d.getCurrentArm(arm).getPalmPosition());
  }

  @Test
  public void testIK3D() throws Exception {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d", "InverseKinematics3D");
    // InMoovArm ia = new InMoovArm("i01");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    // start from a centered joint configuration so we can iterate without
    // loosing rank
    // in our jacobian!
    ik3d.centerAllJoints(arm);
    ik3d.moveTo(arm, 100.0, 0.0, 50.0);
    Point p = ik3d.currentPosition(arm);
    double[][] positions = ik3d.createJointPositionMap(arm);
    int x = positions[0].length;
    int y = positions.length;
    for (int j = 0; j < y; j++) {
      for (int i = 0; i < x; i++) {
        log.info(positions[j][i] + " ");
      }

    }
    // Last point:
    log.warn("Last Point: " + p.toString());
    // TODO: this doesn't actually assert the position was reached! ouch.
    Assert.assertNotNull(p);
  }

  /**
   * Constructor DH pose is InMoov servo rest. Classic hanging thetas
   * (-90, 90, 0, 90) still place the palm at (80, −600, 0) mm with default
   * lengths and no tool offset.
   */
  @Test
  public void testInMoovLeftArmHangingPose() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    dh.getLink(0).setTheta(MathUtils.degToRad(-90));
    dh.getLink(1).setTheta(MathUtils.degToRad(90));
    dh.getLink(2).setTheta(MathUtils.degToRad(0));
    dh.getLink(3).setTheta(MathUtils.degToRad(90));
    Point p = dh.getPalmPosition();
    log.info("Hanging palm {}", p);
    Assert.assertEquals("DH X (shoulder width)", 80.0, p.getX(), 1.0);
    Assert.assertEquals("DH Y (hanging down)", -600.0, p.getY(), 1.0);
    Assert.assertEquals("DH Z (no forward offset)", 0.0, p.getZ(), 1.0);
  }

  @Test
  public void testRestPoseServoMapping() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    Assert.assertEquals(MathUtils.degToRad(-80.0), dh.getLink(0).getTheta(), 1e-6);
    Assert.assertEquals(MathUtils.degToRad(75.0), dh.getLink(1).getTheta(), 1e-6);
    Assert.assertEquals(MathUtils.degToRad(90.0), dh.getLink(2).getTheta(), 1e-6);
    Assert.assertEquals(MathUtils.degToRad(90.0), dh.getLink(3).getTheta(), 1e-6);
    Assert.assertEquals(InMoov2Arm.OMOPLATE_SERVO_REST, MathUtils.radToDeg(dh.getLink(0).getTheta()) + dh.getLink(0).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.SHOULDER_SERVO_REST, MathUtils.radToDeg(dh.getLink(1).getTheta()) + dh.getLink(1).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.ROTATE_SERVO_REST, MathUtils.radToDeg(dh.getLink(2).getTheta()) + dh.getLink(2).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.BICEP_SERVO_REST, MathUtils.radToDeg(dh.getLink(3).getTheta()) + dh.getLink(3).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.OMOPLATE_SERVO_MIN, MathUtils.radToDeg(dh.getLink(0).getMin()) + dh.getLink(0).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.OMOPLATE_SERVO_MAX, MathUtils.radToDeg(dh.getLink(0).getMax()) + dh.getLink(0).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.SHOULDER_SERVO_MIN, MathUtils.radToDeg(dh.getLink(1).getMin()) + dh.getLink(1).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.SHOULDER_SERVO_MAX, MathUtils.radToDeg(dh.getLink(1).getMax()) + dh.getLink(1).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.ROTATE_SERVO_MIN, MathUtils.radToDeg(dh.getLink(2).getMin()) + dh.getLink(2).getOffset(), 1e-6);
    Assert.assertEquals(InMoov2Arm.ROTATE_SERVO_MAX, MathUtils.radToDeg(dh.getLink(2).getMax()) + dh.getLink(2).getOffset(), 1e-6);
  }

  @Test
  public void testCenterAllJointsInServoRange() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    dh.centerAllJoints();
    double omo = MathUtils.radToDeg(dh.getLink(0).getTheta()) + dh.getLink(0).getOffset();
    double shoulder = MathUtils.radToDeg(dh.getLink(1).getTheta()) + dh.getLink(1).getOffset();
    double rotate = MathUtils.radToDeg(dh.getLink(2).getTheta()) + dh.getLink(2).getOffset();
    double bicep = MathUtils.radToDeg(dh.getLink(3).getTheta()) + dh.getLink(3).getOffset();
    Assert.assertTrue("omoplate servo " + omo, omo >= InMoov2Arm.OMOPLATE_SERVO_MIN && omo <= InMoov2Arm.OMOPLATE_SERVO_MAX);
    Assert.assertTrue("shoulder servo " + shoulder, shoulder >= InMoov2Arm.SHOULDER_SERVO_MIN && shoulder <= InMoov2Arm.SHOULDER_SERVO_MAX);
    Assert.assertTrue("rotate servo " + rotate, rotate >= InMoov2Arm.ROTATE_SERVO_MIN && rotate <= InMoov2Arm.ROTATE_SERVO_MAX);
    Assert.assertTrue("bicep servo " + bicep, bicep >= InMoov2Arm.BICEP_SERVO_MIN && bicep <= InMoov2Arm.BICEP_SERVO_MAX);
  }

  @Test
  public void testWorldFrameRoundTrip() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-world", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    Point origin = new Point(0.20, 1.30, 0.05);
    Point ikPalm = ik3d.currentPosition(arm);
    Point wristWorld = new Point(origin.getX() + ikPalm.getX() / InverseKinematics3D.IK_MM_PER_JME_METER,
        origin.getY() + ikPalm.getY() / InverseKinematics3D.IK_MM_PER_JME_METER, origin.getZ() + ikPalm.getZ() / InverseKinematics3D.IK_MM_PER_JME_METER);

    Point scale = ik3d.calibrateToSimulator(origin, ikPalm, wristWorld);
    Assert.assertEquals(InverseKinematics3D.IK_MM_PER_JME_METER, scale.getX(), 1e-6);
    Assert.assertEquals(InverseKinematics3D.IK_MM_PER_JME_METER, scale.getY(), 1e-6);
    Assert.assertEquals(InverseKinematics3D.IK_MM_PER_JME_METER, scale.getZ(), 1e-6);

    Point world = ik3d.toWorldFrame(ikPalm);
    Assert.assertEquals(0.0, world.distanceTo(wristWorld), 1e-9);
    Point ikAgain = ik3d.toIkFrame(wristWorld);
    Assert.assertEquals(0.0, ikAgain.distanceTo(ikPalm), 1e-6);
    Assert.assertEquals(0.0, ik3d.currentPositionWorld(arm).distanceTo(wristWorld), 1e-6);
  }

  /**
   * Wrist-forward Z is a last-frame tool offset, not a fitted DH origin. Origin
   * stays on the omoplate node.
   */
  @Test
  public void testOmoplateOriginAndToolOffsetMatchesWrist() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-z", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    Point omoplate = new Point(0.20, 1.30, 0.05);
    Point ikPalm = ik3d.currentPosition(arm);
    Point wristWorld = new Point(omoplate.getX() + ikPalm.getX() / InverseKinematics3D.IK_MM_PER_JME_METER,
        omoplate.getY() + ikPalm.getY() / InverseKinematics3D.IK_MM_PER_JME_METER, 0.22);

    ik3d.calibrateToWorld(omoplate.getX(), omoplate.getY(), omoplate.getZ());
    Point tool = ik3d.fitToolOffsetFromWorld(arm, wristWorld);
    Assert.assertNotNull(tool);
    Point world = ik3d.currentPositionWorld(arm);
    Assert.assertEquals("world follows wrist", 0.0, world.distanceTo(wristWorld), 1e-6);
    Assert.assertEquals("origin X is omoplate", 0.20, ik3d.getWorldOrigin().getX(), 1e-9);
    Assert.assertEquals("origin Z is omoplate not wrist", 0.05, ik3d.getWorldOrigin().getZ(), 1e-9);
  }

  @Test
  public void testToolOffsetRotatesWithArm() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-tool", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    Point omoplate = new Point(0.20, 1.30, 0.05);
    Point ikPalm = ik3d.currentPosition(arm);
    Point wristWorld = new Point(omoplate.getX() + ikPalm.getX() / InverseKinematics3D.IK_MM_PER_JME_METER,
        omoplate.getY() + ikPalm.getY() / InverseKinematics3D.IK_MM_PER_JME_METER, omoplate.getZ() + ikPalm.getZ() / InverseKinematics3D.IK_MM_PER_JME_METER + 0.05);
    ik3d.calibrateToWorld(omoplate.getX(), omoplate.getY(), omoplate.getZ());
    ik3d.fitToolOffsetFromWorld(arm, wristWorld);
    Point restWorld = ik3d.currentPositionWorld(arm);
    dh.getLink(3).setTheta(MathUtils.degToRad(135.0));
    Point movedWorld = ik3d.toWorldFrame(dh.getPalmPosition());
    Assert.assertTrue("EE moved with bicep", movedWorld.distanceTo(restWorld) > 0.01);
    Point tool = dh.getToolOffset();
    Point with = dh.getPalmPosition();
    dh.setToolOffset(null);
    Point without = dh.getPalmPosition();
    dh.setToolOffset(tool);
    Assert.assertEquals("tool length conserved", tool.magnitude(), with.distanceTo(without), 1e-6);
  }

  @Test
  public void testFitVinMoovLinkLengths() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-fitlen", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    Point omoplate = new Point(0, 0, 0);
    Point shoulder = new Point(0.05, 0, 0);
    Point rotate = new Point(0.05, 0, 0.09);
    Point bicep = new Point(0.05, -0.25, 0.09);
    Point wrist = new Point(0.05, -0.50, 0.09);
    Assert.assertTrue(ik3d.fitVinMoovLinkLengths(arm, omoplate, shoulder, rotate, bicep, wrist));
    Assert.assertEquals(50.0, dh.getLink(0).getA(), 0.1);
    Assert.assertEquals(90.0, dh.getLink(1).getD(), 0.1);
    Assert.assertEquals(250.0, dh.getLink(2).getD(), 0.1);
    Assert.assertEquals(250.0, dh.getLink(3).getA(), 0.1);
  }

  @Test
  public void testOmoplateDeltaPreservesPalmZ() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    Point rest = dh.getPalmPosition();
    dh.getLink(0).setTheta(MathUtils.degToRad(-50.0));
    Point moved = dh.getPalmPosition();
    Assert.assertEquals("omoplate rotates about DH Z", rest.getZ(), moved.getZ(), 1.0);
    Assert.assertTrue(Math.abs(moved.getX() - rest.getX()) + Math.abs(moved.getY() - rest.getY()) > 10.0);
  }

  @Test
  public void testWorldFrameXFlip() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-xflip", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    Point origin = new Point(0.20, 1.30, 0.05);
    Point ikPalm = ik3d.currentPosition(arm);
    Point wristWorld = new Point(origin.getX() - ikPalm.getX() / InverseKinematics3D.IK_MM_PER_JME_METER,
        origin.getY() + ikPalm.getY() / InverseKinematics3D.IK_MM_PER_JME_METER, origin.getZ() + ikPalm.getZ() / InverseKinematics3D.IK_MM_PER_JME_METER);

    Point scale = ik3d.calibrateToSimulator(origin, ikPalm, wristWorld);
    Assert.assertEquals(-InverseKinematics3D.IK_MM_PER_JME_METER, scale.getX(), 1e-6);
    Assert.assertEquals(InverseKinematics3D.IK_MM_PER_JME_METER, scale.getY(), 1e-6);
    Assert.assertEquals(0.0, ik3d.toWorldFrame(ikPalm).distanceTo(wristWorld), 1e-9);
  }

  @Test
  public void testMoveToWorldFrame() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-moveworld", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik3d.calibrateToWorld(0.20, 1.30, 0.05);
    ik3d.centerAllJoints(arm);
    Point startWorld = ik3d.currentPositionWorld(arm);
    double targetY = startWorld.getY() + 0.02;
    ik3d.moveTo(startWorld.getX(), targetY, startWorld.getZ());
    Point reached = ik3d.currentPositionWorld(arm);
    log.info("moveTo world start {} reached {}", startWorld, reached);
    Assert.assertNotNull(ik3d.worldPosition);
    Assert.assertEquals(0.0, ik3d.worldPosition.distanceTo(reached), 1e-9);
    Assert.assertEquals("world Y after moveTo", targetY, reached.getY(), 0.005);
    Assert.assertEquals("world X after moveTo", startWorld.getX(), reached.getX(), 0.005);
    Assert.assertEquals("world Z after moveTo", startWorld.getZ(), reached.getZ(), 0.005);
  }

  @Test
  public void testComputePositionFromServos() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-fromservos", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    Runtime.setAllVirtual(true);
    String[] names = new String[] { "i01.leftArm.omoplate", "i01.leftArm.shoulder", "i01.leftArm.rotate", "i01.leftArm.bicep" };
    double[] servoPos = new double[] { 10.0, 30.0, 90.0, 0.0 };
    for (int i = 0; i < names.length; i++) {
      Servo servo = (Servo) Runtime.start(names[i], "Servo");
      servo.moveTo(servoPos[i]);
    }
    Point world = ik3d.computePositionFromServos();
    Assert.assertNotNull(world);
    Assert.assertEquals(MathUtils.degToRad(-80.0), dh.getLink(0).getTheta(), 1e-6);
    Assert.assertEquals(MathUtils.degToRad(75.0), dh.getLink(1).getTheta(), 1e-6);
    Assert.assertEquals(MathUtils.degToRad(90.0), dh.getLink(2).getTheta(), 1e-6);
    Assert.assertEquals(MathUtils.degToRad(90.0), dh.getLink(3).getTheta(), 1e-6);
    Assert.assertNotNull(ik3d.worldPosition);
    // Uncalibrated overlay must be meters (DH mm / 1000), never ~5 m from mm-as-meters
    Assert.assertEquals("rest X m", 0.18, world.getX(), 0.05);
    Assert.assertEquals("rest Y m (hanging)", -0.56, world.getY(), 0.08);
    Assert.assertTrue("FK reach is an arm not a room: " + world, world.magnitude() < 1.0);
  }

  @Test
  public void testCalibrateFromWorldSamplesMatchesWrist() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-calsamples", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    Point ikPalm = ik3d.currentPosition(arm);
    Point omoplate = new Point(0.32, 2.616, -0.037);
    Point wrist = new Point(omoplate.getX() + ikPalm.getX() / InverseKinematics3D.IK_MM_PER_JME_METER, omoplate.getY() + ikPalm.getY() / InverseKinematics3D.IK_MM_PER_JME_METER,
        omoplate.getZ() + ikPalm.getZ() / InverseKinematics3D.IK_MM_PER_JME_METER);
    Point world = ik3d.calibrateFromWorldSamples(omoplate, wrist);
    Assert.assertNotNull(world);
    Assert.assertEquals("IK world matches sim wrist", 0.0, world.distanceTo(wrist), 1e-6);
  }

  @Test
  public void testCalibrateFromWorldSamplesYFlip() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-yflip", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    Point ikPalm = ik3d.currentPosition(arm);
    Point omoplate = new Point(0.20, 1.45, 0.05);
    Point wrist = new Point(omoplate.getX() + ikPalm.getX() / InverseKinematics3D.IK_MM_PER_JME_METER, omoplate.getY() - ikPalm.getY() / InverseKinematics3D.IK_MM_PER_JME_METER,
        omoplate.getZ() + ikPalm.getZ() / InverseKinematics3D.IK_MM_PER_JME_METER);
    Point scale = ik3d.inferWorldScale(ikPalm, wrist.subtract(omoplate));
    Assert.assertEquals(-InverseKinematics3D.IK_MM_PER_JME_METER, scale.getY(), 1e-6);
    Point world = ik3d.calibrateFromWorldSamples(omoplate, wrist);
    Assert.assertEquals("Y-flipped mesh still matches", 0.0, world.distanceTo(wrist), 1e-6);
  }

  @Test
  public void testRejectHugeFittedLinkLength() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-hugelen", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    double a0 = dh.getLink(0).getA();
    Point omoplate = new Point(0, 0, 0);
    Point shoulder = new Point(2.8, 0, 0);
    Assert.assertFalse(ik3d.fitVinMoovLinkLengths(arm, omoplate, shoulder, null, null, null));
    Assert.assertEquals(a0, dh.getLink(0).getA(), 1e-9);
  }

  @Test
  public void testParseArmLinkName() {
    String[] parsed = InverseKinematics3D.parseArmLinkName("i01.leftArm.omoplate");
    Assert.assertArrayEquals(new String[] { "i01", "left" }, parsed);
  }

  /**
   * Compute from servos / MoveTo publish world-frame palm; JME overlay listens
   * so the simulator green marker stays in sync without starting the GL app.
   */
  @Test
  public void testAttachPublishesWorldPositionToSimulator() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-jme", "InverseKinematics3D");
    JMonkeyEngine jme = (JMonkeyEngine) Runtime.create("jme-overlay", "JMonkeyEngine");
    ik3d.attach(jme);
    Assert.assertTrue(ik3d.hasSubscribed(jme.getFullName(), "publishWorldPosition"));
    Point world = new Point(0.11, 1.22, 0.33);
    jme.onWorldPosition(world);
    Assert.assertNotNull(jme.ikLeftHandWorld);
    Assert.assertEquals(0.11f, jme.ikLeftHandWorld.x, 1e-5);
    Assert.assertEquals(1.22f, jme.ikLeftHandWorld.y, 1e-5);
    Assert.assertEquals(0.33f, jme.ikLeftHandWorld.z, 1e-5);
  }

}
