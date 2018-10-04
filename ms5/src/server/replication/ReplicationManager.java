package server.replication;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app_kvServer.KVServer;
import common.Hashing;
import common.HostRepresentation;
import logger.Constants;

/**
 * This class receives replica-data (sent from coordinator of another server)
 * and processes it (setting the replica objects of the server and saving the
 * key-value pairs into the corresponding files)
 */

public class ReplicationManager {

	// replicas where replicated data from other servers is saved
	private Replica replica1 = null;
	private Replica replica2 = null;

	// server object, this replication manager belongs to
	private KVServer server;

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	public ReplicationManager(KVServer server) {
		this.server = server;
		logger.debug("Replication Manager initialized");
	}

	/**
	 * This method initializes a single replica. The integer parameter
	 * determines if the replica1 or replica2 are going to be initialized. The
	 * String parameter determines the replicas responsibility range.
	 * 
	 * @param responsabilityKey
	 * @param replicaNumber
	 * @return
	 */

	public boolean initializeReplica(String responsabilityKey, int replicaNumber) {
		logger.debug("initialize Replica " + replicaNumber + "...");

		HostRepresentation replicatedServer = server.getMetadata().getResponsibleServer(responsabilityKey);

		if (replicaNumber == 1) {
			replica1 = new Replica(replicatedServer, replicaNumber, server.getPort());
		} else if (replicaNumber == 2) {
			replica2 = new Replica(replicatedServer, replicaNumber, server.getPort());
		} else {
			return false;
		}
		logger.debug("Replica " + replicaNumber + " initialized");
		return true;
	}

	/**
	 * method decides which replica is responsible for the key and invokes the
	 * delete method in this replica
	 * 
	 * @param key
	 * @return
	 */
	public boolean replicateDelete(String key) {
		HostRepresentation responsibleReplica = server.getMetadata().getResponsibleServer(Hashing.hashIt(key));

		boolean deleteStatus = false;
		try {
			if (replica1 != null) {
				if (responsibleReplica.equals(replica1.getRepresentation())) {
					deleteStatus = replica1.delete(key);
					logger.debug("delete in replica 1");
				}
			}
			if (replica2 != null) {
				if (responsibleReplica.equals(replica2.getRepresentation())) {
					deleteStatus = replica2.delete(key);
					logger.debug("delete in replica 2");
				}
			}
		} catch (Exception e) {
			deleteStatus = false;
		}
		return deleteStatus;
	}

	/**
	 * this method is invoked by a replication message from another server to
	 * apply new changes (PUT) to its replicas
	 * 
	 * @param key
	 * @param value
	 * @return
	 */

	public boolean replicatePut(String key, String value) {
		HostRepresentation responsibleReplica = server.getMetadata().getResponsibleServer(Hashing.hashIt(key));

		boolean storeStatus = false;

		try {
			// save received key-value pair to corresponding replica
			if (replica1 != null) {
				if (responsibleReplica.equals(replica1.getRepresentation())) {
					// REPLICA1 is responsible
					logger.debug("store into replica 1");
					storeStatus = replica1.put(key, value);
				}
			}
			if (replica2 != null) {
				if (responsibleReplica.toHash().equals(replica2.getRepresentation().toHash())) {
					// REPLICA2 is responsible
					logger.debug("store into replica 2");
					storeStatus = replica2.put(key, value);
				}
			}
		} catch (Exception e) {
			storeStatus = false;
		}
		return storeStatus;
	}

	/**
	 * This method moves the complete data from replica 1 to the coordinator.
	 * The coordinator takes over the responsibility of its first replica.
	 */
	public void takeOverReplica1() {
		// get data map from file
		Map<String, String> data = replica1.getData();
		// put every single key-value pair into coordinators database
		for (String key : data.keySet()) {
			server.getCoordinator().put(key, data.get(key));
		}

		/*
		 * the following segment is used to transfer the subscription-data from
		 * replica1 to this server's subscription-data
		 */
		for (int i = 0; i < replica1.getReplicatedSubscription().getSubscriberMap().size(); i++) {
			// get a client List for every key in the subscriber list
			String tempKey = replica1.getReplicatedSubscription().getSubscriberMap().firstKey();
			List<HostRepresentation> tempSubscriberList = replica1.getReplicatedSubscription().getSubscriberMap()
					.get(replica1.getReplicatedSubscription().getSubscriberMap().firstKey());

			/*
			 * insert every item into the server's subscriber list via
			 * subscribe(), thus replication of the subscriptions is guaranteed
			 */
			for (int j = 0; j < tempSubscriberList.size(); j++) {
				server.getSubscriptionService().subscribe(tempKey, tempSubscriberList.get(j));
			}

			/*
			 * delete the current entry from our replication (as the replication
			 * is not up to date anymore, due to the occurred failure)
			 */
			replica1.getReplicatedSubscription().getSubscriberMap()
					.remove(replica1.getReplicatedSubscription().getSubscriberMap().firstKey());

		}
	}

	/**
	 * method decides which replica is responsible for the key and invokes the
	 * subscribe method in this replica
	 * 
	 * @param key
	 *            the key on which the subscription occurs
	 * @param client
	 *            the client which wants to subscribe
	 * @return success-status
	 */
	public boolean replicateSubscribe(String key, HostRepresentation client) {
		HostRepresentation responsibleReplica = server.getMetadata().getResponsibleServer(Hashing.hashIt(key));
		
		boolean subscribeStatus = false;
		try {
			if (replica1 != null) {
				if (responsibleReplica.equals(replica1.getRepresentation())) {
					subscribeStatus = replica1.getReplicatedSubscription().subscribe(key, client);
					logger.debug("add subscriber in replica 1");
				}
			}
			if (replica2 != null) {
				if (responsibleReplica.equals(replica2.getRepresentation())) {
					subscribeStatus = replica2.getReplicatedSubscription().subscribe(key, client);
					logger.debug("add subscriber in replica 2");
				}
			}
		} catch (Exception e) {
			subscribeStatus = false;
		}
		return subscribeStatus;
	}

	/**
	 * method decides which replica is responsible for the key and invokes the
	 * unsubscribe method in this replica
	 * 
	 * @param key
	 *            the key on which the unsubscription occurs
	 * @param client
	 *            the client which wants to unsubscribe
	 * @return success-status
	 */
	public boolean replicateUnsubscribe(String key, HostRepresentation client) {
		HostRepresentation responsibleReplica = server.getMetadata().getResponsibleServer(Hashing.hashIt(key));

		boolean unsubscribeStatus = false;
		try {
			if (replica1 != null) {
				if (responsibleReplica.equals(replica1.getRepresentation())) {
					unsubscribeStatus = replica1.getReplicatedSubscription().unsubscribe(key, client);
					logger.debug("remove subscriber from replica 1");
				}
			}
			if (replica2 != null) {
				if (responsibleReplica.equals(replica2.getRepresentation())) {
					unsubscribeStatus = replica2.getReplicatedSubscription().unsubscribe(key, client);
					logger.debug("remove subscriber from replica 2");
				}
			}
		} catch (Exception e) {
			unsubscribeStatus = false;
		}
		return unsubscribeStatus;
	}

}
