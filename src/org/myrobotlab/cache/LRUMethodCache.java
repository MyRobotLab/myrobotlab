package org.myrobotlab.cache;

import java.lang.reflect.Method;
import java.util.HashMap;

public class LRUMethodCache {

	private static LRUMethodCache instance = null;
	
	private HashMap<String, Method> cacheMap = new HashMap<String,Method>();
	
	protected LRUMethodCache() {
		// Exists only to defeat instantiation.
	}
	
	public static LRUMethodCache getInstance() {
		// optimistic first test
		if(instance == null) {
			// if we missed on that, now we synchonize.
			synchronized(LRUMethodCache.class) {
				instance = new LRUMethodCache();
			}
		}
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
