package server;

import message.MessageInterface;
import message.RegistrationMessage;

public class MessageDecoder {

	private MessageInterface message;
	private MessageInterface returnMessage;
	private DBInteraction calenderDataBase;

	public MessageDecoder() {
		
	}

	public void processMessageType(byte[] msgBytesFromClient) {

		switch (msgBytesFromClient[0]) {
		case 0x00:
		case 0x01:
		case 0x02:
			message = new RegistrationMessage(msgBytesFromClient);
		// TODO add other message types
		}
		calenderDataBase = new DBInteraction();
		returnMessage = calenderDataBase.processMessage(message);
	}

}
