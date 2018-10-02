package message;

public interface RegistrationMessageInterface {
	public enum MESSAGETYPE{
		REGISTRATION_REQUEST,
		REGISTRATION_SUCCESS, 
		REGISTRATION_FAILED,
		REGISTRATION_ERROR;

	}
	public static final byte SEPARATE = (byte) 0x13;
	public static final byte END = (byte) 0x14;
	
	
	
}
