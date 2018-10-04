package server.replication;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app_kvServer.KVServer;
import common.CommunicationModule;
import common.HostRepresentation;
import common.messages.KVReplicationMessage;
import common.messages.KVReplicationMessageInterface;
import common.messages.Message;
import logger.Constants;

/**
 * The reconciliation manager invokes the saving process of its corresponding
 * server. So this manager sends Replication Messages to its defined replication
 * server nodes to save its data into their replicas.
 */
public class ReconciliationManager {

	KVServer server;

	// replication server that should contain this servers data
	private HostRepresentation replicationServer1;
	private HostRepresentation replicationServer2;

	private String coordinatorKey;

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	public ReconciliationManager(KVServer server) {
		logger.debug("Initialize Reconciliation Manager...");
		this.server = server;

		coordinatorKey = server.getResponsibiltyKey();

		logger.debug("Set Coordinator key " + coordinatorKey);

		// set first successor of this server = first replication server
		this.replicationServer1 = server.getMetadata().getSuccessor(coordinatorKey);
		logger.debug(
				"Set Replication server 1: " + replicationServer1.getAddress() + ":" + replicationServer1.getPort());
		// set second successor of this server = second replication server
		this.replicationServer2 = server.getMetadata().getSuccessor(replicationServer1.toHash());
		logger.debug(
				"Set Replication server 2: " + replicationServer2.getAddress() + ":" + replicationServer2.getPort());

		initReplicas();

		logger.debug("Reconciliation Manager initialized");
	}

	/**
	 * This method updates its responsible replication servers according to the
	 * servers metadata and replicates the servers data completely to both
	 * replication servers.
	 */
	public void updateReplicationServers(HostRepresentation replicationServer1Old,
			HostRepresentation replicationServer2Old) {

		/*
		 * check if the servers responsible replication servers were changed due
		 * to the metadata update.
		 */
		if (!replicationServer1.equals(replicationServer1Old) || !replicationServer2.equals(replicationServer2Old)) {

			logger.debug("updated replication servers - replicate data");

			// replicate all data to the two replication servers
			this.replicateData();
		}
		/*
		 * Else, no change for this server's replicas occurred, nothing has to
		 * be changed
		 */
	}

	/**
	 * This method only creates a socket to the passed server representation,
	 * sends a replication message and logs the success message.
	 * 
	 * @param replicaRep
	 * @param msgToSend
	 */
	private void sendReplicationMessage(HostRepresentation replicaRep, Message msgToSend) {
		try {
			// start connection to replication server
			CommunicationModule communicate = new CommunicationModule(replicaRep.getAddress(), replicaRep.getPort());
			communicate.createSocket();

			// replicate one single key-value pair to the responsible
			// replication server
			communicate.send(msgToSend.getBytes());

			// receive reply of the replication server if replication successful
			// (R_SUCCESS or R_ERROR)
			Message receivedMsg = new Message(communicate.receive());
			communicate.closeSocket();

			KVReplicationMessage msgFromReplicationServer = new KVReplicationMessage(receivedMsg.getData());

			if (msgFromReplicationServer.getStatusType().equals(KVReplicationMessageInterface.StatusType.R_SUCCESS)) {
				logger.info("Replication Message to " + replicaRep.getAddress() + ":" + replicaRep.getPort()
						+ " successful");
			} else {
				logger.error("Replication Message to " + replicaRep.getAddress() + ":" + replicaRep.getPort()
						+ " not successful");
			}
		} catch (IOException e) {
			logger.error("error in communication with replication servers");
			e.printStackTrace();
		}
	}

	/**
	 * This method replicates one single key-value pair to both corresponding
	 * replica nodes.
	 * 
	 * @param key
	 * @param value
	 * @param replicaRep
	 *            this server knows which server node he has to save his
	 *            replication to
	 */
	public void replicatePUT(String key, String value) {
		// create put message to send to replication servers
		KVReplicationMessage sendMsg = new KVReplicationMessage(KVReplicationMessageInterface.StatusType.R_PUT, key,
				value);
		Message msgToSend = new Message(Message.MessageType.REPLICATION, sendMsg.getBytes());

		// send messages
		sendReplicationMessage(replicationServer1, msgToSend);
		sendReplicationMessage(replicationServer2, msgToSend);
	}

