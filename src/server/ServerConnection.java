package server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.*;

import logger.Constants;

public class ServerConnection extends Thread {

	private int port;
	private ServerSocket serverSocket;
	private boolean running; // CalenderServer thread runs

	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	/**
	 * Start CalenderServer at given port
	 *
	 * @param port: given port for storage server to operate
	 */

	public ServerConnection(int port) {
		this.port = port;
		running = initSocket();
		// start run method
		this.start();
		logger.info("Server instance started");
	}

	/**
	 * Initializes and starts the server. Loops until the server should be closed and creates a new thread for each client.
	 */
	public void run() {
		logger.debug("CalenderServer Thread " + Thread.currentThread().getId() + " started");
		if (serverSocket != null) {
			while (running) {
				try {
					Socket client = serverSocket.accept();
					CalenderServerThread connection = new CalenderServerThread(client);
					new Thread(connection).start();
					logger.info(
							"Connected to " + client.getInetAddress().getHostName() + " on port " + client.getPort());
				} catch (IOException e) {
					logger.error("Error! " + "Unable to establish connection.");
					shutDown();
					logger.info("Server stopped.");
				}
			}
		}
		logger.debug("CalenderServer Thread " + Thread.currentThread().getId() + " stopped");
		System.exit(0);
	}

	/**
	 * Main entry point for the echo server application.
	 * 
	 * @param args contains the port number at args[0].
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 1) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: (int) <port>");
			} else if (args.length == 1) {
				int port = Integer.parseInt(args[0]);
				new ServerConnection(port);
			}
		} catch (NumberFormatException nfe) {
			System.out.println("Error! Invalid argument ! Not a number!");
			System.out.println("Usage: (int) <port>");
			System.exit(1);
		}
	}

	/**
	 * initSocket initializes a Server Socket that listens on the given port.
	 * 
	 * @return true, if the Server Socket found a Socket on the given port and
	 *         connected to it
	 */
	private boolean initSocket() {
		logger.info("Initialize server ...");
		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server listening on port: " + serverSocket.getLocalPort());
			return true;

		} catch (IOException e) {
			logger.error("Error! Cannot open server socket:");
			if (e instanceof BindException) {
				logger.error("Port " + port + " is already bound!");
			}
			return false;
		}
	}

	/**
	 * Stops the server in so far as it won't listen at the given port any more.
	 */
	public void shutDown() {
		running = false;

		try {
			serverSocket.close();

		} catch (IOException e) {
			logger.error("Error! " + "Unable to close socket on port: " + port);
		}
		logger.debug("ShutDown");
	}

	public int getPort() {
		return this.port;
	}
}