package server.replication;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app_kvServer.KVServer;
import client.ClientConnection;
import client.KVStore;
import common.*;
import common.messages.KVMessage;
import common.messages.KVMessageInterface;
import logger.Constants;
import server.KVServerStore;

/**
 * Each KVServer contains one coordinator object, that contains the responsible
 * data of the actual server. The coordinator object knows and communicates
 * with/updates the (maximum) two servers, which backup its replicated data. It
 * has a method to replicate its data to these two replica servers (invoke
 * put-method from kvserverstore to put all datasets). It has a method to put a
 * single dataset to all replicas and itself.
 * 
 *
 */

public class Coordinator {

	private String responsibilityKey;

	private String filename;
	private KVServerStore kvserverstore;
	private KVServer server;
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	/**
	 * Constructor for the coordinator object. With the parameters metadata and
	 * server representation, the initialization process is accomplished.
	 * Therefore, the servers which will store this server's replicated data,
	 * are set. Additionally, the filename for this server's storage
	 * functionality is derived from the given server representation.
	 * 
	 * @param server
	 *            server to which this coordinator , to be able to look up the
	 *            respective instances of coordinated and replica servers
	 * 
	 */
	public Coordinator(KVServer server) {
		logger.debug("Initialize Coordinator...");
		this.server = server;

		responsibilityKey = server.getResponsibiltyKey();

		logger.debug("Set Coordinators Responsibility Key " + responsibilityKey);

		// set file for servers responsibility range
		filename = "db_" + responsibilityKey + "_" + server.getPort() + ".txt";
		this.kvserverstore = new KVServerStore(filename);

		logger.debug("Coordinator initialized");
	}

	/*
	 * Provides the single data update to its own file and cascades the change
	 * to the two replica servers (put, update, depending on value)
	 */
	public KVMessageInterface put(String key, String value) {
		KVMessageInterface msg;
		// save locally
		try {
			msg = kvserverstore.put(key, value);
			logger.debug("saved locally");
		} catch (Exception e) {
			logger.error("not saved locally");
			e.printStackTrace();
			return new KVMessage(KVMessageInterface.StatusType.PUT_ERROR);
		}
		// replicate change to responsible replication servers via
		// reconciliation manager
		try {
			ReconciliationManager recMgr = new ReconciliationManager(server);
			recMgr.replicatePUT(key, value);
			logger.debug("sent put replication to replication servers");
		} catch (Exception e) {
			logger.error("put replication error");
			e.printStackTrace();
		}
		// return to client (only local store-status considered)
		return msg;
	}

	public KVMessageInterface get(String key) {
		try {
			return kvserverstore.get(key);
		} catch (Exception e) {
			return new KVMessage(KVMessageInterface.StatusType.GET_ERROR);
		}
	}

	/**
	 * Provides the single data delete from its own file and cascades the change
	 * to the two replica servers
	 * 
	 * @param key
	 * @param filename
	 */
	public KVMessageInterface delete(String key) {
		KVMessageInterface msg;
		// delete locally
		try {
			ArrayList<String> toDelete = new ArrayList<String>();
			toDelete.add(key);
			msg = kvserverstore.delete(toDelete);
			logger.debug("saved locally");
		} catch (Exception e) {
			logger.error("not saved locally");
			e.printStackTrace();
			return new KVMessage(KVMessageInterface.StatusType.DELETE_ERROR);
		}
		// replicate change to responsible replication servers via
		// reconciliation manager
		try {
			ReconciliationManager recMgr = new ReconciliationManager(server);
			recMgr.replicateDELETE(key);
			logger.debug("sent delete replication to replication servers");
		} catch (Exception e) {
			logger.error("delete replication error");
			e.printStackTrace();
		}
		// return to client (only local store-status considered)
		return msg;
	}

	/**
	 * This method moves the complete data of this coordinator to the new
	 * responsible server.
	 * 
	 * @param range
	 * @param server
	 */

	public void moveData(Range range, HostRepresentation server) {
		Range start = null;
		Range end = null;

		/*
		 * split range into two intervals, if the border of the hash-ring is
		 * included in the range
		 */
		if (range.getLower_limit().compareTo(range.getUpper_limit()) > 0) {
			start = new Range(range.getLower_limit(), "ffffffffffffffffffffffffffffffff");
			end = new Range("00000000000000000000000000000000", range.getUpper_limit());
		}

		// else only one range has to be used
		else {
			start = new Range(range.getLower_limit(), range.getUpper_limit());
		}

		logger.debug("Moving from " + range.getLower_limit() + " to " + range.getUpper_limit());

		/*
		 * the following paragraph is utilized for distinguishing those
		 * key-value-pairs, which are not in the server's range anymore - and
		 * the ones, which are still to be administered
		 */
		try {
			FileReader fr = new FileReader(this.filename);
			BufferedReader br = new BufferedReader(fr);

			/*
			 * keysToDelete holds the list of keys to be deleted, which will
			 * later be passed on to the delete() Method
			 */
			ArrayList<String> keysToDelete = new ArrayList<String>();

			/*
			 * create a new client object in order to replicate to the other
			 * server
			 */
			ClientConnection clientConnection = new ClientConnection(server.getAddress(), server.getPort());
			try {
				clientConnection.connect();
			} catch (IOException e) {
				logger.error("Connection Error @move data " + server.toString());
			}
			KVStore kvStore = new KVStore(clientConnection);

			/*
			 * read every line one by one, and determine whether or not it shall
			 * be added to the keysToDelete list
			 */
			String zeile = "";
			while ((zeile = br.readLine()) != null) {
				String[] tokens = zeile.trim().split("\\s+");
				String hashedKey = Hashing.hashIt(tokens[0]);
				if (hashedKey.compareTo(start.getLower_limit()) > 0
						&& hashedKey.compareTo(start.getUpper_limit()) <= 0) {
					kvStore.put(tokens[0], tokens[1]);
					keysToDelete.add(tokens[0]);
				}
				if (end != null) {
					if (hashedKey.compareTo(end.getLower_limit()) > 0
							&& hashedKey.compareTo(end.getUpper_limit()) <= 0) {
						kvStore.put(tokens[0], tokens[1]);
						keysToDelete.add(tokens[0]);
					}
				}
			}
			br.close();
			fr.close();

			// delete the list of keys, keysToDelete
			kvserverstore.delete(keysToDelete);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteFile() {
		kvserverstore.deleteFile();
	}

	public Map<String, String> getData() {
		// moveData returns complete data saved in file
		return this.kvserverstore.getData();
	}

}
