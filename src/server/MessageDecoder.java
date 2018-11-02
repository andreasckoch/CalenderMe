package server;

import message.*;

public class MessageDecoder {

	private MessageInterface returnMessage;


	public MessageDecoder() {

	}

	public MessageInterface processMessage(byte[] msgBytesFromClient) {

		switch (msgBytesFromClient[0]) {
		// 0x00 and 0x01 (operation success/failure) are not processed by server
		case 0x02:
		case 0x03:
			RegistrationHandler registrationHandler = new RegistrationHandler(new RegistrationMessage(msgBytesFromClient));
			returnMessage = registrationHandler.process();
			System.out.println(((RegistrationMessage) returnMessage).getMessageType());
		case 0x05:
			LoginHandler loginHandler = new LoginHandler(new LoginMessage(msgBytesFromClient));
			returnMessage = loginHandler.process();
		// TODO add other message types
		default:
			returnMessage = new ErrorMessage();
		}
		
		
		return returnMessage;
	}

}
