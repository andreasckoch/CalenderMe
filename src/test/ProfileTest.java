package test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Communication;
import common.Constants;
import message.RegistrationMessage;
import message.LoginMessage;
import message.MessageInterface.MESSAGETYPE;
import message.ProfileMessage;
import server.ServerConnection;

public class ProfileTest {

	private static ProfileMessage profileMessagePrivate;
	private static ProfileMessage profileMessagePublic;
	private static ProfileMessage profileMessagePrivateRevert;
	private static ProfileMessage profileMessagePublicRevert;
	private static String ip;
	private static int port;
	private static Thread server;

	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);

	@BeforeClass
	public static void initialize() {

		ip = "localhost";
		port = 50000;

		profileMessagePrivate = new ProfileMessage(MESSAGETYPE.PROFILE_UPDATE_PRIVATE, "tetris@edrismail.com", "changed", "changed",
				"changed");
		profileMessagePublic = new ProfileMessage(MESSAGETYPE.PROFILE_UPDATE_PUBLIC, "tetris@edrismail.com", "edited", "edited",
				"edited");
		profileMessagePrivateRevert = new ProfileMessage(MESSAGETYPE.PROFILE_UPDATE_PRIVATE, "tetris@edrismail.com", "tetrisedris", "69",
				"i bin da edris");
		profileMessagePublicRevert = new ProfileMessage(MESSAGETYPE.PROFILE_UPDATE_PUBLIC, "tetris@edrismail.com", "Edris Tetris", "",
				"My name is Edris Tetris");

		server = new ServerConnection(port);

	}

	@Test
	public void editProfile() throws Exception {

		Thread editThread = new Thread() {
			@Override
			public void run() {
				try {
					server.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				ProfileMessage profileMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileMessagePrivate.getMsgBytes());
					profileMsgBack = new ProfileMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				assertThat(profileMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));

				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileMessagePublic.getMsgBytes());
					profileMsgBack = new ProfileMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(profileMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
			}
		};
		
		editThread.start();
		
		editThread.join();
		
		logger.info("profileTest successful!");
	}
	
	@After
	public void revertProfile() throws Exception {

		Thread revertThread = new Thread() {
			@Override
			public void run() {
				try {
					server.join(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				ProfileMessage profileMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileMessagePrivateRevert.getMsgBytes());
					profileMsgBack = new ProfileMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}
				assertThat(profileMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));

				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileMessagePublicRevert.getMsgBytes());
					profileMsgBack = new ProfileMessage(communicator.receive());
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(profileMsgBack.getMessageType(), is(MESSAGETYPE.OPERATION_SUCCESS));
			}
		};
		
		revertThread.start();
		
		revertThread.join();
		
		logger.info("profileTestRevert successful!");
	}

}
