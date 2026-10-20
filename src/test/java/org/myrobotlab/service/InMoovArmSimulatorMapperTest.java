package org.myrobotlab.service;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.framework.Plan;
import org.myrobotlab.jme3.UserDataConfig;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.DHRobotArm;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.config.InMoov2Config;
import org.myrobotlab.service.config.JMonkeyEngineConfig;
import org.myrobotlab.test.AbstractTest;

/**
 * Confirms the historical VinMoov arm mapper scale mismatch vs DH / IK servo
 * degrees, and that the default InMoov2 plan is now 1:1 around rest.
 */
public class InMoovArmSimulatorMapperTest extends AbstractTest {

  @Test
  public void testHistoricalMappersWereNotOneToOne() {
    MapperLinear oldBicep = new MapperLinear(0.0, 180.0, 0.0, -150.0, true, false);
    MapperLinear oldRotate = new MapperLinear(0.0, 180.0, -80.0, 80.0, true, false);
    MapperLinear oldOmoplate = new MapperLinear(0.0, 180.0, -10.0, 180.0, true, false);
    MapperLinear oldShoulder = new MapperLinear(0.0, 180.0, 30.0, -150.0, true, false);

    double bicepDelta = 45.0;
    Assert.assertEquals("old bicep scale ~0.83", -37.5, oldBicep.calcOutput(InMoov2Arm.BICEP_SERVO_REST + bicepDelta) - oldBicep.calcOutput(InMoov2Arm.BICEP_SERVO_REST), 0.01);

    double rotateDelta = 60.0;
    Assert.assertEquals("old rotate scale ~0.89", 53.333, oldRotate.calcOutput(InMoov2Arm.ROTATE_SERVO_REST + rotateDelta) - oldRotate.calcOutput(InMoov2Arm.ROTATE_SERVO_REST),
        0.01);

    double omoDelta = 35.0;
    Assert.assertEquals("old omoplate scale ~1.06", 36.944, oldOmoplate.calcOutput(InMoov2Arm.OMOPLATE_SERVO_REST + omoDelta) - oldOmoplate.calcOutput(InMoov2Arm.OMOPLATE_SERVO_REST),
        0.01);

    // Shoulder was already 1:1 (slope −1) with rest → 0
    Assert.assertEquals(0.0, oldShoulder.calcOutput(InMoov2Arm.SHOULDER_SERVO_REST), 1e-9);
    Assert.assertEquals(-60.0, oldShoulder.calcOutput(InMoov2Arm.SHOULDER_SERVO_REST + 60.0) - oldShoulder.calcOutput(InMoov2Arm.SHOULDER_SERVO_REST), 1e-9);
  }

  @Test
  public void testAlignedMappersAreOneToOneAroundRest() {
    MapperLinear bicep = InMoov2Arm.vinMoovArmMapper(InMoov2Arm.BICEP_SERVO_REST, -1);
    MapperLinear shoulder = InMoov2Arm.vinMoovArmMapper(InMoov2Arm.SHOULDER_SERVO_REST, -1);
    MapperLinear rotateL = InMoov2Arm.vinMoovArmMapper(InMoov2Arm.ROTATE_SERVO_REST, 1);
    MapperLinear rotateR = InMoov2Arm.vinMoovArmMapper(InMoov2Arm.ROTATE_SERVO_REST, -1);
    MapperLinear omoL = InMoov2Arm.vinMoovArmMapper(InMoov2Arm.OMOPLATE_SERVO_REST, 1);
    MapperLinear omoR = InMoov2Arm.vinMoovArmMapper(InMoov2Arm.OMOPLATE_SERVO_REST, -1);

    assertRestIsBindPose(bicep, InMoov2Arm.BICEP_SERVO_REST);
    assertRestIsBindPose(shoulder, InMoov2Arm.SHOULDER_SERVO_REST);
    assertRestIsBindPose(rotateL, InMoov2Arm.ROTATE_SERVO_REST);
    assertRestIsBindPose(rotateR, InMoov2Arm.ROTATE_SERVO_REST);
    assertRestIsBindPose(omoL, InMoov2Arm.OMOPLATE_SERVO_REST);
    assertRestIsBindPose(omoR, InMoov2Arm.OMOPLATE_SERVO_REST);

    assertOneToOne(bicep, InMoov2Arm.BICEP_SERVO_REST, 45.0, -1);
    assertOneToOne(shoulder, InMoov2Arm.SHOULDER_SERVO_REST, 60.0, -1);
    assertOneToOne(rotateL, InMoov2Arm.ROTATE_SERVO_REST, 60.0, 1);
    assertOneToOne(rotateR, InMoov2Arm.ROTATE_SERVO_REST, 60.0, -1);
    assertOneToOne(omoL, InMoov2Arm.OMOPLATE_SERVO_REST, 35.0, 1);
    assertOneToOne(omoR, InMoov2Arm.OMOPLATE_SERVO_REST, 35.0, -1);
  }

