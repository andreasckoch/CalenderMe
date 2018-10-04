package common.messages;

import java.util.ArrayList;
import java.util.List;

import common.HostRepresentation;


public class KVReplicationMessage implements KVReplicationMessageInterface {

	private KVReplicationMessageInterface.StatusType statusType;
	private String key;
	private String value = null;
	private String responsabilityKey;
	private HostRepresentation client;
	private int replica;
	private byte[] msgBytes;
	
	private static final char LINE_FEED = 0x0A;
	private static final char RETURN = 0x0D;

	private static final String typeid = "statusType";
	private static final String keyid = "key";
	private static final String valueid = "value";
	private static final String clientid = "client"; //subscription replication feature
	
	//special properties for ReplicationMessages:
	private static final String responsabilityKeyid = "responsabilityKey";
			//-> tells the receiving server the responsabilityKey for its replicas
	private static final String replicaid = "replica"; 
			//-> tells the receiving server which replica (1 or 2) he will receive
	
	/**
	 * Constructor for sending StatusType only. For example: R_SUCCESS, R_ERROR
	 * @param statusType
	 */
	
	public KVReplicationMessage(KVReplicationMessageInterface.StatusType statusType) {
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
	 * Constructor for R_REQUEST messages that contain the number of the replica (1 or 2)
	 * and the responsabilityKey for the according replica.
	 * This message will only get a R_SUCCESS or R_ERROR as response
	 * 		R_SUCCESS: the receiving server accepts to receive the replica data
	 * 		R_ERROR: the receiving server does not accept to receive the replica data
	 * 				because due to his current metadata it is not his responsible replica.
	 * 				The sending server has to wait and try another R_REQUEST until the server
	 * 				accepts and has the right metadata.
	 * @param statusType
	 * @param replica
	 * @param responsabilityKey
	 */
	
	public KVReplicationMessage(KVReplicationMessageInterface.StatusType statusType, int replica, String responsabilityKey) {
		this.statusType = statusType;
		this.replica = replica;
		this.responsabilityKey = responsabilityKey;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();
		byte[] position1 = replicaid.getBytes();
		String replicaString = ""+replica;
		byte[] value1 = replicaString.getBytes();
		byte[] position2 = responsabilityKeyid.getBytes();
		byte[] value2 = responsabilityKey.getBytes();

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
	 * Constructor for replication PUT messages from server to server instance.
	 * @param statusType
	 * @param key
	 * @param value
	 */
	
	public KVReplicationMessage(KVReplicationMessageInterface.StatusType statusType, String key, String value) {
		this.statusType = statusType;
		this.key = key;
		this.value = value;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();
		byte[] position1 = keyid.getBytes();
		byte[] value1 = key.getBytes();
		byte[] position2 = valueid.getBytes();
		if(value==null) {
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
	 * Constructor for replication DELETE Messages from server to server.
	 * 
	 * @param statusType
	 * @param key
	 */
	public KVReplicationMessage(KVReplicationMessageInterface.StatusType statusType, String key) {
		this.statusType = statusType;
		this.key = key;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = statusType.name().getBytes();
		byte[] position1 = keyid.getBytes();
		byte[] value1 = key.getBytes();

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
	 * Constructor for SUBSCRIBE/UNSUBSCRIBE replication messages that contain statusType,
	 * key and ClientRepresentation:
	 * 
	 * @param statusType
	 * @param key
	 * @param value
	 */
	
	public KVReplicationMessage(KVReplicationMessageInterface.StatusType statusType, String key,
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
	
	/**
	 * Constructor for deserializing replication messages from a byte array.
	 * @param msgBytes
	 */
	
	public KVReplicationMessage(byte[] msgBytes) {
		this.msgBytes = msgBytes;

		// readingValue == true: token to read is a label-value
		// readingValue == false: token to read is a label
		boolean readingValue = false;
		List<Byte> nextToken = new ArrayList<Byte>();
		String label = null;

		for (int i = 0; i < msgBytes.length; i++) {

			if (msgBytes[i] == LINE_FEED || i == msgBytes.length - 1) {
				// last byte
				if (i == msgBytes.length - 1)
					nextToken.add(msgBytes[i]);

				// create array from List in order to be able to create Strings
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
						this.statusType = KVReplicationMessageInterface.StatusType.valueOf(new String(nextTokenArr));
						nextToken.clear();
					} else if (label.equals(keyid)) {
						this.key = new String(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(valueid)) {
						this.value = new String(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(responsabilityKeyid)) {
						this.responsabilityKey = new String(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(replicaid)) {
						this.replica = Integer.parseInt(new String(nextTokenArr));
						nextToken.clear();
					}
					else if (label.equals(clientid)) {
						this.client = new HostRepresentation(nextTokenArr);
						nextToken.clear();
					}
					else {
						throw new IllegalArgumentException("Error in message interpretation");
					}
					readingValue = false;
				}

			} else {
				nextToken.add(msgBytes[i]);
			}
		}
	}

	@Override
	public StatusType getStatusType() {
		return this.statusType;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public int getReplica() {
		return this.replica;
	}

	@Override
	public String getResponsabilityKey() {
		return this.responsabilityKey;
	}
	
	@Override
	public HostRepresentation getClientRep() {
		return client;
	}

	@Override
	public byte[] getBytes() {
		return msgBytes;
	}
	
}
