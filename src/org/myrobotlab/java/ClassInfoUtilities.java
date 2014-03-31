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
import java.util.*;

import koala.dynamicjava.util.*;

/**
 * This class contains a collection of utility methods for reflection.
 *
 * @author  Stephane Hillion
 * @version 1.0 - 1999/07/23
 */

public class ClassInfoUtilities {
    /**
     * Looks for a constructor in the given class or in super classes of this class.
     * @param cl   the class of which the constructor is a member
     * @param ac   the arguments classes (possibly not the exact declaring classes)
     */
    public static ConstructorInfo lookupConstructor(ClassInfo cl, ClassInfo[] ac)
	throws NoSuchMethodException {
	List ms = getConstructors(cl, ac.length);
	List mm = new LinkedList();

	// Search for the constructors with good parameter types and
	// put them in 'mm'
	Iterator it = ms.iterator();
	while (it.hasNext()) {
	    ConstructorInfo m = (ConstructorInfo)it.next();
	    if (hasCompatibleSignatures(m.getParameterTypes(), ac)) {
		mm.add(m);
	    }
	}

	if (mm.isEmpty()) {
	    throw new NoSuchMethodException(cl.getName()+" constructor");
	}

	// Select the most specific constructor
	it = mm.iterator();
	ConstructorInfo result = (ConstructorInfo)it.next();

	while (it.hasNext()) {
	    result = selectTheMostSpecificConstructor(result,
						      (ConstructorInfo)it.next());
	}

	return result;
    }

    /**
     * Tests whether c1 is assignable from c2. This function works on
     * every types (primitive or not)
     * @param c1 a class info
     * @param c2 a class info
     */
    public static boolean isAssignableFrom(ClassInfo c1, ClassInfo c2) {
	if (c1.isPrimitive()) {
	    if (!c1.equals(c2)) {
		if (c2 == null || !c2.isPrimitive()) {
		    return false;
		} else {
		    Class cl1 = c1.getJavaClass();
		    Class cl2 = c2.getJavaClass();
		    if (cl1 == short.class) {
			return cl2 == byte.class;
		    } else if (cl1 == int.class) {
			return cl2 == byte.class  ||
			       cl2 == short.class ||
			       cl2 == char.class;
		    } else if (cl1 == long.class) {
			return cl2 == byte.class  ||
			       cl2 == short.class ||
			       cl2 == int.class;
		    } else if (cl1 == float.class) {
			return cl2 == byte.class  ||
			       cl2 == short.class ||
			       cl2 == int.class   ||
			       cl2 == float.class;
		    } else if (cl1 == double.class) {
			return cl2 == byte.class  ||
			       cl2 == short.class ||
			       cl2 == int.class   ||
			       cl2 == float.class ||
			       cl2 == double.class;
		    } else {
			return false;
		    }
		}
	    } else {
		return true;
	    }
	} else {
	    if (c2 == null || c1.equals(c2)) {
		return true;
	    } else {
		if (isAncestorOf(c1, c2) || isInterfaceOf(c1, c2)) {
		    return true;
		} else {
		    return false;
		}
	    }
	}
    }

    /**
     * Returns a field with the given name declared in the given
     * class or in the superclasses of the given class
     * @param cl   the class where the field must look for the field
     * @param name the name of the field
     */
    public static FieldInfo getField(ClassInfo cl, String name)
	throws NoSuchFieldException, AmbiguousFieldException {
	ClassInfo c = cl;
	while (c != null) {
	    FieldInfo[] fs = c.getFields();
	    for (int i = 0; i < fs.length; i++) {
		if (fs[i].getName().equals(name)) {
		    return fs[i];
		}
	    }

	    ClassInfo[] ints = c.getInterfaces();
	    FieldInfo f = null;
	    for (int i = 0; i < ints.length; i++) {
		FieldInfo tmp = null;
		try {
		    tmp = getField(ints[i], name);
		} catch(NoSuchFieldException ex) {
		}
		if (tmp != null) {
		    if (f != null && !f.equals(tmp)) {
			throw new AmbiguousFieldException(name);
		    }
		    f = tmp;
		}
	    }
	    if (f != null) {
		return f;
	    }
	    c = c.getSuperclass();
	}
	throw new NoSuchFieldException(name);
    }
    
