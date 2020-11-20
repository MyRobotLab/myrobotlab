package org.saintandreas.math;

import java.nio.FloatBuffer;

import javax.annotation.Nonnull;

public abstract class Vector<ResultType extends Vector<ResultType>> {

  @SuppressWarnings("unchecked")
  private final ResultType This() {
    return (ResultType) this;
  }

  protected abstract ResultType build(float[] v);

  protected abstract ResultType build(float s);

  public abstract float[] toArray();

  public abstract float angleBetween(@Nonnull ResultType v);

  /**
   * Returns true if this vector is a unit vector (length() ~= 1), returns false
   * otherwise.
   * 
   * @return true if this vector is a unit vector (length() ~= 1), or false
   *         otherwise.
   */
  public final boolean isUnitVector() {
    return FastMath.isWithinEpsilon(lengthSquared(), 1);
  }

  /**
   * <code>length</code> calculates the magnitude of this vector.
   * 
   * @return the length or magnitude of the vector.
   */
  public final float length() {
    return FastMath.sqrt(lengthSquared());
  }

  /**
   * are these two vectors almost the same? they both have the same x and y
   * values (within epsilon).
   * 
   * @param o
   *          the object to compare for equality
   * @return true if they are equal
   */
  public final boolean equalsEpsilon(ResultType v) {
    return equalsEpsilon(v, FastMath.ZERO_TOLERANCE);
  }

  /**
   * synonym for mult
   * 
   * @param scalar
   *          the value to multiply this vector by.
   * @return the new vector.
   */
  public final ResultType scale(float scalar) {
    return mult(scalar);
  }

  /**
   * <code>divide</code> divides the values of this vector by a scalar and
   * returns the result. The values of this vector remain untouched.
   * 
   * @param scalar
   *          the value to divide this vectors attributes by.
   * @return the result <code>Vector</code>.
   */
  public final ResultType divide(float scalar) {
    return mult(1f / scalar);
  }

  /**
   * <code>negate</code> returns the negative of this vector. All values are
   * negated and set to a new vector.
   * 
   * @return the negated vector.
   */
  public final ResultType negate() {
    return mult(-1);
  }

  /**
   * <code>subtract</code> subtracts the values of a given vector from those of
   * this vector creating a new vector object. If the provided vector is null,
   * an exception is thrown.
   * 
   * @param vec
   *          the vector to subtract from this vector.
   * @return the result vector.
   */
  public final ResultType subtract(ResultType vec) {
    return add(vec.negate());
  }

  /**
   * <code>normalize</code> returns the unit vector of this vector.
   * 
   * @return unit vector of this vector.
   */
  public final ResultType normalize() {
    float lengthSquared = lengthSquared();
    if (lengthSquared == 0 && FastMath.isWithinEpsilon(lengthSquared, 1)) {
      return This();
    }

    return divide(FastMath.sqrt(lengthSquared));
  }

  /**
   * <code>distance</code> calculates the distance between this vector and
   * vector v.
   * 
   * @param v
   *          the second vector to determine the distance.
   * @return the distance between the two vectors.
   */
  public final float distance(ResultType v) {
    return FastMath.sqrt(distanceSquared(v));
  }


  /**
   * <code>divide</code> divides the values of this vector by a scalar and
   * returns the result. The values of this vector remain untouched.
   *
   * @param scalar
   *            the value to divide this vectors attributes by.
   * @return the result <code>Vector</code>.
   */
  public final ResultType divide(ResultType v) {
      return mult(v.inverse());
  }
  
  public final void fillBuffer(FloatBuffer buffer) {
    buffer.put(toArray());
  }


  /**
   *
   * <code>scaleAdd</code> multiplies this vector by a scalar then adds the
   * given Vector3f.
   *
   * @param scalar
   *            the value to multiply this vector by.
   * @param add
   *            the value to add
   */
  public final ResultType scaleAdd(float scalar, ResultType add) {
    return mult(scalar).add(add);
  }

  public final ResultType project(ResultType other){
      float n = this.dot(other); // A . B
      float d = other.lengthSquared(); // |B|^2
      return other.normalize().mult(n/d);
  }


