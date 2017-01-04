package org.myrobotlab.openni;

import java.awt.image.BufferedImage;

public class OpenNiData {
  public int frameNumber;
  public Skeleton skeleton;
  public PImage depthPImage;
  public PImage rbgPImage;
  transient public BufferedImage depth;
  transient public BufferedImage rgb;
  public int[] depthMap;
  transient public BufferedImage display;
  public PVector[] depthMapRW;
}
