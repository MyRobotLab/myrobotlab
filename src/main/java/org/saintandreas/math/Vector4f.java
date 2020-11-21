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
public final class Vector4f extends Vector4<Vector4f> implements java.io.Serializable {
    static final long serialVersionUID = 1;

    public final static Vector4f ZERO = new Vector4f(0, 0, 0, 0);
    public final static Vector4f NAN = new Vector4f(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
    public final static Vector4f UNIT_X = new Vector4f(1, 0, 0, 0);
    public final static Vector4f UNIT_Y = new Vector4f(0, 1, 0, 0);
    public final static Vector4f UNIT_Z = new Vector4f(0, 0, 1, 0);
    public final static Vector4f UNIT_W = new Vector4f(0, 0, 0, 1);
    public final static Vector4f UNIT_XYZW = new Vector4f(1, 1, 1, 1);

    public final static Vector4f COLOR_RED = new Vector4f(1, 0, 0, 1);
    public final static Vector4f COLOR_GREEN = new Vector4f(0, 1, 0, 1);
    public final static Vector4f COLOR_BLUE = new Vector4f(0, 0, 1, 1);
    public final static Vector4f COLOR_WHITE = new Vector4f(1, 1, 1, 1);

    public final static Vector4f POSITIVE_INFINITY = new Vector4f(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    public final static Vector4f NEGATIVE_INFINITY = new Vector4f(
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY);


    /**
     * Constructor instantiates a new <code>Vector3f</code> with default
     * values of (0,0,0).
     *
     */
    public Vector4f() {
      this(0);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> with default
     * values of (0,0,0).
     *
     */
    public Vector4f(float s) {
      super(s);
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
    public Vector4f(float x, float y, float z, float w) {
      super(x, y, z, w);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> that is a copy
     * of the provided vector
     * @param v The Vector3f to copy
     */
    public Vector4f( Vector2f v, float z, float w ) {
      super(v.x, v.y, z, w);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> that is a copy
     * of the provided vector
     * @param v The Vector3f to copy
     */
    public Vector4f( Vector2f v, float z) {
      super(v.x, v.y, z, 1);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> that is a copy
     * of the provided vector
     * @param v The Vector3f to copy
     */
    public Vector4f( Vector2f v) {
      super(v.x, v.y, 0, 1);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> that is a copy
     * of the provided vector
     * @param v The Vector3f to copy
     */
    public Vector4f( Vector3f v, float w ) {
      super(v.x, v.y, v.z, w);
    }

    /**
     * Constructor instantiates a new <code>Vector3f</code> that is a copy
     * of the provided vector
     * @param v The Vector3f to copy
     */
    public Vector4f( Vector3f v) {
      super(v.x, v.y, v.z, 1);
    }

    @Override
    protected Vector4f build(float x, float y, float z, float w) {
      return new Vector4f(x,y,z,w);
    }

    @Override
    protected Vector4f build(float s) {
      return new Vector4f(s);
    }

}