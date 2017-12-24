package org.myrobotlab.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRUCache - ultra simplistic LRU cache
 * http://chriswu.me/blog/a-lru-cache-in-10-lines-of-java/
 * 
 * @param <K> - the key for the cache
 * @param <V> - the value stored by the cache.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private int cacheSize;

  public LRUCache(int cacheSize) {
    super(16, 0.75F, true);
    this.cacheSize = cacheSize;
  }

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() >= cacheSize;
  }
}
