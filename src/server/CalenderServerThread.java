package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.logging.log4j.*;

import common.Communication;
import logger.Constants;
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
		try {
			output = client.getOutputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			input = client.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	@Override
	public void run() {
		try {

			while (isOpen) {
				try {
					//TODO remove sysout
					System.out.println(clientSocket.getInetAddress().getHostAddress());
					Communication communicationWorker = new Communication(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
					byte[] msgBytesFromClient = communicationWorker.receive();
					
					// Abfrage der möglichen Befehle wie RegisterRequest oder LoginRequest
					messageDecoder = new MessageDecoder();
					MessageInterface returnMessage = messageDecoder.processMessage(msgBytesFromClient);
					communicationWorker.send(returnMessage.getMsgBytes());
					
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

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
