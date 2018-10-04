package testing;

import org.junit.Test;

import client.ClientConnection;
import common.HostRepresentation;
import common.messages.KVSubscriptionMessageInterface.StatusType;
import junit.framework.TestCase;
import common.messages.KVSubscriptionMessage;
import subscription.Subscriber;

public class SubscribeTest extends TestCase{
	
	private ClientConnection clientConnection;
	private Subscriber subscriber;
	
	public void setUp() {
		clientConnection = new ClientConnection("localhost", 50000);
		try {
			clientConnection.connect();
			subscriber = new Subscriber(clientConnection);
			//Fake ServerRepresentation for the server, that he "knows" where to send the notifications 
			subscriber.clientid = new HostRepresentation("localhost", 40000);
		} catch (Exception e) {
		}
	}

	public void tearDown() {
		clientConnection.disconnect();
	}
	
	@Test
	/**
	 * Tests if a client can subscribe to a key
	 * A client subscribes to a key and we test if we get a SUCCESS message in return from the server.
	 * 
	 */
    public void testSubscribeToKey() {
		
        String key = "foo";
        KVSubscriptionMessage response = null;
        Exception ex = null;

        try {
            response = subscriber.subscribe(key);
            
        } catch (Exception e) {
            ex = e;
        }
        assertTrue(ex == null && response.getStatusType() == StatusType.SUCCESS);
    }

	@Test
	/**
	 * Tests if the client can unsubscribe from a key
	 */
    public void testUnsubscribeFromKey() {
		
		
        String key = "foo";
        KVSubscriptionMessage response = null;
        Exception ex = null;

        try {
            response = subscriber.unsubscribe(key);
            
        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex == null && response.getStatusType() == StatusType.SUCCESS);
    }
}
