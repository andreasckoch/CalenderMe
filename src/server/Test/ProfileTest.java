package server.Test;

import static org.junit.Assert.fail;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Constants;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.Profile;
import proto.CalenderMessagesProto.Registration;
import server.ServerConnection;

public class ProfileTest {
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);

	private static Basic registrationMsg;
	private static Basic registrationDeleteMsg;
	private static Basic profileMsg;
	private static Basic profileUpdateMsg;
	private static Basic profileDeleteMsg;
	private static String email;
	private static String pw;
	private static String name;
	private static String location;
	private static String bio;
	private static String organisation;
	private static String fakename;
	private static String fakelocation;
	private static String fakebio;
	private static String fakeorganisation;
	private static String ip;
	private static int port;
	private static Thread server;
	private static Helper helper;

	@BeforeClass
	public static void initialize() {
		email = "test@totallynotafakemail.com";
		pw = "yaya1234";
		name = "morty";
		location = "universe c137";
		bio = "father, world destroyer, vindicator";
		organisation = "citadel of ricks";
		fakename = "jerry";
		fakelocation = "home";
		fakebio = "renouned supporter of pluto being a planet";
		fakeorganisation = "Hungry for Apples Inc.";
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
		profileMsg = Basic.newBuilder().setType(Basic.MessageType.PROFILE)
				.setProfile(
						Profile.newBuilder()
						.setEmail(email)
						.setName(name)
						.setLocation(location)
						.setBio(bio)
						.setOrganisation(organisation).build()
						).build();
		profileUpdateMsg = Basic.newBuilder().setType(Basic.MessageType.PROFILE)
				.setProfile(
						Profile.newBuilder()
						.setEmail(email)
						.setName(fakename)
						.setLocation(fakelocation)
						.setBio(fakebio)
						.setOrganisation(fakeorganisation).build()
						).build();
		profileDeleteMsg = Basic.newBuilder().setType(Basic.MessageType.PROFILE)
				.setProfile(
						Profile.newBuilder()
						.setEmail(email)
						.setName("")
						.setLocation("")
						.setBio("")
						.setOrganisation("").build()
						).build();

	server = new ServerConnection(port);			
	port = ((ServerConnection) server).getPort();
	
	helper = new Helper();
	Helper.setIp(ip);
	Helper.setPort(port);
	}

	@Test
	public void profileTestForServer() throws Exception {
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
		regThread.start();
		
		Thread profileThread = helper.createThreadSuccess(profileMsg, regThread);
		profileThread.start();
		
		Thread profileUpdateThread = helper.createThreadSuccess(profileUpdateMsg, profileThread);
		profileUpdateThread.start();
		
		Thread profileDeleteThread = helper.createThreadSuccess(profileDeleteMsg, profileUpdateThread);
		profileDeleteThread.start();
		
		Thread regDelThread = helper.createThreadSuccess(registrationDeleteMsg, profileDeleteThread);
		regDelThread.start();
	
		regDelThread.join();
		
		if (helper.getAssertionError() != null) {
			fail();
		}
		
		logger.info("profileTestForServer successful!");
	}

}
