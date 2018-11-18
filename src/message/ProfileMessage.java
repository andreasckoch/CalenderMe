package message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.Constants;

public class ProfileMessage implements MessageInterface {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(Constants.COMMUNICATION_NAME);

	private String email = null;
	private String name = null;
	private String age = null;
	private String bio = null;
	private MESSAGETYPE messageType;
	private byte[] msgBytes;

	public ProfileMessage(MESSAGETYPE messageType, String email, String name, String age, String bio) {
		this.email = email;
		this.name = name;
		this.age = age;
		this.bio = bio;
		this.messageType = messageType;
		this.valuesToBytes();
	}

	public ProfileMessage(byte[] msgBytes) {
		this.msgBytes = msgBytes;
		this.bytesToValues();
	}

	public ProfileMessage(MESSAGETYPE messageType) {
		this.messageType = messageType;
		this.valuesToBytes();
	}

	private void valuesToBytes() {

		if (this.messageType == MESSAGETYPE.PROFILE_UPDATE_PUBLIC
				|| this.messageType == MESSAGETYPE.PROFILE_UPDATE_PRIVATE) {

			// calculate size of msgBytes
			byte[] temp = new byte[1 + 1 + this.email.length() + 1 + this.name.length() + 1 + this.age.length() + 1
					+ this.bio.length() + 1];

			// first entry is messageType converted to one byte
			temp[0] = MessageHelper.messageTypeToByte(this.messageType);

			// add values to msgBytes
			temp = MessageHelper.addNextValueToBytes(temp, this.email.getBytes(), 1);
			temp = MessageHelper.addNextValueToBytes(temp, this.name.getBytes(), 2 + this.email.length());
			temp = MessageHelper.addNextValueToBytes(temp, this.age.getBytes(),
					3 + this.email.length() + this.name.length());
			temp = MessageHelper.addNextValueToBytes(temp, this.bio.getBytes(),
					4 + this.email.length() + this.name.length() + this.age.length());

			temp[5 + this.email.length() + this.name.length() + this.age.length()
					+ this.bio.length()] = MessageHelper.END;

			this.msgBytes = temp;

		} else {
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

		// only for a profile update request fill values
		if (this.messageType == MESSAGETYPE.PROFILE_UPDATE_PUBLIC
				|| this.messageType == MESSAGETYPE.PROFILE_UPDATE_PRIVATE) {
			// consider SEPARATE and END bytes when choosing positions
			this.email = new String(MessageHelper.getNextValueFromBytes(this.msgBytes, 2));
			this.name = new String(MessageHelper.getNextValueFromBytes(this.msgBytes, 3 + this.email.length()));
			this.age = new String(
					MessageHelper.getNextValueFromBytes(this.msgBytes, 4 + this.email.length() + this.name.length()));
			this.bio = new String(MessageHelper.getNextValueFromBytes(this.msgBytes,
					5 + this.email.length() + this.name.length() + this.age.length()));
		}
	}

	public String getEmail() {
		return email;
	}
	
	public String getName() {
		return name;
	}

	public String getAge() {
		return age;
	}

	public String getBio() {
		return bio;
	}

	public MESSAGETYPE getMessageType() {
		return messageType;
	}

	@Override
	public byte[] getMsgBytes() {
		return msgBytes;
	}
}
