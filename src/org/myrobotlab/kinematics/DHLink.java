package org.myrobotlab.kinematics;

/**
 * A link class to encapsulate the D-H parameters for a given link in a 
 * robotic arm.
 * 
 * d - the "depth" along the previous joint's z axis
 * θ (theta) - the rotation about the previous z (the angle between the common normal and the previous x axis)
 * r - the radius of the new origin about the previous z (the length of the common normal)
 * α (alpha) - the rotation about the new x axis (the common normal) to align the old z to the new z.
 * 
 * @author kwatters
 *
 */
public class DHLink {
	private double d;
	private double theta;
	private double r;
	private double alpha;
	
	private Matrix m;
	
	// TODO: add max/min angle
	
	public DHLink(double d, double r, double theta, double alpha) {
		super();
		this.d = d;
		this.r = r;
		this.theta = theta;
		this.alpha = alpha;		
		m = resolveMatrix();
	}

	/**
	 * return a 4x4 homogenous transformation matrix for the given D-H parameters
	 * 
	 * @return
	 */
	public Matrix resolveMatrix() {
		Matrix m = new Matrix(4,4);
		
		// elements we need
		double cosTheta = Math.cos(theta);
		double sinTheta = Math.sin(theta);
		double cosAlpha = Math.cos(alpha);
		double sinAlpha = Math.sin(alpha);
		
		// first row of homogenous xform
		m.elements[0][0] = cosTheta;
		m.elements[0][1] = -1 * sinTheta;
		m.elements[0][2] = 0;
		m.elements[0][3] = r;
		
		// 2nd row of homogenous xform
		m.elements[1][0] = sinTheta * cosAlpha;
		m.elements[1][1] = cosTheta * cosAlpha;
		m.elements[1][2] = -1 * sinAlpha;
		m.elements[1][3] = -1 * d * sinAlpha;

		// 3rd row of homogenous xform
		m.elements[2][0] = sinTheta * sinAlpha;
		m.elements[2][1] = cosTheta * sinAlpha;
		m.elements[2][2] = cosAlpha;
		m.elements[2][3] = d * cosAlpha;

		// 4th row of homogenous xform
		m.elements[3][0] = 0;
		m.elements[3][1] = 0;
		m.elements[3][2] = 0;
		m.elements[3][3] = 1;
		return m;
		
	}
	
	// move to an angle
	public void moveToAngle(double angle) {
		// TODO: which parameter?  
		this.theta = angle;		
		// update transform?		
		m = resolveMatrix();
	}
	
	public double getD() {
		return d;
	}
	public void setD(double d) {
		this.d = d;
	}
	public double getA() {
		return r;
	}
	public void setA(double a) {
		this.r = a;
	}
	public double getTheta() {
		return theta;
	}
	public void setTheta(double theta) {
		this.theta = theta;
	}
	public double getAlpha() {
		return alpha;
	}
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
}
