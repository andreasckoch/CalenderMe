package message;

public interface MessageInterface {

	public enum MESSAGETYPE {
		REGISTRATION_REQUEST,
		OPERATION_SUCCESS, 
		OPERATION_FAILED,
		REGISTRATION_ERROR,
		REGISTRATION_DELETE_REQUEST,
		LOGIN_REQUEST,
		LOGIN_ERROR,
		ERROR
	}
	
	public byte[] getMsgBytes();
}
