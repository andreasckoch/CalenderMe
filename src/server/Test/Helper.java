package server.Test;

import static org.junit.Assert.assertEquals;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.Communication;
import common.Constants;
import common.RandomString;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.ClientBasic;

public class Helper {
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);
	
	private static AssertionError assertionError;
	private static String ip;
	private static int port;
	private static ClientBasic msgBack;
	
	protected static String getIp() {
		return ip;
	}

	protected static void setIp(String ip) {
		Helper.ip = ip;
	}

	protected static int getPort() {
		return port;
	}

	protected static void setPort(int port) {
		Helper.port = port;
	}
	
	protected static ClientBasic getMsgBack() {
		return msgBack;
	}

	protected AssertionError getAssertionError() {
		return assertionError;
	}
	
	protected void setAssertionError(AssertionError exc) {
		assertionError = exc;
	}
	
	protected static List<String[]> getRandomUsers(int num_user) {
		int email_length = 30;
		int pw_length = 10;
		List<String[]> users = new ArrayList<String[]>();
		String easy = RandomString.digits + "ACEFGHJKLMNPQRUVWXYabcdefhijkprstuvwx";
		RandomString emails = new RandomString(email_length, new SecureRandom(), easy);
		RandomString pws = new RandomString(pw_length, new SecureRandom(), easy);
		for (int i = 0; i < num_user; i++) {
			String[] entry = {emails.nextString(), pws.nextString()};
			users.add(entry);
		}
		return users;
	}	

	public Thread createThreadSuccessWaitForServer(Basic message, Thread previousThread) {
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
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}
			}
		};
	}
	
	public Thread createThreadSuccess(Basic message, Thread previousThread) {
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
				try {
					assertEquals(ClientBasic.MessageType.SUCCESS, msgBack.getType());					
				}
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}			
			}
		};
	}
	
	public Thread createThreadError(Basic message, Thread previousThread) {
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
				try {
					assertEquals(ClientBasic.MessageType.ERROR, msgBack.getType());
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
				}
			}
		};
	}
	
	public Thread createThreadSuccess(Basic message, Thread[] previousThreads) {
		return new Thread() {
			@Override
			public void run() {
				try {
					for (Thread previousThread : previousThreads) {
						previousThread.join();						
					}
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
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}			
			}
		};
	}
	
	public Thread createThreadError(Basic message, Thread[] previousThreads) {
		return new Thread() {
			@Override
			public void run() {
				try {
					for (Thread previousThread : previousThreads) {
						previousThread.join();						
					}
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
					assertEquals(ClientBasic.MessageType.ERROR, msgBack.getType());
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
				}
			}
		};
	}
	
	public Thread createThreadAppointmentCreation(Basic message) {
		return new Thread() {			
			@Override
			public void run() {
				msgBack = null;
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
					assertEquals(ClientBasic.MessageType.APPOINTMENT_INIT_RESPONSE, msgBack.getType());					
				}
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}			
			}
		};
	}
	
	public Thread createThreadAttendeeInfo(Basic message) {
		return new Thread() {			
			@Override
			public void run() {
				msgBack = null;
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
					assertEquals(ClientBasic.MessageType.APPOINTMENT_ATTENDANT_RESPONSE, msgBack.getType());					
				}
				catch (AssertionError err) {
					assertionError = err;
					err.printStackTrace();
					logger.error("Error message: " + msgBack.getError().getErrorMessage());
				}			
			}
		};
	}
}


