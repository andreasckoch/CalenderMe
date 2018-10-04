package client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import common.messages.KVMessageInterface;
import common.messages.KVMessageInterface.StatusType;
import common.messages.Message;
import common.messages.Message.MessageType;
import common.CommunicationModule;
import common.messages.KVMessage;
import logger.Constants;

public class KVStore {

	private static final Logger logger = LogManager.getLogger(Constants.APP_NAME);

	private int tryCount = 0;
	private ClientConnection clientConnection;
	
	

	private CommunicationModule communicationModule;

	
	public KVStore(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;
	}

	/**
	 * Inserts a key-value pair into the KVServer.
	 *
	 * @param key
	 *            the key that identifies the given value.
	 * @param value
	 *            the value that is indexed by the given key.
	 * @return a message that confirms the insertion of the tuple or an error.
	 * @throws Exception
	 *             if put command cannot be executed (e.g. not connected to any
	 *             KV server).
	 */
	public KVMessageInterface put(String key, String value) throws Exception {
		Message msgToPut;
		KVMessage kvMsg;

		// counter of SERVER_NOT_RESPONSIBLE responses
		if (tryCount > 4) {
			tryCount = 0;
			return new KVMessage(StatusType.PUT_ERROR);
		}
		if (value == null) {
			kvMsg = new KVMessage(StatusType.DELETE, key, value);

		} else {
			kvMsg = new KVMessage(StatusType.PUT, key, value);
		}

		// send and receive
		logger.debug("Send PUT/DELETE to Server: " + clientConnection.getAddress() + ":" + clientConnection.getPort());
		msgToPut = new Message(MessageType.CLIENT, kvMsg.getBytes());
		communicationModule = clientConnection.getCommunicationModule();
		communicationModule.send(msgToPut.getBytes());
		byte[] receivedBytes = communicationModule.receive();
		KVMessage msgFromServer = new KVMessage(receivedBytes);

		// checking server response
		if (msgFromServer.getStatus() == KVMessageInterface.StatusType.SERVER_NOT_RESPONSIBLE) {
			clientConnection.setMetadata(msgFromServer.getMetadata());
			logger.debug("Server not responsible");
			// connecting to the server which is responsible in the new metadata
			clientConnection.connectResponsibleServer(key);
			tryCount++;
			return put(key, value);

		} else {
			tryCount = 0;
			return msgFromServer;
		}
	}

	/**
	 * Retrieves the value for a given key from the KVServer.
	 *
	 * @param key
	 *            the key that identifies the value.
	 * @return the value, which is indexed by the given key.
	 * @throws Exception
	 *             if put command cannot be executed (e.g. not connected to any
	 *             KV server).
	 */
	public KVMessageInterface get(String key) throws Exception {
		Message msgToPut;
		KVMessage kvMsg;

		// counter of SERVER_NOT_RESPONSIBLE responses
		if (tryCount > 4) {
			tryCount = 0;
			return new KVMessage(StatusType.GET_ERROR);
		}
		
		// send and receive
		kvMsg = new KVMessage(StatusType.GET, key);
		msgToPut = new Message(MessageType.CLIENT, kvMsg.getBytes());
		logger.debug("Send GET to Server: " + clientConnection.getAddress() + ":" + clientConnection.getPort());
		communicationModule = clientConnection.getCommunicationModule();
		communicationModule.send(msgToPut.getBytes());
		KVMessageInterface msgFromServer = new KVMessage(communicationModule.receive());

		// checking server response
		if (msgFromServer.getStatus() == KVMessageInterface.StatusType.SERVER_NOT_RESPONSIBLE) {
			clientConnection.setMetadata(msgFromServer.getMetadata());
			logger.debug("Server not responsible");
			// connecting to the server which is responsible in the new metadata
			clientConnection.connectResponsibleServer(key);
			tryCount++;
			return get(key);
		} else {
			tryCount = 0;
			return msgFromServer;
		}
	}
}
