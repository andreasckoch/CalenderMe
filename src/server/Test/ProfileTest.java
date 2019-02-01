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
	}

	@Test
	public void profileTestForServer() throws Exception {
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
		
		Thread profileThread = new Thread() {
			@Override
			public void run() {
				try {
					regThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic profileMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileMsg);
					profileMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(profileMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		profileThread.start();
		
		Thread profileUpdateThread = new Thread() {
			@Override
			public void run() {
				try {
					profileThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic profileMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileUpdateMsg);
					profileMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(profileMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		profileUpdateThread.start();
				
		Thread profileDeleteThread = new Thread() {
			@Override
			public void run() {
				try {
					profileUpdateThread.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Basic profileMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(profileDeleteMsg);
					profileMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				assertThat(profileMsgBack.getType(), is(Basic.MessageType.SUCCESS));
			}
		};
		profileDeleteThread.start();
		
		Thread regDelThread = new Thread() {
			@Override
			public void run() {
				try {
					profileDeleteThread.join();
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
		
		
		logger.info("profileTestForServer successful!");
	}

}
