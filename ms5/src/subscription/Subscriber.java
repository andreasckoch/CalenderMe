package subscription;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import client.ClientConnection;
import common.CommunicationModule;
import common.HostRepresentation;
import common.messages.Message;
import common.messages.KVSubscriptionMessageInterface.StatusType;
import common.messages.KVSubscriptionMessage;
import common.messages.Message.MessageType;
import logger.Constants;

/**
 * Sends a subscribe  and unsubscribe message to the responsible server
 *
 */
public class Subscriber {	
	
	private static final Logger logger = LogManager.getLogger(Constants.APP_NAME);

	public HostRepresentation clientid;
	private int tryCount = 0;
	private CommunicationModule communicationModule;
	private ClientConnection clientConnection;
	
	public Subscriber(ClientConnection clientConnection, HostRepresentation clientid) {
		this.clientConnection = clientConnection;
		this.clientid = clientid;
	}
	
	/**
	 * only important for testing
	 * 
	 * @param clientConnection
	 */
	public Subscriber(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;
	}
	
	/**
	 * Sends a subscribe message to the server and waits for a subscribe success message
	 * 
	 * @param key that should be subscribed on
	 * @return answer from server (SUCCESS/ERROR)
	 */
	public KVSubscriptionMessage subscribe(String key) throws Exception{
		
		Message msgToSub;
		KVSubscriptionMessage kvSubMsg;

		// counter of SERVER_NOT_RESPONSIBLE responses
		if (tryCount > 4) {
			tryCount = 0;
			
			return new KVSubscriptionMessage(StatusType.ERROR);
		}
		
		//build KVSubscriptionMessage
		kvSubMsg = new KVSubscriptionMessage(StatusType.SUBSCRIBE, key, clientid);

		// send and receive
		logger.debug("Send subscribe message to Server: " + clientConnection.getAddress() + ":" + clientConnection.getPort());
		msgToSub = new Message(MessageType.SUBSCRIPTION, kvSubMsg.getBytes());
		communicationModule = clientConnection.getCommunicationModule();
		communicationModule.send(msgToSub.getBytes());
		byte[] receivedBytes = communicationModule.receive();
		KVSubscriptionMessage msgFromServer = new KVSubscriptionMessage(receivedBytes);

		// checking server response
		if (msgFromServer.getStatusType() == StatusType.SERVER_NOT_RESPONSIBLE) {
			clientConnection.setMetadata(msgFromServer.getMetadata());
			logger.debug("Server not responsible");
			// connecting to the server which is responsible in the new metadata
			clientConnection.connectResponsibleServer(key);
			tryCount++;
			return subscribe(key);

		} else {
			System.out.println("KVStore> OUTPUT: SUBSCRIBE " + msgFromServer.getStatusType());
			tryCount = 0;
			return msgFromServer;
		}
	}
	
	/**
	 * Sends an unsubscribe message to the server and waits for a subscribe success message
	 * 
	 * @param key that should be unsubscribed on
	 * @return answer from server (SUCCESS/ERROR)
	 */
	public KVSubscriptionMessage unsubscribe(String key) throws Exception{
		
		//Send a KVSubscribtionMessage(UNSUB, key) to the right server
		
		Message msgToUnsub;
		KVSubscriptionMessage kvUnsubMsg;

		// counter of SERVER_NOT_RESPONSIBLE responses
		if (tryCount > 4) {
			tryCount = 0;
			
			return new KVSubscriptionMessage(StatusType.ERROR);
		}

		//build KVSubscriptionMessage
		kvUnsubMsg = new KVSubscriptionMessage(StatusType.UNSUBSCRIBE, key, clientid);
		
		// send and receive
		logger.debug("Send unsubscribe message to Server: " + clientConnection.getAddress() + ":" + clientConnection.getPort());
		msgToUnsub = new Message(MessageType.SUBSCRIPTION, kvUnsubMsg.getBytes());
		communicationModule = clientConnection.getCommunicationModule();
		communicationModule.send(msgToUnsub.getBytes());
		byte[] receivedBytes = communicationModule.receive();
		KVSubscriptionMessage msgFromServer = new KVSubscriptionMessage(receivedBytes);

		// checking server response
		if (msgFromServer.getStatusType() == StatusType.SERVER_NOT_RESPONSIBLE) {
			clientConnection.setMetadata(msgFromServer.getMetadata());
			logger.debug("Server not responsible");
			// connecting to the server which is responsible in the new metadata
			clientConnection.connectResponsibleServer(key);
			tryCount++;
			return subscribe(key);

		} else {
			System.out.println("KVStore> OUTPUT: UNSUBSCRIBE " + msgFromServer.getStatusType());
			tryCount = 0;
			return msgFromServer;
		}
		
	}
}
