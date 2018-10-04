package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class CommunicationModule {

	private Socket clientSocket;
	private InputStream input;
	private OutputStream output;
	
	private String ip;
	private int port;
	private boolean isConnection;
	public HostRepresentation server = new HostRepresentation(ip, port);
	
	public CommunicationModule(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		output = clientSocket.getOutputStream();
		input = clientSocket.getInputStream();
		isConnection=true;
	}

	public CommunicationModule(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	/**
	 * Method sends a message as byte array over the socket
	 * 
	 * @param message Message as Byte-Array that has to be sent
	 */
	
	public void send(byte[] message) {
		try {
			output.write(message);
			output.flush();
		} catch (IOException io) {
			System.out.println("Send not successful");
		}
	}
	
	/**
	 * Method waits for a Message from the Client one the socket and converts the input stream
	 * to a byte array
	 * 
	 * @return received message as Byte-Array
	 * @throws Exception
	 */
	
	public byte[] receive() throws IOException {
		int a = input.read();
		if(a == -1) {
			return null;
		}
		char b = (char) a;
		String msg = "" + b;
		while (b != 0x0D) {
			b = (char) input.read();
			if (b != 0x0D) {
				msg = msg + b;
			}
		}
		return msg.getBytes();
	}
	
	public void createSocket() throws UnknownHostException, IOException {
		this.clientSocket = new Socket(ip, port);
		output = clientSocket.getOutputStream();
		input = clientSocket.getInputStream();
		isConnection=true;
	}
	
	public void closeSocket() throws IOException {
		if (clientSocket != null) {
			input.close();
			output.close();
			clientSocket.close();
			clientSocket = null;
			isConnection=false;
		}
	}

	public boolean isConnection() {
		return isConnection;
	}

	public void setConnection(boolean isConnection) {
		this.isConnection = isConnection;
	}
	
}
