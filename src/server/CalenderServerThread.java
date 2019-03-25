package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.logging.log4j.*;

import common.Communication;
import common.Constants;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.ClientBasic;

public class CalenderServerThread implements Runnable {

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private boolean isOpen;

	private Socket clientSocket;
	private InputStream input;
	private OutputStream output;
	private MessageDecoder messageDecoder;
	
	public CalenderServerThread(Socket client) {
		this.clientSocket = client;
		this.isOpen = true;
		
	}




	@Override
	public void run() {
		try {

			while (isOpen) {
				try {
					Communication communicationWorker = new Communication(clientSocket);
					Basic message = communicationWorker.serverReceive();
					
					// Retrieval of all requests such as RegisterRequest oder LoginRequest
					if (message != null){
						logger.info("Received Message. Decoding..");
						messageDecoder = new MessageDecoder();
						ClientBasic returnMessage = messageDecoder.processMessage(message);
						communicationWorker.send(returnMessage);
					}
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} finally {
			try {
				if (clientSocket != null) {
					input.close();
					output.close();
					clientSocket.close();
					logger.info("Disconnected from Client on port " + clientSocket.getPort() + " on thread " + Thread.currentThread().getId());
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}	
}
