package org.myrobotlab.math;

import java.io.Serializable;

public final class Mapper implements Serializable {

	private static final long serialVersionUID = 1L;

	// input range
	double minX;
	double maxX;

	// output range
	double minY;
	double maxY;

	// clipping
	double minOutput;
	double maxOutput;

	boolean inverted = false;

	public Mapper(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;

		this.minOutput = minY;
		this.maxOutput = maxY;
	}

	final public double calc(double in) {
		double c = minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
		if (c < minOutput) {
			return minOutput;
		}
		if (c > maxOutput) {
			return maxOutput;
		}
		return c;
	}

	final public int calcInt(double in) {
		return (int) calc(in);
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}

	public double getMinX() {
		return minX;
	}

	public double getMinY() {
		return minY;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean invert) {
		if (invert && !inverted) {
			double t = minX;
			minX = maxX;
			maxX = t;
			inverted = true;
		} else {
			inverted = false;
		}
	}

	public void setMax(double max) {
		maxOutput = max;
	}

	public void setMin(double min) {
		minOutput = min;
	}

}
