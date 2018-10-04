package client;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.CommunicationModule;
import common.Hashing;
import common.Metadata;
import common.HostRepresentation;
import logger.Constants;

public class ClientConnection implements KVCommInterface{
	private Metadata metadata = null;
	private CommunicationModule communicationModule;
	public String address;
	public int port;
	
	private static final Logger logger = LogManager.getLogger(Constants.APP_NAME);
	
	public ClientConnection(String address, int port) {
		this.address = address;
		this.port = port;
		this.communicationModule = new CommunicationModule(address, port);
	}
	
	@Override
	public void connect() throws UnknownHostException, IOException {
		communicationModule.createSocket();
	}

	/**
	 * Tears down connection
	 */
	@Override
	public void disconnect() {
		try {
			communicationModule.closeSocket();
		} catch (Exception e) {
			System.out.println("Disconnect not successful!");
		}
	}
	
	/**
	 * checks Metadata for responsible server for the given key
	 * and reconnects if the client is connected to the wrong server
	 * 
	 * @param key
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connectResponsibleServer(String key) throws UnknownHostException, IOException {
		if (metadata != null) {
			Hashing.hashIt(key);
			HostRepresentation serverRep = metadata.getResponsibleServer(Hashing.hashIt(key));
			if (!this.address.equals(serverRep.getAddress()) || this.port != serverRep.getPort()) {
				disconnect();
				address = serverRep.getAddress();
				port = serverRep.getPort();
				communicationModule = new CommunicationModule(this.address, this.port);
				communicationModule.createSocket();
				logger.debug("Lookup in Metadata because connected Server is not responsible");
			}
		}
	}
	
	public void setMetadata(Metadata metadata){
		this.metadata = metadata;
	}
	
	public Metadata getMetadata() {
		return metadata;
	}
	
	public String getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public CommunicationModule getCommunicationModule() {
		return communicationModule;
	}
	
}
