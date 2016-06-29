package org.myrobotlab.cache;

import java.lang.reflect.Method;

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

  public synchronized void addCacheEntry(String key, Method value) {
    cacheMap.put(key, value);
  }

  public synchronized Method getCacheEntry(String key) {
    return cacheMap.get(key);
  }

}
