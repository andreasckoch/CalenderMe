package account.Test;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.*;

import common.Communication;
import message.RegistrationMessage;
import message.RegistrationMessageInterface;
import message.RegistrationMessageInterface.MESSAGETYPE;

public class UserTest {

	@Test
	public void registrationMessageCreationTest() throws Exception {

		String email = "test@totallynotafakemail.com";
		String pw = "yaya1234";

		RegistrationMessage registrationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_REQUEST, email, pw);

		byte[] msgBytes = new byte[] { 0, 19, 116, 101, 115, 116, 64, 116, 111, 116, 97, 108, 108, 121, 110, 111, 116,
				97, 102, 97, 107, 101, 109, 97, 105, 108, 46, 99, 111, 109, 19, 121, 97, 121, 97, 49, 50, 51, 52, 20 };
		assertThat(registrationMsg.getMsgBytes(), CoreMatchers.is(msgBytes));

		RegistrationMessage registrationMsgBack = new RegistrationMessage(registrationMsg.getMsgBytes());

		assert (registrationMsgBack.getEmail().equals(email));
		assert (registrationMsgBack.getPw().equals(pw));
		assert (registrationMsgBack.getMessageType().equals(MESSAGETYPE.REGISTRATION_REQUEST));
	}

	@Test
	public void registrationTestForServer() throws Exception {

		String email = "test@totallynotafakemail.com";
		String pw = "yaya1234";
		String ip = "192.168.101.27";
		int port = 9001;

		RegistrationMessage registrationMsg = new RegistrationMessage(MESSAGETYPE.REGISTRATION_REQUEST, email, pw);

		Communication communicator = new Communication(ip, port);
		communicator.send(registrationMsg.getMsgBytes());

		RegistrationMessage registrationMsgBack = new RegistrationMessage(registrationMsg.getMsgBytes());

		assert (registrationMsgBack.getEmail().equals(email));
		assert (registrationMsgBack.getPw().equals(pw));
		assert (registrationMsgBack.getMessageType().equals(MESSAGETYPE.REGISTRATION_REQUEST));
	}
}
