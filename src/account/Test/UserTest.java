package account.Test;

import static org.junit.Assert.*;

import org.junit.*;

import message.RegistrationMessage;
import message.RegistrationMessageInterface;

public class UserTest {

	@Test
	public void registrationTest() throws Exception {
		
		String email = "test@totallynotafakemail.com";
		String pw = "yaya1234";
		
		RegistrationMessage registrationMsg = new RegistrationMessage(RegistrationMessageInterface.MESSAGETYPE.REGISTRATION_REQUEST, email, pw);
	}
	
}
