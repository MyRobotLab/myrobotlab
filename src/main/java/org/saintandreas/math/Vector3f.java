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

import javax.annotation.Nonnull;

/**
 * <code>Vector3f</code> defines a Vector for a three float value tuple.
 * <code>Vector3f</code> can represent any three dimensional value, such as a
 * vertex, a normal, etc. Utility methods are also included to aid in
 * mathematical calculations.
 *
 * @author Mark Powell
 * @author Joshua Slack
 * @author Brad Davis
 */
public final class Vector3f extends Vector<Vector3f> implements java.io.Serializable {
    static final long serialVersionUID = 1;

    public final static Vector3f ZERO = new Vector3f(0, 0, 0);
    public final static Vector3f NAN = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    public final static Vector3f UNIT_X = new Vector3f(1, 0, 0);
    public final static Vector3f UNIT_Y = new Vector3f(0, 1, 0);
    public final static Vector3f UNIT_Z = new Vector3f(0, 0, 1);
    public final static Vector3f UNIT_XYZ = new Vector3f(1, 1, 1);
    public final static Vector3f POSITIVE_INFINITY = new Vector3f(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    public final static Vector3f NEGATIVE_INFINITY = new Vector3f(
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY);


    /**
     * the x value of the vector.
     */
    public final float x;

    /**
     * the y value of the vector.
     */
    public final float y;

    /**
     * the z value of the vector.
     */
    public final float z;

    /**
     * Constructor instantiates a new <code>Vector3f</code> with default
     * values of (0,0,0).
     *
     */
    public Vector3f() {
        this(0);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> with default
     * values of (0,0,0).
     *
     */
    public Vector3f(float s) {
        x = y = z = s;
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> with provides
     * values.
     *
     * @param x
     *            the x value of the vector.
     * @param y
     *            the y value of the vector.
     * @param z
     *            the z value of the vector.
     */
    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Promote a 2 vec to a 3 vec
     * @param v
     * @param z
     */
    public Vector3f(@Nonnull Vector2f v, float z) {
      this(v.x, v.y, z);
    }

    /**
     * Promote a 2 vec to a 3 vec
     * @param v
     * @param z
     */
    public Vector3f(@Nonnull Vector2f v) {
      this(v, 0);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    /**
     * <code>toString</code> returns the string representation of this vector.
     * The format is:
     *
     * org.jme.math.Vector3f [X=XX.XXXX, Y=YY.YYYY, Z=ZZ.ZZZZ]
     *
     * @return the string representation of this vector.
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * <code>cross</code> calculates the cross product of this vector with a
     * parameter vector v.
     *
     * @param v
     *            the vector to take the cross product of with this.
     * @return the cross product vector.
     */
    public Vector3f cross(Vector3f v) {
      float resX = ((y * v.z) - (z * v.y)); 
      float resY = ((z * v.x) - (x * v.z));
      float resZ = ((x * v.y) - (y * v.x));
      return new Vector3f(resX, resY, resZ);
    }

    /**
     * <code>angleBetween</code> returns (in radians) the angle between two vectors.
     * It is assumed that both this vector and the given vector are unit vectors (iow, normalized).
     * 
     * @param otherVector a unit vector to find the angle against
     * @return the angle in radians.
     */
    @Override
    public float angleBetween(Vector3f otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }

    /*
    public static void generateOrthonormalBasis(Vector3f u, Vector3f v, Vector3f w) {
        w.normalizeLocal();
        generateComplementBasis(u, v, w);
    }

    public static void generateComplementBasis(Vector3f u, Vector3f v,
            Vector3f w) {
        float fInvLength;

        if (FastMath.abs(w.x) >= FastMath.abs(w.y)) {
            // w.x or w.z is the largest magnitude component, swap them
            fInvLength = FastMath.invSqrt(w.x * w.x + w.z * w.z);
            u.x = -w.z * fInvLength;
            u.y = 0.0f;
            u.z = +w.x * fInvLength;
            v.x = w.y * u.z;
            v.y = w.z * u.x - w.x * u.z;
            v.z = -w.y * u.x;
        } else {
            // w.y or w.z is the largest magnitude component, swap them
            fInvLength = FastMath.invSqrt(w.y * w.y + w.z * w.z);
            u.x = 0.0f;
            u.y = +w.z * fInvLength;
            u.z = -w.y * fInvLength;
            v.x = w.y * u.z - w.z * u.y;
            v.y = -w.x * u.z;
            v.z = w.x * u.y;
        }
    }
    */

    /**
     * Saves this Vector3f into the given float[] object.
     * 
     * @param floats
     *            The float[] to take this Vector3f. If null, a new float[3] is
     *            created.
     * @return The array, with X, Y, Z float values in that order
     */
    @Override
    public float[] toArray() {
      return new float[] { x, y, z};
    }

    /**
     * are these two vectors the same? they are is they both have the same x,y,
     * and z values.
     *
     * @param o
     *            the object to compare for equality
     * @return true if they are equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector3f)) { return false; }

        if (this == o) { return true; }

        Vector3f comp = (Vector3f) o;
        if (Float.compare(x,comp.x) != 0) return false;
        if (Float.compare(y,comp.y) != 0) return false;
        if (Float.compare(z,comp.z) != 0) return false;
        return true;
    }

    /**
     * @param index
     * @return x value if index == 0, y value if index == 1 or z value if index ==
     *         2
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    public float get(int index) {
        switch (index) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
        }
        throw new IllegalArgumentException("index must be either 0, 1 or 2");
    }

    @Override
    protected Vector3f build(float[] v) {
      return new Vector3f(v[0], v[1], v[2]);
    }

    @Override
    protected Vector3f build(float s) {
      return new Vector3f(s);
    }
}