  public final ResultType add(@Nonnull ResultType v) {
    float[] a = toArray();
    float[] b = v.toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] += b[i];
    }
    return build(a);
  }

  public final ResultType add(@Nonnull float s) {
    float[] a = toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] += s;
    }
    return build(a);
  }

  public final ResultType subtract(@Nonnull float s) {
    return add(-s);
  }

  public final ResultType mult(float scalar) {
    float[] a = toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] *= scalar;
    }
    return build(a);
  }

  /**
   * Not marked final as quaternions have a different
   * idea of the inverse 
   * @return
   */
  public ResultType mult(@Nonnull ResultType v) {
    float[] a = toArray();
    float[] b = v.toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] *= b[i];
    }
    return build(a);
  }

  /**
   * Not marked final as quaternions have a different
   * idea of the inverse 
   * @return
   */
  public ResultType inverse() {
    float[] a = toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] = 1f / a[i];
    }
    return build(a);
  }

  
  public final float dot(@Nonnull ResultType v) {
    float[] a = toArray();
    float[] b = v.toArray();
    float result = 0;
    for (int i = 0; i < a.length; ++i) {
      result += a[i] * b[i];
    }
    return result;
  }

  public final ResultType interpolate(@Nonnull ResultType v, float changeAmnt) {
    float[] a = toArray();
    float[] b = v.toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] = FastMath.interpolateLinear(changeAmnt, a[i], b[i]);
    }
    return build(a);
  }


  /**
   * <code>maxLocal</code> computes the maximum value for each
   * component in this and <code>other</code> vector. The result is stored
   * in this vector.
   * @param other
   */
  public final ResultType max(ResultType v){
    float[] a = toArray();
    float[] b = v.toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] = Math.max(a[i], b[i]);
    }
    return build(a);
  }

  /**
   * <code>minLocal</code> computes the minimum value for each
   * component in this and <code>other</code> vector. The result is stored
   * in this vector.
   * @param other
   */
  public final ResultType min(ResultType v){
    float[] a = toArray();
    float[] b = v.toArray();
    for (int i = 0; i < a.length; ++i) {
      a[i] = Math.min(a[i], b[i]);
    }
    return build(a);
  }

  /**
   * are these two vectors almost the same? they both have the same x and y
   * values (within epsilon).
   * 
   * @param o
   *          the object to compare for equality
   * @return true if they are equal
   */
  public final boolean equalsEpsilon(ResultType v, float epsilon) {
    float[] a = toArray();
    float[] b = v.toArray();
    for (int i = 0; i < a.length; ++i) {
      if (!FastMath.isWithinEpsilon(a[i], b[i], epsilon)) {
        return false;
      }
    }
    return true;
  }

  /**
   * <code>distanceSquared</code> calculates the distance squared between this
   * vector and vector v.
   * 
   * @param v
   *          the second vector to determine the distance squared.
   * @return the distance squared between the two vectors.
   */
  public final float distanceSquared(@Nonnull ResultType v) {
    float[] a = toArray();
    float[] b = v.toArray();
    float result = 0;
    for (int i = 0; i < a.length; ++i) {
      float f = a[i] - b[i];
      result += f * f;
    }
    return result;
  }

  /**
   * <code>lengthSquared</code> calculates the squared value of the magnitude of
   * the vector.
   * 
   * @return the magnitude squared of the vector.
   */
  public final float lengthSquared() {
    float[] a = toArray();
    float result = 0;
    for (int i = 0; i < a.length; ++i) {
      float f = a[i];
      result += f * f;
    }
    return result;
  }

  public final boolean isValid() {
    for (float f : toArray()) {
      if (Float.isNaN(f) || Float.isInfinite(f)) {
        return false;
      }
    }
    return true;
  }


  /**
   * <code>hashCode</code> returns a unique code for this vector object based on
   * it's values. If two vectors are logically equivalent, they will return the
   * same hash code value.
   * 
   * @return the hash code value of this vector.
   */
  @Override
  public final int hashCode() {
    int hash = 37;
    for (float f : toArray()) {
      hash += 37 * hash + Float.floatToIntBits(f);
    }
    return hash;
  }
}
