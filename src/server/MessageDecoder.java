package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logger.Constants;
import message.*;

public class MessageDecoder {

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Handler handler;

	public MessageDecoder() {

	}

	public MessageInterface processMessage(byte[] msgBytesFromClient) {

		switch (msgBytesFromClient[0]) {
		// 0x00 and 0x01 (operation success/failure) are not processed by server
		case 0x02:
		case 0x03:
			handler = new RegistrationHandler(new RegistrationMessage(msgBytesFromClient));
			break;
		case 0x05:
			handler = new LoginHandler(new LoginMessage(msgBytesFromClient));
			break;
			// TODO add other message types
		default:
			return new ErrorMessage();
		
		}

		return handler.process();
	}

}
