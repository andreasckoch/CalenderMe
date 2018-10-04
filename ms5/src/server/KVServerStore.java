package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.*;
import app_kvServer.KVServer;
import common.messages.KVMessageInterface;
import common.messages.KVMessage;
import common.messages.KVMessageInterface.StatusType;
import logger.Constants;

public class KVServerStore {

	private String filename;
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	public KVServerStore(String filename) {
		this.filename = filename;
		File datei = new File(filename);
		FileWriter writer;
		try {
			writer = new FileWriter(datei, true);
			writer.write("");
			writer.close();
			logger.debug(filename + " created");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the key and Value on the database of the server.
	 *
	 * @param key
	 *            the key that identifies the value.
	 * @param value
	 *            the value, which is indexed by the given key.
	 * @return KVMessage the return Message the server should send to the client
	 * @throws Exception
	 *             if put command cannot be executed (e.g. not connected to any
	 *             KV Client).
	 * @throws IOException
	 *             if the the key and value command can not be written to a text
	 *             file
	 */

	public synchronized KVMessageInterface put(String key, String value) throws Exception {
		KVMessageInterface msgToClient;
		boolean update = false;

		// key is found in file -> UPDATE
		if (searchFile(key, filename) != null) {
			update = true;
			try {
				// UPDATE
				ArrayList<String> toDelete = new ArrayList<String>();
				toDelete.add(key);
				delete(toDelete);
			} catch (Exception e) {
				// UPDATE ERROR - RETURN ERROR MESSAGE
				logger.error("Error! " + "Unable to update data \n", e);
				msgToClient = new KVMessage(StatusType.PUT_ERROR);
				return msgToClient;
			}
		}

		try {
			saveToFile(key, value);

		} catch (Exception e) {
			// SAVE ERROR - RETURN ERROR MESSAGE
			logger.error("Error! " + "Unable to save to persisted data \n", e);
			msgToClient = new KVMessage(StatusType.PUT_ERROR);
			return msgToClient;
		}
		/* saving in cache and caching method query */
		try {
			saveToCache(key, value);
		} catch (Exception e) {
			// CACHE ERROR - RETURN ERROR MESSAGE
			logger.error("Error! " + "Error in Caching");
			msgToClient = new KVMessage(StatusType.PUT_ERROR);
			return msgToClient;
		}
		logger.info("key and value saved");
		if (update) {
			msgToClient = new KVMessage(StatusType.PUT_UPDATE, key, value);
		} else {
			msgToClient = new KVMessage(StatusType.PUT_SUCCESS, key, value);
		}
		return msgToClient;
	}

	/**
	 * Deletes a data set out of the persistent storage of the KVServer
	 * 
	 * @param key
	 *            the data set with the input key has to be deleted
	 * @return KVMessage containing SUCCESS/ERROR StatusType and key
	 */
	public synchronized KVMessageInterface delete(ArrayList<String> keyList) throws Exception {
		KVMessage m = null;
		// Delete from cache

		for (String key : keyList) {
			try {
				KVServer.dataCache.deleteFromCache(key);
			} catch (Exception e) {
				m = new KVMessage(StatusType.PUT_ERROR);
				return m;
			}
		}

		// Delete from File

		try {
			File tmp = File.createTempFile("tmp", "");

			BufferedReader br = new BufferedReader(new FileReader(filename));
			BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));

			String[] currToken = new String[2];
			String currLine = null;

			while (null != (currLine = br.readLine())) {
				currToken = currLine.split("\\s+");
				if (!keyList.contains(currToken[0])) {
					bw.write(String.format("%s%n", currToken[0] + " " + currToken[1]));
				}
			}

			br.close();
			bw.close();

			File oldFile = new File(filename);
			if (oldFile.delete())
				tmp.renameTo(oldFile);
			try {
				m = new KVMessage(StatusType.DELETE_SUCCESS);
			} catch (Exception e) {
				logger.error("Could not create new message");
			}
		} catch (IOException e) {
			try {
				m = new KVMessage(StatusType.DELETE_ERROR);
			} catch (Exception ex) {
				logger.error("Could not create new message");
			}
		} catch (Exception ex) {
			logger.error("Could not create new message");
		}
		return m;
	}

	/**
	 * Retrieves the value for a given key from the KVServer.
	 *
	 * @param key
	 *            the key that identifies the value.
	 * @return KVMessage the return Message the server should send to the client
	 * @throws FileNotFoundException
	 *             if the requested File was not found.
	 * @throws IOException
	 *             if the the key and value command can not be written to a file
	 */

