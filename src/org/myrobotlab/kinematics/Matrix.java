package org.myrobotlab.kinematics;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Encapsulates a 4x4 matrix
 *
 *
 */
public class Matrix implements Serializable {

  private static final long serialVersionUID = 1L;

  protected int numRows;

  protected int numCols;

  public double[][] elements;

  /**
   * @param sx scaling in the x direction
   * @param sy scaling in the y direction
   * @param sz scaling in the z direction
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
   * @param tx translations in the x direction
   * @param ty translations in the y direction         
   * @param tz translations in the z direction
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
   * @param theta
   *          an angle in radians
   * @return the associated x-axis rotation transformation matrix
   */
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
   */
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
   */
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
  Matrix(Matrix m) {
    numRows = m.numRows;
    numCols = m.numCols;
    elements = new double[numRows][numCols];
    for (int r = 0; r < numRows; r++)
      for (int c = 0; c < numCols; c++)
        this.elements[r][c] = m.elements[r][c];
  }

  /**
   * @param m
   *          a Matrix
   * @return a new matrix which is equal to the sum of this + m
   */
  public Matrix addTo(Matrix m) {
    if (numRows != m.numRows || numCols != m.numCols) {
      System.out.println("dimensions bad in addTo()");
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
      System.out.println("dimensions bad in dot()");
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
      System.out.println("dimensions bad in multiply()");
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
    try{
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
    }
    catch (ArrayIndexOutOfBoundsException e){
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
      System.out.println("dimensions bad in substractFrom()");
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
