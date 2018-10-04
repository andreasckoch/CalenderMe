package common.messages;

import java.util.ArrayList;
import java.util.List;
import common.HostRepresentation;
import common.Metadata;

public class KVSubscriptionMessage {

	private KVSubscriptionMessageInterface.StatusType statusType;

	private Metadata metadata;
	private HostRepresentation client;
	private String key;
	private String value;

	public byte[] msgBytes;

	private static final byte LINE_FEED = (byte) 0x0A;
	private static final byte RETURN = (byte) 0x0D;

	private static final String typeid = "statusType";
	private static final String metadataid = "metadata";
	private static final String clientid = "client";
	private static final String keyid = "key";
	private static final String valueid = "value";

	/**
	 * Constructor for creating an AdminMessage from a byte array
	 * 
	 * @param msgBytes
	 */
	public KVSubscriptionMessage(byte[] msgBytes) {
		this.msgBytes = msgBytes;

		// readingValue == true: next token to read is a label-value
		// readingValue == false: next token to read is a label
		boolean readingValue = false;
		List<Byte> nextToken = new ArrayList<Byte>();
		String label = null;

		for (int i = 0; i < msgBytes.length; i++) {

			if (msgBytes[i] == LINE_FEED || i == msgBytes.length - 1) {
				// last byte
				if (i == msgBytes.length - 1)
					nextToken.add(msgBytes[i]);

				// create array from List in order to be able to create Strings,
				// Metadata, Range etc.
				byte[] nextTokenArr = new byte[nextToken.size()];
				for (int j = 0; j < nextToken.size(); j++) {
					nextTokenArr[j] = nextToken.get(j);
				}

				// set label
				if (!readingValue) {
					label = new String(nextTokenArr);
					nextToken.clear();

					// next token is an identifier again:
					readingValue = true;
				}

				// set label-value
				else {
					if (label.equals(typeid)) {
						this.statusType = KVSubscriptionMessageInterface.StatusType.valueOf(new String(nextTokenArr));
						nextToken.clear();
					} else if (label.equals(metadataid)) {
						this.metadata = new Metadata(nextTokenArr);
						nextToken.clear();
					} else if (label.equals(clientid)) {
						this.client = new HostRepresentation(nextTokenArr);
						nextToken.clear();
					} else if (label.equals(keyid)) {
						this.key = new String(nextTokenArr);
						nextToken.clear();
					} else if (label.equals(valueid)) {
						this.value = new String(nextTokenArr);
						nextToken.clear();
					} else {
						throw new IllegalArgumentException("Error in message interpretation");
					}
					readingValue = false;
				}
			} else {
				nextToken.add(msgBytes[i]);
			}
		}
	}

	/**
	 * Constructor for SubscriptionMessages that only contain the statusType for
	 * example: SUCCESS, ERROR
	 * 
	 * @param statusType
	 */
	public KVSubscriptionMessage(KVSubscriptionMessageInterface.StatusType statusType) {
		this.statusType = statusType;
		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();

		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1];

		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length + 1, value0.length);
		temp[position0.length + 1 + value0.length] = RETURN;

		this.msgBytes = temp;
	}

	/**
	 * Constructor for messages with statusType SERVER_NOT_RESPONSIBLE including
	 * the updated metadata
	 * 
	 * @param statusType
	 *            SERVER_STOPPED, SERVER_NOT_RESPONSIBLE or SERVER_WRITE_LOCK
	 * @param metadata
	 *            updated metadata from the server
	 */

	public KVSubscriptionMessage(KVSubscriptionMessageInterface.StatusType statusType, Metadata metadata) {
		this.statusType = statusType;
		this.metadata = metadata;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();
		byte[] position1 = metadataid.getBytes();
		byte[] value1 = metadata.getBytes();

		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1];

		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length + 1, value0.length);
		temp[position0.length + 1 + value0.length] = LINE_FEED;

		System.arraycopy(position1, 0, temp, position0.length + 1 + value0.length + 1, position1.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length] = LINE_FEED;
		System.arraycopy(value1, 0, temp, position0.length + 1 + value0.length + 1 + position1.length + 1,
				value1.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length] = RETURN;

		this.msgBytes = temp;
	}

	/**
	 * Constructor for PUBLISH messages that contain statusType, key and value:
	 * 
	 * @param statusType
	 * @param key
	 * @param value
	 */

	public KVSubscriptionMessage(KVSubscriptionMessageInterface.StatusType statusType, String key, String value) {
		this.statusType = statusType;
		this.key = key;
		this.value = value;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();
		byte[] position1 = keyid.getBytes();
		byte[] value1 = key.getBytes();
		byte[] position2 = valueid.getBytes();
		if (value == null) {
			value = "";
		}
		byte[] value2 = value.getBytes();

		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1
				+ position2.length + 1 + value2.length + 1];

		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length + 1, value0.length);
		temp[position0.length + 1 + value0.length] = LINE_FEED;

		System.arraycopy(position1, 0, temp, position0.length + 1 + value0.length + 1, position1.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length] = LINE_FEED;
		System.arraycopy(value1, 0, temp, position0.length + 1 + value0.length + 1 + position1.length + 1,
				value1.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length] = LINE_FEED;

		System.arraycopy(position2, 0, temp,
				position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1, position2.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1
				+ position2.length] = LINE_FEED;
		System.arraycopy(value2, 0, temp, position0.length + 1 + value0.length + 1 + position1.length + 1
				+ value1.length + 1 + position2.length + 1, value2.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1 + position2.length + 1
				+ value2.length] = RETURN;

		this.msgBytes = temp;
	}

	/**
	 * Constructor for SUBSCRIBE/UNSUBSCRIBE messages that contain statusType,
	 * key and ClientRepresentation:
	 * 
	 * @param statusType
	 * @param key
	 * @param value
	 */

	public KVSubscriptionMessage(KVSubscriptionMessageInterface.StatusType statusType, String key,
			HostRepresentation client) {
		this.statusType = statusType;
		this.key = key;
		this.client = client;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();
		byte[] position1 = keyid.getBytes();
		byte[] value1 = key.getBytes();
		byte[] position2 = clientid.getBytes();
		byte[] value2 = client.getBytes();

		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1
				+ position2.length + 1 + value2.length + 1];

		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length + 1, value0.length);
		temp[position0.length + 1 + value0.length] = LINE_FEED;

		System.arraycopy(position1, 0, temp, position0.length + 1 + value0.length + 1, position1.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length] = LINE_FEED;
		System.arraycopy(value1, 0, temp, position0.length + 1 + value0.length + 1 + position1.length + 1,
				value1.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length] = LINE_FEED;

		System.arraycopy(position2, 0, temp,
				position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1, position2.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1
				+ position2.length] = LINE_FEED;
		System.arraycopy(value2, 0, temp, position0.length + 1 + value0.length + 1 + position1.length + 1
				+ value1.length + 1 + position2.length + 1, value2.length);
		temp[position0.length + 1 + value0.length + 1 + position1.length + 1 + value1.length + 1 + position2.length + 1
				+ value2.length] = RETURN;

		this.msgBytes = temp;
	}

	public KVSubscriptionMessageInterface.StatusType getStatusType() {
		return statusType;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public HostRepresentation getClientRep() {
		return client;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public byte[] getBytes() {
		return msgBytes;
	}

}
