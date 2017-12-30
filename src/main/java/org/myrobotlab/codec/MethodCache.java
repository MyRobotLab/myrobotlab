package org.myrobotlab.codec;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.TreeMap;

public class MethodCache {

  static final private HashMap<String, Method> cache = new HashMap<String, Method>();

  static final public String getSignature(Class<?> clazz, String methodName, int ordinal) {
    return String.format("%s/%s-%d", clazz.getSimpleName(), methodName, ordinal);
  }

  static final public Class<?>[] getCandidateOnOrdinalSignature(Class<?> clazz, String methodName, int ordinal) throws NoSuchMethodException {
    String signature = getSignature(clazz, methodName, ordinal);
    if (cache.containsKey(signature)) {
      Method m = cache.get(signature);
      return m.getParameterTypes();
    } else {
      TreeMap<Integer, Method> methodScore = new TreeMap<Integer, Method>();
      // changed to getMethods to support inheritance
      // if failure - overloading funny re-implementing a vTable in c++
      // Method[] methods = clazz.getDeclaredMethods();
      Method[] methods = clazz.getMethods();
      for (Method method : methods) {

        // FIXME - future Many to one Map - if incoming data can "hint" would be
        // an optimization
        if (methodName.equals(method.getName())) {
          // name matches - lets do more checking
          Class<?>[] pTypes = method.getParameterTypes();
          int score = 0;
          if (ordinal == pTypes.length) {
            // param length matches
            boolean interfaceInParamList = false;
            for (int i = 0; i < pTypes.length; ++i) {
              // we don't support interfaces
              // because what will we decode too ?
              // we just can't ! :)
              Class<?> type = pTypes[i];

              /*
               * BAD ASSUMPTION - SOME CODECs have a default class they
               * serialize from for List Map HashSet etc..
               */
              /*
               * if (type.isInterface()) { interfaceInParamList = true; break; }
               */

              if (type.isPrimitive() || type.equals(String.class)) {
                ++score;
              }

            }

            if (!interfaceInParamList) {
              // rank / score method
              methodScore.put(score, method);
            }
          }
        }
      } // we checked all methods

      if (methodScore.size() > 0) {
        return methodScore.get(methodScore.lastKey()).getParameterTypes();
      } else {
        throw new NoSuchMethodException(String.format("could not find %s.%s(ordinal %d) in declared methods", clazz.getSimpleName(), methodName, ordinal));
      }
    }

  }

  final public static void cache(Class<?> clazz, Method method) {
    cache.put(getSignature(clazz, method.getName(), method.getParameterTypes().length), method);
  }

}
