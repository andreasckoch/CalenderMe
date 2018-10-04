package app_kvClient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import logger.Constants;
import subscription.SubMsgAcceptThread;
import subscription.Subscriber;
import client.*;
import common.HostRepresentation;
import common.messages.KVMessageInterface;

public class KVClient {

	private static final Logger logger = LogManager.getLogger(Constants.APP_NAME);

	private static final String PROMPT = "KVStore> ";
	private BufferedReader stdin;
	private ClientConnection clientConnection;
	private KVStore kvstore;
	private Subscriber subscriber;
	private boolean stop = false;
	private boolean isConnected = false;
	private HostRepresentation clientid;
	private ServerSocket serverSocket = null;
	private int port;
	private String address;

	public void run() {

		/*
		 * Create socket to get a free port and get port and adress from the socket
		 */
		try {
			serverSocket = new ServerSocket(0);
			port = serverSocket.getLocalPort();
			address = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Clientaddress: " + address + " : " + port);
		} catch (IOException e1) {
			logger.error("Fatal errror: Socket creation failed");
			e1.printStackTrace();
		}

		// Start service to receive incoming KVSubscriptionMessages
		SubMsgAcceptThread accept = new SubMsgAcceptThread(serverSocket);
		new Thread(accept).start();

		// create clientID for subscribe message
		clientid = new HostRepresentation(address, port);

		while (!stop) {

			stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(PROMPT);

			try {
				String cmdLine = stdin.readLine();
				this.handleCommand(cmdLine);
			} catch (IOException e) {
				stop = true;
				printError("CLI does not respond - Application terminated ");
			} catch (NullPointerException nex) {
				printError("Not connected!");
			}
		}
	}

