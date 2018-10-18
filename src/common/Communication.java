package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.logging.log4j.*;

import logger.Constants;
import message.Message;


/* Does the communication between application and server*/
public class Communication {

	private int port;
	private String ip;

	private Socket clientSocket;
	private InputStream input;
	private OutputStream output;
	private boolean isConnection;
	private static final Logger logger = LogManager.getLogger(Constants.COMMUNICATION_NAME);


	public Communication(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public Communication(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		output = clientSocket.getOutputStream();
		input = clientSocket.getInputStream();
		logger.info("Socket was created.");		
		isConnection=true;
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
			logger.info("Message was sent.");
			
		} catch (IOException io) {
			logger.error("Error while sending over socket.", io);
		}
	}

	/**
	 * Method waits for a Message from the Client one the socket and converts the
	 * input stream to a byte array
	 * 
	 * @return received message as Byte-Array
	 * @throws Exception
	 */

	public byte[] receive() throws IOException {
		int nextByte = input.read();
		if (nextByte == -1) {
			return null;
		}
		char nextChar = (char) nextByte;
		String msg = "" + nextChar;
		
		// check last read character for END
		while (nextChar != Message.END) {
			
			// read next char from byte stream
			nextChar = (char) input.read();
			
			if (nextChar != Message.END) {
				msg = msg + nextChar;
			}
		}
		logger.info("Received message.");
		return msg.getBytes();
	}

	public void createSocket() throws UnknownHostException, IOException {
		this.clientSocket = new Socket(ip, port);
		output = clientSocket.getOutputStream();
		input = clientSocket.getInputStream();
		logger.info("Socket was created.");			
		isConnection = true;
	}

	public void closeSocket() throws IOException {
		if (clientSocket != null) {
			input.close();
			output.close();
			clientSocket.close();
			clientSocket = null;
			logger.info("Socket was closed.");
			isConnection = false;
		}
	}

	public boolean isConnection() {
		return isConnection;
	}

	public void setConnection(boolean isConnection) {
		this.isConnection = isConnection;
	}

}
