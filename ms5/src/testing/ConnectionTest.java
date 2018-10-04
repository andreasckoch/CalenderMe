package testing;

import client.ClientConnection;
import junit.framework.TestCase;

import java.net.UnknownHostException;


public class ConnectionTest extends TestCase {


    public void testConnectionSuccess() {

        Exception ex = null;

        ClientConnection clientConnection = new ClientConnection("localhost", 50000);
        
        try {
            clientConnection.connect();
        } catch (Exception e) {
            ex = e;
        }

        assertNull(ex);
    }


    public void testUnknownHost() {
        Exception ex = null;
        ClientConnection clientConnection = new ClientConnection("unknown", 50000);

        try {
            clientConnection.connect();
        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex instanceof UnknownHostException);
    }


    public void testIllegalPort() {
        Exception ex = null;
        ClientConnection clientConnection = new ClientConnection("localhost", 123456789);

        try {
            clientConnection.connect();
        } catch (Exception e) {
            ex = e;
        }

        assertTrue(ex instanceof IllegalArgumentException);
    }

}

