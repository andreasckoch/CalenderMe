package common.messages;

import common.Range;

import java.util.ArrayList;
import java.util.List;

import common.Metadata;
import common.HostRepresentation;

public class KVAdminMessage implements KVAdminMessageInterface {

	private KVAdminMessageInterface.MethodType methodType;
	
	private Range range;
	private Metadata metadata;
	private HostRepresentation server;
	private int cacheSize;
	private String displacementStrategy;
	private String key;
	
	public byte[] msgBytes;
	
	private static final byte LINE_FEED = (byte) 0x0A;
	private static final byte RETURN = (byte) 0x0D;
	
	private static final String typeid = "methodType";
	private static final String metadataid = "metadata";
	private static final String rangeid = "range";
	private static final String serverid = "server";
	private static final String cacheSizeid = "cacheSize";
	private static final String displacementStrategyid = "displacementStrategy";
	private static final String keyid = "key";

	/**
	 * Constructor for creating an AdminMessage from a byte array
	 * @param msgBytes
	 */
	public KVAdminMessage(byte[] msgBytes) {
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

				// create array from List in order to be able to create Strings, Metadata, Range etc.
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
						this.methodType = MethodType.valueOf(new String(nextTokenArr));
						nextToken.clear();
					}
					else if (label.equals(metadataid)) {
						this.metadata = new Metadata(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(rangeid)) {
						this.range = new Range(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(serverid)) {
						this.server = new HostRepresentation(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(cacheSizeid)) {
						this.cacheSize = Integer.parseInt(new String(nextTokenArr));
						nextToken.clear();
					}
					else if (label.equals(displacementStrategyid)) {
						this.displacementStrategy = new String(nextTokenArr);
						nextToken.clear();
					}
					else if (label.equals(keyid)) {
						this.key = new String(nextTokenArr);
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

	/**
	 * Constructor for AdminMessages that only contain the methodType
	 * for example: START, STOP, SHUTDOWN, LOCK_WRITE, UNLOCK_WRITE, ERROR, SUCCESS
	 * 
	 * @param methodType
	 */
	public KVAdminMessage(KVAdminMessageInterface.MethodType methodType) {
		this.methodType = methodType;
		byte[] position0 = typeid.getBytes();
		byte[] value0 = methodType.name().getBytes();
		
		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1];

		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length+1, value0.length);
		temp[position0.length + 1 + value0.length] = RETURN;
		
		this.msgBytes = temp;
	}
	
	/**
	 * Constructor that creates AdminMessages of the methodType INIT_SERVICE
	 * @param methodType
	 * @param metadata
	 * @param cacheSize
	 * @param displacementStrategy
	 */
	public KVAdminMessage(KVAdminMessageInterface.MethodType methodType, Metadata metadata, int cacheSize, String displacementStrategy, String key) {
		this.methodType = methodType;
		this.metadata = metadata;
		this.cacheSize = cacheSize;
		String cacheSizeTemp = ""+cacheSize;
		this.displacementStrategy = displacementStrategy;
		this.key = key;

		byte[] position0 = typeid.getBytes();
		byte[] value0 = methodType.name().getBytes();
		byte[] position1 = metadataid.getBytes();
		byte[] value1 = metadata.getBytes();
		byte[] position2 = cacheSizeid.getBytes();
		byte[] value2 = cacheSizeTemp.getBytes();
		byte[] position3 = displacementStrategyid.getBytes();
		byte[] value3 = displacementStrategy.getBytes();
		byte[] position4 = keyid.getBytes();
		byte[] value4 = key.getBytes();
		
		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1 + 
		                       position1.length + 1 + value1.length + 1 +
		                       position2.length + 1 + value2.length + 1 +
		                       position3.length + 1 + value3.length + 1 +
		                       position4.length + 1 + value4.length + 1];
		
		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length+1, value0.length);
		temp[position0.length + 1 + value0.length] = LINE_FEED;
		
		System.arraycopy(position1, 0, temp, position0.length+1+value0.length+1, position1.length);
		temp[position0.length+1+value0.length+1+position1.length] = LINE_FEED;
		System.arraycopy(value1, 0, temp, position0.length+1+value0.length+1+position1.length+1, value1.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length] = LINE_FEED;
		
		System.arraycopy(position2, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1, position2.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length] = LINE_FEED;
		System.arraycopy(value2, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1, value2.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length] = LINE_FEED;
		
		System.arraycopy(position3, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1, position3.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length] = LINE_FEED;
		System.arraycopy(value3, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length+1, value3.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length+1+value3.length] = LINE_FEED;
	
		System.arraycopy(position4, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length+1+value3.length+1, position4.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length+1+value3.length+1+position4.length] = LINE_FEED;
		System.arraycopy(value4, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length+1+value3.length+1+position4.length+1, value4.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length+1+position3.length+1+value3.length+1+position4.length+1+value4.length] = RETURN;
		
		msgBytes = temp;
	}
	
