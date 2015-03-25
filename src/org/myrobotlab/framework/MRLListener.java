/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.Arrays;

public final class MRLListener implements Serializable {
	private static final long serialVersionUID = 1L;

	public String outMethod; // the keyed out method
	public String name; // globally unique name of Service a Message will be
						// sent to
	public String inMethod; // the method which will be invoked from the
							// Message
	public Class<?>[] paramTypes = null; // the parameter type of the inMethod -
											// named
	// parameterType vs dataType, because this will
	// always specify parameters not return types

	private int _hashCode = 0;

	public MRLListener(String outMethod, String name, String inMethod, Class<?>[] paramTypes) {
		this.outMethod = outMethod;
		this.inMethod = inMethod;
		this.name = name;
		this.paramTypes = paramTypes;
	}

	final public boolean equals(final MRLListener other) {
		// if (paramTypes.toString().equals(other.outMethod))
		if (Arrays.equals(paramTypes, other.paramTypes) && name.equals(other.name) && inMethod.equals(other.inMethod) && outMethod.equals(other.outMethod)) {
			return true;
		}
		return false;
	}

	@Override
	final public int hashCode() {
		if (_hashCode == 0) {
			_hashCode = 37 + outMethod.hashCode() + name.hashCode() + inMethod.hashCode();
			for (int i = 0; i < paramTypes.length; ++i) {
				_hashCode += paramTypes[i].hashCode();
			}
		}

		return _hashCode;
	}

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	@Override
	public String toString() {
		return String.format("%s -will activate-> %s.%s", outMethod, name, inMethod);
	}

}