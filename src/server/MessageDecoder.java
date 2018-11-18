package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.Constants;
import message.*;

public class MessageDecoder {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Handler handler;

	public MessageDecoder() {

	}

	public MessageInterface processMessage(byte[] msgBytesFromClient) {

		switch (MessageHelper.byteToMessageType(msgBytesFromClient[0])) {
		// operation success/failure are not processed by server
		case REGISTRATION:
		case REGISTRATION_DELETE:
		case REGISTRATION_MODIFICATION_EMAIL:
		case REGISTRATION_MODIFICATION_PW:
			handler = new RegistrationHandler(new RegistrationMessage(msgBytesFromClient));
			break;
		case LOGIN:
			handler = new LoginHandler(new LoginMessage(msgBytesFromClient));
			break;
		case PROFILE_UPDATE_PRIVATE:
		case PROFILE_UPDATE_PUBLIC:
			handler = new ProfileHandler(new ProfileMessage(msgBytesFromClient));
		default:
			return new ErrorMessage();
		}
		return handler.process();
	}

}
