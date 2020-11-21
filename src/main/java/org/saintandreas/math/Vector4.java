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
 * <code>Vector4f</code> defines a Vector for a four float value tuple.
 * <code>Vector4f</code> can represent any four dimensional value, such as a
 * vertex, a normal, etc. Utility methods are also included to aid in
 * mathematical calculations.
 *
 * @author Maarten Steur
 * @author Brad Davis
 */

public abstract class Vector4<ResultType extends Vector4<ResultType>> extends Vector<ResultType> implements java.io.Serializable {
    static final long serialVersionUID = 1;
    protected abstract ResultType build(float x, float y, float z, float w);

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
     * the w value of the vector.
     */
    public final float w;

    /**
     * Constructor instantiates a new <code>Vector4f</code> with provides
     * values.
     *
     * @param x
     *            the x value of the vector.
     * @param y
     *            the y value of the vector.
     * @param z
     *            the z value of the vector.
     * @param w
     *            the w value of the vector.
     */
    public Vector4(float s) {
      x = y= z = w = s;
    }



    /**
     * Constructor instantiates a new <code>Vector4f</code> with provides
     * values.
     *
     * @param x
     *            the x value of the vector.
     * @param y
     *            the y value of the vector.
     * @param z
     *            the z value of the vector.
     * @param w
     *            the w value of the vector.
     */
    public Vector4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
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
    public final boolean equals(Object o) {
        if (!(o instanceof Vector4)) { return false; }

        if (this == o) { return true; }

        @SuppressWarnings("unchecked")
        Vector4<ResultType> comp = (Vector4<ResultType>) o;
        if (Float.compare(x,comp.x) != 0) return false;
        if (Float.compare(y,comp.y) != 0) return false;
        if (Float.compare(z,comp.z) != 0) return false;
        if (Float.compare(w,comp.w) != 0) return false;
        return true;
    }

    /**
     * <code>toString</code> returns the string representation of this vector.
     * The format is:
     *
     * org.jme.math.Vector3f [X=XX.XXXX, Y=YY.YYYY, Z=ZZ.ZZZZ, W=WW.WWWW]
     *
     * @return the string representation of this vector.
     */
    @Override
    public final String toString() {
        return "(" + x + ", " + y + ", " + z + ", " + w + ")";
    }

    public final float getX() {
        return x;
    }

    public final float getY() {
        return y;
    }

    public final float getZ() {
        return z;
    }

    public final float getW() {
        return w;
    }

    /**
     * <code>angleBetween</code> returns (in radians) the angle between two vectors.
     * It is assumed that both this vector and the given vector are unit vectors (iow, normalized).
     *
     * @param otherVector a unit vector to find the angle against
     * @return the angle in radians.
     */
    @Override
    public final float angleBetween(ResultType otherVector) {
        float dotProduct = dot(otherVector);
        float angle = FastMath.acos(dotProduct);
        return angle;
    }
    

    /**
     * Saves this Vector3f into the given float[] object.
     *
     * @param floats
     *            The float[] to take this Vector3f. If null, a new float[3] is
     *            created.
     * @return The array, with X, Y, Z float values in that order
     */
    @Override
    public final float[] toArray() {
      return new float[] { x, y, z, w };
    }

    /**
     * @param index
     * @return x value if index == 0, y value if index == 1 or z value if index ==
     *         2
     * @throws IllegalArgumentException
     *             if index is not one of 0, 1, 2.
     */
    public final float get(int index) {
        switch (index) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
            case 3:
                return w;
        }
        throw new IllegalArgumentException("index must be either 0, 1, 2 or 3");
    }

    @Override
    protected ResultType build(float[] v) {
      return build(v[0], v[1], v[2], v[3]);
    }

    @Override
    protected ResultType build(float s) {
      return build(s);
    }
    
}