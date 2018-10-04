package subscription;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.CommunicationModule;
import common.HostRepresentation;
import common.messages.Message;
import logger.Constants;

/**
 * The Publisher Class is responsible for sending publish messages to clients.
 *
 */

public class Publisher {
	
	private Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	
	/**
	 * sends a message (with a KVSubscriptionMassage) to the Client 
	 * 
	 * @param client
	 * @param msg
	 */
	public void sendMsg(HostRepresentation client, Message msg, String key){
		CommunicationModule communication = new CommunicationModule(client.getAddress(), client.getPort());
		try {
			communication.createSocket();
			communication.send(msg.getBytes());
			logger.debug("Published key " + key + " to Client " + client.getAddress() + " " + client.getPort());
			communication.closeSocket();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
