package account;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Communication;
import common.Constants;
import message.RegistrationMessage;
import message.LoginMessage;
import message.MessageInterface.MESSAGETYPE;
import server.ServerConnection;

public class UserTest {
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);
	
	private static RegistrationMessage registrationMsg;
	private static RegistrationMessage registrationDeleteMsg;
	private static RegistrationMessage registrationEmailModificationMsg;
	private static RegistrationMessage registrationPwModificationMsg;
	private static RegistrationMessage registrationDeleteModdedMsg;
	private static LoginMessage loginMsg;
	private static LoginMessage loginFailMsg;
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
		port = 50000;
		
		registrationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION, email, pw);
		registrationDeleteMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_DELETE, email, pw);
		registrationEmailModificationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_MODIFICATION_EMAIL, email, pw, fakeemail);
		registrationPwModificationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_MODIFICATION_PW, fakeemail, pw, fakepw);
		registrationDeleteModdedMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_DELETE, fakeemail, fakepw);

		
		loginMsg = new LoginMessage(MESSAGETYPE.LOGIN, email, pw);
		loginFailMsg = new LoginMessage(MESSAGETYPE.LOGIN, email, fakepw);
		
		server = new ServerConnection(port);
				
	}

	@Test
	public void registrationMessageCreationTest() throws Exception {

		byte[] msgBytes = new byte[] { 2, 19, 116, 101, 115, 116, 64, 116, 111, 116, 97, 108, 108, 121, 110, 111, 116,
				97, 102, 97, 107, 101, 109, 97, 105, 108, 46, 99, 111, 109, 19, 121, 97, 121, 97, 49, 50, 51, 52, 20 };
		assertThat(registrationMsg.getMsgBytes(), is(msgBytes));

		RegistrationMessage registrationMsgBack = new RegistrationMessage(registrationMsg.getMsgBytes());
		
		assertThat(registrationMsgBack.getEmail(), is(email));
		assertThat(registrationMsgBack.getPw(), is(pw));
		assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.REGISTRATION));
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
					
					RegistrationMessage registrationMsgBack = null;
					try {
						Communication communicator = new Communication(ip, port);
						communicator.createSocket();
						communicator.send(registrationMsg.getMsgBytes());
						registrationMsgBack = new RegistrationMessage(communicator.receive());
						communicator.closeSocket();
					} catch (Exception e) {
						e.printStackTrace();
					}

					assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_FAILED));
					assert(registrationMsgBack.getEmail() ==  null);
					assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationEmailModificationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationPwModificationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteModdedMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
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
				
				LoginMessage loginMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(loginFailMsg.getMsgBytes());
					loginMsgBack = new LoginMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(loginMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_FAILED));
				assert(loginMsgBack.getEmail() ==  null);
				assert(loginMsgBack.getPw() == null);
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
				
				LoginMessage loginMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(loginMsg.getMsgBytes());
					loginMsgBack = new LoginMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(loginMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(loginMsgBack.getEmail() ==  null);
				assert(loginMsgBack.getPw() == null);
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
				
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
			}
		};
		
		regDelThread.start();
	
		regDelThread.join();
		logger.info("loginTestForServer successful!");
	}
	
}
