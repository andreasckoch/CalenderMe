package message;

public interface MessageInterface {

	public enum MESSAGETYPE {
		OPERATION_SUCCESS, 
		OPERATION_FAILED,
		REGISTRATION,
		REGISTRATION_DELETE,
		REGISTRATION_MODIFICATION_EMAIL,
		REGISTRATION_MODIFICATION_PW,
		REGISTRATION_ERROR,
		LOGIN,
		LOGIN_ERROR,
		PROFILE_UPDATE_PUBLIC,
		PROFILE_UPDATE_PRIVATE,
		PROFILE_UPDATE_ERROR,
		ERROR
	}
	
	public byte[] getMsgBytes();
}
