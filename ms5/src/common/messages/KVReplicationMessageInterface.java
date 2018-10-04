package common.messages;

import common.HostRepresentation;

/**
 * ReplicationMessage sends single key-value pair from coordinator to
 * replication manager. This way the replicas get updated when the original data
 * changes. It can be a put (which is also an update) or a delete message. The
 * replication manager replies with success or error
 * 
 * R_REQUEST is sent from coordinator to another servers replication manager
 * contains: - its own responsibilityKey - "i am your replica1/replica2!"
 * according to its current metadata (to prevent that the server accepts wrong
 * replicas according to its current metadata, if the accepting server gets the
 * metadata-update slower than the sending coordinator)
 */

public interface KVReplicationMessageInterface {

	public enum StatusType {
		R_REQUEST, //initial request for replication process
		
		R_PUT, R_DELETE, //replicate key value pair changes
		
		R_SUB, R_UNSUB, //replicate subscriptions

		R_SUCCESS, R_ERROR //success message for reply
	}

	public StatusType getStatusType();

	public String getKey();

	public String getValue();

	public int getReplica();

	public String getResponsabilityKey();
	
	public HostRepresentation getClientRep();

	public byte[] getBytes();

}
