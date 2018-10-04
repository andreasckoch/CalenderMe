package message;


import message.Message;

public class RegistrationMessage implements RegistrationMessageInterface {

	private String email;
	private String pw;
	private MESSAGETYPE messageType;
	private byte[] msgBytes;

	public RegistrationMessage(MESSAGETYPE messageType, String email, String pw) {
		this.pw = pw;
		this.email = email;
		this.messageType = messageType;
		this.valuesToBytes();
	}

	public RegistrationMessage(byte[] msgBytes) {
		this.msgBytes = msgBytes;
		this.bytesToValues();
	}

	private void valuesToBytes() {
		// calculate size of msgBytes
		byte[] temp = new byte[1 + 1 + this.email.length() + 1 + this.pw.length() + 1];

		// first entry is messageType converted to one byte
		temp[0] = messageTypeToByte(this.messageType);
		
		// add values to msgBytes
		temp = Message.addNextValueToBytes(temp, this.email.getBytes(), 1);
		temp = Message.addNextValueToBytes(temp, this.pw.getBytes(), 2 + this.email.length());
		
		temp[3 + this.email.length() + this.pw.length()] = Message.END;
		
		this.msgBytes = temp;
	}

	private void bytesToValues() {
		// read first byte to identify the message type
		this.messageType = byteToMessageType(this.msgBytes[0]);

		// only for a request message fill values
		if (this.messageType == MESSAGETYPE.REGISTRATION_REQUEST) {
			// consider SEPARATE and END bytes when choosing positions			
			this.email =  new String(Message.getNextValueFromBytes(this.msgBytes, 2));
			this.pw =  new String(Message.getNextValueFromBytes(this.msgBytes, 3 + this.email.length()));
		}

	}

	private byte messageTypeToByte(MESSAGETYPE messageType) {
		byte indexByte;
		switch (messageType) {
		case REGISTRATION_REQUEST:
			indexByte = 0x00;
			break;
		case REGISTRATION_SUCCESS:
			indexByte = 0x01;
			break;
		case REGISTRATION_FAILED:
			indexByte = 0x02;
			break;
		default:
			indexByte = 0x7f;
			break;
		}
		return indexByte;
	}

	private MESSAGETYPE byteToMessageType(byte msgByte) {
		MESSAGETYPE messageType;
		switch (msgByte) {
		case 0x00:
			messageType = MESSAGETYPE.REGISTRATION_REQUEST;
			break;
		case 0x01:
			messageType = MESSAGETYPE.REGISTRATION_SUCCESS;
			break;
		case 0x02:
			messageType = MESSAGETYPE.REGISTRATION_FAILED;
			break;
		default:
			messageType = MESSAGETYPE.REGISTRATION_ERROR;
			break;
		}
		return messageType;
	}

	public String getEmail() {
		return email;
	}

	public String getPw() {
		return pw;
	}

	public MESSAGETYPE getMessageType() {
		return messageType;
	}

	public byte[] getMsgBytes() {
		return msgBytes;
	}
}
