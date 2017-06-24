/**
 * Factory for creating caches.
 */
package org.myrobotlab.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.myrobotlab.reflection.Reflector;

/**
 * Creates a cache based on configuration for the specified package and class.
 * Singleton.
 * 
 * @author SwedaKonsult
 * 
 */
public class CacheFactory {
  /**
   * Keep this as a singleton
   */
  private final static CacheFactory me = new CacheFactory();

  /**
   * The default initial size of the Cache created.
   */
  private final static int DEFAULT_INITIAL_SIZE = 10;

  /**
   * Cache the caches.
   */
  private final ConcurrentMap<String, Cache> caches;

  /*
   * Get a handle to this factory.
   */
  public static CacheFactory getFactory() {
    return me;
  }

  /**
   * Private constructor.
   */
  private CacheFactory() {
    // TODO need to load configuration about caches
    caches = new ConcurrentHashMap<String, Cache>();
  }

  /*
   * Create a cache using a specific class. This assumes that the constructor
   * does not take any parameters.
   * 
   * @param forClass the cache class that should be used
   */
  public Cache createCache(Class<? extends Cache> forClass) {
    if (forClass == null) {
      return createDefaultCache();
    }
    Cache cache = getExistingCache(forClass);
    if (cache != null) {
      return cache;
    }
    cache = Reflector.<Cache> getNewInstance(forClass, new Object[0]);
    caches.put(createKey(forClass), cache);
    return cache;
  }

  /**
   * Create a default cache type.
   * 
   * @return LocalCache
   */
  private Cache createDefaultCache() {
    return new LocalCache(DEFAULT_INITIAL_SIZE);
  }

  /**
   * Build up the key used for caching.
   * 
   * @param forClass
   * @return
   */
  private String createKey(Class<?> forClass) {
    return forClass.getCanonicalName();
  }

  /**
   * Get a key.
   * 
   * @param forClass
   * @return
   */
  private Cache getExistingCache(Class<? extends Object> forClass) {
    String key = createKey(forClass);
    if (caches.containsKey(key)) {
      return caches.get(key);
    }
    key = forClass.getPackage().getName();
    if (caches.containsKey(key)) {
      return caches.get(key);
    }
    key = forClass.getName();
    if (caches.containsKey(key)) {
      return caches.get(key);
    }
    return null;
  }
}
