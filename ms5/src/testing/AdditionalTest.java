package testing;

import junit.framework.TestCase;
import java.io.IOException;
import java.net.UnknownHostException;
import org.junit.Test;
import client.ClientConnection;
import client.KVStore;
import common.Hashing;
import common.Metadata;
import common.HostRepresentation;
import common.messages.KVMessageInterface;
import ecs.ECSCommunication;
import ecs.Ecs;

public class AdditionalTest extends TestCase {

	private ClientConnection clientConnection;

	public void setUp() {
		clientConnection = new ClientConnection("localhost", 50000);
		try {
			clientConnection.connect();
		} catch (Exception e) {
		}
	}

	public void tearDown() {
		clientConnection.disconnect();
	}

	/*
	 * 
	 * MILESTONE 3 TESTS
	 * 
	 */
	@Test
	public void testMetaDataGet() {

		// create new server-representation elements to be added to the metadata
		// structure
		HostRepresentation s0 = new HostRepresentation("127.0.0.1", 50000); // (hash=358343938402ebb5110716c6e836f5a2)
		HostRepresentation s1 = new HostRepresentation("127.0.0.1", 50001); // (hash=dcee0277eb13b76434e8dcd31a387709)
		HostRepresentation s2 = new HostRepresentation("127.0.0.1", 50002); // (hash=b3638a32c297f43aa37e63bbd839fc7e)
		HostRepresentation s3 = new HostRepresentation("127.0.0.1", 50003); // (hash=a98109598267087dfc364fae4cf24578)

		// hash a random string
		String tempHash = Hashing.hashIt("hallo"); // (hash=598d4c200461b81522a3328565c25f7c)

		// -------> hallo should be in the responsibility range of server 3

		// add the respective representations into the metadata
		Metadata m = new Metadata();
		m.add(s0);
		m.add(s1);
		m.add(s2);
		m.add(s3);

		// locate the hashed value in the metadata and compare it with server
		// 3's
		assertEquals(m.getResponsibleServer(tempHash), s3);

	}

	public void testDualPut() {
		System.out.println("additional65" + "start");
		// start 2 clients, which both try to put the same value on 2 different
		// servers
		ClientConnection dualClient1 = new ClientConnection("127.0.0.1", 50000);
		ClientConnection dualClient2 = new ClientConnection("127.0.0.1", 50001);
		try {
			dualClient1.connect();
			dualClient2.connect();
		} catch (IOException e) {
		}
		KVStore dualPutClient1 = new KVStore(dualClient1);
		KVStore dualPutClient2 = new KVStore(dualClient2);

		try {
			// test a put operation
			assertTrue(dualPutClient1.put("testKey", "testValue")
					.getStatus() == KVMessageInterface.StatusType.PUT_SUCCESS);
			dualPutClient1.put("testKey", null);
			dualClient1.disconnect();
			// retry the put operation, which
			// should now return a PUT_UPDATE
			assertTrue(dualPutClient2.put("testKey", "testValue")
					.getStatus() == KVMessageInterface.StatusType.PUT_SUCCESS);
			dualPutClient2.put("testKey", null);
			dualClient2.disconnect();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Number of active threads from the given thread: " + Thread.activeCount());
	}

	public void testdualClient() {

		Exception putex = null;
		Exception getex = null;
		KVMessageInterface response = null;

		ClientConnection kvClient1 = new ClientConnection("127.0.0.1", 50000);
		ClientConnection kvClient2 = new ClientConnection("127.0.0.1", 50000);
		
		try {
			kvClient1.connect();
			kvClient2.connect();
		} catch (IOException e) {
		}

		KVStore kvPutClient1 = new KVStore(kvClient1);
		KVStore kvPutClient2 = new KVStore(kvClient2);
		
		try {
			kvPutClient1.put("tot", "ja");
		} catch (IOException e) {
			putex = e;
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			response = kvPutClient2.get("tot");
		} catch (IOException e) {
			getex = e;
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertNull(putex);
		assertNull(getex);
		assertTrue(response.getValue().equals("ja"));

	}

	public void testServerStopped() {
		Ecs writeEcs = AllTests.testEcs;
		ClientConnection kvClient1 = new ClientConnection("127.0.0.1", 50000);
		KVStore wlClient = new KVStore(kvClient1);
		try {
			kvClient1.connect();
		} catch (IOException ioe) {
		}
		writeEcs.stop();
		KVMessageInterface kvm = null;
		try {
			kvm = wlClient.put("trash", "trash");
		} catch (IOException ioe) {
		} catch (Exception e) {
		}
		assertTrue(kvm.getStatus().equals(KVMessageInterface.StatusType.SERVER_STOPPED));
		writeEcs.start();
		try {
			KVMessageInterface.StatusType tempResponse = wlClient.put("trash", "trash").getStatus();
			assertTrue(tempResponse.equals(KVMessageInterface.StatusType.PUT_SUCCESS)
					|| tempResponse.equals(KVMessageInterface.StatusType.PUT_UPDATE));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void testWrite_Lock(){
		ClientConnection kvClient1 = new ClientConnection("127.0.0.1", 50000);
		KVStore wlClient = new KVStore(kvClient1);
		
		for(int i=0; i<5; i++){
			ECSCommunication com = new ECSCommunication("127.0.0.1", 50000+i);
			try {
				com.lockWrite();
			} catch (Exception e) {
				e.printStackTrace();
			}
			com.disconnect();
		}
		
		try {
			kvClient1.connect();
		} catch (IOException ioe) {
		}
		KVMessageInterface kvm = null;
		
		
		try {
			kvm = wlClient.put("trash", "trash");
		} catch (IOException ioe) {
		} catch (Exception e) {
		}
		assertTrue(kvm.getStatus().equals(KVMessageInterface.StatusType.SERVER_WRITE_LOCK));
		
		try {
			for(int i=0; i<5; i++){
				ECSCommunication com = new ECSCommunication("127.0.0.1", 50000+i);
				com.unlockWrite();
				com.disconnect();
			}
			KVMessageInterface.StatusType tempResponse = wlClient.put("trash", "trash").getStatus();
			assertTrue(tempResponse.equals(KVMessageInterface.StatusType.PUT_SUCCESS)
					|| tempResponse.equals(KVMessageInterface.StatusType.PUT_UPDATE));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
