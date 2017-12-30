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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * data class to hold method description
 * 
 * @author GroG
 *
 */
public class MethodEntry implements Serializable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MethodEntry.class);

  /**
   * reflected info if available
   */
  transient public Class<?> returnType;
  transient public Class<?>[] parameterTypes;
  
  transient Method method;

  /**
   * string information for serialization
   */
  String[] parameterTypeNames;
  String name;
  String returnTypeName;

  /**
   * from doclet
   */
  public String javaDocString;

  public String getName() {
    return name;
  }

  /**
   * transfer the non serializable java.reflect.Method to a serializable object
   * @param m method
   */
  public MethodEntry(Method m) {
    this.method = m;
    this.name = m.getName();
    Class<?>[] paramTypes = m.getParameterTypes();
    this.parameterTypeNames = new String[paramTypes.length];
    for (int i = 0; i < paramTypes.length; ++i) {
      parameterTypeNames[i] = paramTypes[i].getCanonicalName();
    }
    
    this.parameterTypes = m.getParameterTypes();
    this.returnType = m.getReturnType();
    this.returnTypeName = returnType.getCanonicalName();

  }
  
  public List<String> getParameterNames() {
    Parameter[] parameters = method.getParameters();
    List<String> parameterNames = new ArrayList<>();

    for (Parameter parameter : parameters) {
        if(!parameter.isNamePresent()) {
            throw new IllegalArgumentException("Parameter names are not present!");
        }

        String parameterName = parameter.getName();
        parameterNames.add(parameterName);
    }

    return parameterNames;
}

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
   * @return string
   */
  final public String getSignature() {

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
  
  public String getReturnType(){
    return returnTypeName;
  }
  
  public String getSimpleReturnTypeName(){
    return returnType.getSimpleName();
  }
  
  /*
  public String getSimpleParameterNames(){
    StringBuilder sb = new StringBuilder();
    for (Class<?> param: parameterTypes){
      param.get
      sb.append(param.getSimpleName());
    }
    return sb.toString();
  }
  */

  @Override
  public String toString() {
    return getSignature();
  }

  public List<Map<String,Object>> getParameters() {
    List<Map<String,Object>> params = new ArrayList<Map<String,Object>>();
    
    Parameter[] parameters = method.getParameters();
    Class<?>[] types = method.getParameterTypes();
    
    for (int i = 0; i < types.length; ++i){
      Class<?> type = types[i];
      Parameter parameter = parameters[i];
      Map<String, Object> param = new TreeMap<String,Object>();
      param.put("type", type.getName());
      param.put("simpleName", type.getSimpleName());
      param.put("name", parameter.getName());
      params.add(param);
    }
   
    return params;
  }

  public String getSimpleParameterTypesAndNames() {
    if (method.getName().equals("map")){
      log.info("here");
    }
    List<Map<String,Object>> params = getParameters();
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (int i = 0; i < params.size(); ++i){

      if (i != 0){       
        sb.append(" ");
      }
      Map<String,Object> parm = params.get(i);
      sb.append(String.format("%s %s", parm.get("simpleName"), parm.get("name")));
      if (i != params.size() - 1){
        sb.append(",");
      }
    }
    sb.append(")");
    return sb.toString();
  }

}
