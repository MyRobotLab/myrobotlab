package org.myrobotlab.math.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.service.data.DepthFrame;

public class DepthImageClickTest {

  @Test
  public void centerPixelSitsOnTheOpticalAxis() {
    DepthFrame frame = frame(3, 3, 1);
    frame.depthMm[frame.index(1, 1)] = 1500;

    DepthImageClick click = DepthImageClick.at(frame, 1.5f / 3f, 1.5f / 3f);
    assertEquals(1, click.u);
    assertEquals(1, click.v);
    assertEquals(1500, click.depthMm);
    assertEquals(0f, click.camX, 1e-4f);
    assertEquals(0f, click.camY, 1e-4f);
    assertEquals(1.5f, click.camZ, 1e-4f);
  }

  @Test
  public void rightAndBelowCenterUnprojectPositive() {
    DepthFrame frame = frame(100, 80, 1);
    int u = 70;
    int v = 50;
    frame.depthMm[frame.index(u, v)] = 2000;

    DepthImageClick click = DepthImageClick.at(frame, (u + 0.5f) / 100f, (v + 0.5f) / 80f);
    assertEquals(u, click.u);
    assertEquals(v, click.v);
    // cx = (width-1)/2 = 49.5, so (70-49.5)*2/100 = 0.41
    assertEquals(0.41f, click.camX, 1e-4f);
    assertEquals(0.21f, click.camY, 1e-4f);
    assertEquals(2f, click.camZ, 1e-4f);
    assertTrue(click.camY > 0f);
  }

  @Test
  public void projectRoundTripsTheClick() {
    DepthFrame frame = frame(100, 80, 1);
    int u = 70;
    int v = 50;
    frame.depthMm[frame.index(u, v)] = 2000;
    float nx = (u + 0.2f) / 100f;
    float ny = (v + 0.2f) / 80f;

    DepthImageClick click = DepthImageClick.at(frame, nx, ny);
    float[] back = DepthImageClick.normalize(frame, click.camX, click.camY, click.camZ);
    assertEquals(u / 100f, back[0], 1e-4f);
    assertEquals(v / 80f, back[1], 1e-4f);
  }

  @Test
  public void holeUsesANearbyCellButKeepsTheClickRay() {
    DepthFrame frame = frame(5, 1, 1);
    frame.depthMm[0] = 1000;

    DepthImageClick click = DepthImageClick.at(frame, 2.2f / 5f, 0.5f);
    assertEquals(2, click.u);
    assertEquals(1000, click.depthMm);
    // z from the neighbor, x along the clicked pixel (u = 2, cx = 2)
    assertEquals(0f, click.camX, 1e-4f);
    assertEquals(1f, click.camZ, 1e-4f);
  }

  @Test
  public void emptyAndMissingDepthAreSkipped() {
    assertNull(DepthImageClick.at(null, 0.5f, 0.5f));
    DepthFrame frame = frame(3, 3, 1);
    assertNull(DepthImageClick.at(frame, 0.5f, 0.5f));
    assertNull(DepthImageClick.normalize(frame, 0f, 0f, 0f));
  }

  @Test
  public void strideSamplesTheBlockUnderTheClick() {
    DepthFrame frame = frame(8, 8, 4);
    frame.depthMm = new int[] { 0, 0, 0, 2500 };
    // grid is 2x2; pixel (6, 6) is col 1, row 1
    DepthImageClick click = DepthImageClick.at(frame, 6.5f / 8f, 6.5f / 8f);
    assertEquals(6, click.u);
    assertEquals(6, click.v);
    assertEquals(2500, click.depthMm);
    assertEquals(2.5f, click.camZ, 1e-4f);
  }

  private static DepthFrame frame(int width, int height, int stride) {
    DepthFrame frame = new DepthFrame();
    frame.width = width;
    frame.height = height;
    frame.stride = stride;
    frame.fx = 100f;
    frame.fy = 100f;
    frame.cx = (width - 1) / 2f;
    frame.cy = (height - 1) / 2f;
    frame.minDepthMm = 1;
    frame.maxDepthMm = 10000;
    frame.depthMm = new int[frame.gridWidth() * frame.gridHeight()];
    return frame;
  }
}
