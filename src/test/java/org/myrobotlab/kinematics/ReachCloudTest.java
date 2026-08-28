package org.myrobotlab.kinematics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.InMoov2Arm;
import org.myrobotlab.test.AbstractTest;

public class ReachCloudTest extends AbstractTest {

  @Test
  public void sampleCountIsJointGrid() {
    FabrikArm arm = leftArm();
    List<Point> palms = arm.samplePalmWorkspace(3);
    assertEquals(81, palms.size());
  }

  @Test
  public void samplingRestoresPose() {
    FabrikArm arm = leftArm();
    arm.centerAllJoints();
    Point before = arm.getPalmPosition();
    arm.samplePalmWorkspace(3);
    Point after = arm.getPalmPosition();
    assertEquals(0.0, before.distanceTo(after), 1e-9);
  }

  @Test
  public void restPalmIsInsideSampledVolume() {
    FabrikArm arm = leftArm();
    arm.centerAllJoints();
    Point rest = arm.getPalmPosition();
    PointCloud cloud = ReachCloud.sample(arm, 5, 0.03f);
    assertTrue("need a volume of voxels, got " + cloud.size(), cloud.size() > 20);
    double nearest = nearest(cloud, rest);
    assertTrue("rest palm should land in a 3 cm cell, nearest=" + nearest, nearest < 0.04);
  }

  @Test
  public void workspaceStaysWithinArmLength() {
    FabrikArm arm = leftArm();
    Point origin = arm.getBaseOrigin();
    PointCloud cloud = ReachCloud.sample(arm, 4, 0.02f);
    double max = 0;
    for (org.myrobotlab.math.geometry.Point3df p : cloud.getData()) {
      double d = Math.sqrt((p.x - origin.getX()) * (p.x - origin.getX()) + (p.y - origin.getY()) * (p.y - origin.getY())
          + (p.z - origin.getZ()) * (p.z - origin.getZ()));
      max = Math.max(max, d);
    }
    assertTrue("InMoov arm reach is under 0.9 m, max=" + max, max < 0.9);
    assertTrue("arm should reach at least 20 cm, max=" + max, max > 0.20);
  }

  @Test
  public void voxelizeCollapsesDuplicates() {
    Point a = new Point(0.01, 0.01, 0.01);
    Point b = new Point(0.02, 0.02, 0.02);
    Point c = new Point(1.00, 1.00, 1.00);
    PointCloud sameCell = ReachCloud.voxelize(Arrays.asList(a, b, a), 0.05f);
    assertEquals(1, sameCell.size());
    PointCloud twoCells = ReachCloud.voxelize(Arrays.asList(a, c), 0.05f);
    assertEquals(2, twoCells.size());
  }

  private static FabrikArm leftArm() {
    FabrikArm arm = new FabrikArm("i01.leftArm");
    arm.applyJointFrames(InMoov2Arm.getDefaultJointFrames("i01", "left"), InMoov2Arm.getDefaultEndEffector("left"));
    return arm;
  }

  private static double nearest(PointCloud cloud, Point p) {
    double best = Double.POSITIVE_INFINITY;
    for (org.myrobotlab.math.geometry.Point3df q : cloud.getData()) {
      double dx = q.x - p.getX();
      double dy = q.y - p.getY();
      double dz = q.z - p.getZ();
      best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz));
    }
    return best;
  }
}
