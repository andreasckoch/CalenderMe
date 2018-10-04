package server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import app_kvServer.KVServer;
import app_kvServer.Logger;
import common.HostRepresentation;
import common.Metadata;
import common.Range;
import logger.Constants;
import server.replication.Coordinator;
import server.replication.ReconciliationManager;
import server.replication.ReplicationManager;
import subscription.SubscriptionService;

public class CalenderServer extends Thread{
	public static DataCache dataCache;

	private String responsibiltyKey;
	private int port;
	private int cacheSize;
	private String strategy;

	private Metadata metadata;
	private ServerSocket serverSocket;
	private boolean running; // KVServer thread runs

	private boolean service_running; // true: accept KVClient requests
	private boolean write_lock; // true: don't accept PUT-Requests from KVClient

	private Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Coordinator coordinator = null;
	private ReplicationManager repMgr;

	private SubscriptionService subscriptionService;

	/**
	 * Start KV Server at given port
	 *
	 * @param port
	 *            given port for storage server to operate
	 * @param cacheSize
	 *            specifies how many key-value pairs the server is allowed to
	 *            keep in-memory
	 * @param strategy
	 *            specifies the cache replacement strategy in case the cache is
	 *            full and there is a GET- or PUT-request on a key that is
	 *            currently not contained in the cache. Options are "FIFO",
	 *            "LRU", and "LFU".
	 */

	public KVServer(int port) {
		service_running = false;
		write_lock = false;

		this.port = port;
		running = initSocket();
		this.start();
		logger.info("Server instance started");
	}

	/**
	 * Initializes and starts the server. Loops until the server should be
	 * closed.
	 */
	public void run() {
		logger.debug("KVSERVER Thread " + Thread.currentThread().getId() + " started");
		if (serverSocket != null) {
			while (running) {
				try {
					Socket client = serverSocket.accept();
					ServerConnection connection = new ServerConnection(client, this);
					new Thread(connection).start();
					logger.info(
							"Connected to " + client.getInetAddress().getHostName() + " on port " + client.getPort());
				} catch (IOException e) {
					logger.error("Error! " + "Unable to establish connection.");
					stopServer();
					logger.info("Server stopped.");
				}
			}
		}
		System.out.println("Number of active threads from the given thread: " + Thread.activeCount());
		logger.debug("KVSERVER Thread " + Thread.currentThread().getId() + " stopped");
		System.exit(0);
	}

	/**
	 * Main entry point for the echo server application.
	 * 
	 * @param args
	 *            contains the port number at args[0].
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 1) {
				System.out.println("Error! Invalid number of arguments!");
				System.out.println("Usage: (int) <port>, (int) <cacheSize>, (String) <strategy>!");
			} else if (args.length == 1) {
				int port = Integer.parseInt(args[0]);
				new KVServer(port);
			}
		} catch (NumberFormatException nfe) {
			System.out.println("Error! Invalid argument ! Not a number!");
			System.out.println("Usage: (int) <port>, (int) <cacheSize>, (String) <strategy>!");
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
	 * initKVServer gets started by the ECS Client through a KVAdminMessage,
	 * then sets all required parameters, creates a file linked to the
	 * associated server instance and sets service_running on true The server
	 * accepts KVClient requests now.
	 * 
	 * @param metadata
	 * @param cacheSize
	 * @param displacementStrategy
	 * @param key
	 */
	public void initKVServer(Metadata metadata, int cachesize, String displacementStrategy, String key) {
		this.cacheSize = cachesize;
		this.strategy = displacementStrategy;
		this.metadata = metadata;
		this.responsibiltyKey = key;

		// initialize coordinator
		this.coordinator = new Coordinator(this);

		// initialize replication manager
		repMgr = new ReplicationManager(this);

		// initialize SubscriptionService
		subscriptionService = new SubscriptionService();

		// set caching strategy
		if (strategy.equals(DataCache.CacheType.FIFO.toString())) {
			dataCache = new FIFOCache(DataCache.CacheType.FIFO, cacheSize);
		} else if (strategy.equals(DataCache.CacheType.LRU.toString())) {
			dataCache = new LRUCache(DataCache.CacheType.LRU, cacheSize);
		} else if (strategy.equals(DataCache.CacheType.LFU.toString())) {
			dataCache = new LFUCache(DataCache.CacheType.LFU, cacheSize);
		}

		service_running = true;
		logger.debug("Service successfully initialized");
	}

	/**
	 * Stops the server insofar that it won't listen at the given port any more.
	 */
	public void stopServer() {
		running = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.error("Error! " + "Unable to close socket on port: " + port, e);
		}
	}

	public void startService() {
		logger.debug("Accept Client Messages");
		setService_running(true);
	}

	public void stopService() {
		logger.debug("Stop Service - Reject Client Messages");
		setService_running(false);

		/*
		 * Reset Replication Manager because the server is not allowed to save
		 * replication data anymore.
		 */
		repMgr = null;
	}

	public boolean isService_running() {
		return service_running;
	}

	public void setService_running(boolean sr) {
		this.service_running = sr;
	}

	public void shutDown() {
		running = false;

		coordinator.deleteFile();

		try {
			serverSocket.close();

		} catch (IOException e) {
			logger.error("Error! " + "Unable to close socket on port: " + port);
		}
		logger.debug("ShutDown");
	}

	public void lockWrite() {
		this.write_lock = true;
		logger.debug("lockwrite activated");
	}

	public void unlockWrite() {
		this.write_lock = false;
		logger.debug("lockwrite deactivated");
	}

	public boolean isWrite_locked() {
		return write_lock;
	}

	public void setWrite_locked(boolean wl) {
		this.write_lock = wl;
	}

	public void update(Metadata metadata) {
		HostRepresentation replicationServer1Old = this.getMetadata().getSuccessor(this.getResponsibiltyKey());
		HostRepresentation replicationServer2Old = this.getMetadata().getSuccessor(replicationServer1Old.toHash());
		this.setMetadata(metadata);
		/*
		 * updates reconciliation server if the replication servers changed due
		 * to the change in the topology
		 */
		ReconciliationManager recMgr = new ReconciliationManager(this);
		recMgr.updateReplicationServers(replicationServer1Old, replicationServer2Old);
	}

	public String getResponsibiltyKey() {
		return responsibiltyKey;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public void moveData(Range range, HostRepresentation server) {
		coordinator.moveData(range, server);
	}

	public int getPort() {
		return this.port;
	}

	public Coordinator getCoordinator() {
		return coordinator;
	}

	public ReplicationManager getRepMgr() {
		return repMgr;
	}

	public SubscriptionService getSubscriptionService() {
		return subscriptionService;
	}
}
