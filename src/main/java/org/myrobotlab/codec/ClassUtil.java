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


  private static void crawlInterfaceAncestry(Set<String> ret, Class<?> o) {

    if (o == null) {
      return;
    }
    
    if (o.isInterface()) {
      ret.add(o.getCanonicalName());      
    }
    
    Class<?>[] inter = o.getInterfaces();
    if (inter != null) {
      for (Class<?> i : inter) {
        ret.add(i.getCanonicalName()); 
        // breadth interface search
        for (Class<?> imp : i.getInterfaces()) {
          crawlInterfaceAncestry(ret, imp);
        }
      }
    }

    // depth search
    crawlInterfaceAncestry(ret, o.getSuperclass());
  }

  public static Set<String> getInterfaces(String o) throws ClassNotFoundException {
    return getInterfaces(Class.forName(o));
  }

  public static Set<String> getInterfaces(Class<?> o) {

    Set<String> ret = new HashSet<>();
    crawlInterfaceAncestry(ret, o);

    return ret;
  }

}