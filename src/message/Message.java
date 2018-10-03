package message;

import java.util.ArrayList;
import java.util.List;

public class Message {

	public enum MessageType {
		REGISTER, 
		LOGIN
	}
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

}
