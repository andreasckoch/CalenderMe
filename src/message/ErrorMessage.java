package message;

import message.MessageInterface.MESSAGETYPE;

public class ErrorMessage implements MessageInterface{
	private MESSAGETYPE messageType = MESSAGETYPE.ERROR;
	private byte[] msgBytes = new byte[] {MessageHelper.messageTypeToByte(MESSAGETYPE.ERROR)};
	
	
	public ErrorMessage() {
		
	}
	
	public MESSAGETYPE getMessageType() {
		return messageType;
	}

	@Override
	public byte[] getMsgBytes() {
		return msgBytes;
	}
}
