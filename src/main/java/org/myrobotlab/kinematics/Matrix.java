package org.myrobotlab.kinematics;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Encapsulates a 4x4 matrix
 *
 *
 */
public class Matrix implements Serializable {

  public final static Logger log = LoggerFactory.getLogger(Matrix.class);

  private static final long serialVersionUID = 1L;

  protected int numRows;

  protected int numCols;

  public double[][] elements;

  /**
   * @param sx
   *          scaling in the x direction
   * @param sy
   *          scaling in the y direction
   * @param sz
   *          scaling in the z direction
   * @return the associated scaling transformation matrix
   */
  public static Matrix scaling(double sx, double sy, double sz) {
    Matrix S = new Matrix();
    S.elements[0][0] = sx;
    S.elements[1][1] = sy;
    S.elements[2][2] = sz;
    S.elements[3][3] = 1;
    return S;
  }

  /**
   * @param tx
   *          translations in the x direction
   * @param ty
   *          translations in the y direction
   * @param tz
   *          translations in the z direction
   * @return the associated translation transformation matrix
   */
  public static Matrix translation(double tx, double ty, double tz) {
    Matrix T = new Matrix();
    T.elements[0][0] = 1;
    T.elements[1][1] = 1;
    T.elements[2][2] = 1;
    T.elements[3][3] = 1;
    T.elements[0][3] = tx;
    T.elements[1][3] = ty;
    T.elements[2][3] = tz;
    return T;
  }

  /**
   * Right-handed rotation about X. Prefer this over {@link #xRotation(double)},
   * which uses the opposite sign convention.
   *
   * @param theta
   *          an angle in radians
   * @return the 4x4 rotation matrix
   */
  public static Matrix rotationX(double theta) {
    Matrix R = identity(4);
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    R.elements[1][1] = c;
    R.elements[1][2] = -s;
    R.elements[2][1] = s;
    R.elements[2][2] = c;
    return R;
  }

  /**
   * Right-handed rotation about Y. Prefer this over {@link #yRotation(double)}.
   *
   * @param theta
   *          an angle in radians
   * @return the 4x4 rotation matrix
   */
  public static Matrix rotationY(double theta) {
    Matrix R = identity(4);
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    R.elements[0][0] = c;
    R.elements[0][2] = s;
    R.elements[2][0] = -s;
    R.elements[2][2] = c;
    return R;
  }

  /**
   * Right-handed rotation about Z. Prefer this over {@link #zRotation(double)}.
   * This is the convention the Denavit-Hartenberg link matrices use, so it is
   * also the joint variable of a {@link DHLink}.
   *
   * @param theta
   *          an angle in radians
   * @return the 4x4 rotation matrix
   */
  public static Matrix rotationZ(double theta) {
    Matrix R = identity(4);
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    R.elements[0][0] = c;
    R.elements[0][1] = -s;
    R.elements[1][0] = s;
    R.elements[1][1] = c;
    return R;
  }

  /**
   * Right-handed rotation of {@code theta} radians about an arbitrary axis
   * through the origin (Rodrigues). The axis is normalized; a degenerate axis
   * yields identity.
   */
  public static Matrix rotationAboutAxis(double ax, double ay, double az, double theta) {
    double len = Math.sqrt(ax * ax + ay * ay + az * az);
    if (len < 1e-12) {
      return identity(4);
    }
    double x = ax / len;
    double y = ay / len;
    double z = az / len;
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    double t = 1 - c;
    Matrix R = identity(4);
    R.elements[0][0] = t * x * x + c;
    R.elements[0][1] = t * x * y - s * z;
    R.elements[0][2] = t * x * z + s * y;
    R.elements[1][0] = t * x * y + s * z;
    R.elements[1][1] = t * y * y + c;
    R.elements[1][2] = t * y * z - s * x;
    R.elements[2][0] = t * x * z - s * y;
    R.elements[2][1] = t * y * z + s * x;
    R.elements[2][2] = t * z * z + c;
    return R;
  }

