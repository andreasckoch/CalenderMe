package common.messages;

import common.Range;
import common.Metadata;
import common.HostRepresentation;

public interface KVAdminMessageInterface {

	public enum MethodType {
		INIT_SERVICE,
		START,
		STOP,
		SHUTDOWN,
		LOCK_WRITE,
		UNLOCK_WRITE,
		MOVE_DATA,
		MOVE_REPLICA_DATA,
		UPDATE,
		SUCCESS,
		ERROR,
		HEARTBEAT
	}
	
	/**
	 * 
	 * @return type of the method that has to be executed by the server
	 */
	public MethodType getMethodType();
	
	/**
	 * 
	 * @return 
	 */
	public Range getRange();
	
	/**
	 * 
	 * @return
	 */
	public Metadata getMetadata();
	
	/**
	 * 
	 * @return
	 */
	public HostRepresentation getServerRep();
	
	/**
	 * 
	 * @return
	 */
	public int getCacheSize();
	
	/**
	 * 
	 * @return
	 */
	public String getDisplacementStrategy();

	/**
	 * 
	 * @return
	 */
	public byte[] getBytes();
	
	public String getKey();
}
