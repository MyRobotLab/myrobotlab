package org.myrobotlab.math.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.service.data.DepthFrame;
import org.myrobotlab.service.data.DepthHud;

public class DepthToPointCloudTest {

  @Test
  public void centerPixelIsOnOpticalAxis() {
    DepthFrame frame = new DepthFrame();
    frame.width = 3;
    frame.height = 3;
    frame.stride = 1;
    frame.fx = 100f;
    frame.fy = 100f;
    frame.cx = 1f;
    frame.cy = 1f;
    frame.minDepthMm = 1;
    frame.maxDepthMm = 10000;
    frame.depthMm = new int[] { 0, 0, 0, 0, 1500, 0, 0, 0, 0 };

    PointCloud pc = DepthToPointCloud.convert(frame);
    assertEquals(1, pc.size());
    Point3df p = pc.getData()[0];
    assertEquals(0f, p.x, 1e-4f);
    assertEquals(0f, p.y, 1e-4f);
    assertEquals(1.5f, p.z, 1e-4f);
  }

  @Test
  public void pinholeUnprojectsPixelToTheRight() {
    DepthFrame frame = new DepthFrame();
    frame.width = 3;
    frame.height = 1;
    frame.stride = 1;
    frame.fx = 100f;
    frame.fy = 100f;
    frame.cx = 1f;
    frame.cy = 0f;
    frame.minDepthMm = 1;
    frame.maxDepthMm = 10000;
    frame.depthMm = new int[] { 0, 0, 2000 };

    PointCloud pc = DepthToPointCloud.convert(frame);
    assertEquals(1, pc.size());
    Point3df p = pc.getData()[0];
    // u=2, cx=1, z=2m, fx=100 → x = (2-1)*2/100 = 0.02
    assertEquals(0.02f, p.x, 1e-4f);
    assertEquals(0f, p.y, 1e-4f);
    assertEquals(2f, p.z, 1e-4f);
  }

  @Test
  public void strideSkipsPixels() {
    DepthFrame frame = new DepthFrame();
    frame.width = 4;
    frame.height = 1;
    frame.stride = 2;
    frame.fx = 100f;
    frame.fy = 100f;
    frame.cx = 0f;
    frame.cy = 0f;
    frame.minDepthMm = 1;
    frame.maxDepthMm = 10000;
    frame.depthMm = new int[] { 1000, 1000 };

    assertEquals(2, frame.gridWidth());
    PointCloud pc = DepthToPointCloud.convert(frame);
    assertEquals(2, pc.size());
    assertEquals(0f, pc.getData()[0].x, 1e-4f);
    // second sample is u = 2
    assertEquals(0.02f, pc.getData()[1].x, 1e-4f);
  }

  @Test
  public void syntheticWallAndBox() {
    DepthFrame frame = SyntheticDepth.planeWithBox(0);
    PointCloud pc = DepthToPointCloud.convert(frame);
    assertTrue(pc.size() > 100);
    float minZ = Float.MAX_VALUE;
    float maxZ = 0f;
    for (Point3df p : pc.getData()) {
      minZ = Math.min(minZ, p.z);
      maxZ = Math.max(maxZ, p.z);
    }
    assertTrue("box should be closer than wall", minZ < 1.1f);
    assertTrue("wall around 1.6m", maxZ > 1.4f);
    DepthHud hud = DepthColorMap.toHud(frame);
    assertEquals(frame.gridWidth(), hud.width);
    assertEquals(frame.gridHeight(), hud.height);
    assertEquals(hud.width * hud.height * 3, hud.rgb.length);
  }
}
