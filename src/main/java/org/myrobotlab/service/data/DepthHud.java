package org.myrobotlab.service.data;

import java.io.Serializable;

/**
 * Packed RGB (3 bytes per pixel, row-major) colorized depth for a 2D overlay.
 */
public class DepthHud implements Serializable {

  private static final long serialVersionUID = 1L;

  public int width;
  public int height;

  /** {@code width * height * 3} RGB bytes. */
  public byte[] rgb;

  public String src;
  public long ts = System.currentTimeMillis();

  public DepthHud() {
  }

  public DepthHud(int width, int height, byte[] rgb) {
    this.width = width;
    this.height = height;
    this.rgb = rgb;
  }
}
