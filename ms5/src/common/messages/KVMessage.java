package common.messages;

import java.util.ArrayList;
import java.util.List;

import common.Metadata;

public class KVMessage implements KVMessageInterface {

	private KVMessageInterface.StatusType statusType;
	private String key;
	private String value = null;
	private Metadata metadata;
	private byte[] msgBytes;
	private static final char LINE_FEED = 0x0A;
	private static final char RETURN = 0x0D;

	private static final String typeid = "statusType";
	private static final String keyid = "key";
	private static final String valueid = "value";
	private static final String metadataid = "metadata";

	/**
	 * Constructor for messages that only contain statusType GET_ERROR, GET_SUCCESS, PUT_ERROR, PUT_SUCCESS,
	 * PUT_UPDATE, DELETE_ERROR, DELETE_SUCCESS, SERVER_STOPPED, SERVER_WRITE_LOCK
	 * @param statusType
	 * @throws Exception
	 */
	
	public KVMessage(StatusType statusType) {
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
	 * Constructor for messages with statusType SERVER_NOT_RESPONSIBLE 
	 * including the updated metadata
	 * @param statusType SERVER_STOPPED, SERVER_NOT_RESPONSIBLE or SERVER_WRITE_LOCK
	 * @param metadata updated metadata from the server
	 */

	public KVMessage(StatusType statusType, Metadata metadata) {
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
	 * Constructor for PUT messages that contain statusType, key and value:
	 * @param statusType
	 * @param key
	 * @param value
	 */

	public KVMessage(KVMessageInterface.StatusType statusType, String key, String value) {
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
	 * Constructor for GET Messages only including statusType and key
	 * 
	 * @param statusType
	 * @param key
	 */
	public KVMessage(KVMessageInterface.StatusType statusType, String key) {
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
	 * Constructor for creating a KVMessage from any serialized Message (byte array)
	 * 
	 * @param msgBytes
	 */
	public KVMessage(byte[] msgBytes) {
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
						this.statusType = KVMessage.StatusType.valueOf(new String(nextTokenArr));
						nextToken.clear();
					} else if (label.equals(keyid)) {
						this.key = new String(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(valueid)) {
						this.value = new String(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(metadataid)) {
						this.metadata = new Metadata(nextTokenArr);
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
	public String getKey() {
		return this.key;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public StatusType getStatus() {
		return this.statusType;
	}
	
	@Override
	public Metadata getMetadata() {
		return this.metadata;
	}

	@Override
	public byte[] getBytes() {
		return msgBytes;
	}
}
