package server.replication;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.HostRepresentation;
import common.messages.KVMessageInterface;
import logger.Constants;
import server.KVServerStore;
import subscription.SubscriptionService;

/**
 * The Replica class contains the replicated data from another server. Each
 * KVServer has max. 2 Replica-objects. Each Replica has the same
 * responsibility-range as the corresponding replicated server.
 *
 */

public class Replica {

	private String filename;
	private KVServerStore kvserverstore;
	private HostRepresentation serverRep;
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	private int replicaNumber;
	private SubscriptionService replicatedSubscription;

	/**
	 * Constructor for the replica object, hands over the KVServerStore
	 * instance, which created the replica object, in order to being able to
	 * utilize its put method in this replica object
	 * 
	 * @param kvserverstore
	 *            the KVServerStore object, which created this replica object
	 */
	public Replica(HostRepresentation serverRep, int replica, int port) {
		this.serverRep = serverRep;
		this.replicaNumber = replica;
		this.replicatedSubscription = new SubscriptionService();

		/*
		 * NAMING:
		 * 
		 * db_ [hashed responsibility key of this replica; same as replicated
		 * server] _rep [number of replica] _ [port of server that contains this
		 * replica]
		 */

		this.filename = "db_" + serverRep.toHash() + "_rep" + replica + "_" + port + ".txt";
		this.kvserverstore = new KVServerStore(filename);
	}

	/**
	 * Change the internal filename to the given filename (e.g. when a change in
	 * the server topology occurred and respective replica servers changed)
	 * 
	 * @param filename
	 *            the new filename to be set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Map<String, String> getData() {
		// moveData returns compete data saved in file
		return this.kvserverstore.getData();
	}

	/**
	 * Puts a key value pair into this replica's file, by utilizing the put
	 * method of the KVServerstore class
	 * 
	 * @param key
	 *            the key to be put in the replica file
	 * @param value
	 *            the value to be put in the replica file
	 * @return the returned KVMessage from the invoked put method
	 */
	public boolean put(String key, String value) {
		try {
			// save into corresponding file with KVServerStore put-method
			KVMessageInterface msg = kvserverstore.put(key, value);

			// check return value of the put method and return the corresponding
			// replication statusType
			if (msg.getStatus().equals(KVMessageInterface.StatusType.PUT_SUCCESS)
					|| msg.getStatus().equals(KVMessageInterface.StatusType.PUT_UPDATE)) {
				logger.debug("Saved successful in Replica " + replicaNumber);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Deletes key value pair from replica's file
	 * 
	 * @param key
	 *            the key to be deleted from the replica file
	 * @return the returned KVMessage from the invoked put method
	 */
	public boolean delete(String key) {

		try {
			String tempKey = key;
			ArrayList<String> toDelete = new ArrayList<String>();
			toDelete.add(tempKey);
			KVMessageInterface msg = kvserverstore.delete(toDelete);
			if (msg.getStatus().equals(KVMessageInterface.StatusType.DELETE_SUCCESS)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * deletes file of replica
	 */
	public void deleteFile() {
		kvserverstore.deleteFile();
	}

	public HostRepresentation getRepresentation() {
		return serverRep;
	}

	public HostRepresentation setRepresentation(HostRepresentation serverRep) {
		return this.serverRep = serverRep;
	}

	public SubscriptionService getReplicatedSubscription() {
		return replicatedSubscription;
	}
}
