package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logger.Constants;

public class LRUCache implements DataCache {

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	private CacheType cacheType;
	private int cacheSize;
	private String[][] cache;

	public LRUCache(CacheType cacheType, int cacheSize) {
		this.cacheType = cacheType;
		this.setCacheSize(cacheSize);
		this.cache = new String[cacheSize][3];
	}

	public String[][] getCache() {
		return this.cache;
	}

	/**
	 * returns the cacheSize
	 * 
	 * @return cacheSize
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * sets the cacheSize
	 * 
	 * @param cacheSize
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * returns the cacheType
	 * 
	 * @return cacheType
	 */
	public CacheType getCacheType() {
		return cacheType;
	}

	/**
	 * deletes key and value from the cache
	 * 
	 * @param key
	 *            the key that identifies the value.
	 */
	public void deleteFromCache(String key) {
		for (int i = 0; i < cache.length; i++) {
			if (key.equals(cache[i][0])) {
				cache[i][2] = null;
				cache[i][1] = null;
				cache[i][0] = null;
			}
		}
	}

	/**
	 * moves all elements in the array one up and on place 0 inserts the new key
	 * and value
	 * 
	 * @param cache
	 *            the cache in which the order has to change
	 * @param key
	 *            the key that identifies the value.
	 * @param value
	 *            value, which is indexed by the given key.
	 * @param position
	 *            position in the array to which the sorting should go
	 */
	public void move_one_up_in_Cache(String[][] cache, String key, String value, int position) {
		/* sorts the array to the position position */
		String cacheKey2 = null;
		String cacheValue2 = null;
		for (int j = position; j > 0; j--) {
			if (cache[j - 1][0] != null) {
				/*
				 * checks if cachKey2 is initialized. If it is, set array on
				 * position [j] with the key and value of the variables
				 * cacheKey2 and cacheValue2
				 */
				if (cacheKey2 != null) {
					cache[j][0] = cacheKey2;
					cache[j][1] = cacheValue2;
				}
				/* sort */
				String cacheKey = cache[j - 1][0];
				String cacheValue = cache[j - 1][1];
				cache[j - 1][0] = cache[j][0];
				cache[j - 1][1] = cache[j][1];
				cacheKey2 = cache[j][0];
				cacheValue2 = cache[j][1];
				cache[j][0] = cacheKey;
				cache[j][1] = cacheValue;
			} else {
				/* if the array is not full stop here */
				cache[j - 1][0] = cache[j][0];
				cache[j - 1][1] = cache[j][1];
				break;
			}
		}
		/* insert new element on array[0] */
		cache[0][0] = key;
		cache[0][1] = value;
	}

	/**
	 * Search the cache for key and value and return the result and sort it with
	 * LRU
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @return String returns the value if it was found, else it returns null
	 */
	public String readCache(String key) {
		String value;
		/* read from cache */
		for (int i = 0; i < cache.length; i++) {
			if (key.equals(cache[i][0])) {
				value = cache[i][1];
				int lru_position = i;
				/* change order of cache according to LRU */
				move_one_up_in_Cache(cache, key, value, lru_position);
				return value;
			}
		}
		return null;
	}

	/**
	 * puts the key and the value in the cache with LRU
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @param the
	 *            value, which is indexed by the given key.
	 */
	public void putinCache(String key, String value) {
		// LRU caching
		int lru_position = 0;
		boolean keyinArray = false;
		while (keyinArray == false) {
			/* checks if the key is already in the array */
			for (int i = 0; i < cache.length - 1; i++) {
				if (cache[i][0] == null || key.equals(cache[i][0])) {
					/* found it - set array[position] null */
					cache[i][0] = null;
					cache[i][1] = null;
					lru_position = i;
					keyinArray = true;
					break;
				}
			}
			/* goes in there if the key was found in the array */
			if (keyinArray == true) {
				move_one_up_in_Cache(cache, key, value, lru_position);
				keyinArray = true;
				logger.info(key + " was written in Cache");
				break;
			} else {
				/*
				 * key not in array. Move all one up and set key/value on first
				 * position
				 */
				move_one_up_in_Cache(cache, key, value, cache.length - 1);
				keyinArray = true;
				logger.info(key + " was written in Cache");
				break;
			}
		}
	}

	// for testing
	public void printArray(String[][] cache, String key, String value) {
		for (int i = 0; i < cache.length; i++) {
			System.out.println(cache[i][0] + " , " + cache[i][1] + " , " + cache[i][2]);
		}
	}
}
