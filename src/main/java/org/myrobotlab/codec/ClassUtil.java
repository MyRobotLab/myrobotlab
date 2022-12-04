package org.myrobotlab.codec;

import java.util.HashSet;
import java.util.Set;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Utilities for class inspection and reflection
 * 
 * @author GroG
 *
 */
public class ClassUtil {

  public final static Logger log = LoggerFactory.getLogger(ClassUtil.class);

  private static void crawlInterfaceAncestry(Set<String> ret, Class<?> o, Set<String> filteredInterfaces) {

    if (o == null) {
      return;
    }

    if (o.isInterface()) {
      // ret.add(o.getCanonicalName());
      if (filteredInterfaces != null && filteredInterfaces.contains(o.getCanonicalName())) {
        // filter it out
      } else {
        ret.add(o.getCanonicalName());
      }
    }

    Class<?>[] inter = o.getInterfaces();
    if (inter != null) {
      for (Class<?> i : inter) {
        if (filteredInterfaces != null && filteredInterfaces.contains(i.getCanonicalName())) {
          // filter it out
        } else {
          ret.add(i.getCanonicalName());
        }
        // breadth interface search
        for (Class<?> imp : i.getInterfaces()) {
          crawlInterfaceAncestry(ret, imp, filteredInterfaces);
        }
      }
    }

    // depth search
    crawlInterfaceAncestry(ret, o.getSuperclass(), filteredInterfaces);
  }

  public static Set<String> getInterfaces(String typeKey) throws ClassNotFoundException {
    return getInterfaces(Class.forName(typeKey), null);
  }

  public static Set<String> getInterfaces(Class<?> type, Set<String> filteredInterfaces) {

    Set<String> ret = new HashSet<>();
    crawlInterfaceAncestry(ret, type, filteredInterfaces);

    return ret;
  }

  public static Set<String> getInterfaces(String typeKey, Set<String> filteredInterfaces) throws ClassNotFoundException {
    return getInterfaces(Class.forName(typeKey), filteredInterfaces);
  }

}