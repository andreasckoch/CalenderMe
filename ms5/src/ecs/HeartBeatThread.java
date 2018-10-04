package ecs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.HostRepresentation;
import common.messages.KVAdminMessageInterface;
import logger.Constants;

/**
 * The Heartbeat thread is initialized when initService was called the first
 * time in the ECS. From then on every 30 seconds a heartbeat message is sent to
 * each registered and running server node. If one server does not reply
 * anymore, it is removed from the ECS' metadata and serverRunning list. The not
 * responding server is also added into the serverList again.
 *
 */

public class HeartBeatThread implements Runnable {

	private Ecs ecs;
	private List<HostRepresentation> ServerRunning;

	private static final Logger logger = LogManager.getLogger(Constants.ECS_NAME);

	public HeartBeatThread(Ecs ecs, List<HostRepresentation> ServerRunning) {
		this.ServerRunning = new ArrayList<HostRepresentation>(ServerRunning);
		this.ecs = ecs;
	}

	@Override
	public void run() {
		while (true) {
			heartbeat();
			try {
				/* wait 30 secs for next heartbeat signal */
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void heartbeat() {
		KVAdminMessageInterface msgFromServer;
		for (HostRepresentation i : ServerRunning) {
			try {
				ECSCommunication communication = new ECSCommunication(i.getAddress(), i.getPort());
				msgFromServer = communication.heartbeat();
				communication.disconnect();
			} catch (Exception e) {
				logger.debug("HEARTBEAT: NO MESSAGE FROM SERVER " + i.getAddress() + ":" + i.getPort());
				msgFromServer = null;
			}
			try {
				
				//if there is a not responding server
				if (msgFromServer == null) {
					logger.debug("Remove Server from running server list...");
					/* Remove current server from ServerRunning list */
					ecs.getServerRunning().remove(i);

					logger.debug("Remove Server from metadata...");
					ecs.getServerList().add(i);

					/* update metadata */
					ecs.getMetadata().remove(i);

					/*
					 * update the metadata of the two successors of the new
					 * responsible server, so its sent replication messages can
					 * be interpreted correctly
					 */
					try {
						//read successors from metadata
						HostRepresentation r1= ecs.getMetadata().getSuccessor(i.toHash());
						HostRepresentation r2= ecs.getMetadata().getSuccessor(r1.toHash());
						//send individual updates on metadata to the successors
						ECSCommunication communication1 = new ECSCommunication(r1.getAddress(), r1.getPort());
						communication1.update(ecs.getMetadata());
						communication1.disconnect();
						ECSCommunication communication2 = new ECSCommunication(r2.getAddress(), r2.getPort());
						communication2.update(ecs.getMetadata());
						communication2.disconnect();
						logger.debug("Sent metadata to server: " + i.toString());
						//wait 0,1 seconds for the successors to execute change
						this.wait(100);
					} catch (IOException e) {
						logger.error("Unable to connect to " + i.toString());
					} catch (Exception ex) {
						logger.error("Unable to send metadata to server: " + i.toString());
					}
					
					/* update successor */
					ecs.updateReplicaServer(i);
					
					logger.debug("update metadata on all servers...");
					ecs.updateServersMetadata();
					
					//add a new server node with standard cacheSize and strategy to replace the crashed server
					ecs.addNode(ecs.getCacheSize(), ecs.getStrategy());
				}
				logger.debug("Sent heartbeat request to server: " + i.toString());
			} catch (Exception ex) {
				logger.error("ERROR: Unable to send heartbeat request to server: " + i.toString());
			}
		}

		

		this.ServerRunning = new ArrayList<HostRepresentation>(ecs.getServerRunning());

		logger.debug("Updated Metadata: " + ecs.getMetadata());

	}

}
