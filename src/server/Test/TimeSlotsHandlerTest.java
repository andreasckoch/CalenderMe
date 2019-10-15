package server.Test;

import static org.junit.Assert.fail;

import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import common.Constants;
import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.Registration;
import proto.CalenderMessagesProto.TimeSlot;
import proto.CalenderMessagesProto.TimeSlots;
import server.ServerConnection;

import server.Test.TimeSlotsTest;

public class TimeSlotsHandlerTest {
/*
 * 1) Timeslots hinzufügen
 * 2) Template hinzufügen
 * 3) Template zu lang
 * 4) Template updaten
 * 5) Timeslots updaten
 * 6) Timeslots teilweise und template komplett löschen
 * 7) Durch Löschen des accounts alle verbliebenen timeslots löschen
 */
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);

	private static Basic registrationMsg;
	private static Basic registrationDeleteMsg;
	private static String email;
	private static String pw;
	private static String ip;
	private static int port;
	private static Thread server;
	private static Helper helper;

	private static Basic timeslotsMsg;
	private static Basic templateMsg;
	private static Basic templateErrorMsg;
	private static Basic templateUpdateMsg;
	private static Basic templateDeleteMsg;
	private static Basic timeslotsUpdateMsg;
	private static Basic timeslotsDeleteMsg;

	
	@BeforeClass
	public static void initialize() {
		email = "test@totallynotafakemail.com";
		pw = "yaya1234";
		ip = "localhost";
		port = 0;		
		
		Calendar cali = Calendar.getInstance();
		long time_now = cali.getTimeInMillis();
		int slot_length = 12;
		
		long slot1_start = time_now + 300000 - (time_now % 300000);
		long slot1_end = slot1_start + slot_length * 300000;  // 12 * 5min --> 1h
		
		cali.add(Calendar.DATE, 1);
		cali.set(Calendar.HOUR_OF_DAY, 10);
		cali.set(Calendar.MINUTE, 0);
		cali.set(Calendar.SECOND, 0);
		cali.set(Calendar.MILLISECOND, 0);
		long slot2_start = cali.getTimeInMillis();
		long slot2_end = slot2_start + slot_length * 300000;
		
		long hour = 3600000;
		long[][] template = {{(long)9.5*hour, 10*hour}, {10*hour, 12*hour}, {12*hour, 13*hour}, {13*hour, 14*hour},  
							{15*hour, 16*hour}, {16*hour, 18*hour}, {18*hour, 22*hour}, {22*hour, (long) 22.5*hour}};
		long[][] template_error = {{(long)9.5*hour, 10*hour}, {10*hour, 12*hour}, {12*hour, 13*hour}, {13*hour, 14*hour},  
				{15*hour, 16*hour}, {16*hour, 18*hour}, {18*hour, 22*hour}, {22*hour, (long) 22.5*hour}};
		for (int i = 0; i < template_error.length; i++) {
			long shift = 3 * 7 * 24 * hour;
			template_error[i][0] += shift;
			template_error[i][1] += shift;
		}
		
		long[][] template_update = template;
		template_update[0][0] = (long) 8.5*hour;
		template_update[0][1] = 9*hour;
		template_update[1][0] = 9*hour;
		
		//logger.debug("template_error: {}", (Object) template_error);
		//logger.debug("Time: {}, Slot 1: {} - {}", time_now, slot1_start, slot1_end);
		
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
		
		timeslotsMsg = Basic.newBuilder().setType(Basic.MessageType.TIMESLOTS)
				.setTimeSlots(
						TimeSlots.newBuilder()
						.setEmail(email)
						.addSlots(TimeSlot.newBuilder()
								.setStartUnixTime(slot1_start)
								.setEndUnixTime(slot1_end)
								.setCategory(AppointmentMsg.Category.HOBBIES)
								.build())
						.addSlots(TimeSlot.newBuilder()
								.setStartUnixTime(slot2_start)
								.setEndUnixTime(slot2_end)
								.setCategory(AppointmentMsg.Category.BUSINESS)
								.build())).build();
		
		templateMsg = get2WeekTemplateMsgFromDayTemplate(template);

		templateErrorMsg = get2WeekTemplateMsgFromDayTemplate(template_error);
		
		templateUpdateMsg = get2WeekTemplateMsgFromDayTemplate(template_update);
		
		templateDeleteMsg = Basic.newBuilder().setType(Basic.MessageType.TIMESLOTS)
				.setTimeSlots(
						TimeSlots.newBuilder()
						.setEmail(email)
						.setRemoveTemplate(true)
						.build()).build();
		
		long shift = (long) 0.5 * hour;
		timeslotsUpdateMsg = Basic.newBuilder().setType(Basic.MessageType.TIMESLOTS)
				.setTimeSlots(
						TimeSlots.newBuilder()
						.setEmail(email)
						.addSlots(TimeSlot.newBuilder()
								.setStartUnixTime(shift + slot1_start)
								.setEndUnixTime(shift + slot1_end)
								.setCategory(AppointmentMsg.Category.HOBBIES)
								.build())
						.addSlots(TimeSlot.newBuilder()
								.setStartUnixTime(shift + slot2_start)
								.setEndUnixTime(shift + slot2_end)
								.setCategory(AppointmentMsg.Category.BUSINESS)
								.build())).build();
		
		timeslotsDeleteMsg = Basic.newBuilder().setType(Basic.MessageType.TIMESLOTS)
				.setTimeSlots(
						TimeSlots.newBuilder()
						.setEmail(email)
						.setRemoveTimeslots(true)
						.addSlots(TimeSlot.newBuilder()
								.setStartUnixTime(slot1_start)
								.setEndUnixTime(2*shift + slot1_end)
								.build())
								.build()).build();

	server = new ServerConnection(port);			
	port = ((ServerConnection) server).getPort();
	
	helper = new Helper();
	Helper.setIp(ip);
	Helper.setPort(port);
	}

	private static Basic get2WeekTemplateMsgFromDayTemplate(long[][] template) {
		TimeSlots.Builder timeBuilder = TimeSlots.newBuilder();
		timeBuilder.setEmail(email);
		timeBuilder.setUpdateTemplate(true);
		int[] template_days = {0, 1, 2, 3, 4, 7, 8, 9, 10, 11};
		timeBuilder = TimeSlotsTest.addToTemplateMsgFromDayTemplate(timeBuilder, template, template_days);
		Basic templateMessage = Basic.newBuilder().setType(Basic.MessageType.TIMESLOTS)
				.setTimeSlots(timeBuilder.build()).build();
		return templateMessage;
	}

	@Test
	public void timeslotsTestForServer() throws Exception {
		Thread regThread = helper.createThreadSuccessWaitForServer(registrationMsg, server);
		regThread.start();
		
		Thread timeslotsThread = helper.createThreadSuccess(timeslotsMsg, regThread);
		timeslotsThread.start();
		
		Thread templateThread = helper.createThreadSuccess(templateMsg, timeslotsThread);
		templateThread.start();
		
		Thread templateErrorThread = helper.createThreadError(templateErrorMsg, templateThread);
		templateErrorThread.start();
		
		Thread templateUpdateThread = helper.createThreadSuccess(templateUpdateMsg, templateErrorThread);
		templateUpdateThread.start();
		
		Thread templateDeleteThread = helper.createThreadSuccess(templateDeleteMsg, templateUpdateThread);
		templateDeleteThread.start();

		Thread timeslotsUpdateThread = helper.createThreadSuccess(timeslotsUpdateMsg, templateDeleteThread);
		timeslotsUpdateThread.start();
		
		Thread timeslotsDeleteThread = helper.createThreadSuccess(timeslotsDeleteMsg, timeslotsUpdateThread);
		timeslotsDeleteThread.start();
		
		Thread regDelThread = helper.createThreadSuccess(registrationDeleteMsg, timeslotsDeleteThread);
		regDelThread.start();
	
		regDelThread.join();
		
		if (helper.getAssertionError() != null) {
			fail();
		}
		
		logger.info("profileTestForServer successful!");
	}
	
	
}
