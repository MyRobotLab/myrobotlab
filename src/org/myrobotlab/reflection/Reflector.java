/**
 * Helper class for instantiating objects using string values.
 */
package org.myrobotlab.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * 
 * @author SwedaKonsult &amp; GroG
 *
 */
public class Reflector {

  // private static HashMap<String, Method> cache = new HashMap<String,
  // Method>();

  static final Logger log = LoggerFactory.getLogger(Reflector.class);

  // final static String getSignature

  /**
   * Allow for checking if a boxed primitive is being used.
   */
  public final static HashSet<Class<?>> primitiveTypes;

  static {

    primitiveTypes = new HashSet<Class<?>>(8);
    primitiveTypes.add(Boolean.class);
    primitiveTypes.add(Character.class);
    primitiveTypes.add(Byte.class);
    primitiveTypes.add(Short.class);
    primitiveTypes.add(Integer.class);
    primitiveTypes.add(Long.class);
    primitiveTypes.add(Float.class);
    primitiveTypes.add(Double.class);
  }

  /*
   * Create an instance of Class.
   * 
   * @param c
   *          any class that extends the expected return type T
   * @param params params for constructor i guess?
   * @return null if anything fails
   */
  @SuppressWarnings("unchecked")
  public static <T> T getNewInstance(Class<? extends T> c, Object... params) {
    if (c == null) {
      return null;
    }
    try {
      Class<?>[] paramTypes = getParameterTypes(params);
      Constructor<?> mc = c.getConstructor(paramTypes);
      return (T) mc.newInstance(params);
    } catch (Exception e) {
      Logging.logError(e);
    }
    return null;
  }

  /*
   * Create an instance of the classname.
   * @param classname class name
   * @param params params to pass to constructor
   * @return null if anything fails
   */
  public static <T> T getNewInstance(String classname, Object... params) {
    if (classname == null || classname.isEmpty()) {
      return null;
    }
    try {
      @SuppressWarnings("unchecked")
      Class<? extends T> c = (Class<? extends T>) Class.forName(classname);
      return Reflector.<T> getNewInstance(c, params);
    } catch (Exception e) {
      Logging.logError(e);
    }
    return null;
  }

  /**
   * Parse the Class out of the passed-in objects. If an object is null, null
   * will be used.
   * 
   * @param params
   * @return
   */
  private static Class<?>[] getParameterTypes(Object[] params) {
    Class<?>[] paramTypes = null;
    // Class<?>[] paramTypes = null;
    if (params == null) {
      return paramTypes;
    }
    paramTypes = new Class[params.length];
    for (int i = 0; i < params.length; ++i) {
      if (params[i] == null) {
        paramTypes[i] = null;
        continue;
      }
      paramTypes[i] = params[i].getClass();
    }
    return paramTypes;
  }

  /**
   * @param cls class
   * @return an empty/default boxed primitive. This is somewhat heavy since it
   * creates a boxed instance of the primitive.
   */
  public static Object getPrimitive(Class<?> cls) {
    if (cls.isAssignableFrom(Integer.class)) {
      return 0;
    }
    if (cls.isAssignableFrom(Byte.class)) {
      byte b = 0;
      return b;
    }
    if (cls.isAssignableFrom(Short.class)) {
      short s = 0;
      return s;
    }
    if (cls.isAssignableFrom(Double.class)) {
      return 0d;
    }
    if (cls.isAssignableFrom(Float.class)) {
      return 0f;
    }
    if (cls.isAssignableFrom(Long.class)) {
      return 0l;
    }
    if (cls.isAssignableFrom(Boolean.class)) {
      return Boolean.FALSE;
    }
    return '\u0000';
  }

  /*
   * Invoke in the context of this Service. It is suggested to use one of the
   * primitive overload methods when the expected result is a primitive. This is
   * for 2 reasons: (1) the primitive will be boxed in this case which means
   * more overhead (2) if something fails a NULL is returned which results in an
   * exception on the calling end
   * @param object object
   * @param method method
   * @param params list of params to pass
   * 
   * @return null if anything fails
   * @throws NullPointerException
   *           if the expected return type is a primitive
   */
  @SuppressWarnings("unchecked")
  public static <T> T invokeMethod(Object object, String method, Object... params) {
    if (object == null || method == null || method.isEmpty()) {
      return null;
    }
    Class<?> c = object.getClass();
    Class<?>[] paramTypes = getParameterTypes(params);
    try {
      Method meth = c.getMethod(method, paramTypes);
      return (T) meth.invoke(object, params);
    } catch (Exception e) {
      Logging.logError(e);
    }
    return null;
  }

  /**
   * Test if the item is a boxed primitive.
   * @param item the item to test
   * 
   * @return true if it is a boxed primitive
   */
  public static boolean isPrimitive(Object item) {
    return primitiveTypes.contains(item);
  }
}
