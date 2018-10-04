package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logger.Constants;

public class LFUCache implements DataCache {

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	private CacheType cacheType;
	private int cacheSize;
	private String[][] cache;

	public LFUCache(CacheType cacheType, int cacheSize) {
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
	 * LFU
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @return String returns the value if it was found, else it returns null
	 */
	public String readCache(String key) {
		String value;

		for (int i = 0; i < cache.length; i++) {
			if (key.equals(cache[i][0])) {
				value = cache[i][1];
				int lfu_index = Integer.parseInt(cache[i][2]);
				cache[i][2] = "" + (lfu_index + 1);

				return value;
			}
		}

		return null;
	}

	/**
	 * puts the key and the value in the cache with LFU
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @param the
	 *            value, which is indexed by the given key.
	 */
	public void putinCache(String key, String value) {
		// LFU caching
		if (readCache(key) != null) {
			for (int i = 0; i < cache.length; i++) {
				if (key.equals(cache[i][0])) {
					cache[i][0] = key;
					cache[i][1] = value;
					cache[i][2] = "1";
					logger.info(key + "was written in Cache");
					break;
				}
			}
		} else {
			boolean arrayVoll = true;
			// if the array is not full, place the key in an empty spot
			for (int i = 0; i < cache.length; i++) {
				if (cache[i][0] == null) {
					cache[i][0] = key;
					cache[i][1] = value;
					cache[i][2] = "1";
					arrayVoll = false;
					logger.info(key + "was written in Cache");
					break;
				}
			}

			if (arrayVoll) {
				// replace the LFU element
				int lowest_int = 0;
				for (int i = 0; i < cache.length; i++) {

					if (Integer.parseInt(cache[lowest_int][2]) > Integer.parseInt(cache[i][2])) {
						lowest_int = i;
					}
				}
				cache[lowest_int][0] = key;
				cache[lowest_int][1] = value;
				cache[lowest_int][2] = "" + 1;
				logger.info(key + "was written in Cache");
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