  /**
   * The solver's joint angle must equal the mesh angle including its sign.
   *
   * <p>
   * This used to compare absolute values, which passed while the shoulder and
   * bicep actually turned the solver's model the opposite way from the simulated
   * arm — the mismatch was invisible at rest and grew with every degree of
   * motion.
   * </p>
   */
  @Test
  public void testDhServoDeltaMatchesMeshDeltaSigned() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "left");
    MapperLinear[] mappers = new MapperLinear[] { InMoov2Arm.vinMoovArmMapper(InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.OMOPLATE_MESH_SLOPE_LEFT),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.SHOULDER_MESH_SLOPE),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.ROTATE_MESH_SLOPE_LEFT),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.BICEP_SERVO_REST, InMoov2Arm.BICEP_MESH_SLOPE) };
    double[] rests = new double[] { InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST };
    for (int i = 0; i < 4; i++) {
      DHLink link = dh.getLink(i);
      double restTheta = MathUtils.radToDeg(link.getTheta());
      for (double delta : new double[] { -20.0, 10.0, 30.0 }) {
        double servo = rests[i] + delta;
        if (servo < link.servoMin || servo > link.servoMax) {
          continue;
        }
        link.setFromServoDegrees(servo);
        double dhDelta = MathUtils.radToDeg(link.getTheta()) - restTheta;
        double meshDelta = mappers[i].calcOutput(servo) - mappers[i].calcOutput(rests[i]);
        Assert.assertEquals("link " + i + " signed theta delta must equal signed mesh delta at servo " + servo, meshDelta, dhDelta, 1e-9);
      }
      link.setFromServoDegrees(rests[i]);
    }
  }

  /** The right arm's reversed nodes must be reflected in its solver model too. */
  @Test
  public void testRightArmServoDeltaMatchesMeshDeltaSigned() {
    DHRobotArm dh = InMoov2Arm.getDHRobotArm("i01", "right");
    MapperLinear[] mappers = new MapperLinear[] { InMoov2Arm.vinMoovArmMapper(InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.OMOPLATE_MESH_SLOPE_RIGHT),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.SHOULDER_MESH_SLOPE),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.ROTATE_MESH_SLOPE_RIGHT),
        InMoov2Arm.vinMoovArmMapper(InMoov2Arm.BICEP_SERVO_REST, InMoov2Arm.BICEP_MESH_SLOPE) };
    double[] rests = new double[] { InMoov2Arm.OMOPLATE_SERVO_REST, InMoov2Arm.SHOULDER_SERVO_REST, InMoov2Arm.ROTATE_SERVO_REST, InMoov2Arm.BICEP_SERVO_REST };
    for (int i = 0; i < 4; i++) {
      DHLink link = dh.getLink(i);
      double servo = rests[i] + 25.0;
      link.setFromServoDegrees(servo);
      double meshDelta = mappers[i].calcOutput(servo) - mappers[i].calcOutput(rests[i]);
      Assert.assertEquals("right link " + i + " signed", meshDelta, MathUtils.radToDeg(link.getTheta()), 1e-9);
    }
  }

  @Test
  public void testInMoov2DefaultPlanUsesAlignedArmMappers() {
    InMoov2Config cfg = new InMoov2Config();
    Plan plan = cfg.getDefault(new Plan("i01"), "i01");
    JMonkeyEngineConfig sim = (JMonkeyEngineConfig) plan.get("i01.simulator");
    Assert.assertNotNull(sim);
    assertPlanMapper(sim, "i01.leftArm.bicep", InMoov2Arm.BICEP_SERVO_REST, -1);
    assertPlanMapper(sim, "i01.leftArm.shoulder", InMoov2Arm.SHOULDER_SERVO_REST, -1);
    assertPlanMapper(sim, "i01.leftArm.rotate", InMoov2Arm.ROTATE_SERVO_REST, 1);
    assertPlanMapper(sim, "i01.leftArm.omoplate", InMoov2Arm.OMOPLATE_SERVO_REST, 1);
    assertPlanMapper(sim, "i01.rightArm.bicep", InMoov2Arm.BICEP_SERVO_REST, -1);
    assertPlanMapper(sim, "i01.rightArm.shoulder", InMoov2Arm.SHOULDER_SERVO_REST, -1);
    assertPlanMapper(sim, "i01.rightArm.rotate", InMoov2Arm.ROTATE_SERVO_REST, -1);
    assertPlanMapper(sim, "i01.rightArm.omoplate", InMoov2Arm.OMOPLATE_SERVO_REST, -1);
  }

  private static void assertRestIsBindPose(MapperLinear mapper, double rest) {
    Assert.assertEquals("rest servo → 0° mesh", 0.0, mapper.calcOutput(rest), 1e-9);
  }

  private static void assertOneToOne(MapperLinear mapper, double rest, double delta, double slope) {
    double meshDelta = mapper.calcOutput(rest + delta) - mapper.calcOutput(rest);
    Assert.assertEquals("1° servo = 1° mesh", slope * delta, meshDelta, 1e-9);
  }

  private static void assertPlanMapper(JMonkeyEngineConfig sim, String node, double rest, double slope) {
    UserDataConfig udc = sim.nodes.get(node);
    Assert.assertNotNull(node, udc);
    Assert.assertNotNull(node + " mapper", udc.mapper);
    assertRestIsBindPose(udc.mapper, rest);
    assertOneToOne(udc.mapper, rest, 20.0, slope);
  }
}
