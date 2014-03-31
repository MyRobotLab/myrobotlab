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


import java.lang.reflect.*;

/**
 * The instances of this class provides informations about
 * class methods compiled to JVM bytecode.
 *
 * @author  Stephane Hillion
 * @version 1.0 - 1999/06/03
 */

public class JavaMethodInfo implements MethodInfo {
    /**
     * The underlying method
     */
    private Method javaMethod;

    /**
     * The parameters types
     */
    private ClassInfo[] parameters;

    /**
     * The exception types
     */
    private ClassInfo[] exceptions;

    /**
     * Returns the modifiers for the method represented by this object
     */
    public int getModifiers() {
        return javaMethod.getModifiers();
    }

    /**
     * Creates a new method info
     * @param f the java method
     */
    public JavaMethodInfo(Method f) {
        javaMethod = f;
    }

    /**
     * Returns a Class object that represents the return type
     * of the method represented by this object
     */
    public ClassInfo getReturnType() {
        return new JavaClassInfo(javaMethod.getReturnType());
    }

    /**
     * Returns the name of the underlying method
     */
    public String getName() {
        return javaMethod.getName();
    }

    /**
     * Returns an array of class infos that represent the parameter
     * types, in declaration order, of the method represented
     * by this object
     */
    public ClassInfo[] getParameterTypes() {
	if (parameters == null) {
	    Class[] pcs = javaMethod.getParameterTypes();
	    parameters  = new ClassInfo[pcs.length];

	    for (int i = 0; i < pcs.length; i++) {
		parameters[i] = new JavaClassInfo(pcs[i]);
	    }
	}
        return (ClassInfo[])parameters.clone();
    }

    /**
     * Returns an array of Class infos that represent the types of
     * the exceptions declared to be thrown by the underlying method
     */
    public ClassInfo[] getExceptionTypes() {
	if (exceptions == null) {
	    Class[] ecs = javaMethod.getExceptionTypes();
	    exceptions  = new ClassInfo[ecs.length];

	    for (int i = 0; i < ecs.length; i++) {
		exceptions[i] = new JavaClassInfo(ecs[i]);
	    }
	}
        return (ClassInfo[])exceptions.clone();
    }

    /**
     * Indicates whether some other object is "equal to" this one
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof JavaMethodInfo)) {
            return false;
        }
        return javaMethod.equals(((JavaMethodInfo)obj).javaMethod);
    }
}
