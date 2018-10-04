package ecs;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.CommunicationModule;
import common.Metadata;
import common.HostRepresentation;
import common.messages.KVAdminMessage;
import common.messages.KVAdminMessageInterface;
import common.Range;
import logger.Constants;

public class Ecs {

	private static final Logger logger = LogManager.getLogger(Constants.ECS_NAME);

	// List of not initialized server
	private List<HostRepresentation> serverList = new ArrayList<HostRepresentation>();
	// List of initialized server
	private List<HostRepresentation> ServerRunning = new ArrayList<HostRepresentation>();
	
	//standard cacheSize and strategy
	private int cacheSize;
	private String strategy;

	private Metadata metadata;
	private boolean init = false;

	public Ecs(String config) {
		parseConfig(config + ".config");
	}

	/**
	 * initialize service
	 * 
	 * @param numberOfNodes
	 * @param CacheSize
	 * @param displacementStrategy
	 * @throws InterruptedException
	 */
	public boolean initService(int numberOfNodes, int cacheSize, String strategy) throws InterruptedException {
		//set standard cacheSize and strategy
		this.cacheSize = cacheSize;
		this.strategy = strategy;

		/* Init check to see if we already initializied the ecs */
		if (init == true) {
			logger.error("Already initializied");
			return false;
		}

		metadata = new Metadata();

		/* catch if the user chooses to many NumberOfNodes */
		if (numberOfNodes > serverList.size()) {
			numberOfNodes = serverList.size();
			logger.info("Not enough number of Nodes. Set Number of Nodes to the max value: " + serverList.size());
		} else {
			logger.info("Set Number of Nodes to: " + numberOfNodes);
		}

		/* Pick random elements from the serverList */
		ServerRunning = pickRandomElements(serverList, numberOfNodes);

		/* remove running servers from serverList */
		for (HostRepresentation i : ServerRunning) {
			serverList.remove(i);
		}

		for (HostRepresentation i : ServerRunning) {

			String path = System.getProperty("user.dir");

			String script = "ssh -n " + i.getAddress() + " nohup java -jar " + path + "/ms4-server.jar " + i.getPort()+ " &";
			System.out.println(script);

			Runtime run = Runtime.getRuntime();

			try {
				run.exec(script);
				metadata.add(i);
				logger.info("Launched the SSH Processes for server " + i.toString());
			} catch (IOException e) {
				logger.error("Unable to inizalize server " + i.getAddress() + ":" + i.getPort());
			}
		}

		logger.debug("5 second wait to make sure all server are ready");
		Thread.sleep(5000);

		/* Initializing servers */
		for (HostRepresentation i : ServerRunning) {

			ECSCommunication communication = new ECSCommunication(i.getAddress(), i.getPort());

			KVAdminMessageInterface msgFromServer = null;
			try {
				msgFromServer = communication.initKVServer(metadata, cacheSize, strategy, i.toHash());
			} catch (Exception e) {
				System.out.println("Error! Server could not be initialized.");
				logger.error("Server could not be initialized.");
			}
			if (msgFromServer == null) {
				logger.error("No server response");
			} else {
				if (msgFromServer.getMethodType().equals(KVAdminMessage.MethodType.SUCCESS)) {
					logger.debug("Successfully initialized");
				} else {
					logger.error("Could not initialize server");
				}
				communication.disconnect();
				logger.debug("Disconnect");
			}

		}
		init = true;

		/* start heartbeat signal */

		HeartBeatThread heartbeat = new HeartBeatThread(this, ServerRunning);
		new Thread(heartbeat).start();
		logger.debug("Heartbeat started");
		
		return true;
	}

	/**
	 * starts storage service
	 */
	public void start() {
		for (HostRepresentation i : ServerRunning) {
			ECSCommunication communication = new ECSCommunication(i.getAddress(), i.getPort());
			try {
				KVAdminMessageInterface msgFromServer = communication.start();
				logger.debug(msgFromServer.getMethodType());
				logger.info("Started: " + i.getAddress() + ":" + i.getPort());
				communication.disconnect();
			} catch (Exception e) {
				logger.error("Unable to start: " + i.getAddress() + ":" + i.getPort());
				e.printStackTrace();
			}
		}
	}

	/**
	 * stops storage service
	 */
	public void stop() {
		for (HostRepresentation i : ServerRunning) {
			ECSCommunication communication = new ECSCommunication(i.getAddress(), i.getPort());
			try {
				KVAdminMessageInterface msgFromServer = communication.stop();
				logger.debug(msgFromServer.getMethodType());
				logger.info("Stopped: " + i.getAddress() + ":" + i.getPort());
				communication.disconnect();
			} catch (Exception e) {
				logger.error("Unable to stop: " + i.getAddress() + ":" + i.getPort());
				e.printStackTrace();
			}
		}
	}

