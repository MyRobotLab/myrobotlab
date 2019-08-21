/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This is a data class to hold method description. Its constructed from java.reflect.Method and quickly
 * moves the relevant data to serializable types on construction.
 * 
 * @author GroG
 *
 */
public class MethodEntry implements Serializable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MethodEntry.class);

  /**
   * the reflect method
   */
  transient Method method;

  /**
   * string information for serialization
   */
  String objectName;
  String name;
  String[] parameterNames;
  String[] parameterTypeNames;
  String returnTypeName;
  String simpleReturnTypeName;

  /**
   * from doclet
   */
  public String javaDocString;

  public String getName() {
    return name;
  }

  /**
   * transfer the non serializable java.reflect.Method to a serializable object
   * 
   * @param m
   *          method
   */
  public MethodEntry(Method m) {
    this.method = m;
    this.name = m.getName();
    
    Class<?>[] paramTypes = m.getParameterTypes();
    Parameter[] parameters = method.getParameters();
    
    objectName = m.getDeclaringClass().getCanonicalName();
    
    parameterNames = new String[paramTypes.length];

    for (int i = 0; i < paramTypes.length; ++i) {
      Parameter parameter = parameters[i];
      if (!parameter.isNamePresent()) {
        // log.warn("{}.{} parameter names are not present!", m.getDeclaringClass().getSimpleName(), m.getName());
        continue;
      }

      String parameterName = parameter.getName();
      parameterNames[i] = parameterName;
    }
        
    this.parameterTypeNames = new String[paramTypes.length];
    for (int i = 0; i < paramTypes.length; ++i) {
      parameterTypeNames[i] = paramTypes[i].getCanonicalName();
    }

    this.returnTypeName = method.getReturnType().getCanonicalName();
    this.simpleReturnTypeName = method.getReturnType().getSimpleName();
  }
  
  public String [] getSimpleParameterNames() {
    String[] ret = new String[parameterTypeNames.length];
    for (int i = 0; i < parameterTypeNames.length; ++i) {
      String s = parameterTypeNames[i].substring(parameterTypeNames[i].lastIndexOf(".")+1);      
      ret[i] = s;
    }  
    return ret;
  }

  public String[] getParameterNames() {
    return parameterNames;
  }

  // FIXME - rename getSignature(boolean simple) - with getSignature(true) default - don't make static
  final static public String getPrettySignature(String methodName, Class<?>[] parameterTypes, Class<?> returnType) {

    StringBuffer sb = new StringBuffer();
    sb.append(methodName);
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

  /**
   * getSignature provides a way to create a stringified method signature the
   * simplest way is to get the results from Class.getName() - this is a bit
   * different/arbitrary from the JNA format of method signatures
   * 
   * @return string
   */
  final public String getSignature() {

    Class<?>[] parameterTypes = method.getParameterTypes();
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
    sb.append(method.getReturnType().getCanonicalName());

    return sb.toString();
  }

  public Class<?> getReturnType() {
    return method.getReturnType();
  }
  
  public String getReturnTypeName() {
    return returnTypeName;
  }

  public String getSimpleReturnTypeName() {
    return simpleReturnTypeName;
  }

  @Override
  public String toString() {
    // recently changed 2019.08.17 GroG
    // return getSignature();
    
    StringBuilder sb = new StringBuilder();
    String[] p = getSimpleParameterNames();
    for (int i = 0; i < p.length; ++i) {
      sb.append(p[i]);
      if (i < p.length - 1) {
        sb.append(",");
      }
    }
    return String.format("%s.%s(%s)", objectName, name, sb.toString());
  }

  public List<Map<String, Object>> getParameters() {
    List<Map<String, Object>> params = new ArrayList<Map<String, Object>>();

    Parameter[] parameters = method.getParameters();
    Class<?>[] types = method.getParameterTypes();

    for (int i = 0; i < types.length; ++i) {
      Class<?> type = types[i];
      Parameter parameter = parameters[i];
      Map<String, Object> param = new TreeMap<String, Object>();
      param.put("type", type.getName());
      param.put("simpleName", type.getSimpleName());
      param.put("name", parameter.getName());
      params.add(param);
    }

    return params;
  }

  public Class<?>[] getParameterTypes() {
    return method.getParameterTypes();
  }

}
