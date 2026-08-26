package org.myrobotlab.math.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DepthCloudJmeTest {

  @Test
  public void cameraYFlipsToJmeUp() {
    float[] corners = new float[24];
    DepthCloudJme.voxelCorners(0.1f, 0.2f, 1.5f, 1f, 0f, corners);
    assertEquals(0.1f, corners[0], 1e-5f);
    assertEquals(-0.2f, corners[1], 1e-5f);
    assertEquals(1.5f, corners[2], 1e-5f);
  }

  @Test
  public void parentScaleIsCompensated() {
    assertEquals(1f, DepthCloudJme.effectiveScale(1f, 1f), 1e-6f);
    assertEquals(100f, DepthCloudJme.effectiveScale(1f, 0.01f), 1e-4f);
    assertEquals(1f, DepthCloudJme.effectiveScale(0f, 1f), 1e-6f);
  }

  @Test
  public void voxelHasExtent() {
    float[] corners = new float[24];
    DepthCloudJme.voxelCorners(0, 0, 1, 1f, 0.05f, corners);
    float minZ = Float.MAX_VALUE;
    float maxZ = -Float.MAX_VALUE;
    for (int i = 0; i < 8; i++) {
      minZ = Math.min(minZ, corners[i * 3 + 2]);
      maxZ = Math.max(maxZ, corners[i * 3 + 2]);
    }
    assertEquals(0.95f, minZ, 1e-5f);
    assertEquals(1.05f, maxZ, 1e-5f);
  }

  @Test
  public void cubeIndexCount() {
    int[] idx = DepthCloudJme.cubeIndices(2);
    assertEquals(72, idx.length);
    assertEquals(8, idx[36]);
    assertTrue(idx[71] >= 8);
  }
}
