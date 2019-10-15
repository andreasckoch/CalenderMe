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

public class GroupTest {
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);

	private static Basic registrationMsg;
	private static Basic registrationMsg2;
	private static Basic registrationMsg3;
	private static Basic registrationMsg4;
	private static Basic registrationDeleteMsg;
	private static Basic registrationDeleteMsg2;
	private static Basic registrationDeleteMsg3;
	private static Basic registrationDeleteMsg4;
	private static Basic groupMsg;
	private static Basic groupUpdateMsg;
	private static Basic groupUpdate2Msg;
	private static Basic groupUpdate3Msg;
	private static Basic groupDeleteMsg;
	private static Basic groupDelete2Msg;
	private static String email;
	private static String pw;
	private static String groupID;
	private static String groupName;
	private static String[] members = {"ricksmail@getschwifty.fky", "birdperson@imabirdperson.bird", "lookatme@immrmeeseeks.cando"};
	private static String groupDescription;
	private static String ip;
	private static int port;
	private static Thread server;

	private static Helper helper;



	// TODO: 1) test case with membership in multiple groups
	//		 2) remove members that are still in groups
	
	
	@BeforeClass
	public static void initialize() {
		email = "test@totallynotafakemail.com";
		pw = "yaya1234";

		groupName = "citadel of ricks";
		groupDescription = "Rick and Morty Cast";

		ip = "localhost";
		port = 0;		

		
		registrationMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(email)
						.setPassword(pw).build()
						).build();
		registrationMsg2 = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(members[0])
						.setPassword(pw).build()
						).build();
		registrationMsg3 = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(members[1])
						.setPassword(pw).build()
						).build();
		registrationMsg4 = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(members[2])
						.setPassword(pw).build()
						).build();
		registrationDeleteMsg = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(email)
						.setPassword(pw)
						.setDeleteAccount(true).build()
						).build();
		registrationDeleteMsg2 = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(members[0])
						.setPassword(pw)
						.setDeleteAccount(true).build()
						).build();
		registrationDeleteMsg3 = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(members[1])
						.setPassword(pw)
						.setDeleteAccount(true).build()
						).build();
		registrationDeleteMsg4 = Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(members[2])
						.setPassword(pw)
						.setDeleteAccount(true).build()
						).build();
		groupMsg = Basic.newBuilder().setType(Basic.MessageType.GROUP)
				.setGroup(
						Group.newBuilder()
						.setEmail(email)
						.setName(groupName)
						.addMembers(
								Person.newBuilder()
									.setEmail(members[0]))
						.addMembers(
								Person.newBuilder()
									.setEmail(members[1]))
						.addMembers(
								Person.newBuilder()
									.setEmail(members[2]))
						.setDescription(groupDescription).build()
						).build();
		

	server = new ServerConnection(port);			
	port = ((ServerConnection) server).getPort();
	
	helper = new Helper();
	Helper.setIp(ip);
	Helper.setPort(port);
	}

	@Test
	public void groupTestForServer() throws Exception {
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
		regThread.start();
		
		Thread regThread2 = helper.createThreadSuccess(registrationMsg2, regThread);
		regThread2.start();
		
		Thread regThread3 = helper.createThreadSuccess(registrationMsg3, regThread);
		regThread3.start();
		
		Thread regThread4 = helper.createThreadSuccess(registrationMsg4, regThread);
		regThread4.start();
		
		Thread groupThread = new Thread() {
			@Override
			public void run() {
				try {
					regThread4.join();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				ClientBasic groupMsgBack = null;
				try {
					Communication communicator = new Communication(ip, port);
					communicator.createSocket();
					communicator.send(groupMsg);
					groupMsgBack = communicator.receive();
					communicator.closeSocket();
				} catch (Exception e) {
					e.printStackTrace();
				}

				try {
					assertThat(groupMsgBack.getType(), is(ClientBasic.MessageType.GROUP_RESPONSE));					
				}
				catch (AssertionError ae) {
					helper.setAssertionError(ae);
				}
				groupID = groupMsgBack.getGroupResponse().getId();
				
			}
		};
		groupThread.start();		
		
		groupThread.join();
		
		groupUpdateMsg = Basic.newBuilder().setType(Basic.MessageType.GROUP)
				.setGroup(
						Group.newBuilder()
						.setEmail(email)
						.setId(groupID)
						.addPromoteToAdmins(
								Person.newBuilder()
									.setEmail(members[0])).build()
						).build();
		
		Thread groupUpdateThread = helper.createThreadSuccess(groupUpdateMsg, groupThread);
		groupUpdateThread.start();
		
		groupUpdate2Msg = Basic.newBuilder().setType(Basic.MessageType.GROUP)
				.setGroup(
						Group.newBuilder()
						.setEmail(email)
						.setId(groupID)
						.addRemoveMembers(
								Person.newBuilder()
									.setEmail(members[1]))
						.addRemoveMembers(
								Person.newBuilder()
									.setEmail(members[2])).build()
						).build();
		
		Thread groupUpdateThread2 = helper.createThreadSuccess(groupUpdate2Msg, groupUpdateThread);
		groupUpdateThread2.start();
		
		groupUpdate3Msg = Basic.newBuilder().setType(Basic.MessageType.GROUP)
				.setGroup(
						Group.newBuilder()
						.setEmail(email)
						.setId(groupID)
						.addRemoveMembers(
								Person.newBuilder()
								.setEmail(members[0])).build()
						).build();
		
		Thread groupUpdateThread3 = helper.createThreadError(groupUpdate3Msg, groupUpdateThread2);
		groupUpdateThread3.start();
		
		groupDeleteMsg = Basic.newBuilder().setType(Basic.MessageType.GROUP)
				.setGroup(
						Group.newBuilder()
						.setEmail(email)
						.setId(groupID)
						.setQuit(true).build()
						).build();
		
		Thread groupDeleteThread = helper.createThreadSuccess(groupDeleteMsg, groupUpdateThread3);
		groupDeleteThread.start();
		
		groupDelete2Msg = Basic.newBuilder().setType(Basic.MessageType.GROUP)
				.setGroup(
						Group.newBuilder()
						.setEmail(members[0])
						.setId(groupID)
						.setQuit(true).build()
						).build();
		
		Thread groupDeleteThread2 = helper.createThreadSuccess(groupDelete2Msg, groupDeleteThread);
		groupDeleteThread2.start();
		
		Thread regDelThread = helper.createThreadSuccess(registrationDeleteMsg, groupDeleteThread2);
		regDelThread.start();
		
		Thread regDelThread2 = helper.createThreadSuccess(registrationDeleteMsg2, regDelThread);
		regDelThread2.start();
		
		Thread regDelThread3 = helper.createThreadSuccess(registrationDeleteMsg3, regDelThread2);
		regDelThread3.start();
	
		Thread regDelThread4 = helper.createThreadSuccess(registrationDeleteMsg4, regDelThread3);
		regDelThread4.start();
	
		regDelThread4.join();
		
		if (helper.getAssertionError() != null) {
			fail();
		}
		
		logger.info("groupTestForServer successful!");
	}

	

}
