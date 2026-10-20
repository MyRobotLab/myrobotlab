package org.myrobotlab.math.geometry;

import org.myrobotlab.service.data.DepthFrame;

/**
 * Synthetic depth for simulator overlay without a USB camera: a back wall and a
 * closer box so the cloud has visible structure.
 */
public final class SyntheticDepth {

  private SyntheticDepth() {
  }

  public static DepthFrame planeWithBox(int width, int height, float wallMeters, float boxMeters, long frameId) {
    DepthFrame frame = new DepthFrame();
    frame.width = width;
    frame.height = height;
    frame.stride = 1;
    frame.fx = width * 0.9f;
    frame.fy = width * 0.9f;
    frame.cx = width / 2f;
    frame.cy = height / 2f;
    frame.minDepthMm = 200;
    frame.maxDepthMm = 8000;
    frame.src = "synthetic";
    frame.frameId = frameId;
    frame.depthMm = new int[width * height];

    int wallMm = Math.round(wallMeters * 1000f);
    int boxMm = Math.round(boxMeters * 1000f);
    int boxX0 = width / 3;
    int boxX1 = (width * 2) / 3;
    int boxY0 = height / 3;
    int boxY1 = (height * 2) / 3;

    // Slow pulse so a live overlay is obvious without hardware.
    double pulse = 40.0 * Math.sin(frameId * 0.15);
    int boxNow = boxMm + (int) pulse;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int mm = wallMm;
        if (x >= boxX0 && x < boxX1 && y >= boxY0 && y < boxY1) {
          mm = boxNow;
        }
        frame.depthMm[y * width + x] = mm;
      }
    }
    paintRgb(frame, boxX0, boxX1, boxY0, boxY1);
    return frame;
  }

  /**
   * Gray wall and blue box so the RGB mesh is obvious without a color camera.
   */
  static void paintRgb(DepthFrame frame, int boxX0, int boxX1, int boxY0, int boxY1) {
    int w = frame.width;
    int h = frame.height;
    frame.rgbWidth = w;
    frame.rgbHeight = h;
    frame.rgb = new byte[w * h * 3];
    int i = 0;
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        boolean box = x >= boxX0 && x < boxX1 && y >= boxY0 && y < boxY1;
        if (box) {
          frame.rgb[i++] = (byte) 40;
          frame.rgb[i++] = (byte) 140;
          frame.rgb[i++] = (byte) 220;
        } else {
          frame.rgb[i++] = (byte) 180;
          frame.rgb[i++] = (byte) 180;
          frame.rgb[i++] = (byte) 176;
        }
      }
    }
  }

  public static DepthFrame planeWithBox(long frameId) {
    return planeWithBox(160, 120, 1.6f, 0.85f, frameId);
  }
}