  /**
   * Rigid frame whose Z axis is {@code (zx,zy,zz)} and whose origin is
   * {@code (ox,oy,oz)}. X and Y are an arbitrary but deterministic orthonormal
   * completion.
   *
   * <p>
   * Used to turn a measured joint (a point on the axis plus the axis direction)
   * into a link frame. Because the completion only spins the frame about its own
   * Z, and Z is the joint variable, the choice does not affect kinematics.
   * </p>
   */
  public static Matrix frameFromZAxis(double zx, double zy, double zz, double ox, double oy, double oz) {
    double len = Math.sqrt(zx * zx + zy * zy + zz * zz);
    if (len < 1e-12) {
      Matrix m = identity(4);
      m.elements[0][3] = ox;
      m.elements[1][3] = oy;
      m.elements[2][3] = oz;
      return m;
    }
    double[] z = { zx / len, zy / len, zz / len };
    // pick the world axis least aligned with z so the cross product is stable
    double[] helper = { 1, 0, 0 };
    double ax = Math.abs(z[0]);
    double ay = Math.abs(z[1]);
    double az = Math.abs(z[2]);
    if (ay <= ax && ay <= az) {
      helper = new double[] { 0, 1, 0 };
    } else if (az <= ax && az <= ay) {
      helper = new double[] { 0, 0, 1 };
    }
    double[] x = cross(helper, z);
    double xLen = Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
    x = new double[] { x[0] / xLen, x[1] / xLen, x[2] / xLen };
    double[] y = cross(z, x);

    Matrix m = identity(4);
    for (int r = 0; r < 3; r++) {
      m.elements[r][0] = x[r];
      m.elements[r][1] = y[r];
      m.elements[r][2] = z[r];
    }
    m.elements[0][3] = ox;
    m.elements[1][3] = oy;
    m.elements[2][3] = oz;
    return m;
  }

  private static double[] cross(double[] a, double[] b) {
    return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
  }

  /**
   * @param theta
   *          an angle in radians
   * @return the associated x-axis rotation transformation matrix
   * @deprecated left-handed sign convention — use {@link #rotationX(double)}
   */
  @Deprecated
  public static Matrix xRotation(double theta) {
    Matrix R = new Matrix();
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    R.elements[0][0] = 1;
    R.elements[1][1] = c;
    R.elements[2][2] = c;
    R.elements[3][3] = 1;
    R.elements[1][2] = s;
    R.elements[2][1] = -s;
    return R;
  }

  /**
   * @param theta
   *          an angle in radians
   * @return the associated y-axis rotation transformation matrix
   * @deprecated left-handed sign convention — use {@link #rotationY(double)}
   */
  @Deprecated
  public static Matrix yRotation(double theta) {
    Matrix R = new Matrix();
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    R.elements[0][0] = c;
    R.elements[1][1] = 1;
    R.elements[2][2] = c;
    R.elements[3][3] = 1;
    R.elements[2][0] = s;
    R.elements[0][2] = -s;
    return R;
  }

  /**
   * @param theta
   *          an angle in radians
   * @return the associated z-axis rotation transformation matrix
   * @deprecated left-handed sign convention — use {@link #rotationZ(double)}
   */
  @Deprecated
  public static Matrix zRotation(double theta) {
    Matrix R = new Matrix();
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    R.elements[0][0] = c;
    R.elements[1][1] = c;
    R.elements[2][2] = 1;
    R.elements[3][3] = 1;
    R.elements[0][1] = s;
    R.elements[1][0] = -s;
    return R;
  }

