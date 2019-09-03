package org.myrobotlab.IntegratedMovement;

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
	    double pitch = Math.atan2(m.elements[1][2], m.elements[2][2]);
	    double c2 = Math.sqrt(Math.pow(m.elements[0][0], 2) + Math.pow(m.elements[0][1], 2));
	    double yaw = Math.atan2(-1 * m.elements[0][2], c2);
	    double s1 = Math.sin(pitch);
	    double c1 = Math.cos(pitch);
	    double roll = Math.atan2((s1*m.elements[2][0]) - (c1 * m.elements[1][0]), (c1 * m.elements[1][1]) - (s1 * m.elements[2][1]));
	    Point position = new Point(x, y, z, roll * 180 / Math.PI, pitch * 180 / Math.PI, yaw * 180 / Math.PI);
//	    double cp = Math.cos(pitch);
//	    double cy = Math.cos(yaw);
//	    double cr = Math.cos(roll);
//	    double sp = Math.sin(pitch);
//	    double sy = Math.sin(yaw);
//	    double sr = Math.sin(roll);
//	    Matrix m1 = new Matrix(4,4);
//	    m1.elements[0][0] = cy * cr;
//	    m1.elements[0][1] = cy * sr;
//	    m1.elements[0][2] = -1 * sy;
//	    m1.elements[1][0] = (sp * sy * cr) - (cp * sr);
//	    m1.elements[1][1] = (sp * sy * sr) + (cp * cr);
//	    m1.elements[1][2] = sp * cy;
//	    m1.elements[2][0] = (cp * sy * cr) + (sp * sr);
//	    m1.elements[2][1] = (cp * sy * sr) - (sp * cr);
//	    m1.elements[2][2] = cp * cy;
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
		mf = tmf.invert().mult(mf).mult(tmf);
		Quaternion q = new Quaternion().fromRotationMatrix(mf);
		float[] angles = new float[3];
		q.toAngles(angles);
		return q;
	}
	
}
