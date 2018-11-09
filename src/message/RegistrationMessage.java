package message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.Constants;
import message.MessageHelper;

public class RegistrationMessage implements MessageInterface {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(Constants.COMMUNICATION_NAME);

	private String email = null;
	private String pw = null;
	private String changedField = null;
	private MESSAGETYPE messageType;
	private byte[] msgBytes;

	public RegistrationMessage(MESSAGETYPE messageType, String email, String pw) {
		this.pw = pw;
		this.email = email;
		this.messageType = messageType;
		this.valuesToBytes();
	}

	public RegistrationMessage(MESSAGETYPE messageType, String email, String pw, String changedField) {
		this.pw = pw;
		this.email = email;
		this.messageType = messageType;
		this.changedField = changedField;
		this.valuesToBytes();
	}

	public RegistrationMessage(byte[] msgBytes) {
		this.msgBytes = msgBytes;
		this.bytesToValues();
	}

	public RegistrationMessage(MESSAGETYPE messageType) {
		this.messageType = messageType;
		this.valuesToBytes();
	}

	private void valuesToBytes() {

		if (this.messageType == MESSAGETYPE.REGISTRATION || this.messageType == MESSAGETYPE.REGISTRATION_DELETE) {
			// calculate size of msgBytes
			byte[] temp = new byte[1 + 1 + this.email.length() + 1 + this.pw.length() + 1];

			// first entry is messageType converted to one byte
			temp[0] = MessageHelper.messageTypeToByte(this.messageType);

			// add values to msgBytes
			temp = MessageHelper.addNextValueToBytes(temp, this.email.getBytes(), 1);
			temp = MessageHelper.addNextValueToBytes(temp, this.pw.getBytes(), 2 + this.email.length());

			temp[3 + this.email.length() + this.pw.length()] = MessageHelper.END;

			this.msgBytes = temp;

		} else if (this.messageType == MESSAGETYPE.REGISTRATION_MODIFICATION_EMAIL
				|| this.messageType == MESSAGETYPE.REGISTRATION_MODIFICATION_PW) {

			// calculate size of msgBytes
			byte[] temp = new byte[1 + 1 + this.email.length() + 1 + this.pw.length() + 1 + this.changedField.length()
					+ 1];

			// first entry is messageType converted to one byte
			temp[0] = MessageHelper.messageTypeToByte(this.messageType);

			// add values to msgBytes
			temp = MessageHelper.addNextValueToBytes(temp, this.email.getBytes(), 1);
			temp = MessageHelper.addNextValueToBytes(temp, this.pw.getBytes(), 2 + this.email.length());
			temp = MessageHelper.addNextValueToBytes(temp, this.changedField.getBytes(),
					3 + this.email.length() + this.pw.length());

			temp[4 + this.email.length() + this.pw.length() + this.changedField.length()] = MessageHelper.END;

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
		if (this.messageType == MESSAGETYPE.REGISTRATION || this.messageType == MESSAGETYPE.REGISTRATION_DELETE
				|| this.messageType == MESSAGETYPE.REGISTRATION_MODIFICATION_EMAIL
				|| this.messageType == MESSAGETYPE.REGISTRATION_MODIFICATION_PW) {
			// consider SEPARATE and END bytes when choosing positions
			this.email = new String(MessageHelper.getNextValueFromBytes(this.msgBytes, 2));
			this.pw = new String(MessageHelper.getNextValueFromBytes(this.msgBytes, 3 + this.email.length()));
		}
		if (this.messageType == MESSAGETYPE.REGISTRATION_MODIFICATION_EMAIL
				|| this.messageType == MESSAGETYPE.REGISTRATION_MODIFICATION_PW) {
			this.changedField = new String(
					MessageHelper.getNextValueFromBytes(this.msgBytes, 4 + this.email.length() + this.pw.length()));
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

	public String getChangedField() {
		return changedField;
	}
}
