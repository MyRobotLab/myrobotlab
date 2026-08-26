package org.myrobotlab.math.geometry;

import java.awt.Color;

import org.myrobotlab.service.data.DepthFrame;
import org.myrobotlab.service.data.DepthHud;

/**
 * HSV colorization of depth in millimeters (near = red, far = blue).
 */
public final class DepthColorMap {

  private DepthColorMap() {
  }

  public static Color colorForMm(int mm, int minMm, int maxMm) {
    if (mm <= 0 || mm < minMm || mm > maxMm) {
      return Color.BLACK;
    }
    float span = Math.max(1, maxMm - minMm);
    float t = (mm - minMm) / span;
    if (t < 0f) {
      t = 0f;
    } else if (t > 1f) {
      t = 1f;
    }
    return Color.getHSBColor(0.66f * t, 0.9f, 0.9f);
  }

  public static void rgba(int mm, int minMm, int maxMm, float[] out, int offset) {
    Color c = colorForMm(mm, minMm, maxMm);
    out[offset] = c.getRed() / 255f;
    out[offset + 1] = c.getGreen() / 255f;
    out[offset + 2] = c.getBlue() / 255f;
    out[offset + 3] = mm > 0 ? 1f : 0f;
  }

  public static void rgbBytes(int mm, int minMm, int maxMm, byte[] out, int offset) {
    Color c = colorForMm(mm, minMm, maxMm);
    out[offset] = (byte) c.getRed();
    out[offset + 1] = (byte) c.getGreen();
    out[offset + 2] = (byte) c.getBlue();
  }

  public static DepthHud toHud(DepthFrame frame) {
    if (frame == null || frame.depthMm == null) {
      return null;
    }
    int gw = frame.gridWidth();
    int gh = frame.gridHeight();
    if (gw <= 0 || gh <= 0) {
      return null;
    }
    byte[] rgb = new byte[gw * gh * 3];
    int i = 0;
    for (int row = 0; row < gh; row++) {
      for (int col = 0; col < gw; col++) {
        int mm = frame.depthMm[frame.index(col, row)];
        rgbBytes(mm, frame.minDepthMm, frame.maxDepthMm, rgb, i);
        i += 3;
      }
    }
    DepthHud hud = new DepthHud(gw, gh, rgb);
    hud.src = frame.src;
    hud.ts = frame.ts;
    return hud;
  }
}
