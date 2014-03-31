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

import koala.dynamicjava.tree.*;
import koala.dynamicjava.util.*;

/**
 * The instances of the classes that implements this interface
 * are used to find the fully qualified name of classes and to
 * manage the loading of these classes.
 *
 * @author  Stephane Hillion
 * @version 1.0 - 1999/06/12
 */

public interface ClassFinder {
    /**
     * Returns the current package
     */
    String getCurrentPackage();

    /**
     * Loads the class info that match the given name in the source file
     * @param  cname the name of the class to find
     * @return the class info
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    ClassInfo lookupClass(String cname) throws ClassNotFoundException;

    /**
     * Loads the class info that match the given name in the source file
     * @param  cname the name of the class to find
     * @param  cinfo the context where 'cname' was found
     * @return the class info
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    ClassInfo lookupClass(String cname, ClassInfo cinfo) throws ClassNotFoundException;

    /**
     * Adds a type declaration in the class info list
     * @param  cname the name of the class
     * @param decl the type declaration
     */
    ClassInfo addClassInfo(String cname, TypeDeclaration decl);

}
