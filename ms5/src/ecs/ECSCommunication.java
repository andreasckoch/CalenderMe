package ecs;

import java.io.IOException;
import java.net.UnknownHostException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import common.messages.KVAdminMessage;
import common.messages.KVAdminMessageInterface;
import common.messages.Message;
import logger.Constants;
import common.CommunicationModule;
import common.Metadata;
import common.Range;
import common.HostRepresentation;

public class ECSCommunication {

	private static final Logger logger = LogManager.getLogger(Constants.ECS_NAME);
	public static boolean isRunning = false;
	private CommunicationModule module;

	public ECSCommunication(String server, int port) {
		try {
			module = new CommunicationModule(server, port);
			module.createSocket();
			logger.debug("Connected to " + server + ":" + port);
		} catch (UnknownHostException e) {
			logger.error("Could not find the server");
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Could not Connect");
		}
	}

	/**
	 * sends a message to the server with the command to initialize. Gets a
	 * KVAdminMessage in return
	 * 
	 * @param key
	 * @param metadata
	 * @return response
	 * @throws Exception
	 */

	public KVAdminMessageInterface initKVServer(Metadata metadata, int cacheSize, String strategy, String key)
			throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.INIT_SERVICE,
				metadata, cacheSize, strategy, key);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * command to start the storage service
	 * 
	 * @return response
	 * @throws Exception
	 */
	public KVAdminMessageInterface start() throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.START);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * command to stop the storage service
	 * 
	 * @return response
	 * @throws IOException
	 */
	public KVAdminMessageInterface stop() throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.STOP);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * command to shut down the server
	 * 
	 * @return response
	 * @throws IOException
	 */

	public KVAdminMessageInterface shutDown() throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.SHUTDOWN);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * command to give lock_Write
	 * 
	 * @return response
	 * @throws IOException
	 */

	public KVAdminMessageInterface lockWrite() throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.LOCK_WRITE);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * command to unlock_Write
	 * 
	 * @return response
	 * @throws IOException
	 */
	public KVAdminMessageInterface unlockWrite() throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.UNLOCK_WRITE);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * command to move Data to another server
	 * 
	 * @param range
	 * @param server
	 * @return response
	 * @throws IOException
	 */
	public KVAdminMessageInterface moveData(Range range, HostRepresentation server) throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.MOVE_DATA, range,
				server);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * Command to update Metadata
	 * 
	 * @param metadata
	 * @return response
	 * @throws IOException
	 */
	public KVAdminMessageInterface update(Metadata metadata) throws Exception {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.UPDATE, metadata);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * send check message to the server
	 * 
	 * @return success if the server answers
	 */
	public KVAdminMessageInterface heartbeat() {
		try {
			KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.HEARTBEAT);
			module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
			byte[] bytesFromServer = module.receive();
			KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
			return msgFromServer;
		} catch (IOException e) {
			logger.error("Server crashed");
			return null;
		}
	}

	/**
	 * command to remove replica data to his predecessor
	 * 
	 * @param metadata
	 *            up to date metadata
	 * @return SUCCESS or ERROR Message
	 * @throws IOException
	 */
	public KVAdminMessageInterface moveReplicaData(Metadata metadata) throws IOException {
		KVAdminMessageInterface msgToServer = new KVAdminMessage(KVAdminMessageInterface.MethodType.MOVE_REPLICA_DATA, metadata);
		module.send(new Message(Message.MessageType.ADMIN, msgToServer.getBytes()).getBytes());
		byte[] bytesFromServer = module.receive();
		KVAdminMessageInterface msgFromServer = new KVAdminMessage(bytesFromServer);
		return msgFromServer;
	}

	/**
	 * Tears down connection
	 */
	public void disconnect() {
		try {
			if (module.isConnection()) {
				module.closeSocket();
				isRunning = false;
			}
		} catch (Exception e) {
			System.out.println("Disconnect not successful!");
		}
	}

}
