package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logger.Constants;
import message.*;

public class MessageDecoder {

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private MessageInterface returnMessage;

	public MessageDecoder() {

	}

	public MessageInterface processMessage(byte[] msgBytesFromClient) {

		switch (msgBytesFromClient[0]) {
		// 0x00 and 0x01 (operation success/failure) are not processed by server
		case 0x02:
		case 0x03:
			RegistrationHandler registrationHandler = new RegistrationHandler(
					new RegistrationMessage(msgBytesFromClient));
			returnMessage = registrationHandler.process();
			break;
		case 0x05:
			LoginHandler loginHandler = new LoginHandler(new LoginMessage(msgBytesFromClient));
			returnMessage = loginHandler.process();
			break;
			// TODO add other message types
		default:
			returnMessage = new ErrorMessage();
			break;
		}

		return returnMessage;
	}

}
