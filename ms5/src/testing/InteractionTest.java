package testing;

import client.ClientConnection;
import client.KVStore;
import common.messages.KVMessageInterface;
import common.messages.KVMessageInterface.StatusType;
import junit.framework.TestCase;
import org.junit.Test;


public class InteractionTest extends TestCase {

    private ClientConnection clientConnection;
    private KVStore kvClient;

    public void setUp() {
        clientConnection = new ClientConnection("localhost", 50000);
        try {
        	clientConnection.connect();
        	kvClient = new KVStore(clientConnection);
        } catch (Exception e) {
        }
    }

    public void tearDown() {
    	clientConnection.disconnect();
    }


    @Test
    public void testPut() {
        String key = "foo";
        String value = "bar";
        KVMessageInterface response = null;
        Exception ex = null;

        try {
        	kvClient.put(key, null);
            response = kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex == null && response.getStatus() == StatusType.PUT_SUCCESS);
    }

    @Test
    public void testPutDisconnected() {
        clientConnection.disconnect();
        String key = "foo";
        String value = "bar";
        Exception ex = null;

        try {
            kvClient.put(key, value);
        } catch (Exception e) {
            ex = e;
        }

        assertNotNull(ex);
    }

    @Test
    public void testUpdate() {
    	System.out.println("testUpdate");
        String key = "updateTestValue";
        String initialValue = "initial";
        String updatedValue = "updated";

        KVMessageInterface response = null;
        Exception ex = null;

        try {
            kvClient.put(key, initialValue);
            response = kvClient.put(key, updatedValue);

        } catch (Exception e) {
            ex = e;
            ex.printStackTrace();
        }
        assertTrue(ex == null && response.getStatus() == StatusType.PUT_UPDATE
                && response.getValue().equals(updatedValue));
    }

    @Test
    public void testDelete() {
    	System.out.println("testDelete");
        String key = "deleteTestValue";
        String value = "toDelete";

        KVMessageInterface response = null;
        Exception ex = null;

        try {
            kvClient.put(key, value);
            response = kvClient.put(key, null);

        } catch (Exception e) {
            ex = e;
        }
        assertTrue(ex==null && response.getStatus()==StatusType.DELETE_SUCCESS);
    }

    @Test
    public void testGet() {
        System.out.println("testGet");
        System.out.println("Number of active threads from the given thread: " + Thread.activeCount());
        String key = "foo";
        String value = "bar";
        KVMessageInterface response = null;
        Exception ex = null;

        try {
            kvClient.put(key, value);
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex == null && response.getValue().equals("bar"));
        System.out.println("Number of active threads from the given thread: " + Thread.activeCount());
    }

    @Test
    public void testGetUnsetValue() {
    	System.out.println("testGetUnsetValue");
        String key = "anunsetvalue";
        KVMessageInterface response = null;
        Exception ex = null;

        try {
            response = kvClient.get(key);
        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex == null && response.getStatus() == StatusType.GET_ERROR);
    }

}
