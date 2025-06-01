/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

/**
 * <code>Matrix3f</code> defines a 3x3 matrix. Matrix data is maintained
 * internally and is accessible via the get and set methods. Convenience methods
 * are used for matrix operations as well as generating a matrix from a given
 * set of values.
 * 
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Matrix3f implements java.io.Serializable {
  static final long serialVersionUID = 1;
  protected final float m00, m01, m02;
  protected final float m10, m11, m12;
  protected final float m20, m21, m22;
  public static final Matrix3f ZERO = new Matrix3f(0, 0, 0, 0, 0, 0, 0, 0, 0);
  public static final Matrix3f IDENTITY = new Matrix3f();

  /**
   * Constructor instantiates a new <code>Matrix3f</code> object. The initial
   * values for the matrix is that of the identity matrix.
   * 
   */
  public Matrix3f() {
    m01 = m02 = m10 = m12 = m20 = m21 = 0;
    m00 = m11 = m22 = 1;
  }

  /**
   * constructs a matrix with the given values.
   * 
   * @param m00
   *          0x0 in the matrix.
   * @param m01
   *          0x1 in the matrix.
   * @param m02
   *          0x2 in the matrix.
   * @param m10
   *          1x0 in the matrix.
   * @param m11
   *          1x1 in the matrix.
   * @param m12
   *          1x2 in the matrix.
   * @param m20
   *          2x0 in the matrix.
   * @param m21
   *          2x1 in the matrix.
   * @param m22
   *          2x2 in the matrix.
   */
  public Matrix3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {

    this.m00 = m00;
    this.m01 = m01;
    this.m02 = m02;
    this.m10 = m10;
    this.m11 = m11;
    this.m12 = m12;
    this.m20 = m20;
    this.m21 = m21;
    this.m22 = m22;
  }

  /**
   * Recreate Matrix using the provided axis.
   * 
   * @param uAxis
   *          Vector3f
   * @param vAxis
   *          Vector3f
   * @param wAxis
   *          Vector3f
   */
  public Matrix3f(Vector3f uAxis, Vector3f vAxis, Vector3f wAxis) {
    m00 = uAxis.x;
    m10 = uAxis.y;
    m20 = uAxis.z;

    m01 = vAxis.x;
    m11 = vAxis.y;
    m21 = vAxis.z;

    m02 = wAxis.x;
    m12 = wAxis.y;
    m22 = wAxis.z;
  }

  /**
   * Copy constructor that creates a new <code>Matrix3f</code> object that is
   * the same as the provided matrix.
   * 
   * @param m
   *          the matrix to copy.
   */
  public Matrix3f(Matrix3f m) {
    m00 = m.m00;
    m01 = m.m01;
    m02 = m.m02;
    m10 = m.m10;
    m11 = m.m11;
    m12 = m.m12;
    m20 = m.m20;
    m21 = m.m21;
    m22 = m.m22;
  }

  /**
   * Copy constructor that creates a new <code>Matrix3f</code> object that is
   * the same as the provided matrix.
   * 
   * @param m
   *          the matrix to copy.
   */
  protected Matrix3f(Matrix3fTemp m) {
    m00 = m.m00;
    m01 = m.m01;
    m02 = m.m02;
    m10 = m.m10;
    m11 = m.m11;
    m12 = m.m12;
    m20 = m.m20;
    m21 = m.m21;
    m22 = m.m22;
  }

  /**
   * <code>fromAngleAxis</code> sets this matrix4f to the values specified by an
   * angle and an axis of rotation. This method creates an object, so use
   * fromAngleNormalAxis if your axis is already normalized.
   * 
   * @param angle
   *          the angle to rotate (in radians).
   * @param axis
   *          the axis of rotation.
   */
  public Matrix3f(float angle, Vector3f axis) {
    float fCos = FastMath.cos(angle);
    float fSin = FastMath.sin(angle);
    float fOneMinusCos = ((float) 1.0) - fCos;
    float fX2 = axis.x * axis.x;
    float fY2 = axis.y * axis.y;
    float fZ2 = axis.z * axis.z;
    float fXYM = axis.x * axis.y * fOneMinusCos;
    float fXZM = axis.x * axis.z * fOneMinusCos;
    float fYZM = axis.y * axis.z * fOneMinusCos;
    float fXSin = axis.x * fSin;
    float fYSin = axis.y * fSin;
    float fZSin = axis.z * fSin;

    m00 = fX2 * fOneMinusCos + fCos;
    m01 = fXYM - fZSin;
    m02 = fXZM + fYSin;
    m10 = fXYM + fZSin;
    m11 = fY2 * fOneMinusCos + fCos;
    m12 = fYZM - fXSin;
    m20 = fXZM - fYSin;
    m21 = fYZM + fXSin;
    m22 = fZ2 * fOneMinusCos + fCos;
  }

  /**
   * <code>get</code> retrieves a value from the matrix at the given position.
   * If the position is invalid a <code>JmeException</code> is thrown.
   * 
   * @param i
   *          the row index.
   * @param j
   *          the colum index.
   * @return the value at (i, j).
   */
  @SuppressWarnings("fallthrough")
  public float get(int i, int j) {
    switch (i) {
      case 0:
        switch (j) {
          case 0:
            return m00;
          case 1:
            return m01;
          case 2:
            return m02;
        }
      case 1:
        switch (j) {
          case 0:
            return m10;
          case 1:
            return m11;
          case 2:
            return m12;
        }
      case 2:
        switch (j) {
          case 0:
            return m20;
          case 1:
            return m21;
          case 2:
            return m22;
        }
    }
    throw new IllegalArgumentException("Invalid indices into matrix.");
  }

  /**
   * <code>get(float[])</code> returns the matrix in row-major or column-major
   * order.
   * 
   * @param data
   *          The array to return the data into. This array can be 9 or 16
   *          floats in size. Only the upper 3x3 are assigned to in the case of
   *          a 16 element array.
   * @param rowMajor
   *          True for row major storage in the array (translation in elements
   *          3, 7, 11 for a 4x4), false for column major (translation in
   *          elements 12, 13, 14 for a 4x4).
   */
  public void get(float[] data, boolean rowMajor) {
    if (data.length == 9) {
      if (rowMajor) {
        data[0] = m00;
        data[1] = m01;
        data[2] = m02;
        data[3] = m10;
        data[4] = m11;
        data[5] = m12;
        data[6] = m20;
        data[7] = m21;
        data[8] = m22;
      } else {
        data[0] = m00;
        data[1] = m10;
        data[2] = m20;
        data[3] = m01;
        data[4] = m11;
        data[5] = m21;
        data[6] = m02;
        data[7] = m12;
        data[8] = m22;
      }
    } else if (data.length == 16) {
      if (rowMajor) {
        data[0] = m00;
        data[1] = m01;
        data[2] = m02;
        data[4] = m10;
        data[5] = m11;
        data[6] = m12;
        data[8] = m20;
        data[9] = m21;
        data[10] = m22;
      } else {
        data[0] = m00;
        data[1] = m10;
        data[2] = m20;
        data[4] = m01;
        data[5] = m11;
        data[6] = m21;
        data[8] = m02;
        data[9] = m12;
        data[10] = m22;
      }
    } else {
      throw new IndexOutOfBoundsException("Array size must be 9 or 16 in Matrix3f.get().");
    }
  }

  /**
   * <code>getColumn</code> returns one of three columns specified by the
   * parameter. This column is returned as a <code>Vector3f</code> object.
   * 
   * @param i
   *          the column to retrieve. Must be between 0 and 2.
   * @return the column specified by the index.
   */
  public Vector3f getColumn(int i) {
    float vx, vy, vz;
    switch (i) {
      case 0:
        vx = m00;
        vy = m10;
        vz = m20;
        break;
      case 1:
        vx = m01;
        vy = m11;
        vz = m21;
        break;
      case 2:
        vx = m02;
        vy = m12;
        vz = m22;
        break;
      default:
        throw new IllegalArgumentException("Invalid column index. " + i);
    }
    return new Vector3f(vx, vy, vz);
  }

  /**
   * <code>getColumn</code> returns one of three rows as specified by the
   * parameter. This row is returned as a <code>Vector3f</code> object.
   * 
   * @param i
   *          the row to retrieve. Must be between 0 and 2.
   * @return the row specified by the index.
   */
  public Vector3f getRow(int i) {
    float vx, vy, vz;
    switch (i) {
      case 0:
        vx = m00;
        vy = m01;
        vz = m02;
        break;
      case 1:
        vx = m10;
        vy = m11;
        vz = m12;
        break;
      case 2:
        vx = m20;
        vy = m21;
        vz = m22;
        break;
      default:
        throw new IllegalArgumentException("Invalid row index. " + i);
    }
    return new Vector3f(vx, vy, vz);
  }

  public void fillFloatArray(float[] f, boolean columnMajor) {
    if (columnMajor) {
      f[0] = m00;
      f[1] = m10;
      f[2] = m20;
      f[3] = m01;
      f[4] = m11;
      f[5] = m21;
      f[6] = m02;
      f[7] = m12;
      f[8] = m22;
    } else {
      f[0] = m00;
      f[1] = m01;
      f[2] = m02;
      f[3] = m10;
      f[4] = m11;
      f[5] = m12;
      f[6] = m20;
      f[7] = m21;
      f[8] = m22;
    }
  }

  /**
   * @return true if this matrix is identity
   */
  public boolean isIdentity() {
    return (m00 == 1 && m01 == 0 && m02 == 0) && (m10 == 0 && m11 == 1 && m12 == 0) && (m20 == 0 && m21 == 0 && m22 == 1);
  }

  /**
   * <code>mult</code> multiplies this matrix by a given matrix. The result
   * matrix is returned as a new object. If the given matrix is null, a null
   * matrix is returned.
   * 
   * @param mat
   *          the matrix to multiply this matrix by.
   * @return the result matrix.
   */
  public Matrix3f mult(Matrix3f mat) {
    float temp00, temp01, temp02;
    float temp10, temp11, temp12;
    float temp20, temp21, temp22;
    temp00 = m00 * mat.m00 + m01 * mat.m10 + m02 * mat.m20;
    temp01 = m00 * mat.m01 + m01 * mat.m11 + m02 * mat.m21;
    temp02 = m00 * mat.m02 + m01 * mat.m12 + m02 * mat.m22;
    temp10 = m10 * mat.m00 + m11 * mat.m10 + m12 * mat.m20;
    temp11 = m10 * mat.m01 + m11 * mat.m11 + m12 * mat.m21;
    temp12 = m10 * mat.m02 + m11 * mat.m12 + m12 * mat.m22;
    temp20 = m20 * mat.m00 + m21 * mat.m10 + m22 * mat.m20;
    temp21 = m20 * mat.m01 + m21 * mat.m11 + m22 * mat.m21;
    temp22 = m20 * mat.m02 + m21 * mat.m12 + m22 * mat.m22;

    return new Matrix3f(temp00, temp01, temp02, temp10, temp11, temp12, temp20, temp21, temp22);
  }

  /**
   * <code>mult</code> multiplies this matrix by a given <code>Vector3f</code>
   * object. The result vector is returned. If the given vector is null, null
   * will be returned.
   * 
   * @param v
   *          the vector to multiply this matrix by.
   * @return the result vector.
   */
  public Vector3f mult(Vector3f v) {
    return new Vector3f(m00 * v.x + m01 * v.y + m02 * v.z, m10 * v.x + m11 * v.y + m12 * v.z, m20 * v.x + m21 * v.y + m22 * v.z);
  }

  private static class Matrix3fTemp {
    public float m00, m01, m02;
    public float m10, m11, m12;
    public float m20, m21, m22;
  };

  /**
   * Inverts this matrix as a new Matrix3f.
   * 
   * @return The new inverse matrix
   */
  public Matrix3f invert() {
    float det = determinant();
    if (FastMath.abs(det) <= FastMath.FLT_EPSILON) {
      return ZERO;
    }

    float idet = 1f / det;
    Matrix3fTemp store = new Matrix3fTemp();
    store.m00 = m11 * m22 - m12 * m21;
    store.m01 = m02 * m21 - m01 * m22;
    store.m02 = m01 * m12 - m02 * m11;
    store.m10 = m12 * m20 - m10 * m22;
    store.m11 = m00 * m22 - m02 * m20;
    store.m12 = m02 * m10 - m00 * m12;
    store.m20 = m10 * m21 - m11 * m20;
    store.m21 = m01 * m20 - m00 * m21;
    store.m22 = m00 * m11 - m01 * m10;
    store.m00 *= idet;
    store.m01 *= idet;
    store.m02 *= idet;
    store.m10 *= idet;
    store.m11 *= idet;
    store.m12 *= idet;
    store.m20 *= idet;
    store.m21 *= idet;
    store.m22 *= idet;
    return new Matrix3f(store);
  }

  /**
   * Returns a new matrix representing the adjoint of this matrix.
   * 
   * @return The adjoint matrix
   */
  public Matrix3f adjoint() {
    Matrix3fTemp store = new Matrix3fTemp();
    store.m00 = m11 * m22 - m12 * m21;
    store.m01 = m02 * m21 - m01 * m22;
    store.m02 = m01 * m12 - m02 * m11;
    store.m10 = m12 * m20 - m10 * m22;
    store.m11 = m00 * m22 - m02 * m20;
    store.m12 = m02 * m10 - m00 * m12;
    store.m20 = m10 * m21 - m11 * m20;
    store.m21 = m01 * m20 - m00 * m21;
    store.m22 = m00 * m11 - m01 * m10;
    return new Matrix3f(store);
  }

  /**
   * <code>determinant</code> generates the determinant of this matrix.
   * 
   * @return the determinant
   */
  public float determinant() {
    float fCo00 = m11 * m22 - m12 * m21;
    float fCo10 = m12 * m20 - m10 * m22;
    float fCo20 = m10 * m21 - m11 * m20;
    float fDet = m00 * fCo00 + m01 * fCo10 + m02 * fCo20;
    return fDet;
  }

  /**
   * <code>transpose</code> transposes this Matrix. This is inconsistent with
   * general value vs local semantics, but is preserved for backwards
   * compatibility. Use transposeNew() to transpose to a new object (value).
   * 
   * @return this object for chaining.
   */
  public Matrix3f transpose() {
    return new Matrix3f(m00, m10, m20, m01, m11, m21, m02, m12, m22);
  }

  /**
   * 
   * <code>hashCode</code> returns the hash code value as an integer and is
   * supported for the benefit of hashing based collection classes such as
   * Hashtable, HashMap, HashSet etc.
   * 
   * @return the hashcode for this instance of Matrix4f.
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    int hash = 37;
    hash = 37 * hash + Float.floatToIntBits(m00);
    hash = 37 * hash + Float.floatToIntBits(m01);
    hash = 37 * hash + Float.floatToIntBits(m02);

    hash = 37 * hash + Float.floatToIntBits(m10);
    hash = 37 * hash + Float.floatToIntBits(m11);
    hash = 37 * hash + Float.floatToIntBits(m12);

    hash = 37 * hash + Float.floatToIntBits(m20);
    hash = 37 * hash + Float.floatToIntBits(m21);
    hash = 37 * hash + Float.floatToIntBits(m22);

    return hash;
  }

  /**
   * are these two matrices the same? they are is they both have the same mXX
   * values.
   * 
   * @param o
   *          the object to compare for equality
   * @return true if they are equal
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Matrix3f) || o == null) {
      return false;
    }

    if (this == o) {
      return true;
    }

    Matrix3f comp = (Matrix3f) o;
    if (Float.compare(m00, comp.m00) != 0) {
      return false;
    }
    if (Float.compare(m01, comp.m01) != 0) {
      return false;
    }
    if (Float.compare(m02, comp.m02) != 0) {
      return false;
    }

    if (Float.compare(m10, comp.m10) != 0) {
      return false;
    }
    if (Float.compare(m11, comp.m11) != 0) {
      return false;
    }
    if (Float.compare(m12, comp.m12) != 0) {
      return false;
    }

    if (Float.compare(m20, comp.m20) != 0) {
      return false;
    }
    if (Float.compare(m21, comp.m21) != 0) {
      return false;
    }
    if (Float.compare(m22, comp.m22) != 0) {
      return false;
    }

    return true;
  }

  /**
   * <code>scale</code> scales the operation performed by this matrix on a
   * per-component basis.
   * 
   * @param scale
   *          The scale applied to each of the X, Y and Z output values.
   * @return m
   */
  public Matrix3f scale(Vector3f scale) {
    return new Matrix3f(m00 * scale.x, m01 * scale.y, m02 * scale.z, m10 * scale.x, m11 * scale.y, m12 * scale.z, m20 * scale.x, m21 * scale.y, m22 * scale.z);
  }

  static boolean equalIdentity(Matrix3f mat) {
    if (Math.abs(mat.m00 - 1) > 1e-4) {
      return false;
    }
    if (Math.abs(mat.m11 - 1) > 1e-4) {
      return false;
    }
    if (Math.abs(mat.m22 - 1) > 1e-4) {
      return false;
    }

    if (Math.abs(mat.m01) > 1e-4) {
      return false;
    }
    if (Math.abs(mat.m02) > 1e-4) {
      return false;
    }

    if (Math.abs(mat.m10) > 1e-4) {
      return false;
    }
    if (Math.abs(mat.m12) > 1e-4) {
      return false;
    }

    if (Math.abs(mat.m20) > 1e-4) {
      return false;
    }
    if (Math.abs(mat.m21) > 1e-4) {
      return false;
    }
    return true;
  }
}
