package org.myrobotlab.service;

import org.junit.Assert;
import org.junit.Test;
import org.myrobotlab.kinematics.FabrikArm;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.test.AbstractTest;

public class FabrikTest extends AbstractTest {

  @Test
  public void testStartAndMoveTo() {
    Fabrik fabrik = (Fabrik) Runtime.start("fabrik-test", "Fabrik");
    fabrik.setCurrentArm("left", "i01", "left");
    fabrik.centerAllJoints("left");
    Point start = fabrik.currentPosition("left");
    Assert.assertNotNull(start);
    Point reached = fabrik.moveTo("left", start.getX() + 0.02, start.getY(), start.getZ());
    Assert.assertNotNull(reached);
    Assert.assertTrue("FABRIK moveTo error " + reached.distanceTo(new Point(start.getX() + 0.02, start.getY(), start.getZ())),
        reached.distanceTo(new Point(start.getX() + 0.02, start.getY(), start.getZ())) < 0.003);
    Runtime.release("fabrik-test");
  }

  @Test
  public void testMoveToKnownReachablePoseWithinMillimeters() {
    Fabrik fabrik = (Fabrik) Runtime.start("fabrik-far", "Fabrik");
    fabrik.setCurrentArm("left", "i01", "left");
    fabrik.centerAllJoints("left");
    FabrikArm arm = fabrik.getCurrentArm("left");
    arm.setFromServoDegrees(1, 140);
    Point goal = arm.getPalmPosition();
    arm.centerAllJoints();
    Point reached = fabrik.moveTo("left", goal);
    Assert.assertNotNull(reached);
    double err = reached.distanceTo(goal);
    Assert.assertTrue("FABRIK far moveTo error " + err + " m", err < 0.003);
    Runtime.release("fabrik-far");
  }

  @Test
  public void testPublishedServoMapHasInMoovNames() {
    Fabrik fabrik = (Fabrik) Runtime.start("fabrik-map", "Fabrik");
    fabrik.setCurrentArm("left", "i01", "left");
    fabrik.centerAllJoints("left");
    java.util.Map<String, Double> servos = fabrik.getCurrentArm("left").toServoMap();
    Assert.assertTrue(servos.containsKey("i01.leftArm.omoplate"));
    Assert.assertTrue(servos.containsKey("i01.leftArm.shoulder"));
    Assert.assertTrue(servos.containsKey("i01.leftArm.rotate"));
    Assert.assertTrue(servos.containsKey("i01.leftArm.bicep"));
    Runtime.release("fabrik-map");
  }
}
