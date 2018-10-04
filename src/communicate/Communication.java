package communicate;



/* Does the communication between application and server*/
public class Communication {

	private int port;
	private byte[] msgBytes;
	private String ip;

	public Communication(String ip, int port, byte[] msgBytes ) {
		this.ip=ip;
		this.port = port;
		this.msgBytes=msgBytes;
	}
	
	public boolean send() {
		
		
		
		return true;
	}
	
	
}
