package server;

import message.MessageInterface;
import message.RegistrationMessage;

public class MessageDecoder {

	private MessageInterface message;
	private MessageInterface returnMessage;
	private DBInteraction calenderDataBaseInteraction;

	public MessageDecoder() {
		
	}

	public MessageInterface processMessageType(byte[] msgBytesFromClient) {

		switch (msgBytesFromClient[0]) {
		case 0x00:
		case 0x01:
		case 0x02:
			message = new RegistrationMessage(msgBytesFromClient);
		// TODO add other message types
		}
		calenderDataBaseInteraction = new DBInteraction();
		returnMessage = calenderDataBaseInteraction.processMessage(message);
		
		return returnMessage;
	}

}
