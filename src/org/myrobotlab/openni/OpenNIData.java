package org.myrobotlab.openni;

import java.awt.image.BufferedImage;

public class OpenNIData {
	public int frameNumber;
	public Skeleton skeleton;
	public PImage depthPImage;
	public PImage rbgPImage;
	public BufferedImage depth;
	public BufferedImage rgb;
	public int[] depthMap;
	public BufferedImage display;
}