	/**
	 * sends a delete command to the replica node
	 * 
	 * @param key
	 * @param replicaRep
	 */
	public void replicateDELETE(String key) {
		// create delete message to send to replication servers
		KVReplicationMessage replicationMessage = new KVReplicationMessage(
				KVReplicationMessageInterface.StatusType.R_DELETE, key);
		Message msgToSend = new Message(Message.MessageType.REPLICATION, replicationMessage.getBytes());

		// send messages
		sendReplicationMessage(replicationServer1, msgToSend);
		sendReplicationMessage(replicationServer2, msgToSend);
	}

	/**
	 * This method sends Replication Messages to its responsible replication
	 * server nodes to invoke the initialization of the replica objects.
	 */
	public void initReplicas() {
		// create R_REQUEST to initialize Replicas of responsible replication
		// servers
		logger.debug("Initialize Replicas via Replication Message...");
		KVReplicationMessage replicationMessage1 = new KVReplicationMessage(
				KVReplicationMessageInterface.StatusType.R_REQUEST, 1, coordinatorKey);
		Message initReplica1 = new Message(Message.MessageType.REPLICATION, replicationMessage1.getBytes());

		KVReplicationMessage replicationMessage2 = new KVReplicationMessage(
				KVReplicationMessageInterface.StatusType.R_REQUEST, 2, coordinatorKey);
		Message initReplica2 = new Message(Message.MessageType.REPLICATION, replicationMessage2.getBytes());

		// send messages
		sendReplicationMessage(replicationServer1, initReplica1);
		sendReplicationMessage(replicationServer2, initReplica2);
	}

	/**
	 * This method replicates its server's complete data to its corresponding
	 * replication server nodes.
	 */
	public void replicateData() {

		logger.debug("Initialize complete data replication to replication server nodes...");

		// initialize new replicas on replication server nodes
		initReplicas();

		// get a map of all data saved in coordinator
		Map<String, String> data = server.getCoordinator().getData();

		logger.debug("Daten vom Coordinator: " + data.toString());

		// put every single key-value pair to this server's replication server
		// nodes
		for (String key : data.keySet()) {
			logger.debug("key: " + key + ", value: " + data.get(key));
			this.replicatePUT(key, data.get(key));
		}

		logger.debug("Complete data replication to replication server nodes completed");

	}

	/**
	 * this method sends a replication of the occurred subscription to the
	 * responsible two replication servers
	 * 
	 * @param key
	 *            the key to which the subscription occurred
	 * @param client
	 *            the client who subscribed
	 */
	public void replicateSubscribe(String key, HostRepresentation client) {
		// create subscription message to send to replication servers
		KVReplicationMessage subReplicationMsg = new KVReplicationMessage(
				KVReplicationMessageInterface.StatusType.R_SUB, key, client);
		Message msgToSend = new Message(Message.MessageType.REPLICATION, subReplicationMsg.getBytes());

		// send messages
		sendReplicationMessage(replicationServer1, msgToSend);
		sendReplicationMessage(replicationServer2, msgToSend);
	}

	/**
	 * this method sends a replication of the occurred unsubscription to the
	 * responsible two replication servers
	 * 
	 * @param key
	 *            the key to which the unsubscription occurred
	 * @param client
	 *            the client who unsubscribed
	 */
	public void replicateUnsubscribe(String key, HostRepresentation client) {
		// create subscription message to send to replication servers
		KVReplicationMessage subReplicationMsg = new KVReplicationMessage(
				KVReplicationMessageInterface.StatusType.R_UNSUB, key, client);
		Message msgToSend = new Message(Message.MessageType.REPLICATION, subReplicationMsg.getBytes());

		// send messages
		sendReplicationMessage(replicationServer1, msgToSend);
		sendReplicationMessage(replicationServer2, msgToSend);
	}
}