	public synchronized KVMessageInterface get(String key) throws Exception {

		KVMessage msgToClient;
		String value = null;
		try {
			value = searchCache(key);
		} catch (Exception e) {
			logger.error("Error! " + "Error in Caching");
			msgToClient = new KVMessage(StatusType.GET_ERROR);
			return msgToClient;
		}

		// read from cache
		if (value != null) {
			logger.info("found " + key + " " + value + " in cache");
			msgToClient = new KVMessage(StatusType.GET_SUCCESS, key, value);
			if (KVServer.dataCache.getCacheType() == DataCache.CacheType.LFU
					|| KVServer.dataCache.getCacheType() == DataCache.CacheType.LRU) {
				KVServer.dataCache.putinCache(key, value);
			}
			return msgToClient;
		}
		// read from File
		String result = searchFile(key, filename);
		if (result != null) {
			KVServer.dataCache.putinCache(key, value);
			return new KVMessage(StatusType.GET_SUCCESS, key, result);
		} else {
			return new KVMessage(StatusType.GET_ERROR, key);
		}
	}

	public void deleteFile() {
		File oldFile = new File(filename);

		try {
			Files.deleteIfExists(oldFile.toPath());
		} catch (IOException e1) {
			logger.error("Error! " + "Unable to delete file. Bufferedreader still open!");
		}

	}

	/**
	 * Method searches for the data set with the given input key in the cache
	 * and returns the value of this data set
	 * 
	 * @param key
	 *            key that is searched for in the cache
	 * @return value of the data set found that contains the given key
	 * @throws Exception
	 *             if cache does not contain a data set with given key
	 */
	private String searchCache(String key) throws Exception {
		return KVServer.dataCache.readCache(key);

	}

	/**
	 * Method searches for the data set with the given input key in the
	 * persistent storage and returns the value of this data set
	 * 
	 * @param key
	 *            key that is searched for in the persistent storage
	 * @return value of the data set found that contains the given key
	 * @throws Exception
	 *             if cache does not contain a data set with given key
	 */
	private String searchFile(String key, String filename) throws Exception {
		logger.info("not found in cache. Search File");
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);

			String zeile = "";

			while ((zeile = br.readLine()) != null) {
				String[] tokens = zeile.trim().split("\\s+");
				// Search the file for the key
				if (tokens[0].equals(key)) {
					StringBuilder keybuilder = new StringBuilder();
					StringBuilder valuebuilder = new StringBuilder();
					keybuilder.append(tokens[0]);
					for (int i = 1; i < tokens.length; i++) {
						valuebuilder.append(tokens[i]);
						if (i != tokens.length - 1) {
							valuebuilder.append(" ");
						}
					}
					br.close();
					logger.info("key and value found in file");
					return valuebuilder.toString();
				}
			}
			logger.info("Key not found in file");
			br.close();
			return null;
		} catch (FileNotFoundException fileex) {
			logger.error("File not found");
		} catch (Exception ex) {
			logger.error("Error");
		}
		return null;
	}

	/**
	 * Method saves a key value pair to the file
	 * @param key
	 * @param value
	 * @throws Exception
	 */
	private void saveToFile(String key, String value) throws Exception {
		FileWriter writer;
		File datei = new File(filename);
		try {
			writer = new FileWriter(datei, true);
			writer.write(key + " " + value);
			writer.write(System.getProperty("line.separator"));
			writer.flush();
			writer.close();
		} catch (Exception e) {
			logger.error("Could not write to file", e);
		}
	}

	/**
	 * Method saves a key value pair to the cache
	 * @param key
	 * @param value
	 */
	private void saveToCache(String key, String value) {
		if (KVServer.dataCache.getCacheType() == DataCache.CacheType.FIFO) {
			// caching method FIFO
			logger.info("Caching" + DataCache.CacheType.FIFO + "started");
			KVServer.dataCache.putinCache(key, value);
		} else if (KVServer.dataCache.getCacheType() == DataCache.CacheType.LFU) {
			// caching method LFU
			logger.info("Caching" + DataCache.CacheType.LFU + "started");
			KVServer.dataCache.putinCache(key, value);
		} else if (KVServer.dataCache.getCacheType() == DataCache.CacheType.LRU) {
			// caching method LRU
			logger.info("Caching" + DataCache.CacheType.LRU + "started");
			KVServer.dataCache.putinCache(key, value);
		}
	}
	
	/**
	 * This method returns a HashMap of the current data in the file administered
	 * by this KVServerStore instance.
	 * @return
	 */

	public Map<String, String> getData() {
		Map<String, String> hashMapTemp = new HashMap<String, String>();
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			String zeile = "";

			while ((zeile = br.readLine()) != null) {
				String[] tokens = zeile.trim().split("\\s+");
				StringBuilder keybuilder = new StringBuilder();
				StringBuilder valuebuilder = new StringBuilder();
				keybuilder.append(tokens[0]);
				for (int i = 1; i < tokens.length; i++) {
					valuebuilder.append(tokens[i]);
					if (i != tokens.length - 1) {
						valuebuilder.append(" ");
					}
					hashMapTemp.put(keybuilder.toString(), valuebuilder.toString());
				}
				logger.info("Key not found in file");
				br.close();
			}
			return hashMapTemp;
		} catch (FileNotFoundException fileex) {
			logger.error("File not found");
			return hashMapTemp;
		} catch (Exception ex) {
			logger.error("Error");
			return hashMapTemp;
		}
	}

}
