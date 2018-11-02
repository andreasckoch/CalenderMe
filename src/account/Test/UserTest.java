package account.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import common.Communication;
import message.RegistrationMessage;
import message.MessageInterface.MESSAGETYPE;
import server.ServerConnection;

public class UserTest {
	
	
	private RegistrationMessage registrationMsg;
	private String email;
	private String pw;
	private String ip;
	private int port;

	@Before
	public void initialize() {
		email = "test@totallynotafakemail.com";
		pw = "yaya1234";
		ip = "localhost";
		port = 50000;

		registrationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_REQUEST, email, pw);
		
		new Thread() {
			@Override
			public void run() {
				@SuppressWarnings("unused")
				ServerConnection server = new ServerConnection(port);
			}
		}.start();
		
	}

	@Test
	public void registrationMessageCreationTest() throws Exception {

		byte[] msgBytes = new byte[] { 2, 19, 116, 101, 115, 116, 64, 116, 111, 116, 97, 108, 108, 121, 110, 111, 116,
				97, 102, 97, 107, 101, 109, 97, 105, 108, 46, 99, 111, 109, 19, 121, 97, 121, 97, 49, 50, 51, 52, 20 };
		assertThat(registrationMsg.getMsgBytes(), is(msgBytes));

		RegistrationMessage registrationMsgBack = new RegistrationMessage(registrationMsg.getMsgBytes());
		
		assertThat(registrationMsgBack.getEmail(), is(email));
		assertThat(registrationMsgBack.getPw(), is(pw));
		assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.REGISTRATION_REQUEST));
	}

	@Test
	public synchronized void registrationTestForServer() throws Exception {
		
		wait(3000);
		
		new Thread() {
			@Override
			public void run() {
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					System.out.printf("registrationMsg - Email: %s, Pw: %s", registrationMsg.getEmail(), registrationMsg.getPw());
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
			}
		}.start();
		/*
		wait(2000);
		
		RegistrationMessage registrationDeleteMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_DELETE_REQUEST, email, pw);
		
		new Thread() {
			@Override
			public void run() {
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
			}
		}.start();
		
		wait(1000);
	*/
	}
	
	
	@Test
	public synchronized void doubleRegistrationTestForServer() throws Exception {

		new Thread() {
			@Override
			public void run() {
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
			}
		}.start();
		
		wait(1500);
		
		for (int i = 0; i < 10; i++) {
			new Thread() {
				@Override
				public void run() {
					RegistrationMessage registrationMsgBack = null;
					try {
						Communication communicator = new Communication(ip, port);
						communicator.createSocket();
						communicator.send(registrationMsg.getMsgBytes());
						registrationMsgBack = new RegistrationMessage(communicator.receive());
					} catch (Exception e) {
						e.printStackTrace();
					}
	
					assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_FAILED));
					assert(registrationMsgBack.getEmail() ==  null);
					assert(registrationMsgBack.getPw() == null);
				}
			}.start();
		}
		
		wait(1500);
		
		
		RegistrationMessage registrationDeleteMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_DELETE_REQUEST, email, pw);
		
		new Thread() {
			@Override
			public void run() {
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationDeleteMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
				assert(registrationMsgBack.getEmail() ==  null);
				assert(registrationMsgBack.getPw() == null);
			}
		}.start();
		
		wait(1500);
	}
}
