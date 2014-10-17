package org.myrobotlab.util;

public final class Map {

	float minX;
	float maxX;
	float minY;
	float maxY;

	public Map(float minX, float maxX, float minY, float maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}

	final public int calc(float s) {
		return java.lang.Math.round(minY + ((s - minX) * (maxY - minY)) / (maxX - minX));
	}

}
