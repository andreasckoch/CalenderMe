package app_kvEcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import ecs.Ecs;
import logger.Constants;


public class ECSClient {
	
	private static final Logger logger = LogManager.getLogger(Constants.ECS_NAME);
	private static final String PROMPT = "admin> ";
	private BufferedReader stdin;
	private boolean stop = false;
	@SuppressWarnings("unused")
	private boolean isConnected = false;
	private int numberOfNodes;
	private int cacheSize;
	@SuppressWarnings("unused")
	private String displacementStrategy;
	public Ecs ecsclient = new Ecs("config");
	
	public void run() {
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

		if (tokens[0].equals("initService")) {
			if (tokens.length == 4) {
				try {
					numberOfNodes = Integer.parseInt(tokens[1]);
					cacheSize = Integer.parseInt(tokens[2]);
					if (tokens[3].equals("FIFO")||tokens[3].equals("LFU")||tokens[3].equals("LRU")){
						displacementStrategy = tokens[3];
						ecsclient.initService(numberOfNodes, cacheSize, tokens[3]);
						
					}else{
						logger.error("Wrong displacement strategy");
						printHelp();
					}
					
					
				} catch (NumberFormatException nfe) {
					printError("No valid NumberOfNodes or CacheSize");
					logger.info("Unable to parse argument <numberOfNodes> or <cacheSize>", nfe);
					isConnected = false;
				} catch (Exception e) {
					printError("initService Error");
				}
			} else {
				printError("Invalid number of parameters!");
			}

		} else if (tokens[0].equals("start")) {
			if (tokens.length == 1) {
				ecsclient.start();
				
			}else{
				printError("No parameters needed");
			}

		} else if (tokens[0].equals("stop")) {
			if (tokens.length == 1) {
				ecsclient.stop();
				
			}else{
				printError("No parameters needed");
			}
		
		} else if (tokens[0].equals("shutDown")) {
			if (tokens.length == 1) {
				ecsclient.shutDown();
				
			}else{
				printError("No parameters needed");
			}

		} else if (tokens[0].equals("addNode")) {
			if (tokens.length == 3) {
				try {
					cacheSize = Integer.parseInt(tokens[1]);
					ecsclient.addNode(cacheSize, tokens[2]);
				} catch (NumberFormatException nfe) {
					printError("No valid CacheSize");
					logger.info("Unable to parse argument <cacheSize>", nfe);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				printError("Invalid number of parameters!");
			}

		} else if (tokens[0].equals("removeNode")) {
			if (tokens.length == 1) {
				try {
						
					ecsclient.removeNode();
					
				} catch (NumberFormatException nfe) {
					printError("No valid CacheSize");
					logger.info("Unable to parse argument <cacheSize>", nfe);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				printError("Invalid number of parameters!");
			}

		
		} else if (tokens[0].equals("logLevel")) {

			if (tokens.length == 2) {
				if (!setLevel(tokens[1])) {
					printError("No valid log level!");
					printPossibleLogLevels();
				} else {
					System.out.println(PROMPT + "Log level changed to level " + tokens[1]);
					setLevel(tokens[1]);
				}
			} else {
				printError("Invalid number of parameters!");
			}

		} else if (tokens[0].equals("help")) {
			printHelp();
		} else if (tokens[0].equals("quit")) {
			stop = true;
			System.out.println(PROMPT + "Application exit!");
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
		sb.append("::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("initService <numberOfNodes> <cacheSize> <displacementStrategy>");
		sb.append("\t initalizes a number of new Server\n");
		sb.append(PROMPT).append("start");
		sb.append("\t\t\t\t\t\t\t\t starts the storage service \n");
		sb.append(PROMPT).append("stop");
		sb.append("\t\t\t\t\t\t\t\t stops the storage service \n");
		sb.append(PROMPT).append("shutDown");
		sb.append("\t\t\t\t\t\t\t\t Stops all server instances and exits the remote processes \n");
		sb.append(PROMPT).append("addNode <cacheSize> <displacementStrategy>");
		sb.append("\t\t\t\t adds a new Server with cacheSice and FIFO/LRU/LFU \n");
		sb.append(PROMPT).append("removeNode");
		sb.append("\t\t\t\t\t\t\t\t removes a arbitrary server \n");

		sb.append(PROMPT).append("logLevel");
		sb.append("\t\t\t\t\t\t\t\t changes the logLevel \n");
		sb.append(PROMPT).append("\t\t\t\t\t\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");

		sb.append(PROMPT).append("quit ");
		sb.append("\t\t\t\t\t\t\t\t exits the program");
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
		Configurator.setLevel("ecs", level);
		ctx.updateLoggers();
		return true;
	}
	

	private void printError(String error) {
		System.out.println(PROMPT + "Error! " + error);
	}
	
	/**
	 * Main entry point for the Client application
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ECSClient ecs = new ECSClient();
			ecs.run();
		} catch (Exception e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