  /**
   * Constructs new 4x4 matrix, initializes to it to zeros
   */
  Matrix() {
    numRows = 4;
    numCols = 4;
    elements = new double[numRows][numCols];
    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++) {
        elements[r][c] = 0.0;
      }
  }

  public Matrix(int rows, int cols) {
    numRows = rows;
    numCols = cols;
    elements = new double[numRows][numCols];
    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++) {
        elements[r][c] = 0.0;
      }
  }

  /**
   * Copy constructor
   */
  public Matrix(Matrix m) {
    numRows = m.numRows;
    numCols = m.numCols;
    elements = new double[numRows][numCols];
    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++)
        this.elements[r][c] = m.elements[r][c];
  }

  public static Matrix identity(int n) {
    Matrix m = new Matrix(n, n);
    for (int i = 0; i < n; i++) {
      m.elements[i][i] = 1.0;
    }
    return m;
  }

  /**
   * Rigid 4x4: {@code T(tx,ty,tz) * Rz(roll) * Ry(yaw) * Rx(pitch)} with
   * right-handed rotations, angles in radians. Inverse of
   * {@link #toRollPitchYaw()}.
   */
  public static Matrix rigid(double tx, double ty, double tz, double roll, double pitch, double yaw) {
    Matrix tr = translation(tx, ty, tz);
    Matrix rot = rotationZ(roll).multiply(rotationY(yaw).multiply(rotationX(pitch)));
    return tr.multiply(rot);
  }

  /**
   * Decompose the rotation of a rigid transform into the same
   * {@code Rz(roll) Ry(yaw) Rx(pitch)} convention {@link #rigid} builds, in
   * radians.
   *
   * @return {@code { roll, pitch, yaw }}
   */
  public double[] toRollPitchYaw() {
    double sinYaw = -elements[2][0];
    sinYaw = Math.max(-1.0, Math.min(1.0, sinYaw));
    double yaw = Math.asin(sinYaw);
    double roll;
    double pitch;
    if (Math.abs(Math.cos(yaw)) > 1e-6) {
      roll = Math.atan2(elements[1][0], elements[0][0]);
      pitch = Math.atan2(elements[2][1], elements[2][2]);
    } else {
      roll = 0;
      pitch = Math.atan2(elements[0][1], elements[1][1]);
    }
    return new double[] { roll, pitch, yaw };
  }

  /**
   * Affine 4x4 with optional axis scaling.
   *
   * @deprecated a negative scale is a reflection, which turns a kinematic chain
   *             left-handed and silently mirrors every solved joint angle. Use
   *             {@link #rigid} instead.
   */
  @Deprecated
  public static Matrix affine(double tx, double ty, double tz, double roll, double pitch, double yaw, double sx, double sy, double sz) {
    Matrix rigid = rigid(tx, ty, tz, roll, pitch, yaw);
    if (sx == 1.0 && sy == 1.0 && sz == 1.0) {
      return rigid;
    }
    return rigid.multiply(scaling(sx, sy, sz));
  }

  /**
   * Transform a point by this 4x4 affine matrix (last row assumed {@code 0 0 0 1}).
   */
  public Point transformPoint(Point p) {
    if (p == null) {
      return null;
    }
    double x = elements[0][0] * p.getX() + elements[0][1] * p.getY() + elements[0][2] * p.getZ() + elements[0][3];
    double y = elements[1][0] * p.getX() + elements[1][1] * p.getY() + elements[1][2] * p.getZ() + elements[1][3];
    double z = elements[2][0] * p.getX() + elements[2][1] * p.getY() + elements[2][2] * p.getZ() + elements[2][3];
    return new Point(x, y, z, p.getRoll(), p.getPitch(), p.getYaw());
  }

  /**
   * Inverse of an affine 4x4 {@code [A t; 0 1]}. Returns null if A is singular.
   */
  public Matrix invertAffine() {
    if (numRows != 4 || numCols != 4) {
      return null;
    }
    double[][] a = new double[3][3];
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        a[r][c] = elements[r][c];
      }
    }
    double det = a[0][0] * (a[1][1] * a[2][2] - a[1][2] * a[2][1]) - a[0][1] * (a[1][0] * a[2][2] - a[1][2] * a[2][0]) + a[0][2] * (a[1][0] * a[2][1] - a[1][1] * a[2][0]);
    if (Math.abs(det) < 1e-12) {
      return null;
    }
    double invDet = 1.0 / det;
    double[][] invA = new double[3][3];
    invA[0][0] = (a[1][1] * a[2][2] - a[1][2] * a[2][1]) * invDet;
    invA[0][1] = (a[0][2] * a[2][1] - a[0][1] * a[2][2]) * invDet;
    invA[0][2] = (a[0][1] * a[1][2] - a[0][2] * a[1][1]) * invDet;
    invA[1][0] = (a[1][2] * a[2][0] - a[1][0] * a[2][2]) * invDet;
    invA[1][1] = (a[0][0] * a[2][2] - a[0][2] * a[2][0]) * invDet;
    invA[1][2] = (a[0][2] * a[1][0] - a[0][0] * a[1][2]) * invDet;
    invA[2][0] = (a[1][0] * a[2][1] - a[1][1] * a[2][0]) * invDet;
    invA[2][1] = (a[0][1] * a[2][0] - a[0][0] * a[2][1]) * invDet;
    invA[2][2] = (a[0][0] * a[1][1] - a[0][1] * a[1][0]) * invDet;
    double tx = elements[0][3];
    double ty = elements[1][3];
    double tz = elements[2][3];
    Matrix inv = identity(4);
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        inv.elements[r][c] = invA[r][c];
      }
    }
    inv.elements[0][3] = -(invA[0][0] * tx + invA[0][1] * ty + invA[0][2] * tz);
    inv.elements[1][3] = -(invA[1][0] * tx + invA[1][1] * ty + invA[1][2] * tz);
    inv.elements[2][3] = -(invA[2][0] * tx + invA[2][1] * ty + invA[2][2] * tz);
    return inv;
  }

  /**
   * @param m
   *          a Matrix
   * @return a new matrix which is equal to the sum of this + m
   */
  public Matrix addTo(Matrix m) {
    if (numRows != m.numRows || numCols != m.numCols) {
      log.info("dimensions bad in addTo()");
      return null;
    }
    Matrix ret = new Matrix(numRows, numCols);

    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++)
        ret.elements[r][c] = this.elements[r][c] + m.elements[r][c];

    return ret;
  }

  /**
   * Computes the dot product (or scalar product) of two matrices by multiplying
   * corresponding elements and summing all the products.
   * 
   * @param m
   *          A Matrix with the same dimensions
   * @return the dot product (scalar product)
   */
  public Double dot(Matrix m) {
    if (numRows != m.numRows || numCols != m.numCols) {
      log.info("dimensions bad in dot()");
      return 0.0;
    }
    double sum = 0;

    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++)
        sum += this.elements[r][c] * m.elements[r][c];

    return sum;
  }

  /**
   * @param val
   *          a scalar
   * @return true if and only if all elements of the matrix equal val
   */
  public boolean equals(double val) {
    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++)
        if (Math.abs(elements[r][c] - val) > .0001)
          return false;
    return true;
  }

  public int getNumCols() {
    return numCols;
  }

  public int getNumRows() {
    return numRows;
  }

  /**
   * Scalar multiplication- multiplies each element by a scalar
   * 
   * @param s
   *          a scalar
   * @return a new matrix which is equal to the product of s*this
   */
  public Matrix multiply(double s) {
    Matrix ret = new Matrix(numRows, numCols);
    for (int i = 0; i < numRows; i++)
      for (int j = 0; j < numCols; j++)
        ret.elements[i][j] = elements[i][j] * s;
    return ret;
  }

  /**
   * @param m
   *          a Matrix
   * @return a new matrix which is equal to the product of this*m
   */
  public Matrix multiply(Matrix m) {
    Matrix ret = new Matrix(numRows, m.numCols);
    if (numCols != m.numRows) {
      log.info("dimensions bad in multiply()");
      return ret;
    }

    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < m.numCols; c++) {
        for (int k = 0; k < numCols; k++) {
          ret.elements[r][c] += this.elements[r][k] * m.elements[k][c];
        }
      }

    return ret;
  }

  /**
   * Calculates the matrix's Moore-Penrose pseudoinverse
   * 
   * @return an MxN matrix which is the matrix's pseudoinverse.
   */
  public Matrix pseudoInverse() {

    int r, c;
    int k = 1;
    Matrix ak = new Matrix(numRows, 1);
    Matrix dk, ck, bk;
    Matrix R_plus;
    try {
      for (r = 0; r < numRows; r++) {
        ak.elements[r][0] = this.elements[r][0];
      }

      if (!ak.equals(0.0)) {
        R_plus = ak.transpose().multiply(1.0 / (ak.dot(ak)));
      } else {
        R_plus = new Matrix(1, numCols);
      }

      while (k < this.numCols) {

        for (r = 0; r < numRows; r++) {
          ak.elements[r][0] = this.elements[r][k];
        }

        dk = R_plus.multiply(ak);
        Matrix T = new Matrix(numRows, k);
        for (r = 0; r < numRows; r++) {
          for (c = 0; c < k; c++) {
            T.elements[r][c] = this.elements[r][c];
          }
        }
        ck = ak.subtractFrom(T.multiply(dk));

        if (!ck.equals(0.0)) {
          bk = ck.transpose().multiply(1.0 / (ck.dot(ck)));
        } else {
          bk = dk.transpose().multiply(1.0 / (1.0 + dk.dot(dk))).multiply(R_plus);
        }

        Matrix N = R_plus.subtractFrom(dk.multiply(bk));
        R_plus = new Matrix(N.numRows + 1, N.numCols);

        for (r = 0; r < N.numRows; r++) {
          for (c = 0; c < N.numCols; c++) {
            R_plus.elements[r][c] = N.elements[r][c];
          }
        }
        for (c = 0; c < N.numCols; c++) {
          R_plus.elements[R_plus.numRows - 1][c] = bk.elements[0][c];
        }
        k++;
      }
      return R_plus;
    } catch (ArrayIndexOutOfBoundsException e) {
      return null;
    }
  }

  /**
   * @param m
   *          a Matrix
   * @return a new matrix which is equal to the difference of this - m
   */
  public Matrix subtractFrom(Matrix m) {
    Matrix ret = new Matrix(numRows, numCols);
    if (numRows != m.numRows || numCols != m.numCols) {
      log.info("dimensions bad in substractFrom()");
      return ret;
    }

    for (int r = 0; r < numRows; r++) {
      for (int c = 0; c < numCols; c++) {
        ret.elements[r][c] = this.elements[r][c] - m.elements[r][c];
      }
    }

    return ret;
  }

  /**
   * @return a String representation of the matrix
   */
  @Override
  public String toString() {
    // TODO: do this better.
    NumberFormat formatter = new DecimalFormat("#0.00000");
    StringBuffer buf = new StringBuffer();
    buf.append("[\n");
    for (int r = 0; r < numRows; r++) {
      buf.append(" [ ");
      for (int c = 0; c < numCols; c++) {
        buf.append(formatter.format(elements[r][c]));
        buf.append(" ");
      }
      buf.append("]\n");
    }
    buf.append("]");
    return buf.toString();
  }

  /**
   * @return the transposed matrix with dimensions numCols x numRows
   */
  public Matrix transpose() {
    Matrix ret = new Matrix(numCols, numRows);

    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++)
        ret.elements[c][r] = elements[r][c];
    return ret;
  }
}
