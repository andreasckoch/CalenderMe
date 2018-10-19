package account.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;

import org.junit.Test;

import common.Communication;
import message.RegistrationMessage;
import message.RegistrationMessageInterface.MESSAGETYPE;
import server.ServerConnection;

public class UserTest {

	@Test
	public void registrationMessageCreationTest() throws Exception {

		String email = "test@totallynotafakemail.com";
		String pw = "yaya1234";

		RegistrationMessage registrationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_REQUEST, email, pw);

		byte[] msgBytes = new byte[] { 0, 19, 116, 101, 115, 116, 64, 116, 111, 116, 97, 108, 108, 121, 110, 111, 116,
				97, 102, 97, 107, 101, 109, 97, 105, 108, 46, 99, 111, 109, 19, 121, 97, 121, 97, 49, 50, 51, 52, 20 };
		assertThat(registrationMsg.getMsgBytes(), is(msgBytes));

		RegistrationMessage registrationMsgBack = new RegistrationMessage(registrationMsg.getMsgBytes());

		assertThat(registrationMsgBack.getEmail(), is(email));
		assertThat(registrationMsgBack.getPw(), is(pw));
		assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.REGISTRATION_REQUEST));
	}

	@Test
	public synchronized void registrationTestForServer() throws Exception {

		String email = "test@totallynotafakemail.com";
		String pw = "yaya1234";
		String ip = "localhost";
		int port = 50000;

		RegistrationMessage registrationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_REQUEST, email, pw);
		System.out.println(registrationMsg.getMessageType());
		System.out.println(registrationMsg.getEmail());
		System.out.println(registrationMsg.getPw());
		System.out.println(registrationMsg.getMsgBytes());

		new Thread() {
			@Override
			public void run() {
				System.out.println("Server started in thread " + Thread.currentThread().getId());
				ServerConnection server = new ServerConnection(port);
			}
		}.start();

		wait(500);
		
		new Thread() {
			@Override
			public void run() {
				System.out.println("Client started in thread " + Thread.currentThread().getId());
				RegistrationMessage registrationMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(registrationMsg.getMsgBytes());
					registrationMsgBack = new RegistrationMessage(communicator.receive());
					System.out.println(registrationMsgBack.getMessageType());
					System.out.println(registrationMsgBack.getEmail());
					System.out.println(registrationMsgBack.getPw());
					System.out.println(registrationMsgBack.getMsgBytes());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Error in thread " + Thread.currentThread().getId());
					e.printStackTrace();
				}


				System.out.println("Asserting in thread " + Thread.currentThread().getId());
				assertThat(registrationMsgBack.getMessageType(), is(MESSAGETYPE.REGISTRATION_SUCCESS));
				assertThat(registrationMsgBack.getEmail(), is(email));
				assertThat(registrationMsgBack.getPw(), is(pw));
			}

		}.start();

	}
}
