package message;

public interface LoginMessageInterface extends MessageInterface{
	public enum MessageType {
		LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILED
	}
}
