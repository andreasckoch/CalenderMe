package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logger.Constants;

public class FIFOCache implements DataCache{

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	private CacheType cacheType;
	private int cacheSize;
	private String[][] cache;

	public FIFOCache(CacheType cacheType, int cacheSize) {
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
	@Override
	public CacheType getCacheType() {
		return cacheType;
	}
	
	/**
	 * deletes key and value from the cache
	 * 
	 * @param key
	 *            the key that identifies the value.
	 */
	@Override
	public void deleteFromCache(String key) {
		for (int i = 0; i < cache.length; i++) {
			if (key.equals(cache[i][0])) {
				cache[i][1] = null;
				cache[i][0] = null;
			}
		}
	}
	
	/**
	 * moves all elements in the array to position one up and on place 0 inserts the new key and value
	 * 
	 * @param cache the cache in which the order has to change
	 * @param key the key that identifies the value.
	 * @param value value, which is indexed by the given key.
	 * @param position position in the array to which the sorting should go
	 */
	public void move_one_up_in_Cache(String[][] cache, String key, String value, int position){
		/* sorts the array to the position position */
		String cacheKey2 = null;
		String cacheValue2 = null;
		for (int j = position; j > 0; j--) {
			if (cache[j - 1][0] != null) {
				/*
				 * checks if cachKey2 is initialized. If it is, set
				 * array on position [j] with the key and value of the
				 * variables cacheKey2 and cacheValue2
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
				/*if the array is not full stop here*/
				cache[j - 1][0] = cache[j][0];
				cache[j - 1][1] = cache[j][1];
				break;
			}
		}
		/*insert new element on array[0]*/
		cache[0][0] = key;
		cache[0][1] = value;
	}

	/**
	 * Search the cache for key and value and return the result
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @return String returns the value if it was found, else it returns null
	 */
	@Override
	public String readCache(String key) {
		String value;
		for (int i = 0; i < cache.length; i++) {
			if (key.equals(cache[i][0])) {
				value = cache[i][1];
				return value;
			}
		}
		return null;
	}
	
	/**
	 * puts the key and the value in the cache with FIFO
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @param the
	 *            value, which is indexed by the given key.
	 */
	@Override
	public void putinCache(String key, String value) {
		boolean isnotfull = true;
		while (isnotfull == true) {
			for (int i = 0; i < cache.length; i++) {
				/*
				 * if the array is not full, put it in the next available slot
				 * or if the key is already there, overwrite it
				 */
				if (cache[i][0] == null || key.equals(cache[i][0])) {
					
					move_one_up_in_Cache(cache, key, value, i);
					// TODO
					//printArray(cache,key,value);
					logger.info(key + " was written in Cache");
					isnotfull = false;
					break;
				}
			}
			/*
			 * else delete the last index (first in cache) and move the other
			 * elements one up. put the key and the value on first position
			 */
			if (isnotfull == true) {
				move_one_up_in_Cache(cache, key, value, cache.length-1);
				// TODO
				//printArray(cache,key,value);
				logger.info(key + " was written in Cache");
			} else {

			}
		}
	}
	
	//for testing
	public void printArray(String[][] cache, String key, String value){
		for (int i = 0; i<cache.length; i++){
			System.out.println(cache[i][0]+" , "+ cache[i][1]+" , "+ cache[i][2]);
		}
	}
}
