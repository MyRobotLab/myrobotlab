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
import java.util.HashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class MethodEntry implements Serializable {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(MethodEntry.class);

	public String name;
	public Class<?> returnType;
	public Class<?>[] parameterTypes;

	private final static HashMap<String, String> primitiveTypeToString = new HashMap<String, String>();
	private final static HashMap<String, Class<?>> StringToPrimitiveType = new HashMap<String, Class<?>>();
	private static boolean initialized = false;

	public String toString() {
		return getSignature(name, parameterTypes, returnType);
	}

	final static public MethodEntry parseSignature(String signature) {
		if (!initialized) {
			init();
		}

		MethodEntry me = new MethodEntry();
		try {

			int p1 = signature.indexOf("(");
			int p2 = signature.indexOf(")");

			me.name = signature.substring(0, p1);

			String[] params = signature.substring(p1 + 1, p2).split(",");
			if (params.length > 0 && params[0].length() > 0) {
				me.parameterTypes = new Class[params.length];
				for (int i = 0; i < params.length; ++i) {
					// log.info(params[i]);
					String param = params[i];
					if (StringToPrimitiveType.containsKey(param)) {
						me.parameterTypes[i] = StringToPrimitiveType.get(param);
					} else {
						me.parameterTypes[i] = Class.forName(params[i]);
					}
					// log.info(c);
				}
			}

			String ret = signature.substring(p2 + 2, signature.length());
			if (StringToPrimitiveType.containsKey(ret)) {
				me.returnType = StringToPrimitiveType.get(ret);
			} else {
				me.returnType = Class.forName(ret);
			}

		} catch (ClassNotFoundException e) {
			Logging.logException(e);
		}

		return me;
	}

	private final static void init() {
		/*
		 * boolean Z byte B char C double D float F int I long J object L short
		 * S void V array [
		 */
		primitiveTypeToString.put("boolean", "Z");
		primitiveTypeToString.put("byte", "B");
		primitiveTypeToString.put("char", "C");
		primitiveTypeToString.put("double", "D");
		primitiveTypeToString.put("float", "F");
		primitiveTypeToString.put("int", "I");
		primitiveTypeToString.put("long", "J");
		primitiveTypeToString.put("short", "S");
		primitiveTypeToString.put("void", "V");

		StringToPrimitiveType.put("boolean", void.class);
		StringToPrimitiveType.put("byte", byte.class);
		StringToPrimitiveType.put("char", char.class);
		StringToPrimitiveType.put("double", double.class);
		StringToPrimitiveType.put("float", float.class);
		StringToPrimitiveType.put("int", int.class);
		StringToPrimitiveType.put("long", long.class);
		StringToPrimitiveType.put("short", short.class);
		StringToPrimitiveType.put("void", void.class);

		initialized = true; // not thread safe

		// StringToPrimitiveType.put
	}

	/*
	 * getSignature provides a way to create a stringified method signature the
	 * simplest way is to get the results from Class.getName() - this is a bit
	 * different/arbitrary from the JNA format of method signatures
	 */
	final static public String getSignature(String name, Class<?>[] parameterTypes, Class<?> returnType) {
		if (!initialized) {
			init();
		}

		StringBuffer sb = new StringBuffer();
		sb.append(name);
		sb.append("(");
		if (parameterTypes != null) {
			for (int i = 0; i < parameterTypes.length; ++i) {
				sb.append(parameterTypes[i].getName());
				if (i < parameterTypes.length - 1) {
					sb.append(",");
				}
			}
		}
		sb.append(") ");
		sb.append(returnType.getName());

		return sb.toString();
	}

	// format derived from: javap -classpath myrobotlab.jar -s
	// org.myrobotlab.service.GUIService
	final static public String getJNASignature(String name, Class<?>[] parameterTypes, Class<?> returnType) {
		if (!initialized) {
			init();
		}

		StringBuffer sb = new StringBuffer();
		sb.append(name);
		sb.append("(");
		for (int i = 0; i < parameterTypes.length; ++i) {
			if (parameterTypes[i].isPrimitive()) {
				sb.append(primitiveTypeToString.get(parameterTypes[i].getName()));
			} else {
				if (parameterTypes[i].isArray()) // heh - Java bug :P
				{
					sb.append(parameterTypes[i].getName());
				} else {
					sb.append("L" + parameterTypes[i].getName() + ";");
				}
			}
		}
		sb.append(")");
		if (returnType.isPrimitive()) {
			sb.append(primitiveTypeToString.get(returnType.getName()));
		} else {
			if (returnType.isArray()) // Java bug :P
			{
				sb.append(returnType.getName());
			} else {
				sb.append("L" + returnType.getName() + ";");
			}
		}

		return sb.toString();
	}

	final static public String getPrettySignature(String name, Class<?>[] parameterTypes, Class<?> returnType) {
		if (!initialized) {
			init();
		}

		StringBuffer sb = new StringBuffer();
		sb.append(name);
		sb.append("(");
		if (parameterTypes != null) {
			for (int i = 0; i < parameterTypes.length; ++i) {
				if (parameterTypes[i] == null) {
					sb.append("null");
				} else {
					String p = parameterTypes[i].getSimpleName();
					sb.append(p);
				}
				if (i < parameterTypes.length - 1) {
					sb.append(",");
				}
			}
		}
		sb.append(") : ");
		if (returnType != null) {
			String r = returnType.getSimpleName();
			sb.append(r);
		}
		return sb.toString();
	}

}
