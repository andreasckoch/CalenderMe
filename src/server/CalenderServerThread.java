package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.logging.log4j.*;

import communicate.Communication;
import logger.Constants;
import message.Message;
import message.MessageInterface;

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
			output = clientSocket.getOutputStream();
			input = clientSocket.getInputStream();

			while (isOpen) {
				try {
					Communication communicationWorker = new Communication(clientSocket.getInetAddress().toString(), clientSocket.getPort());
					byte[] msgBytesFromClient = communicationWorker.receive();
					
					// Abfrage der möglichen Befehle wie RegisterRequest oder LoginRequest
					messageDecoder = new MessageDecoder();
					messageDecoder.processMessageType(msgBytesFromClient);
					
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (IOException ioe) {
			logger.error("Error! Connection could not be established!", ioe);
		} finally {
			try {
				if (clientSocket != null) {
					input.close();
					output.close();
					clientSocket.close();
					logger.info("Disconnected from Client on port " + clientSocket.getPort());
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}	
}
