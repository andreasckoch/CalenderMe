package server.Test;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

import common.Constants;
import common.Constants.Appointments;
import common.RandomString;
import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.AppointmentMsg.Category;
import proto.CalenderMessagesProto.ClientAttendantAppointment;
import proto.CalenderMessagesProto.Conditions;
import proto.CalenderMessagesProto.Person;
import server.Appointment;

public class AppointmentTest extends Appointment {
	/*
	 * class for interacting with one appointment - functionality:
	 * 1) create messages for interacting with server
	 * 2) for comparison purposes extend appointment class --> don't write into database!!
	 */
	
	private static final Logger logger = LogManager.getLogger(Constants.TEST_NAME);
	
	private AppointmentMsg message;
	private static AppointmentMsg staticMessage;
	private static MongoDatabase database = (new MongoClient(new MongoClientURI("mongodb://localhost:27017"))).getDatabase(Constants.DATABASE);
	static Random rand = new Random();
	private static String easy = RandomString.digits + "ACEFGHJKLMNPQRUVWXYabcdefhijkprstuvwx";
	private static RandomString randomShortString = new RandomString(10, new SecureRandom(), easy);
	private static RandomString randomDescription = new RandomString(100, new SecureRandom(), easy);
	static Calendar cal = Calendar.getInstance();
	long time_now = cal.getTimeInMillis();
	int month = cal.get(Calendar.MONTH);
	int day = cal.get(Calendar.DATE);
	int hour = cal.get(Calendar.HOUR_OF_DAY);
	private static double min_attendants_pct = 0.1;
	private static int[] start_days = {1, 2, 3, 4, 5};
	private static int[] end_days = {6, 7, 8, 9, 10};
	private static int[] start_hours = {8, 10, 12, 14, 16, 18};
	private static int[] length_min = {20, 30, 30, 60, 120, 180};
	
	
	protected AppointmentTest(String initiator, List<Person> attendants) {
		// Appointment constructor with random parameters except the participants
		super(createMessageAttendants(initiator, attendants), database);
		this.message = staticMessage;
	}
	
	protected AppointmentTest(String initiator, List<Person> attendants, long start_unix_time, int length) {
		// Appointment constructor with random parameters except the participants and time
		super(createMessageFixedTime(initiator, attendants, start_unix_time, length), database);
		this.message = staticMessage;
	}
	
	protected AppointmentTest(String initiator, List<Person> attendants, long start_unix_timeframe, long end_unix_timeframe, int length) {
		// Appointment constructor with random parameters except the participants and its timeframe
		super(createMessageVariableTime(initiator, attendants, start_unix_timeframe, end_unix_timeframe, length), database);
		this.message = staticMessage;
	}
	
	protected AppointmentTest(AppointmentMsg message) {
		// Appointment constructor with fully defined message
		super(message, database);
		this.message = message;
	}

	/*
	 * Functions for creating appointment messages
	 */
	private static AppointmentMsg createMessageAttendants(String initiator, List<Person> attendants) {
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setInitiator(initiator);
		for (Person attendant : attendants) {
			appBuilder.addAttendants(attendant);
		}
		appBuilder.setStartUnixTime(getRandomTime(start_days));
		appBuilder.setLength(length_min[rand.nextInt(length_min.length)]);
		appBuilder.setMinAttendants((int) Math.floor(attendants.size() * min_attendants_pct));
		appBuilder.setGroupId(randomShortString.nextString());
		appBuilder = fillOutParametersRandomly(appBuilder);
		staticMessage = appBuilder.build(); // field must be set
		return staticMessage;
	}


	private static AppointmentMsg createMessageFixedTime(String initiator, List<Person> attendants, long start_unix_time,
			int length) {
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setInitiator(initiator);
		for (int i = 0; i < attendants.size(); i++) {
			appBuilder.setAttendants(i, attendants.get(i));			
		}
		appBuilder.setStartUnixTime(start_unix_time);
		appBuilder.setLength(length);
		appBuilder.setMinAttendants((int) Math.floor(attendants.size() * min_attendants_pct));
		appBuilder.setGroupId(randomShortString.nextString());
		appBuilder = fillOutParametersRandomly(appBuilder);
		staticMessage = appBuilder.build();
		return staticMessage;
	}

