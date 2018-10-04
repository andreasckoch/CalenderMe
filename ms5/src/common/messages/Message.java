package common.messages;

public class Message {

	public enum MessageType {
		CLIENT, 		//0x00
		ADMIN,  		//0x01
		REPLICATION,	//0x02
		SUBSCRIPTION	//0x03
	}
	
	private byte[] msgBytes;
	private MessageType msgType;
	private byte[] msgData;
	
	public Message(byte[] msgBytes) {
		this.msgBytes = msgBytes;
		if(msgBytes[0] == 0x00) {
			this.msgType = MessageType.CLIENT;
			byte[] msgData = new byte[msgBytes.length-1];
			System.arraycopy(msgBytes, 1, msgData, 0, msgBytes.length-1);
			this.msgData = msgData;
		}
		else if(msgBytes[0] == 0x01) {
			this.msgType = MessageType.ADMIN;
			byte[] msgData = new byte[msgBytes.length-1];
			System.arraycopy(msgBytes, 1, msgData, 0, msgBytes.length-1);
			this.msgData = msgData;
		}
		else if(msgBytes[0] == 0x02) {
			this.msgType = MessageType.REPLICATION;
			byte[] msgData = new byte[msgBytes.length-1];
			System.arraycopy(msgBytes, 1, msgData, 0, msgBytes.length-1);
			this.msgData = msgData;
		}
		else if(msgBytes[0] == 0x03) {
			this.msgType = MessageType.SUBSCRIPTION;
			byte[] msgData = new byte[msgBytes.length-1];
			System.arraycopy(msgBytes, 1, msgData, 0, msgBytes.length-1);
			this.msgData = msgData;
		}
		else{
    		throw new IllegalArgumentException("Wrong Message format");
    	}
		
	}
	
	public Message(MessageType msgType, byte[] msgData) {
		if(msgType.equals(MessageType.CLIENT)) {
			this.msgType = MessageType.CLIENT;
			byte[] msgBytes = new byte[msgData.length+1];
			msgBytes[0] = 0x00;
			System.arraycopy(msgData, 0, msgBytes, 1, msgData.length);
			this.msgBytes = msgBytes;
		}
		else if (msgType.equals(MessageType.ADMIN)) {
			this.msgType = MessageType.ADMIN;
			byte[] msgBytes = new byte[msgData.length+1];
			msgBytes[0] = 0x01;
			System.arraycopy(msgData, 0, msgBytes, 1, msgData.length);
			this.msgBytes = msgBytes;
		}
		else if (msgType.equals(MessageType.REPLICATION)) {
			this.msgType = MessageType.REPLICATION;
			byte[] msgBytes = new byte[msgData.length+1];
			msgBytes[0] = 0x02;
			System.arraycopy(msgData, 0, msgBytes, 1, msgData.length);
			this.msgBytes = msgBytes;
		}
		else if (msgType.equals(MessageType.SUBSCRIPTION)) {
			this.msgType = MessageType.SUBSCRIPTION;
			byte[] msgBytes = new byte[msgData.length+1];
			msgBytes[0] = 0x03;
			System.arraycopy(msgData, 0, msgBytes, 1, msgData.length);
			this.msgBytes = msgBytes;
		}
		else{
    		throw new IllegalArgumentException("Wrong Message format");
    	}
	}
	
	public byte[] getData() {
		return this.msgData;
	}
	
	public byte[] getBytes() {
		return this.msgBytes;
	}
	
	public MessageType getMessageType() {
		return this.msgType;
	}
	
	
}
