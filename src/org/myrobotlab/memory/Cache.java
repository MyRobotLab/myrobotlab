/**
 * Cache interface
 */
package org.myrobotlab.memory;

/**
 * Interface for a single cache. Should be retrieved from CacheManager.
 * 
 * @author SwedaKonsult
 * 
 */
public interface Cache {

  /**
   * Expire an item in the cache.
   * 
   * @param name
   */
  void expire(String name);

  /**
   * Get a value.
   * 
   * @param name
   * @return
   */
  <T> T get(String name, Class<? extends T> c);

  /**
   * Cache a value.
   * 
   * @param name
   * @param value
   */
  void put(String name, Object value);
}