    /**
     * Returns a field with the given name declared in one of the outer
     * classes of the given class
     * @param cl   the inner class
     * @param name the name of the field
     */
    public static FieldInfo getOuterField(ClassInfo cl, String name)
	throws NoSuchFieldException, AmbiguousFieldException {
	boolean sc = Modifier.isStatic(cl.getModifiers());
	ClassInfo   c  = (cl != null) ? cl.getDeclaringClass() : null;
	while (c != null) {
	    sc |= Modifier.isStatic(c.getModifiers());
	    try {
		FieldInfo f = getField(c, name);
		if (!sc || Modifier.isStatic(f.getModifiers())) {
		    return f;
		}
	    } catch (NoSuchFieldException e) {
	    }
	    c = c.getDeclaringClass();
	}
	throw new NoSuchFieldException(name);
    }

    /**
     * Looks for a method in the given class or in super classes of this class.
     * @param cl   the class of which the method is a member
     * @param name the name of the method
     * @param ac   the arguments classes (possibly not the exact declaring classes)
     */
    public static MethodInfo lookupMethod(ClassInfo cl, String name, ClassInfo[] ac)
	throws NoSuchMethodException {
	List ms = getMethods(cl, name, ac.length);
	List mm = new LinkedList();

	// Search for the methods with good parameter types and
	// put them in 'mm'
	Iterator it = ms.iterator();
	while (it.hasNext()) {
	    MethodInfo m = (MethodInfo)it.next();
	    if (hasCompatibleSignatures(m.getParameterTypes(), ac)) {
		mm.add(m);
	    }
	}

	if (mm.isEmpty()) {
	    throw new NoSuchMethodException(name);
	}

	// Select the most specific method
	it = mm.iterator();
	MethodInfo result = (MethodInfo)it.next();

	while (it.hasNext()) {
	    result = selectTheMostSpecificMethod(result, (MethodInfo)it.next());
	}

	return result;
    }

    /**
     * Looks up for a method in an outer classes of this class.
     * @param cl   the inner class 
     * @param name the name of the method
     * @param ac   the arguments classes (possibly not the exact declaring classes)
     */
    public static MethodInfo lookupOuterMethod(ClassInfo cl, String name, ClassInfo[] ac)
	throws NoSuchMethodException {
	boolean   sc = Modifier.isStatic(cl.getModifiers());
	ClassInfo c  = (cl != null) ? cl.getDeclaringClass() : null;
	while (c != null) {
	    sc |= Modifier.isStatic(c.getModifiers());
	    try {
		MethodInfo m = lookupMethod(c, name, ac);
		if (!sc || Modifier.isStatic(m.getModifiers())) {
		    return m;
		}
	    } catch (NoSuchMethodException e) {
	    }
	    c = c.getDeclaringClass();
	}
	throw new NoSuchMethodException(name);
    }

     /**
     * Gets all the methods with the given name in the given class or super classes.
     * Even the redefined methods are returned.
     * @param cl     the class where the method was declared
     * @param name   the name of the method
     * @param params the number of parameters
     * @return a list that contains the found methods, an empty list if no
     *         matching method was found.
     */
    public static List getMethods(ClassInfo cl, String name, int params) {
	List  result = new LinkedList();

	if (cl.isInterface()) {
	    MethodInfo[] ms = cl.getMethods();
	    for (int i = 0; i < ms.length; i++) {
		if (ms[i].getName().equals(name) &&
		    ms[i].getParameterTypes().length == params) {
		    result.add(ms[i]);
		}
	    }
	    ClassInfo[] cs = cl.getInterfaces();
	    for (int i = 0; i < cs.length; i++) {
		result.addAll(getMethods(cs[i], name, params));
	    }
	} else {
	    ClassInfo c      = cl;
	    while (c != null) {
		MethodInfo[] ms = c.getMethods();
		
		for (int i = 0; i < ms.length; i++) {
		    if (ms[i].getName().equals(name) &&
			ms[i].getParameterTypes().length == params) {
			result.add(ms[i]);
		    }
		}
		c = c.getSuperclass();
	    }
	}
	return result;
    }

