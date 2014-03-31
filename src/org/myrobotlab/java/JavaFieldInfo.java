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


import java.lang.reflect.Field;

/**
 * The instances of this class provides informations about
 * class fields compiled to JVM bytecode.
 *
 * @author  Stephane Hillion
 * @version 1.0 - 1999/05/29
 */

public class JavaFieldInfo implements FieldInfo {
    /**
     * The underlying field
     */
    private Field javaField;

    /**
     * Creates a new class info
     * @param f the java field
     */
    public JavaFieldInfo(Field f) {
        javaField = f;
    }

    /**
     * Returns the modifiers for the field represented by this object
     */
    public int getModifiers() {
        return javaField.getModifiers();
    }

    /**
     * Returns the type of the underlying field
     */
    public ClassInfo getType() {
        return new JavaClassInfo(javaField.getType());
    }

    /**
     * Returns the fully qualified name of the underlying field
     */
    public String getName() {
        return javaField.getName();
    }

    /**
     * Indicates whether some other object is "equal to" this one
     */
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof JavaFieldInfo)) {
            return false;
        }
        return ((JavaFieldInfo)obj).javaField.equals(javaField);
    }
    
}
