package org.myrobotlab.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.kinematics.JointFrame;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.config.InverseKinematics3DConfig;
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
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik3d.centerAllJoints(arm);
    log.info("{}", ik3d.getCurrentArm(arm).getPalmPosition());
    Assert.assertNotNull(ik3d.getCurrentArm(arm).getPalmPosition());
  }

  /**
   * The default arm is built from measured joint frames, not hand-tuned
   * Denavit-Hartenberg parameters.
   */
  @Test
  public void testDefaultArmIsBuiltFromJointFrames() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    Assert.assertTrue("arm geometry should be measured joint frames", dh.isMeasured());
    Assert.assertEquals(4, dh.getNumLinks());
    Assert.assertEquals("i01.leftArm.omoplate", dh.getLink(0).getName());
    Assert.assertEquals("i01.leftArm.bicep", dh.getLink(3).getName());
  }

  /**
   * Theta is the mesh rotation angle, so servo rest is theta zero on every joint
   * and one servo degree is one degree of theta. That is the whole joint-space
   * calibration and it is shared with the simulator's node mapper.
   */
  @Test
  public void testRestPoseIsThetaZeroAndServoMapRoundTrips() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    double[] rests = { InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST };
    for (int i = 0; i < 4; i++) {
      DHLink link = dh.getLink(i);
      Assert.assertEquals("link " + i + " rest theta is zero", 0.0, link.getThetaDegrees(), 1e-9);
      Assert.assertEquals("link " + i + " rest servo", rests[i], link.toServoDegrees(), 1e-9);
      Assert.assertEquals("link " + i + " slope magnitude is 1", 1.0, Math.abs(link.getServoSlope()), 1e-9);
    }
    // round trip every joint over its whole range
    for (int i = 0; i < 4; i++) {
      DHLink link = dh.getLink(i);
      for (double servo = link.servoMin; servo <= link.servoMax; servo += 5) {
        link.setFromServoDegrees(servo);
        Assert.assertEquals("link " + i + " servo round trip at " + servo, servo, link.toServoDegrees(), 1e-9);
      }
      link.setFromServoDegrees(rests[i]);
    }
  }

  /** Direction and scale must agree with the simulator's mapper, sign included. */
  @Test
  public void testServoMapMatchesSimulatorMapperSigned() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    org.myrobotlab.math.MapperLinear[] mappers = { InMoov2Arm.vinMoovArmMapper(InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.OMOPLATE_MESH_SLOPE_LEFT),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.SHOULDER_MESH_SLOPE),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.ROTATE_MESH_SLOPE_LEFT),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.BICEP_SERVO_REST, InMoov2Arm.BICEP_MESH_SLOPE) };
    double[] rests = { InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST };
    for (int i = 0; i < 4; i++) {
      DHLink link = dh.getLink(i);
      double servo = rests[i] + 25.0;
      link.setFromServoDegrees(servo);
      double meshDelta = mappers[i].calcOutput(servo) - mappers[i].calcOutput(rests[i]);
      Assert.assertEquals("link " + i + " signed theta must equal signed mesh angle", meshDelta, link.getThetaDegrees(), 1e-9);
      link.setFromServoDegrees(rests[i]);
    }
  }

  @Test
  public void testRestPoseHangsDownLikeVinMoovBind() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    Point rest = dh.getPalmPosition();
    log.info("rest palm {}", rest);
    Assert.assertTrue("rest palm hangs down, not straight up: " + rest, rest.getY() < -0.40);
    Assert.assertEquals("rest near hanging X", 0.12, rest.getX(), 0.05);
    Assert.assertEquals("rest near hanging Z", 0.0, rest.getZ(), 0.05);
  }

  /** Right arm mirrors in X; the rotation directions come from the mapper slopes. */
  @Test
  public void testRightArmMirrorsLeft() {
    Point left = InMoov2Arm.getDHRobotArm("i01", "left").getPalmPosition();
    Point right = InMoov2Arm.getDHRobotArm("i01", "right").getPalmPosition();
    Assert.assertEquals(-left.getX(), right.getX(), 1e-9);
    Assert.assertEquals(left.getY(), right.getY(), 1e-9);
    Assert.assertEquals(left.getZ(), right.getZ(), 1e-9);
  }

  /** The measured tool offset is the wrist relative to the elbow, not an artifact. */
  @Test
  public void testToolOffsetIsForearmSized() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    Point tool = dh.getToolOffset();
    Assert.assertNotNull(tool);
    Assert.assertEquals("tool offset is the forearm", InMoov2Arm.DH_BICEP_A, tool.magnitude(), 0.01);
  }

  @Test
  public void testCenterAllJointsInServoRange() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    dh.centerAllJoints();
    double[][] limits = { { InMoov2Arm.OMOPLATE_SERVO_MIN, InMoov2Arm.OMOPLATE_SERVO_MAX }, { InMoov2Arm.SHOULDER_SERVO_MIN, InMoov2Arm.SHOULDER_SERVO_MAX },
        { InMoov2Arm.ROTATE_SERVO_MIN, InMoov2Arm.ROTATE_SERVO_MAX }, { InMoov2Arm.BICEP_SERVO_MIN, InMoov2Arm.BICEP_SERVO_MAX } };
    for (int i = 0; i < 4; i++) {
      double servo = dh.getLink(i).toServoDegrees();
      Assert.assertTrue("link " + i + " servo " + servo, servo >= limits[i][0] - 1e-9 && servo <= limits[i][1] + 1e-9);
    }
  }

  /** Published joint angles are servo degrees and must never leave the servo range. */
  @Test
  public void testPublishedAnglesStayInServoRange() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-range", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    for (int i = 0; i < 4; i++) {
      // push each joint hard past both limits
      dh.getLink(i).setTheta(dh.getLink(i).getMax() + 5);
      Assert.assertTrue(dh.getLink(i).toServoDegrees() <= dh.getLink(i).servoMax + 1e-9);
      dh.getLink(i).setTheta(dh.getLink(i).getMin() - 5);
      Assert.assertTrue(dh.getLink(i).toServoDegrees() >= dh.getLink(i).servoMin - 1e-9);
      dh.getLink(i).setTheta(0);
    }
  }

  @Test
  public void testBaseTransformSharesJmeOrigin() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-base", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    Point local = new Point(ik3d.currentPosition(arm));
    Point omoplate = new Point(0.32, 2.616, -0.037);
    ik3d.calibrateToWorld(omoplate.getX(), omoplate.getY(), omoplate.getZ());
    Point world = ik3d.currentPosition(arm);
    Assert.assertEquals(omoplate.getX() + local.getX(), world.getX(), 1e-6);
    Assert.assertEquals(omoplate.getY() + local.getY(), world.getY(), 1e-6);
    Assert.assertEquals(omoplate.getZ() + local.getZ(), world.getZ(), 1e-6);
    double[][] map = ik3d.createJointPositionMap(arm);
    Assert.assertEquals("skeleton origin X is omoplate", omoplate.getX(), map[0][0], 1e-9);
    Assert.assertEquals("skeleton origin Y is omoplate", omoplate.getY(), map[0][1], 1e-9);
    Assert.assertEquals("skeleton origin Z is omoplate", omoplate.getZ(), map[0][2], 1e-9);
  }

  @Test
  public void testWorldFrameRoundTrip() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-world", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik3d.calibrateToWorld(0.20, 1.30, 0.05);
    Point world = ik3d.currentPositionWorld(arm);
    Point local = ik3d.toIkFrame(world);
    Assert.assertEquals(0.0, ik3d.toWorldFrame(local).distanceTo(world), 1e-9);
  }

  /**
   * Rebuilding from measured joints puts the model exactly on the sampled end
   * effector, and reproduces it for other joint angles too.
   */
  @Test
  public void testCalibrateFromJointFramesMatchesSample() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-cal", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));

    // pretend the rig sits 1.4 m up and 0.3 m to the left of the JME origin
    List<JointFrame> frames = InMoov2Arm.getDefaultJointFrames("i01", "left");
    Point shift = new Point(0.30, 1.40, -0.02);
    for (JointFrame frame : frames) {
      frame.origin = frame.origin.add(shift);
    }
    Point wrist = InMoov2Arm.getDefaultEndEffector("left").add(shift);

    Point world = ik3d.calibrateFromJointFrames(arm, frames, wrist);
    Assert.assertNotNull(world);
    Assert.assertEquals("model lands on the measured wrist", 0.0, world.distanceTo(wrist), 1e-9);
    Assert.assertEquals("base is the first measured joint", shift.getX(), ik3d.getWorldOrigin().getX(), 1e-9);
    Assert.assertEquals(shift.getY(), ik3d.getWorldOrigin().getY(), 1e-9);
  }

  @Test
  public void testMeasuredSegmentLengths() {
    List<JointFrame> frames = InMoov2Arm.getDefaultJointFrames("i01", "left");
    List<Double> segments = InverseKinematics3D.measuredSegmentLengths(frames, InMoov2Arm.getDefaultEndEffector("left"));
    Assert.assertEquals(4, segments.size());
    Assert.assertEquals("omoplate to shoulder", InMoov2Arm.DH_OMOPLATE_A, segments.get(0), 1e-4);
    Assert.assertEquals("shoulder to rotate", InMoov2Arm.DH_SHOULDER_D, segments.get(1), 1e-4);
    Assert.assertEquals("upper arm", InMoov2Arm.DH_ROTATE_D, segments.get(2), 1e-4);
    Assert.assertEquals("forearm", InMoov2Arm.DH_BICEP_A, segments.get(3), 1e-4);
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
  public void testGuiMoveToUsesPublishedWorldNumbers() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-gui-moveto", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik3d.calibrateToWorld(0.20, 1.30, 0.05);
    ik3d.centerAllJoints(arm);
    Point published = ik3d.worldPosition;
    Assert.assertNotNull(published);

    Point stayed = ik3d.moveTo(published.getX(), published.getY(), published.getZ());
    Assert.assertNotNull(stayed);
    Assert.assertEquals("sending displayed world X,Y,Z back stays put", 0.0, stayed.distanceTo(published), 0.003);

    Point nudged = ik3d.moveTo(published.getX(), published.getY() + 0.02, published.getZ());
    Assert.assertEquals("3-arg moveTo is world Y", published.getY() + 0.02, nudged.getY(), 0.005);
    Assert.assertEquals("3-arg moveTo is world X", published.getX(), nudged.getX(), 0.005);
  }

  @Test
  public void testComputePositionFromServos() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-fromservos", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    Runtime.setAllVirtual(true);
    String[] names = new String[] { "i01.leftArm.omoplate", "i01.leftArm.shoulder", "i01.leftArm.rotate", "i01.leftArm.bicep" };
    double[] servoPos = new double[] { InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST };
    for (int i = 0; i < names.length; i++) {
      Servo servo = (Servo) Runtime.start(names[i], "Servo");
      // Snap immediately. moveTo() only sets targetPos; currentInputPos stays
      // at the default rest (90) until TimeEncoder ticks, so FK would read 80°.
      servo.setPosition(servoPos[i]);
    }
    Point world = ik3d.computePositionFromServos();
    Assert.assertNotNull(world);
    for (int i = 0; i < 4; i++) {
      Assert.assertEquals("servo rest reads back as theta zero, link " + i, 0.0, dh.getLink(i).getThetaDegrees(), 1e-6);
    }
    Assert.assertNotNull(ik3d.worldPosition);
    Assert.assertEquals("rest X m", 0.12, world.getX(), 0.05);
    Assert.assertEquals("rest Y m (hanging)", -0.56, world.getY(), 0.08);
    Assert.assertTrue("FK reach is an arm not a room: " + world, world.magnitude() < 1.0);
  }

  /**
   * Round trip through {@code publishTelemetry} then
   * {@code computePositionFromServos} must be lossless — the old modulo-360 wrap
   * on published angles was not invertible.
   */
  @Test
  public void testTelemetryServoAnglesRoundTrip() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-telemetry", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    dh.centerAllJoints();
    double[] published = new double[4];
    double[] thetas = new double[4];
    for (int i = 0; i < 4; i++) {
      published[i] = dh.getLink(i).toServoDegrees();
      thetas[i] = dh.getLink(i).getTheta();
    }
    for (int i = 0; i < 4; i++) {
      dh.getLink(i).setFromServoDegrees(published[i]);
      Assert.assertEquals("link " + i + " theta survives the servo round trip", thetas[i], dh.getLink(i).getTheta(), 1e-9);
    }
  }

  @Test
  public void testComputePositionFromServosDoesNotRecalibrate() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-norecal", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik3d.calibrateToWorld(0.20, 1.30, 0.05);
    Point origin = ik3d.getWorldOrigin();
    Runtime.setAllVirtual(true);
    Servo omo = (Servo) Runtime.start("i01.leftArm.omoplate", "Servo");
    omo.setPosition(InMoov2Arm.OMOPLATE_SERVO_REST);
    ik3d.computePositionFromServos();
    Assert.assertEquals("origin X unchanged", origin.getX(), ik3d.getWorldOrigin().getX(), 1e-9);
    Assert.assertEquals("origin Y unchanged", origin.getY(), ik3d.getWorldOrigin().getY(), 1e-9);
    Assert.assertEquals("origin Z unchanged", origin.getZ(), ik3d.getWorldOrigin().getZ(), 1e-9);
  }

  @Test
  public void testParseArmLinkName() {
    String[] parsed = InverseKinematics3D.parseArmLinkName("i01.leftArm.omoplate");
    Assert.assertArrayEquals(new String[] { "i01", "left" }, parsed);
  }

  @Test
  public void testStraightLineWorldXFromCenter() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-xline", "InverseKinematics3D");
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    ik3d.setCurrentArm(arm, dh);
    ik3d.calibrateToWorld(0.20, 1.30, 0.05);
    ik3d.centerAllJoints(arm);
    Point start = ik3d.currentPositionWorld(arm);
    log.info("centerAllJoints world palm {}", start);
    Assert.assertNotNull(start);
    double[] deltas = new double[] { 0.03, 0.05, -0.03, -0.05 };
    for (double dx : deltas) {
      ik3d.centerAllJoints(arm);
      Point goal = new Point(start.getX() + dx, start.getY(), start.getZ());
      Point reached = ik3d.moveTo(goal.getX(), goal.getY(), goal.getZ());
      double err = reached.distanceTo(goal);
      log.info("X-line dx={} start {} goal {} reached {} err={}", dx, start, goal, reached, err);
      Assert.assertNotNull(ik3d.ikGoal);
      Assert.assertEquals("green/goal X", goal.getX(), ik3d.ikGoal.getX(), 1e-9);
      Assert.assertEquals("green/goal Y", goal.getY(), ik3d.ikGoal.getY(), 1e-9);
      Assert.assertEquals("green/goal Z", goal.getZ(), ik3d.ikGoal.getZ(), 1e-9);
      Assert.assertTrue("reach X-line dx=" + dx + " err=" + err + " reached=" + reached, err < 0.005);
      Assert.assertEquals("keep Y while jogging X", start.getY(), reached.getY(), 0.01);
      Assert.assertEquals("keep Z while jogging X", start.getZ(), reached.getZ(), 0.01);
    }
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
    Assert.assertTrue(ik3d.hasSubscribed(jme.getFullName(), "publishIkGoal"));
    Point world = new Point(0.11, 1.22, 0.33);
    jme.onWorldPosition(world);
    Assert.assertNotNull(jme.ikLeftHandWorld);
    Assert.assertEquals(0.11f, jme.ikLeftHandWorld.x, 1e-5);
    Assert.assertEquals(1.22f, jme.ikLeftHandWorld.y, 1e-5);
    Assert.assertEquals(0.33f, jme.ikLeftHandWorld.z, 1e-5);
    Point goal = new Point(0.20, 1.30, 0.40);
    jme.onIkGoal(goal);
    Assert.assertEquals("green locks to goal", 0.20f, jme.ikLeftHandWorld.x, 1e-5);
    Assert.assertEquals(1.30f, jme.ikLeftHandWorld.y, 1e-5);
    Assert.assertEquals(0.40f, jme.ikLeftHandWorld.z, 1e-5);
    jme.onWorldPosition(new Point(0.0, 0.0, 0.0));
    Assert.assertEquals("FK updates must not move the green goal", 0.20f, jme.ikLeftHandWorld.x, 1e-5);
    Assert.assertEquals(1.30f, jme.ikLeftHandWorld.y, 1e-5);
    Assert.assertEquals(0.40f, jme.ikLeftHandWorld.z, 1e-5);
  }

  /**
   * A saved calibration must restore the whole chain, not just an origin and a
   * tool offset, so a restart without the simulator still solves correctly.
   */
  @Test
  public void testPersistedCalibrationRestoresMeasuredChain() {
    InverseKinematics3D ik3d = (InverseKinematics3D) Runtime.start("ik3d-persist", "InverseKinematics3D");
    ik3d.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    List<JointFrame> frames = InMoov2Arm.getDefaultJointFrames("i01", "left");
    Point shift = new Point(0.31, 1.44, 0.02);
    for (JointFrame frame : frames) {
      frame.origin = frame.origin.add(shift);
    }
    Point wrist = InMoov2Arm.getDefaultEndEffector("left").add(shift);
    ik3d.calibrateFromJointFrames(arm, frames, wrist);

    InverseKinematics3DConfig cfg = ik3d.getConfig();
    Assert.assertEquals("all four joints persisted", 4, cfg.joints.size());
    Assert.assertTrue(cfg.endEffectorSet);
    Assert.assertEquals("measured forearm persisted", InMoov2Arm.DH_BICEP_A, cfg.bicepA, 1e-3);

    InverseKinematics3D ik2 = (InverseKinematics3D) Runtime.start("ik3d-persist2", "InverseKinematics3D");
    ik2.setCurrentArm(arm, InMoov2Arm.getDHRobotArm("i01", "left"));
    ik2.apply(cfg);
    Assert.assertEquals(shift.getX(), ik2.getWorldOrigin().getX(), 1e-6);
    Assert.assertEquals(shift.getY(), ik2.getWorldOrigin().getY(), 1e-6);
    Assert.assertEquals(0.0, ik2.currentPositionWorld(arm).distanceTo(wrist), 1e-6);

    // and the restored chain must agree with the original away from rest, too
    DHRobotArm a = ik3d.getCurrentArm(arm);
    DHRobotArm b = ik2.getCurrentArm(arm);
    for (int i = 0; i < 4; i++) {
      double servo = (a.getLink(i).servoMin + a.getLink(i).servoMax) / 2 + 7;
      a.getLink(i).setFromServoDegrees(servo);
      b.getLink(i).setFromServoDegrees(servo);
    }
    Assert.assertEquals("restored chain matches away from the calibration pose", 0.0, a.getPalmPosition().distanceTo(b.getPalmPosition()), 1e-9);
  }

  @Test
  public void testMathUtilsSanity() {
    Assert.assertEquals(Math.PI, MathUtils.degToRad(180), 1e-9);
  }
}
