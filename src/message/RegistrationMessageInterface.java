package message;

public interface RegistrationMessageInterface extends MessageInterface{
	public enum MESSAGETYPE{
		REGISTRATION_REQUEST,
		REGISTRATION_SUCCESS, 
		REGISTRATION_FAILED,
		REGISTRATION_ERROR;

	}
	
	
	
	
}
