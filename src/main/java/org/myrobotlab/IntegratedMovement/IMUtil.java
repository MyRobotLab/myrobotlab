package org.myrobotlab.IntegratedMovement;

import org.apache.commons.math3.util.FastMath;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

public class IMUtil {
	IMUtil(){
		
	}
	
	static public Matrix getIdentityMatrix(){
	    Matrix m = new Matrix(4, 4);
	    // initial frame orientated around x
	    m.elements[0][0] = 1;
	    m.elements[1][1] = 1;
	    m.elements[2][2] = 1;
	    m.elements[3][3] = 1;
	    return m;
	}
	
	static public Point matrixToPoint(Matrix m){
	    double x = m.elements[0][3];
	    double y = m.elements[1][3];
	    double z = m.elements[2][3];
	    double pitch = Math.atan2(-1.0*(m.elements[2][0]), Math.sqrt(m.elements[2][1]*m.elements[2][1] + m.elements[2][2]*m.elements[2][2]));
	    double roll = 0;
	    double yaw = 0;
	    if (pitch == Math.PI/2) {
	      yaw =  Math.atan2(m.elements[2][1]/Math.cos(pitch), m.elements[2][2]) - Math.PI/2;
	    }
	    else if (pitch == -1 * Math.PI/2) {
	      yaw = Math.atan2(m.elements[2][1]/Math.cos(pitch), m.elements[2][2]) *-1 + Math.PI/2;
	    }
	    else {
	      yaw = Math.atan2(m.elements[2][1]/Math.cos(pitch), m.elements[2][2]) + Math.PI/2;
	      roll = Math.atan2(m.elements[1][0]/Math.cos(pitch), m.elements[0][0]) + Math.PI/2;
	    }
	    Point position = new Point(x, y, z, roll * 180 / Math.PI, pitch * 180 / Math.PI, yaw * 180 / Math.PI);
		return position;
	}
	
	static public Matrix3f eulerToMatrix3f(Point p){
		Matrix3f m = new Matrix3f();
		float ch = (float)Math.cos(Math.toRadians(p.getYaw()));
		float ca = (float)Math.cos(Math.toRadians(p.getPitch()));
		float cb = (float)Math.cos(Math.toRadians(p.getRoll()));
		float sa = (float)Math.sin(Math.toRadians(p.getPitch()));
		float sh = (float)Math.sin(Math.toRadians(p.getYaw()));
		float sb = (float)Math.sin(Math.toRadians(p.getRoll()));
		m.set(0, 0, ch * ca);
		m.set(0,1, (-1 * ch * sa * cb) + (sh * sb));
		m.set(0,2,  (ch * sa * sb) + (sh * cb));
		m.set(1,0,  sa);
		m.set(1,1,ca * cb);
		m.set(1,2, -1 * ca * sb);
		m.set(2,0,-1 * sh * ca);
		m.set(2,1, (sh * sa * cb) + (ch * sb));
		m.set(2,2, (-1 * sh * sa * sb) + (ch * cb));
		return m;
	}
	public static Transform matrixToJmeTransform(Matrix m) {
		Matrix4f tm = new Matrix4f((float)m.elements[0][0], (float)m.elements[0][1], (float)m.elements[0][2], 
				(float)m.elements[0][3], (float)m.elements[1][0], (float)m.elements[1][1],
				(float)m.elements[1][2], (float)m.elements[1][3], (float)m.elements[2][0], 
				(float)m.elements[2][1], (float)m.elements[2][2], (float)m.elements[2][3],
				(float)m.elements[3][0], (float)m.elements[3][1], (float)m.elements[3][2],
				(float)m.elements[3][3] );
		Transform t = new Transform();
		t.fromTransformMatrix(tm);
		return t;
	}
	public static Vector3f pointToVector3f(Point point){
		return new Vector3f(-(float)point.getX(), (float)point.getZ(), (float)point.getY());
	}
	
	public static Quaternion matrixToQuaternion(Matrix m){
		Matrix3f mf = new Matrix3f((float)m.elements[0][0], (float)m.elements[0][1], (float)m.elements[0][2], 
				(float)m.elements[1][0], (float)m.elements[1][1], (float)m.elements[1][2]
				, (float)m.elements[2][0], (float)m.elements[2][1], (float)m.elements[2][2]);
		Matrix3f tmf = new Matrix3f(-1, 0, 0, 0, 0, 1, 0, 1, 0);
		//mf = tmf.invert().mult(mf);
		mf = tmf.invert().mult(mf).mult(tmf);
		Quaternion q = new Quaternion().fromRotationMatrix(mf);
		float[] angles = new float[3];
		q.toAngles(angles);
		Quaternion r = new Quaternion().fromAngles(-angles[0], angles[2], angles[1]);
		//Quaternion qf = new Quaternion().fromAngleAxis(-FastMath.PI/2, Vector3f.UNIT_X);
		return q;
	}
	
}
