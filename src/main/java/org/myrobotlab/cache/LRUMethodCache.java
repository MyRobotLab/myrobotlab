package org.myrobotlab.cache;

import java.lang.reflect.Method;
import java.util.Arrays;

public class LRUMethodCache {

  private static LRUMethodCache instance = null;

  // an lru cache object
  private LRUCache<String, Method> cacheMap = null;

  // size of cache
  // private int size = 1024;

  protected LRUMethodCache(int size) {
    // this.size = size;
    // Exists only to defeat instantiation.
    cacheMap = new LRUCache<String, Method>(size);
  }

  public static LRUMethodCache getInstance() {
    // optimistic first test
    if (instance == null) {
      // if we missed on that, now we syncronize.
      synchronized (LRUMethodCache.class) {
        // double lock check.
        if (instance == null) {
          instance = new LRUMethodCache(512);
        }
      }
    }
    // TODO: to make this a true LRU cache we...
    // we should have a max size for the hash map where
    // we expire entries based on how frequently/recently they are used.
    // int size = 1024;
    return instance;
  }

  
  private String makeKey(Object obj, String method, Class<?>[] paramTypes){
    String key = String.format("%s.%s.%s", obj.toString(), method, Arrays.toString(paramTypes));
    return key;
  }

  public Method getCacheEntry(Object obj, String method, Class<?>[] paramTypes) {
    String key = makeKey(obj, method, paramTypes);
    if (cacheMap.containsKey(key)){
      return cacheMap.get(key);
    }
    return null;
  }

  public void addCacheEntry(Object obj, String method, Class<?>[] paramTypes, Method m) {
    cacheMap.put(makeKey(obj, method, paramTypes), m);
  }

}
