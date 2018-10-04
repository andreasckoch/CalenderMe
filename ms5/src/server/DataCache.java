package server;

public interface DataCache {
	public enum CacheType {
		FIFO, /* First in First out */
		LFU, /* Least frequently used */
		LRU, /* Least recently used */
	}
	
	public String[][] getCache();

	/**
	 * returns the cacheType
	 * 
	 * @return cacheType
	 */
	public CacheType getCacheType();


	/**
	 * Search the cache for key and value and return the result and sort it with
	 * LRU
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @return String returns the value if it was found, else it returns null
	 */
	public String readCache(String key);

	/**
	 * deletes key and value from the cache
	 * 
	 * @param key
	 *            the key that identifies the value.
	 */
	public void deleteFromCache(String key);

	

	/**
	 * puts the key and the value in the cache with LFU
	 * 
	 * @param key
	 *            the key that identifies the value.
	 * @param the
	 *            value, which is indexed by the given key.
	 */
	public void putinCache(String key, String value);

	/**
	 * returns the cacheSize
	 * 
	 * @return cacheSize
	 */
	public int getCacheSize();

	/**
	 * sets the cacheSize
	 * 
	 * @param cacheSize
	 */
	public void setCacheSize(int cacheSize);
}
