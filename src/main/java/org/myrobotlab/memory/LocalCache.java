/**
 * Cache class that can be used by any code.
 */
package org.myrobotlab.memory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of the Cache interface that stores information in local
 * memory.
 * 
 * @author SwedaKonsult
 * 
 */
public class LocalCache extends BaseCache {
  /**
   * Default concurrency level - grabbed from ConcurrentHashMap.
   */
  public static final int DEFAULT_CONCURRENCY_LEVEL = 16;
  /**
   * Default load factor - grabbed from ConcurrentHashMap.
   */
  public static final float DEFAULT_LOAD_FACTOR = 0.75f;
  /**
   * The cache of this instance.
   */
  private final ConcurrentMap<String, Object> items;
  private final ConcurrentMap<String, Long> itemTimeouts;
  private final long timeout;
  private long nextTimeout;
  private final boolean useTimeout;

  /**
   * Constructor. Default load factor (0.75) and concurrencyLevel (16). Default
   * timeout: 0 (never).
   * 
   * @param initialCapacity
   *          the initial capacity. The implementation performs internal sizing
   *          to accommodate this many elements.
   * @throws IllegalArgumentException
   *           if the initial capacity of elements is negative.
   */
  public LocalCache(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, 0);
  }

  /**
   * Constructor.
   * 
   * @param initialSize
   *          the initial capacity. The implementation performs internal sizing
   *          to accommodate this many elements.
   * @param loadFactor
   *          the load factor threshold, used to control resizing. Resizing may
   *          be performed when the average number of elements per bin exceeds
   *          this threshold.
   * @param concurrencyLevel
   *          the estimated number of concurrently updating threads. The
   *          implementation performs internal sizing to try to accommodate this
   *          many threads.
   * @param timeout
   *          amount of time in ms after which an item should time out.
   * @throws IllegalArgumentException
   *           if the initial capacity is negative or the load factor or
   *           concurrencyLevel are non-positive.
   */
  public LocalCache(int initialSize, float loadFactor, int concurrencyLevel, int timeout) {
    items = new ConcurrentHashMap<String, Object>(initialSize, loadFactor, concurrencyLevel);
    itemTimeouts = new ConcurrentHashMap<String, Long>(initialSize, loadFactor, concurrencyLevel);
    this.timeout = timeout;
    nextTimeout = 0l;
    useTimeout = this.timeout > 0l;
  }

  @Override
  protected void addToCache(String name, Object value) {
    items.put(name, value);
    // only set a timeout for the item if we want to timeout things in this
    // cache
    if (timeout > 0l) {
      itemTimeouts.put(name, System.currentTimeMillis() + timeout);
    }
  }

  @Override
  protected void clearCache() {
    items.clear();
    // only need to clear things out if they've been used
    if (useTimeout) {
      itemTimeouts.clear();
      nextTimeout = 0l;
    }
  }

  @Override
  protected boolean contains(String name) {
    return items.containsKey(name);
  }

  @Override
  protected void expireItem(String name) {
    if (name == null || name.isEmpty() || !itemTimeouts.containsKey(name) || !useTimeout) {
      return;
    }
    // time it out
    itemTimeouts.put(name, 0l);
    // make sure it's clear that something needs to be timed out
    nextTimeout = 0l;
  }

  @Override
  protected Object getFromCache(String name) {
    if (!items.containsKey(name)) {
      return null;
    }
    return items.get(name);
  }

  @Override
  protected void removeFromCache(String name) {
    if (!items.containsKey(name)) {
      return;
    }
    // TODO is this needed in order to make sure that the handle is removed?
    items.put(name, null);
    items.remove(name);
    // only remove it if we're using timeouts
    if (useTimeout) {
      itemTimeouts.remove(name);
    }
  }

  @Override
  protected void timeoutCache() {
    if (!useTimeout || (nextTimeout > 0l && nextTimeout > System.currentTimeMillis())) {
      // nothing to time out right now
      return;
    }
    // TODO loop through itemTimeouts in order to see if one of them needs
    // to be timed out
  }
}
