package org.myrobotlab.math.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

  @Test
  public void normalizeIntrinsicsLeavesFittedKAlone() {
    DepthFrame frame = new DepthFrame();
    frame.width = 640;
    frame.height = 400;
    frame.fx = 450f;
    frame.fy = 450f;
    frame.cx = 320f;
    frame.cy = 200f;
    assertFalse(frame.normalizeIntrinsics());
    assertEquals(450f, frame.fx, 1e-3f);
    assertEquals(320f, frame.cx, 1e-3f);
  }

  @Test
  public void fullSensorKMakesXyTooSmallUntilNormalized() {
    DepthFrame raw = rightEdgePixel(640, 400, 1400f, 1052f, 776f, 1000);
    PointCloud before = DepthToPointCloud.convert(raw);
    assertEquals(1, before.size());
    float xBefore = before.getData()[0].x;

    DepthFrame scaled = rightEdgePixel(640, 400, 1400f, 1052f, 776f, 1000);
    assertTrue(scaled.normalizeIntrinsics());
    PointCloud after = DepthToPointCloud.convert(scaled);
    assertEquals(1, after.size());
    float xAfter = after.getData()[0].x;
    assertEquals(1f, after.getData()[0].z, 1e-4f);
    assertTrue("normalized K must widen XY at the image edge", Math.abs(xAfter) > Math.abs(xBefore) * 1.5f);
    assertEquals(320f, scaled.cx, 1f);
    assertEquals(200f, scaled.cy, 1f);
  }

  private static DepthFrame rightEdgePixel(int w, int h, float fx, float cx, float cy, int mm) {
    DepthFrame frame = new DepthFrame();
    frame.width = w;
    frame.height = h;
    frame.stride = 1;
    frame.fx = fx;
    frame.fy = fx;
    frame.cx = cx;
    frame.cy = cy;
    frame.minDepthMm = 1;
    frame.maxDepthMm = 10000;
    frame.depthMm = new int[w * h];
    frame.depthMm[h / 2 * w + (w - 1)] = mm;
    return frame;
  }
}
