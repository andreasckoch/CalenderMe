package subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.HostRepresentation;
import common.messages.KVSubscriptionMessage;
import common.messages.Message;
import common.messages.KVSubscriptionMessageInterface.StatusType;
import common.messages.Message.MessageType;
import logger.Constants;

/**
 * This class is responsible for handling incoming subscription-requests on the
 * server's side (excluding requests for keys outside the server's range)
 */
public class SubscriptionService {

	private SortedMap<String, List<HostRepresentation>> subscriberMap;
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	public SubscriptionService() {
		this.subscriberMap = new TreeMap<String, List<HostRepresentation>>();
	}

	/**
	 * this method handles the incoming subscription request by adding the
	 * client to the requested key-list
	 * 
	 * @param subscribedKey
	 *            the key to which the client wants to subscribe to
	 * @param client
	 *            the requester
	 * @return success status: true, if successful | false, if unsuccessful
	 */
	public boolean subscribe(String subscribedKey, HostRepresentation client) {
		logger.debug(client.getAddress() + ":" + client.getPort() + " subscribing to " + subscribedKey);
		try {
			/*
			 * if the desired key is already included in the hashTable, it is
			 * added to the respective list; else a new entry in the hashTable
			 * is created, with the client as only entry in the respective list
			 */
			if (subscriberMap.containsKey(subscribedKey)) {
				subscriberMap.get(subscribedKey).add(client);

				printSubscriberList(subscribedKey);

				return true;
			} else {
				List<HostRepresentation> tempList = new ArrayList<HostRepresentation>();

				tempList.add(client);

				subscriberMap.put(subscribedKey, tempList);

				printSubscriberList(subscribedKey);

				return true;
			}

		} catch (Exception e) {
			// in case of an unexpected exception, false is returned
			return false;
		}

	}

	/**
	 * this method handles the incoming unsubscription request by removing the
	 * client from the requested key-list
	 * 
	 * @param subscribedKey
	 *            the key to which the client wants to unsubscribe from
	 * @param client
	 *            the requester
	 * @return success status: true, if successful | false, if unsuccessful
	 */
	public boolean unsubscribe(String subscribedKey, HostRepresentation client) {
		logger.debug(client.getAddress() + ":" + client.getPort() + " unsubscribing from " + subscribedKey);
		try {
			/*
			 * if the desired key is already included in the hashTable, it is
			 * removed from the respective list
			 */
			if (subscriberMap.containsKey(subscribedKey)) {
				subscriberMap.get(subscribedKey).remove(client);
				/*
				 * if the remove client was the last remaining element of the
				 * clientList, the whole corresponding entry of the hashmap is
				 * deleted
				 */
				if (subscriberMap.get(subscribedKey).size() == 0) {
					subscriberMap.remove(subscribedKey);
				}

				printSubscriberList(subscribedKey);

				return true;
			} // if the key does not exist in the list, there are no clients who
				// have subscribed for it, therefore nothing has to be done
			else {

				printSubscriberList(subscribedKey);

				return true;
			}
		} catch (Exception e) {
			// in case of an unexpected exception, false is returned
			return false;
		}
	}

	private void printSubscriberList(String subscribedKey) {
		// Printing Subscriber List
		List<HostRepresentation> list = new ArrayList<HostRepresentation>();
		list = subscriberMap.get(subscribedKey);
		logger.debug("Print Subscriber List for key '" + subscribedKey + "': " + list);
	}

	/**
	 * search the key-list of subscribed keys for clients, who have subscribed
	 * to a given key
	 * 
	 * @param key
	 *            the key to which the clients are looked for
	 * @return a list of clients, which have subscribed to the given key
	 */
	private List<HostRepresentation> getSubscribers(String key) {
		return subscriberMap.get(key);
	}

	/**
	 * takes the changed key value pair, which shall be published to the
	 * subscribed clients
	 * 
	 * @param key
	 *            the key which has changed
	 * @param value
	 *            the newly changed value
	 */
	public void publish(String key, String value) {
		// get List of recipients for the given key
		List<HostRepresentation> subs = this.getSubscribers(key);

		// create notification message
		Message notificationMsg;
		KVSubscriptionMessage kvnotificationMsg;

		// build KVSubscriptionMessage | Message
		kvnotificationMsg = new KVSubscriptionMessage(StatusType.PUBLISH, key, value);
		notificationMsg = new Message(MessageType.SUBSCRIPTION, kvnotificationMsg.getBytes());

		Publisher publisher = new Publisher();
		if (subs != null) {
			for (HostRepresentation srep : subs) {
				logger.debug(srep);

			}
			
			// publisher sends publish message to each subscriber in the subscriber list
			for (int i = 0; i < subs.size(); i++) {
				publisher.sendMsg(subs.get(i), notificationMsg, key);
			}
		} else {
			logger.debug("no clients subscribed to key " + key);
		}
	}

	public SortedMap<String, List<HostRepresentation>> getSubscriberMap() {
		return subscriberMap;
	}

}
