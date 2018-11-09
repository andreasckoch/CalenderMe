package message;

public class LoginMessage implements MessageInterface{

	private String email = null;
	private String pw = null;
	private MESSAGETYPE messageType;
	private byte[] msgBytes;

	public LoginMessage(MESSAGETYPE messageType, String email, String pw) {
		this.pw = pw;
		this.email = email;
		this.messageType = messageType;
		this.valuesToBytes();
	}

	public LoginMessage(byte[] msgBytes) {
		this.msgBytes = msgBytes;
		this.bytesToValues();
	}

	public LoginMessage(MESSAGETYPE messageType) {
		this.messageType = messageType;
		this.valuesToBytes();
	}

	private void valuesToBytes() {
		
		if (this.messageType == MESSAGETYPE.LOGIN) {
		// calculate size of msgBytes
		byte[] temp = new byte[1 + 1 + this.email.length() + 1 + this.pw.length() + 1];

		// first entry is messageType converted to one byte
		temp[0] = MessageHelper.messageTypeToByte(this.messageType);
		
		// add values to msgBytes
		temp = MessageHelper.addNextValueToBytes(temp, this.email.getBytes(), 1);
		temp = MessageHelper.addNextValueToBytes(temp, this.pw.getBytes(), 2 + this.email.length());
		
		temp[3 + this.email.length() + this.pw.length()] = MessageHelper.END;

		this.msgBytes = temp;
		}
		else {
			// calculate size of msgBytes
			byte[] temp = new byte[2];

			// first entry is messageType converted to one byte
			temp[0] = MessageHelper.messageTypeToByte(this.messageType);
			temp[1] = MessageHelper.END;
			this.msgBytes = temp;
		}
	}

	private void bytesToValues() {
		// read first byte to identify the message type
		this.messageType = MessageHelper.byteToMessageType(this.msgBytes[0]);

		// only for a request message fill values
		if (this.messageType == MESSAGETYPE.LOGIN) {
			// consider SEPARATE and END bytes when choosing positions			
			this.email =  new String(MessageHelper.getNextValueFromBytes(this.msgBytes, 2));
			this.pw =  new String(MessageHelper.getNextValueFromBytes(this.msgBytes, 3 + this.email.length()));
		}

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

	@Override
	public byte[] getMsgBytes() {
		return msgBytes;
	}

}
