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
   * @param name the name of the item
   */
  void expire(String name);

  /*
   * Get a value.
   */
  <T> T get(String name, Class<? extends T> c);

  /**
   * Cache a value.
   * 
   * @param name the name of the item
   * @param value the value of the item
   */
  void put(String name, Object value);
}
