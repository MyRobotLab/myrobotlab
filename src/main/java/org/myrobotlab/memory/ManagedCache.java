/**
 * Internal Cache that allows for managing.
 */
package org.myrobotlab.memory;

/**
 * Interface for use by the CacheManager. Contains additional methods that are
 * required for cache management.
 * 
 * @author SwedaKonsult
 * 
 */
public interface ManagedCache extends Cache {
  /**
   * Clear all values from the cache.
   */
  void clear();

  /**
   * Update the timeout for name so that it is cleaned up the next time
   * timeout() is called.
   * 
   * @param name the name
   */
  @Override
  void expire(String name);

  /**
   * Clear out any values that should be timed out.
   */
  void timeout();
}