	/**
	 * stops storage service and tears down all servers
	 */
	public void shutDown() {

		for (int i = 0; i < ServerRunning.size(); i++) {
			HostRepresentation server = ServerRunning.get(i);
			ECSCommunication communication = new ECSCommunication(server.getAddress(), server.getPort());
			try {
				KVAdminMessageInterface msgFromServer = communication.shutDown();
				serverList.add(ServerRunning.get(i));
				logger.debug("ShutDown: " + msgFromServer.getMethodType() + " from Server " + server.getAddress() + ":"
						+ server.getPort());
				communication.disconnect();
			} catch (Exception e) {
				logger.error("Unable to shutDown: " + server.getAddress() + ":" + server.getPort());
				e.printStackTrace();
			}
		}
		ServerRunning.clear();
		init = false;
	}

	/**
	 * Update metadata for all initialized server
	 * 
	 * @throws Exception
	 *             if unable to send metadata
	 * @throws IOExeption
	 *             if unable to connect to server
	 */
	public void updateServersMetadata() {
		for (HostRepresentation i : ServerRunning) {
			try {
				ECSCommunication communication = new ECSCommunication(i.getAddress(), i.getPort());
				communication.update(metadata);
				communication.disconnect();
				logger.debug("Sent metadata to server: " + i.toString());
			} catch (IOException e) {
				logger.error("Unable to connect to " + i.toString());
			} catch (Exception ex) {
				logger.error("Unable to send metadata to server: " + i.toString());
			}
		}
	}

