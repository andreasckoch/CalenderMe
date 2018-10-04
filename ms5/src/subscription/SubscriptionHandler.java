package subscription;

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.CommunicationModule;
import common.messages.Message;
import common.messages.KVSubscriptionMessageInterface.StatusType;
import common.messages.KVSubscriptionMessage;
import common.messages.Message.MessageType;
import logger.Constants;

/**
 * Handles publish messages from the server
 */
public class SubscriptionHandler implements Runnable {

	private boolean isOpen = true;
	private Socket subscriptionSocket;
	private CommunicationModule communicationModule;

	private static final Logger logger = LogManager.getLogger(Constants.APP_NAME);

	public SubscriptionHandler(Socket subscriptionSocket) {
		this.subscriptionSocket = subscriptionSocket;
	}

	@Override
	public void run() {
		try {
			communicationModule = new CommunicationModule(subscriptionSocket);
			logger.debug("Server Connection: Thread " + Thread.currentThread().getId() + " started");
			while (isOpen) {
				try {
					Message receivedMsg = receiveMessage();
					if (receivedMsg != null) {
						if (receivedMsg.getMessageType().equals(MessageType.SUBSCRIPTION)) {
							logger.debug("SUBSCRIPTION MESSAGE RECEIVED");
							handleSubscriptionMessage(receivedMsg);
						} else {
							logger.error("Wrong message Format");
							break;
						}
					}
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				} catch (IllegalArgumentException iae) {
					logger.error("Wrong message format!");

					communicationModule.send((new KVSubscriptionMessage(StatusType.ERROR)).getBytes());
					isOpen = false;
				} catch (Exception e) {
					isOpen = false;
				}
			}

		} catch (IOException ioe) {
			logger.error("Error! Connection could not be established!", ioe);
		} finally {
			try {
				if (subscriptionSocket != null) {
					communicationModule.closeSocket();
					logger.info("Disconnected from Client on port " + subscriptionSocket.getPort());
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
		logger.debug("Server Connection: Thread " + Thread.currentThread().getId() + " stopped");

	}

	/**
	 * receives incoming messages from the server
	 * 
	 * @return Message from the server
	 * @throws IOException
	 */
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
	 * handles the incoming KVSubscriptionMessages
	 * 
	 * @param receivedMsg
	 */
	private void handleSubscriptionMessage(Message receivedMsg) {
		KVSubscriptionMessage adminMsg = new KVSubscriptionMessage(receivedMsg.getData());
		if (adminMsg.getStatusType().equals(StatusType.PUBLISH)) {
			// check if value != null, then key changed, else key deleted
			if (adminMsg.getValue() != null) {
				logger.info("Key Changed: " + adminMsg.getKey() + " : " + adminMsg.getValue());
				System.out.println("NOTIFICATION: " + "Key Changed: " + adminMsg.getKey() + " : " + adminMsg.getValue());
				System.out.print("KVStore> ");
			} else {
				logger.info("KVStore: " + "Key Deleted: " + adminMsg.getKey());
				System.out.println("NOTIFICATION: " + "Key Deleted: " + adminMsg.getKey());
				System.out.print("KVStore> ");
			}
		}
	}
}