   /**
     * Gets all the constructors in the given class or super classes,
     * even the redefined constructors are returned.
     * @param cl     the class where the constructor was declared
     * @param params the number of parameters
     * @return a list that contains the found constructors, an empty list if no
     *         matching constructor was found.
     */
    private static List getConstructors(ClassInfo cl, int params) {
	List  result = new LinkedList();
	ConstructorInfo[] ms = cl.getConstructors();
	    
	for (int i = 0; i < ms.length; i++) {
	    if (ms[i].getParameterTypes().length == params) {
		result.add(ms[i]);
	    }
	}
	return result;
    }
    
    /**
     * Returns the constructor with the most specific signature.
     */
    private static ConstructorInfo selectTheMostSpecificConstructor(ConstructorInfo m1,
								    ConstructorInfo m2) {
	ClassInfo[] a1 = m1.getParameterTypes();
	ClassInfo[] a2 = m2.getParameterTypes();

	for (int i = 0; i < a1.length; i++) {
	    if (a1[i] != a2[i]) {
		return (isAssignableFrom(a1[i], a2[i])) ? m2 : m1;
	    }
	}

	return m1;
    }

    /**
     * Returns the method with the most specific signature.
     */
    private static MethodInfo selectTheMostSpecificMethod(MethodInfo m1, MethodInfo m2) {
	ClassInfo[] a1 = m1.getParameterTypes();
	ClassInfo[] a2 = m2.getParameterTypes();

	for (int i = 0; i < a1.length; i++) {
	    if (a1[i] != a2[i]) {
		return (isAssignableFrom(a1[i], a2[i])) ? m2 : m1;
	    }
	}
	return m1;
    }

    /**
     * For each element of the given arrays, tests if the first array
     * element is assignable from the second array element. The two arrays are
     * assumed to have the same length.
     */
    private static boolean hasCompatibleSignatures(ClassInfo[] a1, ClassInfo[] a2) {
	for (int i = 0; i < a1.length; i++) {
	    if (!isAssignableFrom(a1[i], a2[i])) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Is c1 an ancestor of c2 ?
     */
    private static boolean isAncestorOf(ClassInfo c1, ClassInfo c2) {
	ClassInfo ci = c2.getSuperclass();
	while (ci != null && !ci.equals(new JavaClassInfo(Object.class))) {
	    if (ci.equals(c1)) {
		return true;
	    }
	    ci = ci.getSuperclass();
	}
	return ci != null && ci.equals(c1);
    }

    /**
     * Is c1 an interface of c2 ?
     */
    private static boolean isInterfaceOf(ClassInfo c1, ClassInfo c2) {
	if (c1.isInterface()) {
	    ClassInfo ci = c2;
	    while (ci != null && !ci.equals(new JavaClassInfo(Object.class))) {
		ClassInfo[] intf = ci.getInterfaces();
		for (int i = 0; i < intf.length; i++) {
		    if (intf[i].equals(c1)) {
			return true;
		    } else if (isInterfaceOf(c1, intf[i])) {
			return true;
		    }
		}
		ci = ci.getSuperclass();
	    }
	    return false;
	} else {
	    return false;
	}
    }

    /**
     * This class contains only static methods, so it is not useful
     * to create instances of it or to extend it.
     */
    private ClassInfoUtilities() {
    }
}