	private static AppointmentMsg createMessageVariableTime(String initiator, List<Person> attendants,
			long start_unix_timeframe, long end_unix_timeframe, int length) {		
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setInitiator(initiator);
		for (int i = 0; i < attendants.size(); i++) {
			appBuilder.setAttendants(i, attendants.get(i));			
		}
		appBuilder.setStartUnixTimeframe(getRandomTime(start_days));
		appBuilder.setEndUnixTimeframe(getRandomTime(end_days));
		appBuilder.setLength(length_min[rand.nextInt(length_min.length)]);
		appBuilder.setMinAttendants((int) Math.round(attendants.size() * min_attendants_pct));
		appBuilder.setGroupId(randomShortString.nextString());
		appBuilder = fillOutParametersRandomly(appBuilder);
		staticMessage = appBuilder.build();
		return staticMessage;
	}

	private static AppointmentMsg.Builder fillOutParametersRandomly(AppointmentMsg.Builder appBuilder) {
		/*
		 * fill out every field except initiator, attendants, key_attendants, min_attendants
		 * start_unix_time, length, start_unix_timeframe, end_unix_timeframe, group_id
		 */
		appBuilder.setLocation(randomShortString.nextString());
		appBuilder.setName(randomShortString.nextString());
		appBuilder.setDescription(randomDescription.nextString());
		appBuilder.setDeadlineUnixTime(getRandomTime(end_days));
		appBuilder.setFlexible(rand.nextBoolean());
		Conditions.Weather weather = Conditions.Weather.forNumber(rand.nextInt(8));
		Conditions conditions = Conditions.newBuilder().setWeather(weather).build();
		appBuilder.setConditions(conditions);
		appBuilder.setCategory(Category.forNumber(rand.nextInt(6)));
		appBuilder.setOnlySameCategory(rand.nextBoolean());
		return appBuilder;
	}
	
	/*
	 * Creating messages for interactions with existing appointment
	 */
	protected AppointmentMsg initChooseDate(String intiator, String id, int index_date_list) {
		// change parent appointment class for comparison
		setStart_unix_time(getChosenDate(index_date_list));
		
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setEmail(intiator);
		appBuilder.setId(id);
		appBuilder.setIndexOfDatesList(index_date_list);
		return appBuilder.build();
	}
	
	protected AppointmentMsg initAddAttendants(String intiator, String id, List<Person> add_attendants) {
		// change parent appointment class for comparison
		initAddToAttendants(add_attendants);
		
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setEmail(intiator);
		appBuilder.setId(id);
		for (int i = 0; i < add_attendants.size(); i++) {
			appBuilder.setAddAttendants(i, add_attendants.get(i));			
		}
		return appBuilder.build();
	}
	
	protected AppointmentMsg initRemoveParticipants(String intiator, String id, List<Person> to_be_removed) {
		// change parent appointment class for comparison
		initRemoveParticipants(to_be_removed);
				
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setEmail(intiator);
		appBuilder.setId(id);
		for (int i = 0; i < to_be_removed.size(); i++) {
			appBuilder.setRemoveParticipant(i, to_be_removed.get(i));			
		}
		return appBuilder.build();
	}
	
	protected AppointmentMsg attendeeAnswer(String attendee, String id, AppointmentMsg.Answer answer) {
		AppointmentMsg.Builder appBuilder = AppointmentMsg.newBuilder();
		appBuilder.setEmail(attendee);
		appBuilder.setId(id);
		appBuilder.setConfirmAppointment(answer);
		return appBuilder.build();
	}
	
	
	protected ClientAttendantAppointment getInfo() {
		return getInfoForAttendant();
	}
	
	protected String getId() {
		return getAppointmentID().toString();
	}
	
	protected void setId(String appointmentIDString) {
		setAppointmentID(appointmentIDString);
	}
	
	protected String getAppointmentName() {
		return getName();
	}
	
	protected String getRandomAttendantEmail() {
		List<Document> attendants = getAttendants();
		int idx = rand.nextInt(attendants.size());
		return attendants.get(idx).getString(Appointments.ATTENDEE_KEY);
	}
	
	/*
	 * Comparing database with variables from super class
	 */
	
	
	/*
	 * Helper functions
	 */
	private static long getRandomTime(int[] days) {
		cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days[rand.nextInt(days.length)]);
		cal.set(Calendar.HOUR_OF_DAY, start_hours[rand.nextInt(start_hours.length)]);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}
	
	protected AppointmentMsg getMessage() {
		return message;
	}
}