	private void handleCommand(String cmdLine) {
		String[] tokens = cmdLine.split("\\s+");

		if (tokens[0].equals("quit")) {
			stop = true;

			if (isConnected == true) {
				clientConnection.disconnect();
			}

			System.out.println(PROMPT + "Application exit!");

		} else if (tokens[0].equals("connect")) {
			if (tokens.length == 3) {
				try {
					if (isConnected == false) {
						clientConnection = new ClientConnection(tokens[1], Integer.parseInt(tokens[2]));
						clientConnection.connect();
						kvstore = new KVStore(clientConnection);
						subscriber = new Subscriber(clientConnection, clientid);
						isConnected = true;
						System.out.println(PROMPT + "Connected to " + tokens[1] + ", " + tokens[2]);
					} else {
						printError("Already connected to a server!");
					}
				} catch (NumberFormatException nfe) {
					printError("No valid address. Port must be a number!");
					logger.info("Unable to parse argument <port>", nfe);
					isConnected = false;
				} catch (UnknownHostException e) {
					printError("Unknown Host!");
					logger.info("Unknown Host!", e);
					isConnected = false;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				printError("Invalid number of parameters!");
			}

		} else if (tokens[0].equals("put")) {
			if (tokens.length >= 3) {
				if (clientConnection != null) {
					StringBuilder key = new StringBuilder();
					StringBuilder value = new StringBuilder();
					key.append(tokens[1]);
					for (int i = 2; i < tokens.length; i++) {
						value.append(tokens[i]);
						if (i != tokens.length - 1) {
							value.append(" ");
						}
					}
					try {
						KVMessageInterface reply = kvstore.put(key.toString(), value.toString());
						System.out.println("KVStore> OUTPUT: " + reply.getStatus());
					} catch (Exception e) {

						printConnectionLost();

					}
				} else {
					printError("Not connected!");
				}
			} else if (tokens.length == 2) {
				if (clientConnection != null) {
					StringBuilder key = new StringBuilder();
					key.append(tokens[1]);
					try {
						// return reply of the server
						KVMessageInterface reply = kvstore.put(key.toString(), null);
						System.out.println(
								"KVStore> OUTPUT: " + reply.getStatus() + ", " + reply.getKey() + ", " + reply.getValue());
					} catch (Exception e) {

						printConnectionLost();

					}
				} else {
					printError("Not connected!");
				}
			}

			else {
				printError("No message passed!");
			}

		} else if (tokens[0].equals("get")) {
			if (tokens.length == 2) {
				if (clientConnection != null) {
					StringBuilder key = new StringBuilder();
					key.append(tokens[1]);
					try {
						KVMessageInterface reply = kvstore.get(key.toString());
						System.out.println(
								"KVStore> OUTPUT: " + reply.getStatus() + ", " + reply.getKey() + ", " + reply.getValue());
					} catch (Exception e) {

						printConnectionLost();

					}
				} else {
					printError("Not connected!");
				}
			} else {
				printError("No message passed!");
			}

		} else if (tokens[0].equals("subscribe")) {

			if (tokens.length == 2) {

				if (clientConnection != null) {
					StringBuilder key = new StringBuilder();
					for (int i = 1; i < tokens.length; i++) {
						key.append(tokens[i]);
						if (i != tokens.length - 1) {
							key.append(" ");
						}
					}
					try {
						subscriber.subscribe(key.toString());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					printError("Not Connected");
				}

			} else {
				printError("No key passed!");
			}

		} else if (tokens[0].equals("unsubscribe")) {

			if (tokens.length == 2) {

				if (clientConnection != null) {
					StringBuilder key = new StringBuilder();
					for (int i = 1; i < tokens.length; i++) {
						key.append(tokens[i]);
						if (i != tokens.length - 1) {
							key.append(" ");
						}
					}
					try {
						subscriber.unsubscribe(key.toString());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					printError("Not Connected");
				}

			} else {
				printError("No key passed!");
			}

		} else if (tokens[0].equals("disconnect")) {

			if (isConnected == true) {
				clientConnection.disconnect();
				isConnected = false;
			} else {
				printError("You were not connected to a server!");
			}

		} else if (tokens[0].equals("logLevel")) {

			if (tokens.length == 2) {
				if (!setLevel(tokens[1])) {
					printError("No valid log level!");
					printPossibleLogLevels();
				} else {
					setLevel(tokens[1]);
					System.out.println(PROMPT + "Log level changed to level " + tokens[1]);
				}
			} else {
				printError("Invalid number of parameters!");
			}

		} else if (tokens[0].equals("help")) {
			printHelp();
		} else {
			printError("Unknown command");
			printHelp();
		}
	}

	private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("HELP (Usage):\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("connect <host> <port>");
		sb.append("\t\t establishes a connection to a server\n");
		sb.append(PROMPT).append("put <key> <value>");
		sb.append("\t\t put or update a tuple on the server \n");
		sb.append(PROMPT).append("put <key>");
		sb.append("\t\t\t deletes a tuple from the server \n");
		sb.append(PROMPT).append("get <key>");
		sb.append("\t\t\t retrieves the value of a tuple from the server \n");
		sb.append(PROMPT).append("subscribe <key>");
		sb.append("\t\t subsribes to the given key \n");
		sb.append(PROMPT).append("unsubscribe <key>");
		sb.append("\t\t unsubscribes from the given key \n");
		sb.append(PROMPT).append("disconnect");
		sb.append("\t\t\t disconnects from the server \n");

		sb.append(PROMPT).append("logLevel");
		sb.append("\t\t\t changes the logLevel \n");
		sb.append(PROMPT).append("\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");

		sb.append(PROMPT).append("quit ");
		sb.append("\t\t\t\t exits the program");
		System.out.println(sb.toString());
	}

	private void printPossibleLogLevels() {
		System.out.println(PROMPT + "Possible log levels are:");
		System.out.println(PROMPT + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
	}

	private boolean setLevel(String levelString) {
		String[] allowedParams = { "ALL", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF" };

		if (!Arrays.asList(allowedParams).contains(levelString)) {
			return false;
		}

		Level level = Level.toLevel(levelString);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configurator.setLevel("CloudDB", level);
		ctx.updateLoggers();
		return true;
	}

	private void printError(String error) {
		System.out.println(PROMPT + "Error! " + error);
	}

	/**
	 * prints error and available server, if connection to the server gets lost
	 */
	private void printConnectionLost() {
		printError("Put/Get Error on Client");
		System.out.println("Please reconnect to another server, this one might be down.");
		System.out.println(
				"Here is an overall list of recently active servers (please exclude the one you are currently connected to) :");
		SortedMap<String, HostRepresentation> tempMap = new TreeMap<String, HostRepresentation>(
				clientConnection.getMetadata().getMetadata());
		int counter = tempMap.size();
		for (int i = 0; i < counter; i++) {
			System.out.println(tempMap.get(tempMap.firstKey()));
			tempMap.remove(tempMap.firstKey());
		}
	}

	/**
	 * Main entry point for the Client application
	 *
	 * @param args
	 *            contains the port number at args[0].
	 */
	public static void main(String[] args) {
		try {
			KVClient kvClient = new KVClient();
			kvClient.run();
		} catch (Exception e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}