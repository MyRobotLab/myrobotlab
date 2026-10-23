package org.myrobotlab.service.data;

import java.io.Serializable;

/**
 * A point picked on the OAK-D image or the simulator depth overlay.
 * <p>
 * {@code nx}/{@code ny} are 0–1 across the aligned RGB and depth images
 * (origin top left) so the WebGui can draw the same marker on both.
 * {@code x}/{@code y}/{@code z} are world meters, Y-up, the same frame
 * {@link org.myrobotlab.service.JMonkeyEngine#publishClickPoint} sends to
 * FABRIK. {@code nx} is negative when the world point could not be projected
 * back onto the image.
 */
public class DepthClick implements Serializable {

  private static final long serialVersionUID = 1L;

  public float nx;
  public float ny;
  public int u;
  public int v;
  public int depthMm;
  public double x;
  public double y;
  public double z;
  /** Meters from the chest camera to {@code (x, y, z)}. */
  public float distanceM;
}
