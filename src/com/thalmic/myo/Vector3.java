package com.thalmic.myo;

public class Vector3 {

    private final double x;
    private final double y;
    private final double z;

    public Vector3() {
	this(0, 0, 0);
    }

    public Vector3(Vector3 vector) {
	this(vector.x, vector.y, vector.z);
    }

    private Vector3(float x, float y, float z) {
	this((double) x, (double) y, (double) z);
    }

    public Vector3(double x, double y, double z) {
	super();
	this.x = x;
	this.y = y;
	this.z = z;
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

    public double magnitude() {
	return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3 normalized() {
	double normal = magnitude();
	return new Vector3(x / normal, y / normal, z / normal);
    }

    public double dot(Vector3 rightHandSide) {
	return x * rightHandSide.x + y * rightHandSide.y + z * rightHandSide.z;
    }

    public Vector3 cross(Vector3 rightHandSide) {
	return new Vector3(x * rightHandSide.z - z * rightHandSide.y, z * rightHandSide.x - x * rightHandSide.z, x * rightHandSide.y - y * rightHandSide.x);
    }

    public double angleTo(Vector3 rightHandSide) {
	return Math.acos(dot(rightHandSide) / (magnitude() * rightHandSide.magnitude()));
    }

    public static Vector3 rotate(Quaternion quat, Vector3 vec) {
	Quaternion qvec = new Quaternion(vec.getX(), vec.getY(), vec.getZ(), 0);
	Quaternion result = quat.multiply(qvec)
		.multiply(quat.conjugate());
	return new Vector3(result.getX(), result.getY(), result.getZ());
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
	Vector3 other = (Vector3) obj;
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
	return "Vector3 [x=" + x + ", y=" + y + ", z=" + z + "]";
    }

}