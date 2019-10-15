package server.Test;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Constants;
import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.ClientAttendantAppointment;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.ClientInitAppointment;
import proto.CalenderMessagesProto.Person;
import server.ServerConnection;

public class AppointmentHandlerTest {
	
	/*
	 * Testing functionality of AppointmentHandler/Appointment
	 * 1. Creating multiple appointments
	 * 2. Time slot matching algorithm
	 * 3. Initiator asks for confirmation of date
	 * 4. Participants answer
	 * 5. Add/Remove Members
	 */
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);
	private static ServerConnection server;
	private static Helper helper;
	private static Random rand = new Random();
	private static List<String[]> users;
	private static List<String> initiators = new ArrayList<String>();
	private static List<int[]> attendants_indices = new ArrayList<int[]>(); // must be same length as initiators!
	
	private static ClientBasic msgBack;
	
	@BeforeClass
	public static void initialize() {
		users = Helper.getRandomUsers(25);  // DON'T FORGET TO ADJUST
		logger.debug("Users: {}", users.get(0));
		
		// 1st appointment
		initiators.add(users.get(0)[0]);
		int[] app1_attendants = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
		attendants_indices.add(app1_attendants);
		// 2nd appointment
		initiators.add(users.get(12)[0]);
		int[] app2_attendants = {13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
		attendants_indices.add(app2_attendants);
//		// 3rd appointment
//		initiators.add(users.get(23)[0]);
//		int[] app3_attendants = {24, 25, 26, 27, 28, 29, 30};
//		attendants_indices.add(app3_attendants);
//		// 4th appointment
//		initiators.add(users.get(31)[0]);
//		int[] app4_attendants = {32, 33, 34, 35, 36, 37, 38, 39, 40};
//		attendants_indices.add(app4_attendants);
//		// 5th appointment
//		initiators.add(users.get(41)[0]);
//		int[] app5_attendants = {42, 43, 44, 45, 46, 47, 48, 49, 50};
//		attendants_indices.add(app5_attendants);
//		// 6th appointment
//		initiators.add(users.get(51)[0]);
//		int[] app6_attendants = {52, 53, 54, 55, 56, 57, 58, 59};
//		attendants_indices.add(app6_attendants);
		
		server = new ServerConnection(0);			
		int port = ((ServerConnection) server).getPort();
		String ip = "localhost";
		helper = new Helper();
		Helper.setIp(ip);
		Helper.setPort(port);
	}
	
	@Test
	public void createMultipleAppointments() throws Exception {
		Thread[] regThreads = registerAllUsers();
		for (Thread regThread : regThreads) {
			regThread.join();						
		}
		
		// create all appointments from initiators, attendants_indices
		List<AppointmentTest> appointments = new ArrayList<AppointmentTest>();
		for (int i = 0; i < initiators.size(); i++) {
			List<Person> attendants = new ArrayList<Person>();
			logger.debug("App {} attendants_indices: {}", i, attendants_indices.get(i));
			for (int idx : attendants_indices.get(i)) {
				attendants.add(Person.newBuilder()
						.setEmail(users.get(idx)[0])
						.build());
			}
			appointments.add(new AppointmentTest(initiators.get(i), attendants));
		}
		
		// send all appointment messages
		appointments = sendAllAppointmentMsgs(appointments);
		
		// compare all generated appointments in database with appointments array
		Thread currentThread = null;
		for (int app_idx = 0; app_idx < appointments.size(); app_idx++) {
			AppointmentTest appointment = appointments.get(app_idx);
			String queryingAttendeeEmail = appointment.getRandomAttendantEmail();
			
			logger.debug("appointmentID sent by attendant: {}", appointment.getId());
			logger.debug("queryingAttendeeEmail {}", queryingAttendeeEmail);
			currentThread = sendGetInfoMsg(queryingAttendeeEmail, appointment.getId());
			currentThread.join();
			msgBack = Helper.getMsgBack();
			if (helper.getAssertionError() != null) {
				logger.error("Retrieval of information failed!");
				fail();
			}
			if (msgBack == null) {
				logger.error("Got message back that is null");
				fail();
			}
			ClientAttendantAppointment compareMsg = appointment.getInfo();
			ClientAttendantAppointment databaseMsg = msgBack.getClientAttendantAppointment();
			compareDatabaseLocal(databaseMsg, compareMsg);
		}
		regThreads = deleteAllUserAccounts(currentThread);
		for (Thread regThread : regThreads) {
			regThread.join();						
		}
		currentThread.join();
		if (helper.getAssertionError() != null) {
			logger.error("Retrieval of information failed!");
			fail();
		}
		logger.info("createMultipleAppointments successful!");
	}


	
	/*
	 * Helper functions for code style and clearness
	 */
	private void compareDatabaseLocal(ClientAttendantAppointment databaseMsg, ClientAttendantAppointment compareMsg) {
		/*
		 * Compare received information about appointment and its information kept locally
		 * Both messages were built with the same function
		 */
		logger.debug("1ST MESSAGE - databaseMsg: {}", databaseMsg);
		logger.debug("2ND MESSAGE - compareMsg: {}", compareMsg);
		boolean sameMessages = databaseMsg.equals(compareMsg);
		if (!sameMessages) {
			logger.error("Comparison of received information about appointment failed!");
			fail();
		}
	}

	private Thread sendGetInfoMsg(String email, String appointmentID) {
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setEmail(email);
		appBuilder.setId(appointmentID);
		appBuilder.setGetInfo(true);
		Basic info_request = Basic.newBuilder().setType(Basic.MessageType.APPOINTMENT)
							.setAppointment(appBuilder.build()).build();
		Thread current = helper.createThreadAttendeeInfo(info_request);
		current.start();
		return current;
	}
	
	private List<AppointmentTest> sendAllAppointmentMsgs(List<AppointmentTest> appointments) {
		/*
		 * IMPORTANT: set appointmentIDs of appointments to correct appointmentIDs on server
		 * - helper.msgBack should have the id if thread terminated
		 * - send all in consecutive threads
		 */
		List<AppointmentTest> returnAppointments = new ArrayList<AppointmentTest>();
		Thread[] appThreads = new Thread[appointments.size()];
		
		for (int i = 0; i < appointments.size(); i++) {
			Basic appMsg = MsgFactory.getBasic(appointments.get(i).getMessage());
			logger.debug(appointments.get(i).getMessage());
			appThreads[i] = helper.createThreadAppointmentCreation(appMsg);
			appThreads[i].start();
			try {
				appThreads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ClientInitAppointment serverResponseMsg = Helper.getMsgBack().getClientInitAppointment();
			AppointmentTest appointmentTest = appointments.get(i);
			appointmentTest.setId(serverResponseMsg.getId());
			returnAppointments.add(appointmentTest);
			
			logger.debug("Create appointment {} in Thread {}", appointments.get(i).getAppointmentName(), appThreads[i].getId());
		}
		if (helper.getAssertionError() != null) {
			logger.error("Creation of appointments failed!");
			fail();
		}
		return returnAppointments;
	}

	private Thread[] registerAllUsers() {
		// register in parallel threads
		Thread[] regThreads = new Thread[users.size()];
		Basic regMsg = MsgFactory.getRegistrationMsg(users.get(0)[0], users.get(0)[1]);
		regThreads[0] = helper.createThreadSuccessWaitForServer(regMsg, server);
		regThreads[0].start();
		logger.debug("Register account {} in Thread {}", users.get(0)[0], regThreads[0].getId());
		for (int i = 1; i < users.size(); i++) {
			regMsg = MsgFactory.getRegistrationMsg(users.get(i)[0], users.get(i)[1]);
			regThreads[i] = helper.createThreadSuccess(regMsg, regThreads[0]);
			regThreads[i].start();
			logger.debug("Register account {} in Thread {}", users.get(i)[0], regThreads[i].getId());
		}
		if (helper.getAssertionError() != null) {
			logger.error("Registration failed!");
			fail();
		}
		return regThreads;
	}
	
	private Thread[] deleteAllUserAccounts(Thread lastThread) {
		// delete accounts in consecutive threads as locks are not yet implemented
		Thread[] regThreads = new Thread[users.size()];
		Basic regMsg = MsgFactory.getRegistrationDeleteMsg(users.get(0)[0], users.get(0)[1]);
		regThreads[0] = helper.createThreadSuccess(regMsg, lastThread);
		regThreads[0].start();
		logger.debug("Delete account {} in Thread {}", users.get(0)[0], regThreads[0].getId());
		for (int i = 1; i < users.size(); i++) {
			regMsg = MsgFactory.getRegistrationDeleteMsg(users.get(i)[0], users.get(i)[1]);
			regThreads[i] = helper.createThreadSuccess(regMsg, regThreads[i-1]);
			regThreads[i].start();
			logger.debug("Delete account {} in Thread {}", users.get(i)[0], regThreads[i].getId());
		}
		if (helper.getAssertionError() != null) {
			logger.error("Deregistration failed!");
			fail();
		}
		return regThreads;
	}
	
	
}