	/**
	 * Constructor for creating UPDATE AdminMessages including a Metadata object
	 * @param methodType
	 * @param metadata
	 */
	public KVAdminMessage(KVAdminMessageInterface.MethodType methodType, Metadata metadata) {
		this.methodType = methodType;
		this.metadata = metadata;
		
		byte[] position0 = typeid.getBytes();
		byte[] value0 = methodType.name().getBytes();
		byte[] position1 = metadataid.getBytes();
		byte[] value1 = metadata.getBytes();
		
		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1 + 
		                       position1.length + 1 + value1.length + 1];
		
		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length+1, value0.length);
		temp[position0.length + 1 + value0.length] = LINE_FEED;
		
		System.arraycopy(position1, 0, temp, position0.length+1+value0.length+1, position1.length);
		temp[position0.length+1+value0.length+1+position1.length] = LINE_FEED;
		System.arraycopy(value1, 0, temp, position0.length+1+value0.length+1+position1.length+1, value1.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length] = RETURN;
		
		this.msgBytes = temp;
	}
	
	/**
	 * Constructor for creating MOVE_DATA AdminMessages including a Range object and a ServerRepresentation object
	 * @param methodType
	 * @param range
	 * @param server
	 */
	public KVAdminMessage(KVAdminMessageInterface.MethodType methodType,  Range range, HostRepresentation server) {
		this.methodType = methodType;
		this.range = range;
		this.server = server;
		
		byte[] position0 = typeid.getBytes();
		byte[] value0 = methodType.name().getBytes();
		byte[] position1 = rangeid.getBytes();
		byte[] value1 = range.getBytes();
		byte[] position2 = serverid.getBytes();
		byte[] value2 = server.getBytes();
		
		// assembling msgBytes into temporary variable
		byte[] temp = new byte[position0.length + 1 + value0.length + 1 + 
		                       position1.length + 1 + value1.length + 1 +
		                       position2.length + 1 + value2.length + 1];
		
		System.arraycopy(position0, 0, temp, 0, position0.length);
		temp[position0.length] = LINE_FEED;
		System.arraycopy(value0, 0, temp, position0.length+1, value0.length);
		temp[position0.length + 1 + value0.length] = LINE_FEED;
		
		System.arraycopy(position1, 0, temp, position0.length+1+value0.length+1, position1.length);
		temp[position0.length+1+value0.length+1+position1.length] = LINE_FEED;
		System.arraycopy(value1, 0, temp, position0.length+1+value0.length+1+position1.length+1, value1.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length] = LINE_FEED;
		
		System.arraycopy(position2, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1, position2.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length] = LINE_FEED;
		System.arraycopy(value2, 0, temp, position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1, value2.length);
		temp[position0.length+1+value0.length+1+position1.length+1+value1.length+1+position2.length+1+value2.length] = RETURN;
		
		msgBytes = temp;
	}

	@Override
	public MethodType getMethodType() {
		return methodType;
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public Range getRange() {
		return range;
	}

	@Override
	public HostRepresentation getServerRep() {
		return server;
	}

	@Override
	public String getDisplacementStrategy() {
		return displacementStrategy;
	}

	@Override
	public int getCacheSize() {
		return cacheSize;
	}

	@Override
	public byte[] getBytes() {
		return msgBytes;
	}
	
	@Override
	public String getKey() {
		return key;
	}
}