package org.myrobotlab.math.geometry;

/**
 * Camera-frame meters → JMonkeyEngine local meters for the chest depth overlay.
 * <p>
 * Camera: X right, Y down, Z forward. JME: X right, Y up, Z forward.
 * If the chest node has a non-unit world scale (common on imported rigs),
 * divide by that scale so 1 m of depth stays 1 m in the scene.
 */
public final class DepthCloudJme {

  private DepthCloudJme() {
  }

  /** Unit cube corners (x,y,z) × 8, used as voxel glyphs. */
  public static final float[] CUBE_CORNER = { -1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1, -1, -1, -1, 1, 1, -1, 1, 1, 1, 1,
      -1, 1, 1 };

  /** 12 triangles (36 indices) into {@link #CUBE_CORNER}. */
  public static final int[] CUBE_INDEX = { 0, 1, 2, 0, 2, 3, 4, 6, 5, 4, 7, 6, 0, 4, 5, 0, 5, 1, 2, 6, 7, 2, 7, 3, 0, 3,
      7, 0, 7, 4, 1, 5, 6, 1, 6, 2 };

  /**
   * Vertex multiplier so camera-frame meters become overlay-local meters.
   * {@code configScale} is the user calibration (default 1 = real-world). If
   * the overlay node has a non-unit world scale, divide it out so 1 m of depth
   * stays 1 m in the JME / IK world.
   */
  public static float effectiveScale(float configScale, float parentWorldScale) {
    float s = configScale <= 0f ? 1f : configScale;
    if (parentWorldScale > 1e-6f) {
      return s / parentWorldScale;
    }
    return s;
  }

  /**
   * Next {@code depthCloudScale} so a click that landed {@code observedM} from
   * the camera will land at {@code knownM} (tape measure).
   */
  public static float nextScale(float currentScale, double observedM, double knownM) {
    float s = currentScale <= 0f ? 1f : currentScale;
    if (observedM < 1e-4 || knownM <= 0 || Double.isNaN(observedM) || Double.isNaN(knownM)) {
      return s;
    }
    return s * (float) (knownM / observedM);
  }

  /**
   * @param out24
   *          length ≥ 24: eight JME-local cube corners (x,y,z)
   */
  public static void voxelCorners(float camX, float camY, float camZ, float scale, float halfExtent, float[] out24) {
    float x = camX * scale;
    float y = -camY * scale;
    float z = camZ * scale;
    for (int c = 0; c < 8; c++) {
      out24[c * 3] = x + CUBE_CORNER[c * 3] * halfExtent;
      out24[c * 3 + 1] = y + CUBE_CORNER[c * 3 + 1] * halfExtent;
      out24[c * 3 + 2] = z + CUBE_CORNER[c * 3 + 2] * halfExtent;
    }
  }

  /**
   * Eight cube corners in JME world meters (Y-up). Unlike
   * {@link #voxelCorners}, this does not flip Y or apply a camera scale.
   */
  public static void worldVoxelCorners(float x, float y, float z, float halfExtent, float[] out24) {
    for (int c = 0; c < 8; c++) {
      out24[c * 3] = x + CUBE_CORNER[c * 3] * halfExtent;
      out24[c * 3 + 1] = y + CUBE_CORNER[c * 3 + 1] * halfExtent;
      out24[c * 3 + 2] = z + CUBE_CORNER[c * 3 + 2] * halfExtent;
    }
  }

  public static int[] cubeIndices(int pointCount) {
    int n = Math.max(0, pointCount);
    int[] idx = new int[n * 36];
    for (int i = 0; i < n; i++) {
      int v = i * 8;
      int o = i * 36;
      for (int k = 0; k < 36; k++) {
        idx[o + k] = v + CUBE_INDEX[k];
      }
    }
    return idx;
  }
}
