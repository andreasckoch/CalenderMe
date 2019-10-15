package server.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Communication;
import common.Constants;
import proto.CalenderMessagesProto.*;
import server.ServerConnection;

public class RegistrationLoginHandlerTest {
	
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
	private static Helper helper;


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
										.setEmail(fakeemail)
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

		
		loginMsg = Basic.newBuilder().setType(Basic.MessageType.LOGIN)
								.setLogin(
										Login.newBuilder()
										.setEmail(email)
										.setPassword(pw).build()
										).build();
		loginFailMsg = Basic.newBuilder().setType(Basic.MessageType.LOGIN)
								.setLogin(
										Login.newBuilder()
										.setEmail(email)
										.setPassword(fakepw).build()
										).build();		
		server = new ServerConnection(port);			
		port = ((ServerConnection) server).getPort();
		
		helper = new Helper();
		Helper.setIp(ip);
		Helper.setPort(port);
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
		
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
		regThread.start();
			
		Thread regDelThread = helper.createThreadSuccess(registrationDeleteMsg, regThread);
		regDelThread.start();
		
		regDelThread.join();
		
		if (helper.getAssertionError() != null) {
			helper.setAssertionError(null);
			fail();
		}
		
		logger.info("registrationTestForServer successful!");
	}
	
	
	@Test
	public void doubleRegistrationTestForServer() throws Exception {
		
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
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
					
					ClientBasic registrationMsgBack = null;
					try {
						Communication communicator = new Communication(ip, port);
						communicator.createSocket();
						communicator.send(registrationMsg);
						registrationMsgBack = communicator.receive();
						communicator.closeSocket();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						assertThat(registrationMsgBack.getType(), is(ClientBasic.MessageType.ERROR));					
					}
					catch (AssertionError ae) {
						helper.setAssertionError(ae);
					}
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
				
				ClientBasic registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg);
					registrationMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					assertThat(registrationMsgBack.getType(), is(ClientBasic.MessageType.SUCCESS));
				}
				catch (AssertionError ae) {
					helper.setAssertionError(ae);
				}
			}
		};
		
		regDelThread.start();
	
		regDelThread.join();
		
		if (helper.getAssertionError() != null) {
			helper.setAssertionError(null);
			fail();
		}
		
		logger.info("doubleRegistrationTestForServer successful!");
	}
	
	
	@Test
	public void registrationDataModificationTestForServer() throws Exception {
		
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
		regThread.start();
		
		Thread emailModThread = helper.createThreadSuccess(registrationEmailModificationMsg, regThread);
		emailModThread.start();
		
		Thread pwModThread = helper.createThreadSuccess(registrationPwModificationMsg, emailModThread);
		pwModThread.start();
				
		Thread regDelThread = helper.createThreadSuccess(registrationDeleteModdedMsg, pwModThread);
		regDelThread.start();
	
		regDelThread.join();
		
		if (helper.getAssertionError() != null) {
			helper.setAssertionError(null);
			fail();
		}
		
		logger.info("registrationDataModificationTestForServer successful!");
	}
	
	@Test
	public void loginTestForServer() throws Exception {
		
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
		regThread.start();
			
		Thread logFailThread = helper.createThreadError(loginFailMsg, regThread);
		logFailThread.start();
		
		Thread logThread = helper.createThreadSuccess(loginMsg, logFailThread);
		logThread.start();
			
		Thread regDelThread = helper.createThreadSuccess(registrationDeleteMsg, logThread);
		regDelThread.start();
	
		regDelThread.join();
		
		if (helper.getAssertionError() != null) {
			helper.setAssertionError(null);
			fail();
		}
		
		logger.info("loginTestForServer successful!");
	}
	
}
