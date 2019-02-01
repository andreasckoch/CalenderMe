package server.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Communication;
import common.Constants;
import proto.CalenderMessagesProto.*;
import server.ServerConnection;

public class RegistrationLoginTest {
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);
	
	private static Basic registrationMsg;
	private static Basic registrationDeleteMsg;
	private static Basic registrationEmailModificationMsg;
	private static Basic registrationPwModificationMsg;
	private static Basic registrationDeleteModdedMsg;
	private static Basic loginMsg;
	private static Basic loginFailMsg;
	private static String email;
	private static String fakeemail;
	private static String pw;
	private static String fakepw;
	private static String ip;
	private static int port;
	private static Thread server;










	@BeforeClass
	public static void initialize() {
		email = "test@totallynotafakemail.com";
		fakeemail = "test@definitelyafakeemail.com";
		pw = "yaya1234";
		fakepw = "nene4321";
		ip = "localhost";
		port = 0;
		
		registrationMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setRegistration(
										Registration.newBuilder()
										.setEmail(email)
										.setPassword(pw).build()
										).build();
		registrationDeleteMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setRegistration(
										Registration.newBuilder()
										.setEmail(email)
										.setPassword(pw)
										.setDeleteAccount(true).build()
										).build();
		registrationEmailModificationMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setRegistration(
										Registration.newBuilder()
										.setEmail(email)
										.setPassword(pw)
										.setChangeEmail(true)
										.setChangedField(fakeemail).build()
										).build();
		registrationPwModificationMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setRegistration(
										Registration.newBuilder()
										.setEmail(email)
										.setPassword(pw)
										.setChangePassword(true)
										.setChangedField(fakepw).build()
										).build();
		registrationDeleteModdedMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setRegistration(
										Registration.newBuilder()
										.setEmail(fakeemail)
										.setPassword(fakepw)
										.setDeleteAccount(true)
										).build();

		
		loginMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setLogin(
										Login.newBuilder()
										.setEmail(email)
										.setPassword(pw).build()
										).build();
		loginFailMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
								.setLogin(
										Login.newBuilder()
										.setEmail(email)
										.setPassword(fakepw).build()
										).build();		
		server = new ServerConnection(port);			
		port = ((ServerConnection) server).getPort();
				
	}	
	
	
	@Test
	public void registrationMessageCreationTest() throws Exception {

		byte[] msgBytes = registrationMsg.toByteArray();

		Basic registrationMsgBack = Basic.parseFrom(msgBytes);
		
		assertThat(registrationMsgBack.getRegistration().getEmail(), is(email));
		assertThat(registrationMsgBack.getRegistration().getPassword(), is(pw));
		assertThat(registrationMsgBack.getType(), is(Basic.MessageType.REGISTRATION));
	}

	@Test
	public void registrationTestForServer() throws Exception {
		
		Thread regThread = new Thread() {
			@Override
			public void run() {
				try {
					server.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		regThread.start();
				
		Thread regDelThread = new Thread() {
			@Override
			public void run() {
				try {
					regThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		
		regDelThread.start();
	
		regDelThread.join();
		logger.info("registrationTestForServer successful!");
	}
	
	
	@Test
	public void doubleRegistrationTestForServer() throws Exception {
		
		Thread regThread = new Thread() {
			@Override
			public void run() {
				try {
					server.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		regThread.start();
		
		Thread[] threads = new Thread[10];
		for (int i = 0; i < 10; i++) {
			threads[i] = new Thread() {
				@Override
				public void run() {
					try {
						regThread.join();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					Basic registrationMsgBack = null;
					try {
						Communication communicator = new Communication(ip, port);
						communicator.createSocket();
						communicator.send(registrationMsg);
						registrationMsgBack = communicator.receive();
						communicator.closeSocket();
					} catch (Exception e) {
						e.printStackTrace();
					}

					assertThat(registrationMsgBack.getType(), is(Basic.MessageType.ERROR));
				}
			};
			threads[i].start();
		}
		
		
		Thread regDelThread = new Thread() {
			@Override
			public void run() {
				try {
					for (Thread thread : threads) {
						thread.join();
					}
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		
		regDelThread.start();
	
		regDelThread.join();
		logger.info("doubleRegistrationTestForServer successful!");
	}
	
	
	@Test
	public void registrationDataModificationTestForServer() throws Exception {
		
		Thread regThread = new Thread() {
			@Override
			public void run() {
				try {
					server.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		regThread.start();
		
		Thread emailModThread = new Thread() {
			@Override
			public void run() {
				try {
					regThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationEmailModificationMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		emailModThread.start();
		
		Thread pwModThread = new Thread() {
			@Override
			public void run() {
				try {
					emailModThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationPwModificationMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		pwModThread.start();
				
		Thread regDelThread = new Thread() {
			@Override
			public void run() {
				try {
					pwModThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteModdedMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		
		regDelThread.start();
	
		regDelThread.join();
		logger.info("registrationDataModificationTestForServer successful!");
	}
	
	@Test
	public void loginTestForServer() throws Exception {
		
		Thread regThread = new Thread() {
			@Override
			public void run() {
				try {
					server.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		regThread.start();
		
		Thread logFailThread = new Thread() {
			@Override
			public void run() {
				try {
					regThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic loginMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(loginFailMsg);
					loginMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(loginMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		logFailThread.start();
		
		Thread logThread = new Thread() {
			@Override
			public void run() {
				try {
					logFailThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic loginMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(loginMsg);
					loginMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(loginMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		logThread.start();
				
		Thread regDelThread = new Thread() {
			@Override
			public void run() {
				try {
					logThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		
		regDelThread.start();
	
		regDelThread.join();
		logger.info("loginTestForServer successful!");
	}
	
}