package org.myrobotlab.math.geometry;

import org.myrobotlab.service.data.DepthFrame;

/**
 * One clicked pixel on an aligned depth / RGB frame, unprojected to camera
 * meters (X right, Y down, Z forward).
 * <p>
 * Normalized {@code nx}/{@code ny} are 0–1 across the image, origin at the
 * top left, so a click on the WebGui preview (any display size) lines up with
 * the same pixel on the simulator mesh.
 */
public final class DepthImageClick {

  public final float nx;
  public final float ny;
  public final int u;
  public final int v;
  public final int depthMm;
  /** Camera-frame meters. */
  public final float camX;
  public final float camY;
  public final float camZ;

  private DepthImageClick(float nx, float ny, int u, int v, int depthMm, float camX, float camY, float camZ) {
    this.nx = nx;
    this.ny = ny;
    this.u = u;
    this.v = v;
    this.depthMm = depthMm;
    this.camX = camX;
    this.camY = camY;
    this.camZ = camZ;
  }

  /**
   * Sample the depth cell under {@code (nx, ny)} and unproject it. Looks a
   * couple of cells around the click when that cell is a hole. Returns null
   * when the frame has no usable depth there.
   */
  public static DepthImageClick at(DepthFrame frame, float nx, float ny) {
    if (frame == null || frame.width <= 0 || frame.height <= 0 || frame.fx == 0f || frame.fy == 0f) {
      return null;
    }
    float x = clamp01(nx);
    float y = clamp01(ny);
    int u = (int) Math.floor(x * frame.width);
    int v = (int) Math.floor(y * frame.height);
    if (u >= frame.width) {
      u = frame.width - 1;
    }
    if (v >= frame.height) {
      v = frame.height - 1;
    }
    int mm = sampleMm(frame, u, v);
    if (mm < 0) {
      return null;
    }
    float z = mm / 1000f;
    float camX = (u - frame.cx) * z / frame.fx;
    float camY = (v - frame.cy) * z / frame.fy;
    return new DepthImageClick(x, y, u, v, mm, camX, camY, z);
  }

  /**
   * Camera meters → normalized image position. {@code [nx, ny, u, v]} with
   * {@code u}/{@code v} in pixels. Null when the point is behind the camera
   * or the frame has no intrinsics.
   */
  public static float[] normalize(DepthFrame frame, float camX, float camY, float camZ) {
    if (frame == null || frame.width <= 0 || frame.height <= 0 || frame.fx == 0f || frame.fy == 0f || camZ < 1e-4f) {
      return null;
    }
    float u = frame.fx * camX / camZ + frame.cx;
    float v = frame.fy * camY / camZ + frame.cy;
    return new float[] { u / frame.width, v / frame.height, u, v };
  }

  private static int sampleMm(DepthFrame frame, int u, int v) {
    int stride = frame.stride <= 0 ? 1 : frame.stride;
    int col = u / stride;
    int row = v / stride;
    int mm = read(frame, col, row);
    if (valid(frame, mm)) {
      return mm;
    }
    int best = -1;
    int bestD = Integer.MAX_VALUE;
    for (int dr = -2; dr <= 2; dr++) {
      for (int dc = -2; dc <= 2; dc++) {
        if (dr == 0 && dc == 0) {
          continue;
        }
        int sample = read(frame, col + dc, row + dr);
        if (!valid(frame, sample)) {
          continue;
        }
        int du = (col + dc) * stride - u;
        int dv = (row + dr) * stride - v;
        int d = du * du + dv * dv;
        if (d < bestD) {
          bestD = d;
          best = sample;
        }
      }
    }
    return best;
  }

  private static int read(DepthFrame frame, int col, int row) {
    if (col < 0 || row < 0 || col >= frame.gridWidth() || row >= frame.gridHeight()) {
      return -1;
    }
    int i = frame.index(col, row);
    if (frame.depthMm == null || i < 0 || i >= frame.depthMm.length) {
      return -1;
    }
    return frame.depthMm[i];
  }

  private static boolean valid(DepthFrame frame, int mm) {
    return mm >= frame.minDepthMm && mm <= frame.maxDepthMm;
  }

  private static float clamp01(float v) {
    if (v < 0f) {
      return 0f;
    }
    if (v > 1f) {
      return 1f;
    }
    return v;
  }
}
