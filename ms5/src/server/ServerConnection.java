package server;

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app_kvServer.KVServer;
import common.CommunicationModule;
import common.Hashing;
import common.HostRepresentation;
import common.messages.KVAdminMessage;
import common.messages.Message;
import common.messages.KVAdminMessageInterface.MethodType;
import common.messages.KVMessage;
import common.messages.KVMessageInterface;
import common.messages.KVReplicationMessage;
import common.messages.KVReplicationMessageInterface;
import common.messages.KVSubscriptionMessage;
import common.messages.KVSubscriptionMessageInterface;
import common.messages.Message.MessageType;
import logger.Constants;
import server.replication.ReconciliationManager;

public class ServerConnection implements Runnable {

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private boolean isOpen;
	private Socket clientSocket;
	private KVServer server;
	private CommunicationModule communicationModule;

	public ServerConnection(Socket clientSocket, KVServer server) {
		this.clientSocket = clientSocket;
		this.isOpen = true;
		this.server = server;
	}

	@Override
	public void run() {
		try {
			communicationModule = new CommunicationModule(clientSocket);
			logger.debug("Server Connection: Thread " + Thread.currentThread().getId() + " started");
			while (isOpen) {
				try {
					Message receivedMsg = receiveMessage();
					if (receivedMsg != null) {
						if (receivedMsg.getMessageType().equals(MessageType.ADMIN)) {
							logger.debug("ADMIN MESSAGE RECEIVED");
							handleAdminMessage(receivedMsg);
						} else if (receivedMsg.getMessageType().equals(MessageType.CLIENT)) {
							logger.debug("CLIENT MESSAGE RECEIVED");
							handleClientMessage(receivedMsg);
						} else if (receivedMsg.getMessageType().equals(MessageType.REPLICATION)) {
							logger.debug("REPLICATION MESSAGE RECEIVED");
							handleReplicationMessage(receivedMsg);
						} else if (receivedMsg.getMessageType().equals(MessageType.SUBSCRIPTION)) {
							logger.debug("SUBSCRIPTION MESSAGE RECEIVED");
							handleSubscriptionMessage(receivedMsg);
						} else {
							break;
						}
					}
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				} catch (IllegalArgumentException iae) {
					logger.error("Wrong message format!");

					communicationModule.send((new KVAdminMessage(MethodType.ERROR)).getBytes());
					isOpen = false;
				} catch (Exception e) {
					isOpen = false;
				}
			}

		} catch (IOException ioe) {
			logger.error("Error! Connection could not be established!", ioe);
		} finally {
			try {
				if (clientSocket != null) {
					communicationModule.closeSocket();
					logger.info("Disconnected from Client on port " + clientSocket.getPort());
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
		logger.debug("Server Connection: Thread " + Thread.currentThread().getId() + " stopped");
	}

	private Message receiveMessage() throws IOException {
		byte[] input = communicationModule.receive();
		Message receivedMsg;
		try {
			receivedMsg = new Message(input);
			if (receivedMsg.getMessageType() == null || receivedMsg.getData() == null)
				throw new IllegalArgumentException("Malformed message");
		} catch (IllegalArgumentException e) {
			receivedMsg = null;
		}

		return receivedMsg;
	}

	/**
	 * handle client messages and invoke respective methods to process them
	 * 
	 * @param receivedMsg
	 *            the message received directly by the server
	 */
	private void handleClientMessage(Message receivedMsg) throws Exception {
		KVMessageInterface reply = null;
		if (server.isService_running()) {
			// create KVMessage from Message
			KVMessage msgFromClient = new KVMessage(receivedMsg.getData());

			String hashedKey = Hashing.hashIt(msgFromClient.getKey());

			HostRepresentation serverRepTemp = server.getMetadata().getResponsibleServer(hashedKey);

			// server is NOT RESPONSIBLE
			if (!serverRepTemp.toHash().equals(server.getResponsibiltyKey())) {
				logger.debug("Server not responsible. Update the clients metadata");
				reply = new KVMessage(KVMessageInterface.StatusType.SERVER_NOT_RESPONSIBLE, server.getMetadata());
			}

			// server is RESPONSIBLE
			else {
				if (msgFromClient.getStatus().equals(KVMessageInterface.StatusType.PUT)) {
					// PUT
					if (server.isWrite_locked() == false) {
						// WRITE LOCK NOT ACTIVATED
						logger.debug("INVOKING PUT METHOD");
						reply = server.getCoordinator().put(msgFromClient.getKey(), msgFromClient.getValue());

						try {
							server.getSubscriptionService().publish(msgFromClient.getKey(), msgFromClient.getValue());
						} catch (Exception e) {
							logger.error("PUBLISHING PUT ERROR", e);
						}
					} else {
						// WRITE LOCK ACTIVATED
						reply = new KVMessage(KVMessageInterface.StatusType.SERVER_WRITE_LOCK);
					}
				} else if (msgFromClient.getStatus().equals(KVMessageInterface.StatusType.GET)) {
					// GET
					reply = server.getCoordinator().get(msgFromClient.getKey());
				} else if (msgFromClient.getStatus().equals(KVMessageInterface.StatusType.DELETE)) {
					// DELETE
					if (server.isWrite_locked() == false) {
						// WRITE LOCK NOT ACTIVATED
						reply = server.getCoordinator().delete(msgFromClient.getKey());
						try {
							server.getSubscriptionService().publish(msgFromClient.getKey(), msgFromClient.getValue());
						} catch (Exception e) {
							logger.error("PUBLISHING DELETE ERROR", e);
						}

					} else {
						// WRITE LOCK ACTIVATED
						reply = new KVMessage(KVMessageInterface.StatusType.SERVER_WRITE_LOCK);
					}
				}
			}
		} else {
			// SERVER STOPPED
			KVMessage msgFromClient = new KVMessage(receivedMsg.getData());
			logger.info("Received Message: " + msgFromClient.getStatus().name());
			reply = new KVMessage(KVMessageInterface.StatusType.SERVER_STOPPED);
		}

		// send reply to client
		communicationModule.send(reply.getBytes());

	}

	/**
	 * handle admin messages and invoke methods in KVServer
	 * 
	 * @param receivedMsg
	 *            the message received directly by the server
	 */

	private void handleAdminMessage(Message receivedMsg) {
		KVAdminMessage adminMsg = new KVAdminMessage(receivedMsg.getData());
		if (adminMsg.getMethodType().equals(MethodType.INIT_SERVICE)) {
			logger.debug("INIT_SERVICE message received");
			server.initKVServer(adminMsg.getMetadata(), adminMsg.getCacheSize(), adminMsg.getDisplacementStrategy(),
					adminMsg.getKey());
			logger.debug("INIT_SERVICE successful");
		} else if (adminMsg.getMethodType().equals(MethodType.LOCK_WRITE)) {
			server.lockWrite();
		} else if (adminMsg.getMethodType().equals(MethodType.START)) {
			server.startService();
		} else if (adminMsg.getMethodType().equals(MethodType.STOP)) {
			server.stopService();
		} else if (adminMsg.getMethodType().equals(MethodType.MOVE_DATA)) {
			server.moveData(adminMsg.getRange(), adminMsg.getServerRep());
		} else if (adminMsg.getMethodType().equals(MethodType.UNLOCK_WRITE)) {
			server.unlockWrite();
		} else if (adminMsg.getMethodType().equals(MethodType.UPDATE)) {
			server.update(adminMsg.getMetadata());
		} else if (adminMsg.getMethodType().equals(MethodType.SHUTDOWN)) {
			communicationModule.send(new KVAdminMessage(MethodType.SUCCESS).getBytes());
			server.shutDown();
		} else if (adminMsg.getMethodType().equals(MethodType.HEARTBEAT)) {
			// success reply automatically sent in lower, more common case
		} else if (adminMsg.getMethodType().equals(MethodType.MOVE_REPLICA_DATA)) {
			server.setMetadata(adminMsg.getMetadata());
			server.getRepMgr().takeOverReplica1();
		}
		logger.debug("Send SUCCESS Reply");
		communicationModule.send(new KVAdminMessage(MethodType.SUCCESS).getBytes());
	}

	/**
	 * handles KVReplicationMessages
	 * 
	 * @param receivedMsg
	 *            the message received directly by the server
	 */
	private void handleReplicationMessage(Message receivedMsg) {
		KVReplicationMessage replicationMsg = new KVReplicationMessage(receivedMsg.getData());
		boolean success = false;

		// only if server is initialized, receive Replication Messages
		if (server.isService_running()) {
			// R_REQUEST: initialize replication manager and replicas
			if (replicationMsg.getStatusType().equals(KVReplicationMessageInterface.StatusType.R_REQUEST)) {
				logger.debug("Replication message type: R_REQUEST");
				success = server.getRepMgr().initializeReplica(replicationMsg.getResponsabilityKey(),
						replicationMsg.getReplica());
			}
			// R_PUT: put or update key-value pair in replica storage
			else if (replicationMsg.getStatusType().equals(KVReplicationMessageInterface.StatusType.R_PUT)) {
				logger.debug("Replication message type: R_PUT");
				success = server.getRepMgr().replicatePut(replicationMsg.getKey(), replicationMsg.getValue());
			}
			// R_DELETE: delete key-value pair in replica storage
			else if (replicationMsg.getStatusType().equals(KVReplicationMessageInterface.StatusType.R_DELETE)) {
				logger.debug("Replication message type: R_DELETE");
				success = server.getRepMgr().replicateDelete(replicationMsg.getKey());
			}
			// R_SUB: replicate subscription
			else if (replicationMsg.getStatusType().equals(KVReplicationMessageInterface.StatusType.R_SUB)) {
				logger.debug("Replication message type: R_SUB");
				success = server.getRepMgr().replicateSubscribe(replicationMsg.getKey(), replicationMsg.getClientRep());

			}
			// R_UNSUB: replicate unsubscription
			else if (replicationMsg.getStatusType().equals(KVReplicationMessageInterface.StatusType.R_UNSUB)) {
				logger.debug("Replication message type: R_UNSUB");
				success = server.getRepMgr().replicateUnsubscribe(replicationMsg.getKey(),
						replicationMsg.getClientRep());
			}
		}

		// REPLY: send success status to other server
		KVReplicationMessage rm;
		if (success) {
			rm = new KVReplicationMessage(KVReplicationMessageInterface.StatusType.R_SUCCESS);
		} else {
			rm = new KVReplicationMessage(KVReplicationMessageInterface.StatusType.R_ERROR);
		}
		Message m = new Message(Message.MessageType.REPLICATION, rm.getBytes());
		communicationModule.send(m.getBytes());
	}

	/**
	 * handles KVSubscriptionMessages
	 * 
	 * @param receivedMsg
	 *            the message received directly by the server
	 */
	private void handleSubscriptionMessage(Message receivedMsg) {
		KVSubscriptionMessage subscriptionMsg = new KVSubscriptionMessage(receivedMsg.getData());
		boolean success = false;

		// variables for responsibility check
		String hashedKey = Hashing.hashIt(subscriptionMsg.getKey());
		HostRepresentation serverRepTemp = server.getMetadata().getResponsibleServer(hashedKey);

		// server is NOT RESPONSIBLE
		if (!serverRepTemp.toHash().equals(server.getResponsibiltyKey())) {
			logger.debug("Server not responsible. Update the clients metadata");
			KVSubscriptionMessage reply = new KVSubscriptionMessage(
					KVSubscriptionMessageInterface.StatusType.SERVER_NOT_RESPONSIBLE, server.getMetadata());
			communicationModule.send(reply.getBytes());
		}

		// server is RESPONSIBLE
		else {
			// handle SUBSCRIBE
			if (subscriptionMsg.getStatusType().equals(KVSubscriptionMessageInterface.StatusType.SUBSCRIBE)) {
				logger.debug("Subscription message type: SUB");
				success = server.getSubscriptionService().subscribe(subscriptionMsg.getKey(),
						subscriptionMsg.getClientRep());

				// replicate change to responsible replication servers
				try {
					ReconciliationManager recMgr = new ReconciliationManager(server);
					recMgr.replicateSubscribe(subscriptionMsg.getKey(), subscriptionMsg.getClientRep());
					logger.debug("sent subscription replication to replication servers");
				} catch (Exception e) {
					logger.error("subscription replication error", e);
				}

			}
			// handle UNSUBSCRIBE
			else if (subscriptionMsg.getStatusType().equals(KVSubscriptionMessageInterface.StatusType.UNSUBSCRIBE)) {
				logger.debug("Subscription message type: UNSUB");
				success = server.getSubscriptionService().unsubscribe(subscriptionMsg.getKey(),
						subscriptionMsg.getClientRep());

				// replicate change to responsible replication servers via
				// reconciliation manager
				try {
					ReconciliationManager recMgr = new ReconciliationManager(server);
					recMgr.replicateUnsubscribe(subscriptionMsg.getKey(), subscriptionMsg.getClientRep());
					logger.debug("sent unsubscription replication to replication servers");
				} catch (Exception e) {
					logger.error("unsubscription replication error", e);
				}

			}
			// send reply message back to client
			KVSubscriptionMessage sm;
			if (success) {
				sm = new KVSubscriptionMessage(KVSubscriptionMessageInterface.StatusType.SUCCESS);
				logger.debug("sent subscription/unsubscription success message");
			} else {
				sm = new KVSubscriptionMessage(KVSubscriptionMessageInterface.StatusType.ERROR);
				logger.debug("sent subscription/unsubscription error message");
			}
			communicationModule.send(sm.getBytes());
		}
	}

}
