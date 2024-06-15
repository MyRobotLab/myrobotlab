package org.myrobotlab.framework;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Instantiator {

  transient public final static Logger log = LoggerFactory.getLogger(Instantiator.class);

  static public Object getNewInstance(Class<?> cast, String classname, Object... params) {
    return getNewInstance(new Class<?>[] { cast }, classname, params);
  }

  static public Object getNewInstance(Class<?>[] cast, String classname, Object... params) {
    try {
      return getThrowableNewInstance(cast, classname, params);
    } catch (Exception e) {
      log.info("getNewInstance failed for {} {} ", cast, classname, e);
    }
    return null;
  }

  static public Object getNewInstance(String classname) {
    return getNewInstance((Class<?>[]) null, classname, (Object[]) null);
  }

  static public Object getNewInstance(String classname, Object... params) {
    return getNewInstance((Class<?>[]) null, classname, params);
  }

  static public Object getThrowableNewInstance(Class<?>[] cast, String classname, Object... params)
      throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Class<?> c;

    c = Class.forName(classname);
    if (params == null) {
      Constructor<?> mc = c.getConstructor();
      return mc.newInstance();
    } else {
      Class<?>[] paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; ++i) {
        paramTypes[i] = params[i].getClass();
      }
      Constructor<?> mc = null;
      if (cast == null) {
        mc = c.getConstructor(paramTypes);
      } else {
        mc = c.getConstructor(cast);
      }
      return mc.newInstance(params); // Dynamically instantiate it
    }
  }

  static public Object invoke(Object obj, String method, Object... params)
      throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    if (obj == null) {
      return null;
    }
    Class<?>[] paramTypes = null;

    if (params != null) {
      paramTypes = new Class[params.length];
      for (int i = 0; i < params.length; ++i) {
        if (params[i] != null) {
          paramTypes[i] = params[i].getClass();
        } else {
          paramTypes[i] = null;
        }
      }
    }

    Method meth = null;
    meth = obj.getClass().getMethod(method, paramTypes); // getDeclaredMethod
                                                         // zod !!!
    Object retobj = meth.invoke(obj, params);
    return retobj;

  }
}
