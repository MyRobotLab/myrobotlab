package org.myrobotlab.math;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

public final class Mapper implements Serializable {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Servo.class);

	// input range
	Double minX;
	Double maxX;

	// output range
	Double minY;
	Double maxY;

	// clipping
	Double minOutput;
	Double maxOutput;

	boolean inverted = false;

	public Mapper(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;

		if (minY < maxY) {
			this.minOutput = minY;
			this.maxOutput = maxY;
		} else {
			this.minOutput = maxY;
			this.maxOutput = minY;
		}
	}

	final public double calc(double in) {
		double c = minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
		if (c < minOutput) {
			log.warn("clipping {} to {}", c, minOutput);
			return minOutput;
		}
		if (c > maxOutput) {
			log.warn("clipping {} to {}", c, maxOutput);
			return maxOutput;
		}
		return c;
	}

	final public int calcInt(int in) {
		return (int) calc(in);
	}

	final public int calcInt(double in) {
		return (int) calc(in);
	}

	public Double getMaxX() {
		return maxX;
	}

	public Double getMaxY() {
		return maxY;
	}

	public Double getMinX() {
		return minX;
	}

	public Double getMinY() {
		return minY;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean invert) {
		if (invert && !inverted) {
			// change state from non-inverted to inverted
			double t = minX;
			minX = maxX;
			maxX = t;
			inverted = true;
		} else if (invert && inverted) {
			// change state from inverted to non-inverted
			double t = minX;
			minX = maxX;
			maxX = t;
			inverted = false;
		} else {
			// inverted to inverted & non-inverted to non-inverted
			// do nothing
		}
	}

	public void setMax(double max) {
		maxOutput = max;
		// maxX = max;
	}

	public void setMin(double min) {
		minOutput = min;
	}

	public double getMinOutput() {
		return minOutput;
	}

	public double getMaxOutput() {
		return maxOutput;
	}

}
