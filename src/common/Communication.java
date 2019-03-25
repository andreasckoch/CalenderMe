package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.logging.log4j.*;

import common.Constants;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.ClientBasic;


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
		isConnection=true;
	}

	/**
	 * To be used by server!
	 * Method sends a message as byte array over the socket
	 * 
	 * @param message Message as Byte-Array that has to be sent
	 */

	public void send(ClientBasic message) {
		try {
			
			message.writeDelimitedTo(output);
			
			output.flush();
			logger.info("Message was sent with message type: {}", message.getType());
			
		} catch (IOException io) {
			logger.error("Error while sending over socket with message type: {}\n{}\n", message.getType(), io);
		}
	}
	
	/**
	 * To be used by client!
	 * @param message
	 */
	public void send(Basic message) {
		try {
			
			message.writeDelimitedTo(output);
			
			output.flush();
			logger.info("Message was sent with message type: {}", message.getType());
			
		} catch (IOException io) {
			logger.error("Error while sending over socket with message type: {}\n{}\n", message.getType(), io);
		}
	}

	/**
	 * To be used by server!
	 * Method waits for a Message from the Client one the socket and converts the
	 * input stream to a byte array
	 * 
	 * @return received message as Byte-Array
	 * @throws Exception
	 */

	public Basic serverReceive() throws IOException {
		
		try {
			return Basic.parseDelimitedFrom(input);			
		} catch (IOException ioe) {
			logger.error("Error while parsing message from input stream!");
			ioe.printStackTrace();
			return null;
		}
	}
	
	/**
	 * To be used by client!
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ClientBasic receive() throws IOException {
			
		try {
			return ClientBasic.parseDelimitedFrom(input);			
		} catch (IOException ioe) {
			logger.error("Error while parsing message from input stream!");
			ioe.printStackTrace();
			return null;
		}
	}

	public void createSocket() throws UnknownHostException, IOException {
		this.clientSocket = new Socket(ip, port);
		output = clientSocket.getOutputStream();
		input = clientSocket.getInputStream();
		logger.info("Socket {} on port {} was created\n", ip, port);
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

}
