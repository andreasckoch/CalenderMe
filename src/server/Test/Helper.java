package server.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import common.Communication;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.ClientBasic;

public class Helper {
	public static Thread createThreadSuccessWaitForServer(Basic message, Thread previousThread, String ip, int port, AssertionError exc) {
		return new Thread() {
			@Override
			public void run() {
				try {
					previousThread.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				ClientBasic msgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(message);
					msgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					assertEquals(ClientBasic.MessageType.SUCCESS, msgBack.getType());					
				}
				catch (AssertionError ae) {
					//exc = ae;
				}
			}
		};
	}
	
	public static Thread createThreadSuccess(Basic message, Thread previousThread, String ip, int port, AssertionError exc) {
		return new Thread() {
			@Override
			public void run() {
				try {
					previousThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				ClientBasic msgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(message);
					msgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				assertEquals(msgBack.getType(), ClientBasic.MessageType.SUCCESS);
			}
		};
	}
	
	public static Thread createThreadError(Basic message, Thread previousThread, String ip, int port, AssertionError exc) {
		return new Thread() {
			@Override
			public void run() {
				try {
					previousThread.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				ClientBasic msgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(message);
					msgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				assertEquals(msgBack.getType(), ClientBasic.MessageType.ERROR);
			}
		};
	}
}
