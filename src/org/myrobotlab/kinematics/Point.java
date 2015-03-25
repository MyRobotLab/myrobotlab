package org.myrobotlab.kinematics;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Represents a 3d point in space.
 * 
 * @author kwatters
 *
 */
public class Point {
	private final double x;
	private final double y;
	private final double z;

	// TODO: consider rotation/orientation
	public Point(double x, double y, double z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public double magnitude() {
		// TODO Auto-generated method stub
		return Math.sqrt(x * x + y * y + z * z);
	}

	public Point subtract(Point p) {
		// TODO Auto-generated method stub
		Point newPoint = new Point(x - p.getX(), y - p.getY(), z - p.getZ());
		return newPoint;
	}

	@Override
	public String toString() {
		// TODO: round this out
		NumberFormat formatter = new DecimalFormat("#0.000000");
		return "(x=" + formatter.format(x) + ", y=" + formatter.format(y) + ", z=" + formatter.format(z) + ")";
	}
}
