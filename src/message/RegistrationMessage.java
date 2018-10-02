package message;

import java.util.ArrayList;
import java.util.List;

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
		// first entry is messageType converted to one byte
		byte[] temp = new byte[1 + 1 + this.email.length() + 1 + this.pw.length() + 1];
		temp[0] = messageTypeToByte(this.messageType);
		temp[1] = SEPARATE;
		System.arraycopy(this.email, 0, temp, 2, this.email.length());
		temp[2 + this.email.length()] = SEPARATE;
		System.arraycopy(this.pw, 0, temp, 3 + this.email.length(), this.pw.length());
		temp[3 + this.email.length() + this.pw.length()] = END;
		
		this.msgBytes = temp;
	}

	private void bytesToValues() {
		// read first byte to identify the message type 
		this.messageType = byteToMessageType(this.msgBytes[0]);
		
		if(this.messageType == MESSAGETYPE.REGISTRATION_REQUEST) {
			// start after SEPARATE bit
			List<Byte> nextToken = new ArrayList<Byte>();
			for (int i = 2; i < this.msgBytes.length; i++) {
				if(this.msgBytes[i] == SEPARATE) {
					break;
				}
				nextToken.add(this.msgBytes[i]);
			}
			this.email = nextToken.toString();
			nextToken.clear();
			
			for (int i = 3 + this.email.length(); i < this.msgBytes.length; i++) {
				if(this.msgBytes[i] == END) {
					break;
				}
				nextToken.add(this.msgBytes[i]);
			}
			this.pw = nextToken.toString();
			
			
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
	
}
