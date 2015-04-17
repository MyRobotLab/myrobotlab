package com.thalmic.myo;

public final class Quaternion {
    private final double x;
    private final double y;
    private final double z;
    private final double w;

    public Quaternion() {
	this(0, 0, 0, 1);
    }

    public Quaternion(Quaternion quaternion) {
	this(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    private Quaternion(float x, float y, float z, float w) {
	this((double) x, (double) y, (double) z, (double) w);
    }

    public Quaternion(double x, double y, double z, double w) {
	super();
	this.x = x;
	this.y = y;
	this.z = z;
	this.w = w;
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

    public double getW() {
	return w;
    }

    public final Quaternion multiply(Quaternion rhs) {
	return new Quaternion(getW() * rhs.getX() + getX() * rhs.getW() + getY() * rhs.getZ() - getZ() * rhs.getY(), getW() * rhs.getY() - getX() * rhs.getZ() + getY() * rhs.getW() + getZ() * rhs.getX(), getW() * rhs.getZ() + getX() * rhs.getY() - getY() * rhs.getX() + getZ() * rhs.getW(), getW() * rhs.getW() - getX() * rhs.getX() - getY() * rhs.getY() - getZ() * rhs.getZ());
    }

    public final Quaternion normalized() {
	double norm = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2) + Math.pow(w, 2));
	return new Quaternion(x / norm, y / norm, z / norm, w / norm);
    }

    public final Quaternion conjugate() {
	return new Quaternion(-x, -y, -z, -w);
    }

    public static Quaternion fromAxisAngle(Vector3 axis, double angle) {
	return new Quaternion(axis.getX() * Math.sin(angle / 2), axis.getY() * Math.sin(angle / 2), axis.getZ() * Math.sin(angle / 2), Math.cos(angle / 2));
    }

    public static Quaternion rotate(Vector3 from, Vector3 to) {
	double cosTheta = from.dot(to);

	if (cosTheta >= 1) {
	    return new Quaternion();
	}

	double k = Math.sqrt(from.dot(from) * to.dot(to));

	if (k <= 0) {
	    return new Quaternion();
	}

	Vector3 cross = from.cross(to);
	if (cosTheta / k <= -1) {
	    Vector3 xAxis = new Vector3(1, 0, 0);
	    Vector3 yAxis = new Vector3(0, 1, 0);

	    cross = from.cross(Math.abs(from.dot(xAxis)) < 1 ? xAxis : yAxis);
	    k = cosTheta = 0;
	}

	return new Quaternion(cross.getX(), cross.getY(), cross.getZ(), k + cosTheta);
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	long temp;
	temp = Double.doubleToLongBits(w);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	temp = Double.doubleToLongBits(x);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	temp = Double.doubleToLongBits(y);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	temp = Double.doubleToLongBits(z);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	Quaternion other = (Quaternion) obj;
	if (Double.doubleToLongBits(w) != Double.doubleToLongBits(other.w)) {
	    return false;
	}
	if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) {
	    return false;
	}
	if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) {
	    return false;
	}
	if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z)) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "Quaternion [x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + "]";
    }
}