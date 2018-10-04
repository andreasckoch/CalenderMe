package testing;

import ecs.Ecs;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static Ecs testEcs;
	
	public static Test suite() {
        TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");
        clientSuite.addTestSuite(ConnectionTest.class);
        clientSuite.addTestSuite(InteractionTest.class);
        clientSuite.addTestSuite(AdditionalTest.class);
        clientSuite.addTestSuite(SubscribeTest.class);
        TestSetup testSetup = new TestSetup(clientSuite) {
	        protected void setUp() throws Exception {
	        	 testEcs= new Ecs("config");
	             System.out.println("initService");
	        	 testEcs.initService(5, 5, "FIFO");
	             System.out.println("startService");
	        	 testEcs.start();
	        }
	        protected void tearDown(  ) throws Exception {
	        	testEcs.shutDown();
            }
        };
        return testSetup;
    }

}
