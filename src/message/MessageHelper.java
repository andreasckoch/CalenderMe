package message;

import message.MessageInterface.MESSAGETYPE;

public class MessageHelper {

	public static final byte SEPARATE = (byte) 0x13;
	public static final byte END = (byte) 0x14;
	
	public static byte[] getNextValueFromBytes(byte[] msgBytes, int startPosition){
		
		int endPosition=0;
		for (int i = startPosition; i < msgBytes.length; i++) {
			if(msgBytes[i] == SEPARATE || msgBytes[i] == END) {
				endPosition=i-1;
				break;
			}
		}
		
		byte[] value= new byte[endPosition-startPosition +1];
		System.arraycopy(msgBytes, startPosition, value, 0, value.length);
		
		return value;
	}
	
	public static byte[] addNextValueToBytes(byte[] msgBytesTemp, byte[] value, int position) {
		msgBytesTemp[position] = SEPARATE;
		System.arraycopy(value, 0, msgBytesTemp, position + 1, value.length);		
		return msgBytesTemp;
	}
	
	public static byte messageTypeToByte(MESSAGETYPE messageType) {
		byte indexByte;
		switch (messageType) {
		case OPERATION_SUCCESS:
			indexByte = 0x00;
			break;
		case OPERATION_FAILED:
			indexByte = 0x01;
			break;
		case REGISTRATION_REQUEST:
			indexByte = 0x02;
			break;
		case REGISTRATION_DELETE_REQUEST:
			indexByte = 0x03;
			break;
		case REGISTRATION_ERROR:
			indexByte = 0x04;
			break;
		default:
			indexByte = 0x7f;
			break;
		}
		return indexByte;
	}

	public static MESSAGETYPE byteToMessageType(byte msgByte) {
		MESSAGETYPE messageType;
		switch (msgByte) {
		case 0x00:
			messageType = MESSAGETYPE.OPERATION_SUCCESS;
			break;
		case 0x01:
			messageType = MESSAGETYPE.OPERATION_FAILED;
			break;
		case 0x02:
			messageType = MESSAGETYPE.REGISTRATION_REQUEST;
			break;
		case 0x03:
			messageType = MESSAGETYPE.REGISTRATION_DELETE_REQUEST;
			break;
		case 0x04:
			messageType = MESSAGETYPE.REGISTRATION_ERROR;
			break;
		case 0x7f:
			messageType = MESSAGETYPE.ERROR;
			break;
		default:
			messageType = MESSAGETYPE.ERROR;
			break;
		}
		return messageType;
	}

}
