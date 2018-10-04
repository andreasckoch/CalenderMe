package common.messages;

import common.HostRepresentation;
import common.Metadata;

public interface KVSubscriptionMessageInterface {

	public enum StatusType {
		SUBSCRIBE, UNSUBSCRIBE, SUCCESS, // successful (un)subscription for
											// given key
		ERROR, // error while (un)subscribing to key (for example server stopped
				// or not reachable)
		SERVER_NOT_RESPONSIBLE,

		PUBLISH

	}

	public StatusType getStatusType();

	public String getKey();

	public String getValue();

	public HostRepresentation getClientRep();

	public Metadata getMetadata();

	public byte[] getBytes();

}
