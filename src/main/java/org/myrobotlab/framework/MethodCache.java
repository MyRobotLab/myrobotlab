package org.myrobotlab.framework;

import org.apache.commons.lang3.StringUtils;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.codec.json.JsonDeserializationException;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 
 *
 *
 *         A method cache whos purpose is to build a cache of methods to be
 *         accessed when needed for invoking. This cache is typically used for
 *         services and populated during Runtime.create({name},{Type}). It's a
 *         static resource and contains a single definition per {Type}.
 * <p>
 *         It has a single map of "all" MethodEntries per type, and several
 *         indexes to that map. The other utility indexes are supposed to be
 *         useful and relevant for service specific access of methods.
 * <p>
 *         The definition of "declared methods" is slightly different for Mrl
 *         services. "Declared methods" are service methods which are expected
 *         to be commonly used. The difference occurs often with abstract
 *         classes such as AbstractSpeechSynthesis. Polly's
 *         class.getDeclaredMethods() would NOT include all the useful methods
 *         commonly defined in AbstractSpeechSynthesis.
 * <p>
 *         FIXME - keys should be explicitly typed full signature with execution
 *          format e.g. method(
 * <p>
 *
 *         The cache is built when new services are created. Method signatures
 *         are used as keys. The keys are string based. All parameters in key
 *         creation are "boxed", this leads to the ability to write actual
 *         functions with primitives e.g. doIt(int x, float y, ...) and invoking
 *         does not need to fail for it to be called directly.
 * <p>
 *         Ancestor classes are all indexed, so there is no "special" handling
 *         to call abstract class methods.
 * <p>
 *         Special indexes are created when a new service gets created that are
 *         explicitly applicable for remote procedure calls e.g. methods which
 *         contain interfaces in the parameters are not part of this index, if
 *         your creating a highly accessable method that you expect to be used
 *         remotely, you would make it with a String {name} reference as a
 *         parameter.
 *
 * @author GroG
 */
public class MethodCache {

  // FIXME - mostly interested in
  //  NOT Object
  //  RARELY Service
  //  OFTEN ANYTHING DEFINED LOWER THAN THAT
  //  WHICH MEANS - filter out Object
  //  CREATE a "Service" Index
  //  -> ALL OTHER METHODS ARE OF INTEREST
  class MethodIndex {
    // index for typeless resolution and invoking
    Map<String, List<MethodEntry>> methodOrdinalIndex = new TreeMap<>();

    // super index of all method entries
    Map<String, MethodEntry> methodsIndex = new TreeMap<>();

    // index based on typeless resolution and invoking without interfaces
    Map<String, List<MethodEntry>> remoteOrdinalIndex = new TreeMap<>();
    // Map<String, List<MethodEntry>> declaredMethodOrdinalIndex = new
    // TreeMap<>();

    // declared methods of both this real concrete service and its parent
    // ending at Service
    Set<String> serviceMethodNameSet = new TreeSet<>();
  }

  private static MethodCache instance;

  public final static Logger log = LoggerFactory.getLogger(MethodCache.class);

  final public static Class<?> boxPrimitive(Class<?> clazz) {
    if (clazz == boolean.class) {
      return Boolean.class;
    } else if (clazz == char.class) {
      return Character.class;
    } else if (clazz == byte.class) {
      return Byte.class;
    } else if (clazz == short.class) {
      return Short.class;
    } else if (clazz == int.class) {
      return Integer.class;
    } else if (clazz == long.class) {
      return Long.class;
    } else if (clazz == float.class) {
      return Float.class;
    } else if (clazz == double.class) {
      return Double.class;
    } else if (clazz == void.class) {
      return Void.class;
    } else {
      log.error("unexpected type class conversion for class {}", clazz.getTypeName());
    }
    return null;
  }

  public static MethodCache getInstance() {
    if (instance != null) {
      return instance;
    }
    synchronized (MethodCache.class) {
      if (instance == null) {
        instance = new MethodCache();
        instance.excludeMethods.add("main");
      }
    }
    return instance;
  }

  Set<String> excludeMethods = new TreeSet<>();

  Map<String, MethodIndex> objectCache = new TreeMap<>();

  protected MethodCache() {
  }

  /*
   * public void cacheMethodEntries(Class<?> object) { Set<Class<?>> exclude =
   * new HashSet<>(); exclude.add(Service.class); exclude.add(Object.class);
   * cacheMethodEntries(object, exclude); }
   */

  // public void cacheMethodEntries(Class<?> object, Class<?> maxSuperType,
  // Set<String> excludeMethods) {
  public void cacheMethodEntries(Class<?> object) {

    if (objectCache.containsKey(object.getTypeName())) {
      log.info("already cached {} methods", object.getSimpleName());
      return;
    }

    long start = System.currentTimeMillis();
    MethodIndex mi = new MethodIndex();
    Method[] methods = object.getMethods();
    Method[] declaredMethods = object.getDeclaredMethods();

    log.info("caching {}'s {} methods and {} declared methods", object.getSimpleName(), methods.length, declaredMethods.length);
    for (Method m : methods) {
      // log.debug("processing {}", m.getName());

      // extremely useful for debugging cache
      // if (m.getName().equals("processResults") &&
      // m.getParameterTypes().length == 1) {
      // log.info("here");
      // }

      String key = getMethodKey(object, m);
      String ordinalKey = getMethodOrdinalKey(object, m);
      boolean hasInterfaceInParamList = hasInterface(m);

      // FIXME - we are "building" an index, not at the moment - "using" the
      // index - so it should be "complete"
      // FIXME - other sub-indexes are "trimmed" for appropriate uses
      // should this use key ?? vs name ? or different class-less signature e.g.
      // main(String[])
      // if (excludeMethods.contains(m.getName())) {
      // continue;
      // }

      // search for interfaces in parameters - if there are any the method is
      // not applicable for remote invoking !

      MethodEntry me = new MethodEntry(m);
      mi.methodsIndex.put(key, me);

      addMethodEntry(mi.methodOrdinalIndex, ordinalKey, me);

      if (!hasInterfaceInParamList) {
        addMethodEntry(mi.remoteOrdinalIndex, ordinalKey, me);
      }

      if (!me.objectName.equals("org.myrobotlab.framework.Service") && !me.objectName.equals("java.lang.Object")) {
        mi.serviceMethodNameSet.add(me.getName());
      }

      log.debug("processed {}", me);
    }

    // log.debug("cached {}'s {} methods and {} declared methods",
    // object.getSimpleName(), methods.length,
    // mi.remoteMethods.keySet().size());
    objectCache.put(object.getTypeName(), mi);
    log.info("cached {} {} methods with {} ordinal signatures in {} ms", object.getSimpleName(), mi.methodsIndex.size(), mi.methodOrdinalIndex.size(),
        System.currentTimeMillis() - start);
  }

  private void addMethodEntry(Map<String, List<MethodEntry>> index, String ordinalKey, MethodEntry me) {
    if (!index.containsKey(ordinalKey)) {
      List<MethodEntry> mel = new ArrayList<>();
      mel.add(me);
      index.put(ordinalKey, mel);
    } else {
      List<MethodEntry> mel = index.get(ordinalKey);
      mel.add(me);
      // FIXME - output more info on collisions
      // log.warn("{} method ordinal parameters collision ", ordinalKey);
    }
  }

  private boolean hasInterface(Method m) {
    Class<?>[] paramTypes = m.getParameterTypes();
    boolean hasInterfaceInParamList = false;
    // exclude interfaces from this index - the preference in design would
    // be to have a
    // string {name} reference to refer to the service instance, however,
    // within in-process
    // python binding, using a reference to an interface is preferred
    for (Class<?> paramType : paramTypes) {
      if (paramType.isInterface() && !paramType.getName().equals(List.class.getCanonicalName()) && !paramType.getName().equals(Map.class.getCanonicalName())) {
        // skipping not applicable for remote invoking
        hasInterfaceInParamList = true;
        break;
      }
    }
    return hasInterfaceInParamList;
  }

  /**
   * clears all cache
   */
  public void clear() {
    objectCache.clear();
  }

  public int getObjectSize() {
    return objectCache.size();
  }

  public int getMethodSize() {
    int size = 0;
    for (MethodIndex mi : objectCache.values()) {
      size += mi.methodsIndex.size();
    }
    return size;
  }

  public Set<String> getCachedObjectNames() {
    return objectCache.keySet();
  }

  public Method getDefaultInvokeMethod(String fullType) {
    try {
      // last ditch effort - try default msg handler method
      Class<?> c = Class.forName(fullType);
      Method m = c.getMethod("defaultInvokeMethod", String.class, Object[].class);
      return m;
    } catch (Exception e) {
      // no default
    }
    return null;
  }

  public Method getMethod(Class<?> object, String methodName, Class<?>... paramTypes) throws ClassNotFoundException {
    String[] paramTypeNames = new String[paramTypes.length];
    for (int i = 0; i < paramTypes.length; ++i) {
      if (paramTypes[i] == null) {
        paramTypeNames[i] = null;
      } else {
        paramTypeNames[i] = paramTypes[i].getTypeName();
      }
    }
    return getMethod(object.getTypeName(), methodName, paramTypeNames);
  }

  /**
   * Use case for in-process non-serialized messages with actual data parameters
   * 
   * @param objectType
   *          - the object to invoke against
   * @param methodName
   *          - method name
   * @param params
   *          - actual parameter
   * @return - the method to invoke
   * @throws ClassNotFoundException
   *           if the class isn't found
   */
  public Method getMethod(Class<?> objectType, String methodName, Object... params) throws ClassNotFoundException {
    Class<?>[] paramTypes = getParamTypes(params);
    return getMethod(objectType, methodName, paramTypes);
  }

  public Class<?>[] getParamTypes(Object... params) {
    Class<?>[] paramTypes = null;
    if (params != null) {
      paramTypes = new Class<?>[params.length];
      for (int i = 0; i < params.length; ++i) {
        if (params[i] == null) {
          paramTypes[i] = null;
        } else {
          paramTypes[i] = params[i].getClass();
        }
      }
    } else {
      paramTypes = new Class<?>[0];
    }
    return paramTypes;
  }

  public Set<String> getMethodNames(String className) {
    MethodIndex mi = objectCache.get(className);

    // MethodIndex has a superset of keys grouped by class
    // class hierarchy can be derived several times under service
    // default we want declared methods

    return mi.serviceMethodNameSet;
  }

  /**
   * A full string interface to get a method - although this is potentially a
   * easy method to use, the most common use case would be used by the framework
   * which will automatically supply fully qualified type names.
   * 
   * @param fullType
   *          full type
   * @param methodName
   *          method to lookup
   * @param paramTypeNames
   *          names of params
   * @return the looked up method
   * @throws ClassNotFoundException
   *           if the fullType class isn't found.
   * 
   */
  public Method getMethod(String fullType, String methodName, String[] paramTypeNames) throws ClassNotFoundException {

    if (!objectCache.containsKey(fullType)) {
      // attempt to load it
      cacheMethodEntries(Class.forName(fullType));
    }

    MethodIndex mi = objectCache.get(fullType);

    // make a key
    String key = makeKey(fullType, methodName, paramTypeNames);

    // get the method - (from the super-map of all methods)
    if (!mi.methodsIndex.containsKey(key)) {
      // a key for the method might not exist because when a key is generated by
      // code
      // utilizing the MethodCache - super-types or interfaces might be used in
      // the parameter list
      // if this is the case we will look based on methods and ordinals
      String ordinalKey = getMethodOrdinalKey(fullType, methodName, paramTypeNames.length);
      List<MethodEntry> possibleMatches = mi.methodOrdinalIndex.get(ordinalKey);
      if (possibleMatches == null) {

        log.error("Method Cache look up Failed! {}.{}({})", fullType, methodName, StringUtils.join(paramTypeNames, ","));

        // if a service provides a methodCacheDefaultMethod - it means whenever
        // no match is found
        // call "this" method, similar to preProcessHook which intercepts msgs
        // when they come off a msg queue
        // but before invoke is called

        return null;
      }
      if (possibleMatches.size() == 1) {
        // woohoo ! we're done - if there is a single match it makes the choice
        // easy ;)
        return possibleMatches.get(0).method;
      } else {
        // now it gets more complex with overloading
        // spin through the possibilites - see if all parameters can be coerced
        // into working
        for (MethodEntry me : possibleMatches) {
          boolean foundMatch = true;
          Class<?>[] definedTypes = me.method.getParameterTypes();
          for (int i = 0; i < paramTypeNames.length; ++i) {
            Class<?> paramClass = null;

            try {
              paramClass = Class.forName(paramTypeNames[i]);
            } catch (ClassNotFoundException e) {
              log.error("while attempting to parameter match {} was not found", paramTypeNames[i], e);
            }
            if (!definedTypes[i].isAssignableFrom(paramClass)) {
              // parameter coercion fails - check other possiblities
              foundMatch = false;
              continue;
            }
          }

          if (!foundMatch) {
            // one of the parameters could not be coerced
            // look at other methods
            continue;
          }
          // We made it through matching all parameters !
          // send back the winner - but lets cache the entry first
          // we will fill the cache here with a new explicit key
          key = makeKey(fullType, methodName, paramTypeNames);
          mi.methodsIndex.put(key, me);
          return me.method;
        }
      }
      log.error("method {} with key signature {} not found in methodsIndex", methodName, key);
      return null;
    }

    // easy - exact key match return method
    return mi.methodsIndex.get(key).method;
  }

  public Map<String, Map<String, MethodEntry>> getRemoteMethods() {
    Map<String, Map<String, MethodEntry>> ret = new TreeMap<>();
    for (String name : objectCache.keySet()) {
      ret.put(name, objectCache.get(name).methodsIndex);
    }
    return ret;
  }

  public Map<String, MethodEntry> getRemoteMethods(String type) {
    if (!type.contains(".")) {
      type = "org.myrobotlab.service." + type;
    }
    if (objectCache.containsKey(type)) {
      return objectCache.get(type).methodsIndex;
    }
    return null;
  }

  final public Object invokeOn(Object obj, String methodName, Object... params)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {

    if (obj == null) {
      log.error("invokeOn object is null");
      return null;
    }

    Object retobj = null;
    MethodCache cache = MethodCache.getInstance();
    Method method = cache.getMethod(obj.getClass(), methodName, params);
    retobj = method.invoke(obj, params);
    return retobj;
  }

  private String getMethodKey(Class<?> object, Method method) {
    // make sure all parameters are boxed - and use those signature keys
    // msgs coming in will "always" be boxed so they will match this signature
    // keys
    Class<?>[] params = method.getParameterTypes();
    String[] paramTypes = new String[method.getParameterTypes().length];
    for (int i = 0; i < params.length; ++i) {
      Class<?> param = params[i];
      if (param.isPrimitive()) {
        paramTypes[i] = boxPrimitive(param).getTypeName();
      } else {
        paramTypes[i] = params[i].getTypeName();
      }
    }
    return makeKey(object.getTypeName(), method.getName(), paramTypes);
  }

  public String makeKey(Class<?> object, String methodName, Class<?>... paramTypes) {
    String[] paramTypeNames = new String[paramTypes.length];
    for (int i = 0; i < paramTypes.length; ++i) {
      paramTypeNames[i] = paramTypes[i].getTypeName();
    }
    return makeKey(object.getTypeName(), methodName, paramTypeNames);
  }

  public String makeKey(String fullType, String methodName, String[] paramTypes) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < paramTypes.length; ++i) {
      sb.append(paramTypes[i]);
      if (i < paramTypes.length - 1) {
        sb.append(",");
      }
    }
    return String.format("%s.%s(%s)", fullType, methodName, sb);
  }

  private String getMethodOrdinalKey(Class<?> object, Method method) {
    return getMethodOrdinalKey(object.getTypeName(), method.getName(), method.getParameterTypes().length);
  }

  private String getMethodOrdinalKey(String fullType, String methodName, int parameterSize) {
    String key = String.format("%s.%s-%s", fullType, methodName, parameterSize);
    return key;
  }

  public List<MethodEntry> getOrdinalMethods(Class<?> object, String methodName, int parameterSize) {
    if (object == null) {
      log.error("getOrdinalMethods on a null object ");
      return null;
    }
    String objectKey = object.getTypeName();

    String ordinalKey = getMethodOrdinalKey(objectKey, methodName, parameterSize);
    MethodIndex methodIndex = objectCache.get(objectKey);
    if (methodIndex == null) {
      log.error("cannot find index of {} !", objectKey);
      return null;
    }
    return methodIndex.methodOrdinalIndex.get(ordinalKey);
  }

  public List<MethodEntry> getRemoteOrdinalMethods(Class<?> object, String methodName, int parameterSize) {
    if (object == null) {
      log.error("getRemoteOrdinalMethods object is null");
      return null;
    }
    String objectKey = object.getTypeName();

    String ordinalKey = getMethodOrdinalKey(objectKey, methodName, parameterSize);
    MethodIndex methodIndex = objectCache.get(objectKey);
    if (methodIndex == null) {
      log.error("cannot find index of {} !", objectKey);
      return null;
    }
    return methodIndex.remoteOrdinalIndex.get(ordinalKey);
  }

  /**
   * Decode parameters from a String Json format into the format
   * specified by the declared method parameter type.
   *
   * <p>
   *     If clazz, methodName, or encodedParameters are null, then null is returned.
   * </p>
   *
   * <p>
   *     FIXME Change encodedParameters to String[] to enforce type safety
   * </p>
   *
   * @param clazz The class to lookup methods for
   * @param methodName The name of the method to decode parameters for
   * @param encodedParams The encoded parameters in JSON format.
   * @return The decoded parameters according to a matched method, or null
   *  if no such method could be found.
   */
  public /*@Nullable*/ Object[] getDecodedJsonParameters(Class<?> clazz, String methodName, Object[] encodedParams) {
    if (encodedParams == null) {
      encodedParams = new Object[0];
    }

    if (clazz == null) {
      log.error("cannot query method cache for null class");

      // Null was already returned for this case in the following
      // conditional but relied on getRemoteOrdinalMethods() returning
      // null for a null class, best to make this explicit
      return null;
    }
    // get templates
    // List<MethodEntry> possible = getOrdinalMethods(clazz, methodName,
    // encodedParams.length);
    List<MethodEntry> possible = getRemoteOrdinalMethods(clazz, methodName, encodedParams.length);
    if (possible == null) {
      log.error("getRemoteOrdinalMethods -> {}.{} with ordinal {} does not exist", clazz, methodName, encodedParams);
      return null;
    }
    Object[] params = new Object[encodedParams.length];
    // iterate through templates - attempt to decode
    for (MethodEntry methodEntry : possible) {
      Class<?>[] paramTypes = methodEntry.getParameterTypes();
      try {
        for (int i = 0; i < encodedParams.length; ++i) {
//          try {
            params[i] = CodecUtils.fromJson((String) encodedParams[i], paramTypes[i]);
//          } catch(JsonDeserializationException e) {
//            log.info("could not decode threw {}.{}( ordinal[{}] {} {})- assuming String - missing quotes?", clazz.getSimpleName(), methodName, i, paramTypes[i].getSimpleName(), encodedParams[i]);
//            // load raw string on
//            params[i] = encodedParams[i];
//          }
        }
        // successfully decoded params
        return params;
      } catch (Exception e) {
        log.info("getDecodedParameters threw clazz {} method {} params {} Message: {}", clazz, methodName, encodedParams.length, e.getMessage());
      }
    }
    // if successful return new msg
    log.error("requested getDecodedJsonParameters({}, {},{}) could not decode", clazz.getSimpleName(), methodName, encodedParams);
    return null;
  }

  public static String formatParams(Object[] params) {
    StringBuilder sb = new StringBuilder();
    if (params != null) {
      for (int i = 0; i < params.length; ++i) {
        sb.append(params[i].getClass().getSimpleName());
        if (i < params.length - 1) {
          sb.append(", ");
        }
      }
    }
    return sb.toString();
  }

  public List<MethodEntry> query(String className, String methodName) {

    if (!className.contains(".")) {
      className = "org.myrobotlab.service." + className;
    }

    MethodIndex methodIndex = objectCache.get(className);
    String keyPart = String.format("%s.%s(", className, methodName);
    List<MethodEntry> ret = new ArrayList<>();

    Set<String> filter = new HashSet<String>();

    // This method is for the UI or specifically for the GSON/JSON interface
    // Its to get a list of potential methods to be used by the UI
    for (String key : methodIndex.methodsIndex.keySet()) {
      if (key.startsWith(keyPart)) {
        // log.info("[{}]", key);
        MethodEntry me = methodIndex.methodsIndex.get(key);
        ret.add(me);
      }
    }
    return ret;
  }

}
