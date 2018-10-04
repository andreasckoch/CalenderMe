package subscription;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import logger.Constants;

/**
 * Thread that is parallelly running to the KVClient-Thread. Accepts serverconnections
 * and hand them over to the SubscriptionHandler, similar to the KVServer
 * and its ServerConnection class.
 *
 */
public class SubMsgAcceptThread implements Runnable {

	private ServerSocket serverSocket;

	private static final Logger logger = LogManager.getLogger(Constants.APP_NAME);

	public SubMsgAcceptThread(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Socket subscriptionSocket = serverSocket.accept();
				SubscriptionHandler subConnection = new SubscriptionHandler(subscriptionSocket);
				new Thread(subConnection).start();
				logger.info("Subscription service started");
			} catch (IOException e1) {
				logger.debug("No subscription received");
			} catch (NullPointerException npe) {
				logger.debug("No subscription received");
			}
		}
	}
}
