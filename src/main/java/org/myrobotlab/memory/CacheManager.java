/**
 * Cache manager
 */
package org.myrobotlab.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manager that should be in charge of caches so that we have a way to clear
 * them out and keep them under control.
 * 
 * @author SwedaKonsult
 * 
 */
public class CacheManager {
  /**
   * Handle to myself as a singleton.
   */
  private final static CacheManager me;

  /**
   * All caches that we're currently managing.
   */
  private final ConcurrentMap<String, ManagedCache> caches;
  /**
   * Keep track of how often each cache is supposed to time out.
   */
  private final ConcurrentMap<String, Integer> cacheTimeouts;

  static {
    me = new CacheManager();
  }

  /*
   * Get a handle to this singleton.
   */
  public static CacheManager getInstance() {
    return me;
  }

  /**
   * Singleton constructor.
   */
  private CacheManager() {
    // start off with 10
    caches = new ConcurrentHashMap<String, ManagedCache>(10);
    cacheTimeouts = new ConcurrentHashMap<String, Integer>(10);
  }

  /**
   * Add a new cache to the list of caches.
   * 
   * @param name
   *          the name used to reference the cache
   * @param cache
   *          the cache to add
   * @param timeoutInterval
   *          the interval in ms of how long items in this cache should be kept
   *          before releasing them
   */
  public void addCache(String name, ManagedCache cache, int timeoutInterval) {
    caches.put(name, cache);
    cacheTimeouts.put(name, timeoutInterval);
  }

  /*
   * Get a handle to one of the caches.
   */
  public Cache getCache(String name) {
    if (!caches.containsKey(name)) {
      return null;
    }
    return caches.get(name);
  }
}
