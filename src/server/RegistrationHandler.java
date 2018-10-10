package server;

import message.MessageInterface;
import message.RegistrationMessage;
import message.RegistrationMessageInterface.MESSAGETYPE;

public class RegistrationHandler {

	private RegistrationMessage message;

	public RegistrationHandler(MessageInterface message) {
		this.message = (RegistrationMessage) message;
	}

	public MessageInterface process() {
		switch (this.message.getMessageType()){
		case REGISTRATION_REQUEST:
			
		}
		return null;
	}

}
