package server;

import message.MessageInterface;
import message.RegistrationMessage;

public class MessageDecoder {

	private MessageInterface returnMessage;


	public MessageDecoder() {

	}

	public MessageInterface processMessage(byte[] msgBytesFromClient) {

		switch (msgBytesFromClient[0]) {
		case 0x00:
		case 0x01:
		case 0x02:
			RegistrationHandler registrationHandler = new RegistrationHandler(new RegistrationMessage(msgBytesFromClient));
			returnMessage = registrationHandler.process();			
		// TODO add other message types
		}
		
		
		return returnMessage;
	}

}
