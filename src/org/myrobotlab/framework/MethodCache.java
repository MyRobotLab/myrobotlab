package org.myrobotlab.framework;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MethodCache {

  static class MethodKey {

    private final String methodName;
    private final Class<?>[] parameterTypes;

    /**
     * Creates a new <code>MethodKey</code> instance.
     *
     * @param methodName
     *          a <code>String</code> value
     * @param parameterTypes
     *          a <code>Class[]</code> value
     */
    MethodKey(String methodName, Class<?>[] parameterTypes) {
      this.methodName = methodName;
      this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(Object other) {
      MethodKey that = (MethodKey) other;
      return this.methodName.equals(that.methodName) && Arrays.equals(this.parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
      // allow overloaded methods to collide; we'll sort it out
      // in equals(). Note that String's implementation of
      // hashCode already caches its value, so there's no point
      // in doing so here.
      return methodName.hashCode();
    }
  }

  public final static Logger log = LoggerFactory.getLogger(MethodCache.class);

  /**
   * The only instance of this class
   */
  transient private static MethodCache instance;

  /**
   * Cache for Methods In fact this is a map (with classes as keys) of a map
   * (with method-names as keys)
   */
  transient private static ThreadLocal cache;

  /** used to track methods we've sought but not found in the past */
  private static final Object NULL_OBJECT = new Object();

  /**
   * Gets the only instance of this class
   * 
   * @return the only instance of this class
   */
  public static MethodCache getInstance() {
    if (instance == null) {
      instance = new MethodCache();
    }
    return instance;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    try {
      LoggingFactory.init(Level.INFO);
      MethodCache mc = MethodCache.getInstance();
      // Class[] paramsTypes = new Class[] { String.class, Double.class,
      // Double.class, Double.class, Double.class };
      // Method method = mc.getMethod(Serial.class, "connect", paramsTypes);
      // log.info(method.getName());

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /**
   * The <i>private</i> constructor for this class. Use getInstance to get an
   * instance (the only one).
   */
  private MethodCache() {
    cache = new ThreadLocal();
  }

  /**
   * Returns the specified method - if any.
   *
   * @param clazz
   *          the class to get the method from
   * @param methodName
   *          the name of the method
   * @param parameterTypes
   *          the parameters of the method
   * @return the found method
   *
   * @throws NoSuchMethodException
   *           if the method can't be found
   */
  public Method getMethod(Class clazz, String methodName, Class[] parameterTypes) throws NoSuchMethodException {
    String className = clazz.getName();
    Map cache = getMethodCache();
    Method method = null;
    Map methods = null;

    // Strategy is as follows:
    // construct a MethodKey to represent the name/arguments
    // of a method's signature.
    //
    // use the name of the class to retrieve a map of that
    // class' methods
    //
    // if a map exists, use the MethodKey to find the
    // associated value object. if that object is a Method
    // instance, return it. if that object is anything
    // else, then it's a reference to our NULL_OBJECT
    // instance, indicating that we've previously tried
    // and failed to find a method meeting our requirements,
    // so return null
    //
    // if the map of methods for the class doesn't exist,
    // or if no value is associated with our key in that map,
    // then we perform a reflection-based search for the method.
    //
    // if we find a method in that search, we store it in the
    // map using the key; otherwise, we store a reference
    // to NULL_OBJECT using the key.

    // Check the cache first.
    MethodKey key = new MethodKey(methodName, parameterTypes);
    methods = (Map) cache.get(className);
    if (methods != null) {
      Object o = methods.get(key);
      if (o != null) { // cache hit
        if (o instanceof Method) { // good cache hit
          return (Method) o;
        } else { // bad cache hit
          // we hit the NULL_OBJECT, so this is a search
          // that previously failed; no point in doing
          // it again as it is a worst case search
          // through the entire classpath.
          return null;
        }
      } else {
        // cache miss: fall through to reflective search
      }
    } else {
      // cache miss: fall through to reflective search
    }

    try {
      method = clazz.getMethod(methodName, parameterTypes);
    } catch (NoSuchMethodException e1) {
      if (!clazz.isPrimitive() && !className.startsWith("java.") && !className.startsWith("javax.")) {

        /*
         * try { // Class helper = ClassUtils.forName(className + "_Helper"); //
         * method = helper.getMethod(methodName, parameterTypes); } catch
         * (ClassNotFoundException e2) { }
         */
      }
    }

    // first time we've seen this class: set up its method cache
    if (methods == null) {
      methods = new HashMap();
      cache.put(className, methods);
    }

    // when no method is found, cache the NULL_OBJECT
    // so that we don't have to repeat worst-case searches
    // every time.

    if (null == method) {
      methods.put(key, NULL_OBJECT);
    } else {
      methods.put(key, method);
    }
    return method;
  }

  /**
   * Returns the per thread hashmap (for method caching)
   */
  private Map getMethodCache() {
    Map map = (Map) cache.get();
    if (map == null) {
      map = new HashMap();
      cache.set(map);
    }
    return map;
  }

}