/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.saintandreas.math;

import java.util.logging.Logger;

/**
 * <code>Quaternion</code> defines a single example of a more general class of
 * hypercomplex numbers. Quaternions extends a rotation in three dimensions to a
 * rotation in four dimensions. This avoids "gimbal lock" and allows for smooth
 * continuous rotation.
 * 
 * <code>Quaternion</code> is defined by four floating point numbers: {x y z w}.
 * 
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Quaternion extends Vector4<Quaternion> implements java.io.Serializable {

  static final long serialVersionUID = 1;

  private static final Logger logger = Logger.getLogger(Quaternion.class
      .getName());
  /**
   * Represents the identity quaternion rotation (0, 0, 0, 1).
   */
  public static final Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);
  public static final Quaternion DIRECTION_Z = fromAxes(Vector3f.UNIT_X,
      Vector3f.UNIT_Y, Vector3f.UNIT_Z);
  public static final Quaternion ZERO = new Quaternion(0, 0, 0, 0);

  /**
   * 
   * <code>fromRotationMatrix</code> generates a quaternion from a supplied
   * matrix. This matrix is assumed to be a rotational matrix.
   * 
   * @param matrix
   *          the matrix that defines the rotation.
   */
  public static Quaternion fromMatrix3f(Matrix3f matrix) {
    return fromMatrix3f(matrix.m00, matrix.m01, matrix.m02, matrix.m10, matrix.m11,
        matrix.m12, matrix.m20, matrix.m21, matrix.m22);
  }

  public static Quaternion fromMatrix3f(float m00, float m01, float m02, float m10, float m11,
      float m12, float m20, float m21, float m22) {
    // Use the Graphics Gems code, from
    // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
    // *NOT* the "Matrix and Quaternions FAQ", which has errors!

    // the trace is the sum of the diagonal elements; see
    // http://mathworld.wolfram.com/MatrixTrace.html
    float t = m00 + m11 + m22;
    float x,y,z,w;

    // we protect the division by s by ensuring that s>=1
    if (t >= 0) { // |w| >= .5
      float s = FastMath.sqrt(t + 1); // |s|>=1 ...
      w = 0.5f * s;
      s = 0.5f / s; // so this division isn't bad
      x = (m21 - m12) * s;
      y = (m02 - m20) * s;
      z = (m10 - m01) * s;
    } else if ((m00 > m11) && (m00 > m22)) {
      float s = FastMath.sqrt(1.0f + m00 - m11 - m22); // |s|>=1
      x = s * 0.5f; // |x| >= .5
      s = 0.5f / s;
      y = (m10 + m01) * s;
      z = (m02 + m20) * s;
      w = (m21 - m12) * s;
    } else if (m11 > m22) {
      float s = FastMath.sqrt(1.0f + m11 - m00 - m22); // |s|>=1
      y = s * 0.5f; // |y| >= .5
      s = 0.5f / s;
      x = (m10 + m01) * s;
      z = (m21 + m12) * s;
      w = (m02 - m20) * s;
    } else {
      float s = FastMath.sqrt(1.0f + m22 - m00 - m11); // |s|>=1
      z = s * 0.5f; // |z| >= .5
      s = 0.5f / s;
      x = (m02 + m20) * s;
      y = (m21 + m12) * s;
      w = (m10 - m01) * s;
    }
    return new Quaternion(x,y,z,w);
  }
  
  /**
   * Constructor instantiates a new <code>Quaternion</code> object initializing
   * all values to zero, except w which is initialized to 1.
   * 
   */
  public Quaternion() {
    super(0, 0, 0, 1);
  }

  /**
   * Constructor instantiates a new <code>Quaternion</code> object from the
   * given list of parameters.
   * 
   * @param x
   *          the x value of the quaternion.
   * @param y
   *          the y value of the quaternion.
   * @param z
   *          the z value of the quaternion.
   * @param w
   *          the w value of the quaternion.
   */
  public Quaternion(float x, float y, float z, float w) {
    super(x, y, z, w);
  }


  /**
   * @return true if this Quaternion is {0,0,0,1}
   */
  public boolean isIdentity() {
    return equalsEpsilon(IDENTITY, FastMath.FLT_EPSILON);
  }

  private static class QuaternionTemp {
    float x, y, z, w;
  }

  private Quaternion(QuaternionTemp q) {
    super(q.x, q.y, q.z, q.w);
  }

  /**
   * <code>fromAngles</code> builds a Quaternion from the Euler rotation angles
   * (x,y,z) aka (pitch, yaw, rall)). Note that we are applying in order: (y, z,
   * x) aka (yaw, roll, pitch) but we've ordered them in x, y, and z for
   * convenience.
   * 
   * @see <a
   *      href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm">http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm</a>
   * 
   * @param xAngle
   *          the Euler pitch of rotation (in radians). (aka Attitude, often rot
   *          around x)
   * @param yAngle
   *          the Euler yaw of rotation (in radians). (aka Heading, often rot
   *          around y)
   * @param zAngle
   *          the Euler roll of rotation (in radians). (aka Bank, often rot
   *          around z)
   */
  public static Quaternion fromAngles(float xAngle, float yAngle, float zAngle) {
    float angle;
    float sinY, sinZ, sinX, cosY, cosZ, cosX;
    angle = zAngle * 0.5f;
    sinZ = FastMath.sin(angle);
    cosZ = FastMath.cos(angle);
    angle = yAngle * 0.5f;
    sinY = FastMath.sin(angle);
    cosY = FastMath.cos(angle);
    angle = xAngle * 0.5f;
    sinX = FastMath.sin(angle);
    cosX = FastMath.cos(angle);

    // variables used to reduce multiplication calls.
    float cosYXcosZ = cosY * cosZ;
    float sinYXsinZ = sinY * sinZ;
    float cosYXsinZ = cosY * sinZ;
    float sinYXcosZ = sinY * cosZ;

    QuaternionTemp temp = new QuaternionTemp();
    temp.w = (cosYXcosZ * cosX - sinYXsinZ * sinX);
    temp.x = (cosYXcosZ * sinX + sinYXsinZ * cosX);
    temp.y = (sinYXcosZ * cosX + cosYXsinZ * sinX);
    temp.z = (cosYXsinZ * cosX - sinYXcosZ * sinX);

    return new Quaternion(temp).normalize();
  }

  /**
   * <code>toAngles</code> returns this quaternion converted to Euler rotation
   * angles (yaw,roll,pitch).<br/>
   * 
   * @see <a
   *      href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm">http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm</a>
   * 
   * @param angles
   *          the float[] in which the angles should be stored, or null if you
   *          want a new float[] to be created
   * @return the float[] in which the angles are stored.
   */
  public float[] toAngles(float[] angles) {
    if (angles == null) {
      angles = new float[3];
    } else if (angles.length != 3) {
      throw new IllegalArgumentException(
          "Angles array must have three elements");
    }

    float sqw = w * w;
    float sqx = x * x;
    float sqy = y * y;
    float sqz = z * z;
    float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
    // is correction factor
    float test = x * y + z * w;
    if (test > 0.499 * unit) { // singularity at north pole
      angles[1] = 2 * FastMath.atan2(x, w);
      angles[2] = FastMath.HALF_PI;
      angles[0] = 0;
    } else if (test < -0.499 * unit) { // singularity at south pole
      angles[1] = -2 * FastMath.atan2(x, w);
      angles[2] = -FastMath.HALF_PI;
      angles[0] = 0;
    } else {
      angles[1] = FastMath.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw); // roll
                                                                                // or
                                                                                // heading
      angles[2] = FastMath.asin(2 * test / unit); // pitch or attitude
      angles[0] = FastMath.atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw); // yaw
                                                                                 // or
                                                                                 // bank
    }
    return angles;
  }

  /**
   * <code>toRotationMatrix</code> converts this quaternion to a rotational
   * matrix. Note: the result is created from a normalized version of this quat.
   * 
   * @return the rotation matrix representation of this quaternion.
   */
  public Matrix3f toRotationMatrix() {

    float norm = norm();
    // we explicitly test norm against one here, saving a division
    // at the cost of a test and branch. Is it worth it?
    float s = (norm == 1f) ? 2f : (norm > 0f) ? 2f / norm : 0;

    // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
    // will be used 2-4 times each.
    float xs = x * s;
    float ys = y * s;
    float zs = z * s;
    float xx = x * xs;
    float xy = x * ys;
    float xz = x * zs;
    float xw = w * xs;
    float yy = y * ys;
    float yz = y * zs;
    float yw = w * ys;
    float zz = z * zs;
    float zw = w * zs;

    // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
    return new Matrix3f(1 - (yy + zz), (xy - zw), (xz + yw), (xy + zw),
        1 - (xx + zz), (yz - xw), (xz - yw), (yz + xw), 1 - (xx + yy));
  }

  /**
   * <code>toRotationMatrix</code> converts this quaternion to a rotational
   * matrix. The result is stored in result. 4th row and 4th column values are
   * untouched. Note: the result is created from a normalized version of this
   * quat.
   * 
   * @param result
   *          The Matrix4f to store the result in.
   * @return the rotation matrix representation of this quaternion.
   */
  public Matrix4f toRotationMatrix4f() {
    float norm = norm();
    // we explicitly test norm against one here, saving a division
    // at the cost of a test and branch. Is it worth it?
    float s = (norm == 1f) ? 2f : (norm > 0f) ? 2f / norm : 0;

    // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
    // will be used 2-4 times each.
    float xs = x * s;
    float ys = y * s;
    float zs = z * s;
    float xx = x * xs;
    float xy = x * ys;
    float xz = x * zs;
    float xw = w * xs;
    float yy = y * ys;
    float yz = y * zs;
    float yw = w * ys;
    float zz = z * zs;
    float zw = w * zs;

    // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
    return new Matrix4f( //
        1 - (yy + zz), (xy - zw), (xz + yw), 0, // 
        (xy + zw), 1 - (xx + zz), (yz - xw), 0, // 
        (xz - yw), (yz + xw), 1 - (xx + yy), 0, //
        0, 0, 0, 1);
  }

  /**
   * <code>getRotationColumn</code> returns one of three columns specified by
   * the parameter. This column is returned as a <code>Vector3f</code> object.
   * The value is retrieved as if this quaternion was first normalized.
   * 
   * @param i
   *          the column to retrieve. Must be between 0 and 2.
   * @param store
   *          the vector object to store the result in. if null, a new one is
   *          created.
   * @return the column specified by the index.
   */
  public Vector3f getRotationColumn(int i) {
    float norm = norm();
    if (norm != 1.0f) {
      norm = FastMath.invSqrt(norm);
    }

    float xx = x * x * norm;
    float xy = x * y * norm;
    float xz = x * z * norm;
    float xw = x * w * norm;
    float yy = y * y * norm;
    float yz = y * z * norm;
    float yw = y * w * norm;
    float zz = z * z * norm;
    float zw = z * w * norm;
    float rx, ry, rz;

    switch (i) {
    case 0:
      rx = 1 - 2 * (yy + zz);
      ry = 2 * (xy + zw);
      rz = 2 * (xz - yw);
      break;
    case 1:
      rx = 2 * (xy - zw);
      ry = 1 - 2 * (xx + zz);
      rz = 2 * (yz + xw);
      break;
    case 2:
      rx = 2 * (xz + yw);
      ry = 2 * (yz - xw);
      rz = 1 - 2 * (xx + yy);
      break;
    default:
      logger.warning("Invalid column index.");
      throw new IllegalArgumentException("Invalid column index. " + i);
    }

    return new Vector3f(rx, ry, rz);
  }

  /**
   * <code>fromAngleAxis</code> sets this quaternion to the values specified by
   * an angle and an axis of rotation. This method creates an object, so use
   * fromAngleNormalAxis if your axis is already normalized.
   * 
   * @param angle
   *          the angle to rotate (in radians).
   * @param axis
   *          the axis of rotation.
   * @return this quaternion
   */
  public static Quaternion fromAngleAxis(float angle, Vector3f axis) {
    Vector3f normAxis = axis.normalize();
    return fromAngleNormalAxis(angle, normAxis);
  }

  /**
   * <code>fromAngleNormalAxis</code> sets this quaternion to the values
   * specified by an angle and a normalized axis of rotation.
   * 
   * @param angle
   *          the angle to rotate (in radians).
   * @param axis
   *          the axis of rotation (already normalized).
   */
  public static Quaternion fromAngleNormalAxis(float angle, Vector3f axis) {
    if (axis.x == 0 && axis.y == 0 && axis.z == 0) {
      return IDENTITY;
    }

    float halfAngle = 0.5f * angle;
    float sin = FastMath.sin(halfAngle);
    return new Quaternion(sin * axis.x, sin * axis.y, sin * axis.z,
        FastMath.cos(halfAngle));
  }

  /**
   * <code>slerp</code> sets this quaternion's value as an interpolation between
   * two other quaternions.
   * 
   * @param q1
   *          the first quaternion.
   * @param q2
   *          the second quaternion.
   * @param t
   *          the amount to interpolate between the two quaternions.
   */
  public static Quaternion slerp(Quaternion q1, Quaternion q2, float t) {
    return q1.slerp(q2, t);
  }

  /**
   * Sets the values of this quaternion to the slerp from itself to q2 by
   * changeAmnt
   * 
   * @param q2
   *          Final interpolation value
   * @param changeAmnt
   *          The amount diffrence
   */
  public Quaternion slerp(Quaternion q2, float changeAmnt) {
    if (this.x == q2.x && this.y == q2.y && this.z == q2.z && this.w == q2.w) {
      return this;
    }

    float result = (this.x * q2.x) + (this.y * q2.y) + (this.z * q2.z)
        + (this.w * q2.w);
    if (result < 0.0f) {
      // Negate the second quaternion and the result of the dot product
      q2 = q2.negate();
      result = -result;
    }

    // Set the first and second scale for the interpolation
    float scale0 = 1 - changeAmnt;
    float scale1 = changeAmnt;

    // Check if the angle between the 2 quaternions was big enough to
    // warrant such calculations
    if ((1 - result) > 0.1f) {
      // Get the angle between the 2 quaternions, and then store the sin()
      // of that angle
      float theta = FastMath.acos(result);
      float invSinTheta = 1f / FastMath.sin(theta);

      // Calculate the scale for q1 and q2, according to the angle and
      // it's sine value
      scale0 = FastMath.sin((1 - changeAmnt) * theta) * invSinTheta;
      scale1 = FastMath.sin((changeAmnt * theta)) * invSinTheta;
    }

    // Calculate the x, y, z and w values for the quaternion by using a
    // special
    // form of linear interpolation for quaternions.
    return new Quaternion(
        (scale0 * this.x) + (scale1 * q2.x),
        (scale0 * this.y) + (scale1 * q2.y),
        (scale0 * this.z) + (scale1 * q2.z),
        (scale0 * this.w) + (scale1 * q2.w));
  }

  /**
   * Sets the values of this quaternion to the nlerp from itself to q2 by blend.
   * 
   * @param q2
   * @param blend
   */
  public Quaternion nlerp(Quaternion q2, float blend) {
    float dot = dot(q2);
    float blendI = 1.0f - blend;
    QuaternionTemp q = new QuaternionTemp();
    if (dot < 0.0f) {
      q.x = blendI * x - blend * q2.x;
      q.y = blendI * y - blend * q2.y;
      q.z = blendI * z - blend * q2.z;
      q.w = blendI * w - blend * q2.w;
    } else {
      q.x = blendI * x + blend * q2.x;
      q.y = blendI * y + blend * q2.y;
      q.z = blendI * z + blend * q2.z;
      q.w = blendI * w + blend * q2.w;
    }
    return new Quaternion(q).normalize();
  }

  /**
   * <code>mult</code> multiplies this quaternion by a parameter quaternion. The
   * result is returned as a new quaternion. It should be noted that quaternion
   * multiplication is not commutative so q * p != p * q.
   * 
   * @param q
   *          the quaternion to multiply this quaternion by.
   * @return the new quaternion.
   */
  @Override
  public Quaternion mult(Quaternion q) {
    QuaternionTemp res = new QuaternionTemp();
    res.x = x * q.w + y * q.z - z * q.y + w * q.x;
    res.y = -x * q.z + y * q.w + z * q.x + w * q.y;
    res.z = x * q.y - y * q.x + z * q.w + w * q.z;
    res.w = -x * q.x - y * q.y - z * q.z + w * q.w;
    return new Quaternion(res);
  }

  /**
   * <code>apply</code> multiplies this quaternion by a parameter matrix
   * internally.
   * 
   * @param matrix
   *          the matrix to apply to this quaternion.
   */
  public Quaternion apply(Matrix3f matrix) {
    return mult(Quaternion.fromMatrix3f(matrix));
  }

  /**
   * 
   * <code>fromAxes</code> creates a <code>Quaternion</code> that represents the
   * coordinate system defined by three axes. These axes are assumed to be
   * orthogonal and no error checking is applied. Thus, the user must insure
   * that the three axes being provided indeed represents a proper right handed
   * coordinate system.
   * 
   * @param axis
   *          the array containing the three vectors representing the coordinate
   *          system.
   */
  public static Quaternion fromAxes(Vector3f[] axis) {
    return fromAxes(axis[0], axis[1], axis[2]);
  }

  /**
   * 
   * <code>fromAxes</code> creates a <code>Quaternion</code> that represents the
   * coordinate system defined by three axes. These axes are assumed to be
   * orthogonal and no error checking is applied. Thus, the user must insure
   * that the three axes being provided indeed represents a proper right handed
   * coordinate system.
   * 
   * @param xAxis
   *          vector representing the x-axis of the coordinate system.
   * @param yAxis
   *          vector representing the y-axis of the coordinate system.
   * @param zAxis
   *          vector representing the z-axis of the coordinate system.
   */
  public static Quaternion fromAxes(Vector3f xAxis, Vector3f yAxis,
      Vector3f zAxis) {
    return Quaternion.fromMatrix3f(new Matrix3f(xAxis.x, yAxis.x, zAxis.x, xAxis.y,
        yAxis.y, zAxis.y, xAxis.z, yAxis.z, zAxis.z));
  }

  /**
   * 
   * <code>toAxes</code> takes in an array of three vectors. Each vector
   * corresponds to an axis of the coordinate system defined by the quaternion
   * rotation.
   * 
   * @param axis
   *          the array of vectors to be filled.
   */
  public void toAxes(Vector3f axis[]) {
    Matrix3f tempMat = toRotationMatrix();
    axis[0] = tempMat.getColumn(0);
    axis[1] = tempMat.getColumn(1);
    axis[2] = tempMat.getColumn(2);
  }

  /**
   * <code>mult</code> multiplies this quaternion by a parameter vector. The
   * result is returned as a new vector.
   * 
   * @param v
   *          the vector to multiply this quaternion by.
   * @return the new vector.
   */
  public Vector3f mult(Vector3f v) {
    float tempX = w * w * v.x + 2 * y * w * v.z - 2 * z * w * v.y + x * x * v.x
        + 2 * y * x * v.y + 2 * z * x * v.z - z * z * v.x - y * y * v.x;
    float tempY = 2 * x * y * v.x + y * y * v.y + 2 * z * y * v.z + 2 * w * z
        * v.x - z * z * v.y + w * w * v.y - 2 * x * w * v.z - x * x * v.y;
    float tempZ = 2 * x * z * v.x + 2 * y * z * v.y + z * z * v.z - 2 * w * y
        * v.x - y * y * v.z + 2 * w * x * v.y - x * x * v.z + w * w * v.z;
    return new Vector3f(tempX, tempY, tempZ);
  }

  /**
   * <code>norm</code> returns the norm of this quaternion. This is the dot
   * product of this quaternion with itself.
   * 
   * @return the norm of the quaternion.
   */
  public float norm() {
    return dot(this);
  }

  /**
   * <code>inverse</code> returns the inverse of this quaternion as a new
   * quaternion. If this quaternion does not have an inverse (if its normal is 0
   * or less), then null is returned.
   * 
   * @return the inverse of this quaternion or null if the inverse does not
   *         exist.
   */
  @Override
  public Quaternion inverse() {
    float norm = norm();
    if (norm > 0.0) {
      float invNorm = 1.0f / norm;
      return new Quaternion(-x * invNorm, -y * invNorm, -z * invNorm, w
          * invNorm);
    }
    // return an invalid result to flag the error
    return null;
  }

  @Override
  protected Quaternion build(float x, float y, float z, float w) {
    return new Quaternion(x, y, z, w);
  }

  @Override
  protected Quaternion build(float[] v) {
    return new Quaternion(v[0], v[1], v[2], v[3]);
  }

  @Override
  protected Quaternion build(float s) {
    return new Quaternion(s, s, s, s);
  }

}