	/**
	 * Adds a new server node into the current ring structure of servers
	 * 
	 * @param CacheSize
	 *            the cache size of the new node
	 * @param displacementStrategy
	 *            the replacement strategy of the new node
	 */
	public boolean addNode(int cacheSize, String displacementStrategy) {
		HostRepresentation newserver;
		newserver = pickRandomElements(serverList, 1).get(0);
		ServerRunning.add(newserver);
		serverList.remove(newserver);

		HostRepresentation successor = metadata.getResponsibleServer(newserver.toHash());

		metadata.add(newserver);

		try {
			initSingleServer(newserver, cacheSize, displacementStrategy);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {

			/* lock_write predecessor */
			ECSCommunication communicationWithsuccessor;
			communicationWithsuccessor = new ECSCommunication(successor.getAddress(), successor.getPort());
			communicationWithsuccessor.lockWrite();

			String newserverHashValue = newserver.toHash();

			Range range = new Range(metadata.getPredecessor(newserverHashValue).toHash(), newserverHashValue);

			/* Move data */
			KVAdminMessageInterface msgFromServer = communicationWithsuccessor.moveData(range, newserver);

			if (msgFromServer.getMethodType().equals(KVAdminMessageInterface.MethodType.SUCCESS)) {

				updateServersMetadata();

				/* Unlock_Write */
				try {
					communicationWithsuccessor.unlockWrite();
				} catch (Exception e) {
					logger.error("Unable to Unlock_Write " + successor.getAddress() + " ; " + successor.getPort());
					e.printStackTrace();
				}
				return true;
			}

			else {
				logger.error(msgFromServer.getMethodType());
			}
			communicationWithsuccessor.disconnect();

		} catch (IOException e) {
			logger.error("Unable to connect to the server");
			return false;
		} catch (Exception ex) {
			logger.error("Unable to connect to the server");
			return false;
		}
		return false;
	}

	/**
	 * remove Node from ServerRunning, update Metadata for all server and send
	 * successor all data
	 * 
	 * @return boolean if
	 */
	public boolean removeNode() {
		/* check for last server */
		if (ServerRunning.size() == 1) {
			shutDown();
			return true;
		}

		/* Randomly choose a server, that should be stopped */
		HostRepresentation serverToBeStopped;
		try {
			serverToBeStopped = pickRandomElements(ServerRunning, 1).get(0);
			ServerRunning.remove(serverToBeStopped);
			serverList.add(serverToBeStopped);
			logger.info("Removing " + serverToBeStopped.toString());
		} catch (IllegalArgumentException e) {
			logger.error("No available server");
			return false;
		}

		String serverToBeStoppedKey = serverToBeStopped.toHash();
		String serverToBeStoppedPredeccessorKey = metadata.getPredecessor(serverToBeStoppedKey).toHash();

		metadata.remove(serverToBeStopped);

		/* Set up connection to the successorServer */
		HostRepresentation successorServer = metadata.getResponsibleServer(serverToBeStoppedKey);

		try {
			/* Set lockWrite */
			ECSCommunication communication = new ECSCommunication(serverToBeStopped.getAddress(),
					serverToBeStopped.getPort());
			communication.lockWrite();

			/* Send successor metadata */
			ECSCommunication communicationWithSuccessor = new ECSCommunication(successorServer.getAddress(),
					successorServer.getPort());
			KVAdminMessageInterface responseFromSuccessor = communicationWithSuccessor.update(metadata);

			if (!responseFromSuccessor.getMethodType().equals(KVAdminMessageInterface.MethodType.SUCCESS)) {
				logger.error("Successor server did not receive metadata");
			}

			communicationWithSuccessor.disconnect();

			/* send data to successor */
			Range range = new Range(serverToBeStoppedPredeccessorKey, serverToBeStoppedKey);
			KVAdminMessageInterface msgFromServer = communication.moveData(range, successorServer);

			if (msgFromServer.getMethodType().equals(KVAdminMessageInterface.MethodType.SUCCESS)) {
				updateServersMetadata();
				communication.shutDown();
				communication.disconnect();
				return true;
			}
			communication.disconnect();
			return false;
		} catch (IOException e) {
			logger.error("Unable to removeNode");
			return false;
		} catch (Exception e) {
			logger.error("Unable to send message to server");
			return false;
		}
	}

	/**
	 * reads in the the configuration file and adds it to the serverList
	 * 
	 * @param filename
	 *            of the file
	 */
	public void parseConfig(String filename) {
		try {
			File file = new File(filename);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			String servername = null;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				servername = tokens[0];
				HostRepresentation temp = new HostRepresentation(tokens[1], Integer.parseInt(tokens[2]));
				serverList.add(temp);
				logger.debug("added " + servername + " to ServerList");
			}
			logger.info("Added " + serverList.size() + " server to serverList");
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * picks a random element in the list
	 * 
	 * @param array
	 *            of elements to pick
	 * @param n,
	 *            number of elements to pick
	 * @return an Array with n elements chosen randomly from the first array
	 */

	public static List<HostRepresentation> pickRandomElements(List<HostRepresentation> array, int n) {
		if (array.size() <= 0) {
			throw new IllegalArgumentException();
		}
		List<HostRepresentation> list = new ArrayList<HostRepresentation>(array.size());

		for (HostRepresentation i : array) {
			list.add(i);
		}

		List<HostRepresentation> answer = new ArrayList<HostRepresentation>(n);
		for (int i = 0; i < n; i++) {
			answer.add(list.get(i));
		}
		return answer;
	}

	public void initSingleServer(HostRepresentation serverrep, int cacheSize, String strategy)
			throws InterruptedException {

		String script = "ssh -n " + serverrep.getAddress() + " nohup java -jar " + "/ms4-server.jar "
				+ serverrep.getPort() + " &";

		Runtime run = Runtime.getRuntime();

		try {
			run.exec(script);
			////////////
			CommunicationModule comm = new CommunicationModule(serverrep.getAddress(), serverrep.getPort());
			comm.createSocket();
			comm.closeSocket();
			logger.error("Starting server successful");
			/////////////
			metadata.add(serverrep);
			logger.info("Launched the SSH Processes for server " + serverrep.toString());
		} catch (IOException e) {
			metadata.remove(serverrep);
			logger.error("Unable to inizalize server " + serverrep.getAddress() + ":" + serverrep.getPort());
		}

		logger.debug("3 second wait to make sure the server is ready");
		Thread.sleep(3000);

		ECSCommunication communication = new ECSCommunication(serverrep.getAddress(), serverrep.getPort());

		KVAdminMessageInterface msgFromServer = null;
		try {
			msgFromServer = communication.initKVServer(metadata, cacheSize, strategy, serverrep.toHash());

			if (msgFromServer.getMethodType().equals(KVAdminMessageInterface.MethodType.SUCCESS)) {
				logger.info("Server succesfully initiated");
				KVAdminMessageInterface start = communication.start();

				if (start.getMethodType().equals(KVAdminMessageInterface.MethodType.SUCCESS)) {
					logger.debug("Server succesfully started");
				} else {
					logger.error("Cannot start server");
				}
			} else {
				logger.error("Cannot initiate server with metadata");
			}
			communication.disconnect();
		} catch (Exception e) {
			logger.error("Unable to connect to " + serverrep.toString());
		}
	}

	/**
	 * This method causes a MOVE_REPLICA_DATA message sent to the successor of a
	 * crashed server.
	 * 
	 * @param serverRep
	 */

	public void updateReplicaServer(HostRepresentation serverRep) {

		/* lookup successor */
		HostRepresentation successor = metadata.getResponsibleServer(serverRep.toHash());

		// set up communication with the crashed servers successor
		ECSCommunication communicate = new ECSCommunication(successor.getAddress(), successor.getPort());

		/*
		 * Send command to take over the responsibility range of its first
		 * replica including the updated metadata.
		 */
		KVAdminMessageInterface moveReplicaDataSuccess;
		try {
			moveReplicaDataSuccess = communicate.moveReplicaData(metadata);
			if (moveReplicaDataSuccess.getMethodType().equals(KVAdminMessageInterface.MethodType.SUCCESS)) {
				logger.debug("Moving Replica Data successful");
			} else {
				logger.error("Moving Replica Data not successful");
			}
		} catch (IOException e) {
			logger.error("Error while sending MOVE_REPLICA_DATA message");
		}

		communicate.disconnect();
	}

	public List<HostRepresentation> getServerRunning() {
		return ServerRunning;
	}

	public void setServerRunning(List<HostRepresentation> servR) {
		this.ServerRunning = servR;
	}

	public List<HostRepresentation> getServerList() {
		return serverList;
	}

	public void setServerList(List<HostRepresentation> servL) {
		this.serverList = servL;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metaD) {
		this.metadata = metaD;
	}
	
	public int getCacheSize() {
		return this.cacheSize;
	}
	
	public String getStrategy() {
		return this.strategy;
	}

}
