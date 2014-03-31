/*
 * DynamicJava - Copyright (C) 1999 Dyade
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dyade shall not be
 * used in advertising or otherwise to promote the sale, use or other
 * dealings in this Software without prior written authorization from
 * Dyade.
 *
 */

package org.myrobotlab.java;

import koala.dynamicjava.util.AmbiguousFieldException;

/**
 * The instances of the classes that implement this interface provide
 * informations about classes.
 *
 * @author  Stephane Hillion
 * @version 1.0 - 1999/05/29
 */

public interface ClassInfo {
    /**
     * Returns the underlying class
     */
    Class getJavaClass();

    /**
     * Whether the underlying class needs compilation
     */
    boolean isCompilable();

    /**
     * Sets the compilable property
     */
    void setCompilable(boolean b);

    /**
     * Returns the declaring class or null
     */
    ClassInfo getDeclaringClass();

    /**
     * Returns the declaring class of an anonymous class or null
     */
    ClassInfo getAnonymousDeclaringClass();

    /**
     * Returns the modifiers flags
     */
    int getModifiers();

    /**
     * Returns the fully qualified name of the underlying class
     */
    String getName();

    /**
     * Returns the class info of the superclass of the class
     * represented by this info
     */
    ClassInfo getSuperclass();

    /**
     * Returns the class infos of the interfaces implemented by
     * the class this info represents
     */
    ClassInfo[] getInterfaces();

    /**
     * Returns the field infos for the current class
     */
    FieldInfo[] getFields();

    /**
     * Returns the constructor infos for the current class
     */
    ConstructorInfo[] getConstructors();

    /**
     * Returns the method infos for the current class
     */
    MethodInfo[] getMethods();

    /**
     * Returns the classes and interfaces declared as members
     * of the class represented by this ClassInfo object.
     */
    ClassInfo[] getDeclaredClasses();

    /**
     * Returns the array type that contains elements of this class
     */
    ClassInfo getArrayType();
    
    /**
     * Whether this object represents an interface
     */
    boolean isInterface();

    /**
     * Whether this object represents an array
     */
    boolean isArray();

    /**
     * Whether this object represents a primitive type
     */
    boolean isPrimitive();

    /**
     * Returns the component type of this array type
     * @exception IllegalStateException if this type do not represent an array
     */
    ClassInfo getComponentType();
}
