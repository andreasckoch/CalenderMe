package server;

import static com.mongodb.client.model.Filters.eq;
import static common.Constants.APPOINTMENT_COLLECTION;
import static common.Constants.NEWS_COLLECTION;
import static common.Constants.TIMESLOTS_COLLECTION;
import static common.Constants.USER_COLLECTION;
import static common.Constants.Appointments.AMOUNT_POSSIBLE_DATES;
import static common.Constants.Appointments.ATTENDANTS;
import static common.Constants.Appointments.ATTENDEE_KEY;
import static common.Constants.Appointments.ATTENDEE_STATUS;
import static common.Constants.Appointments.CATEGORY;
import static common.Constants.Appointments.DAY_MASK;
import static common.Constants.Appointments.DEADLINE_UNIX_TIME;
import static common.Constants.Appointments.DESCRIPTION;
import static common.Constants.Appointments.END_UNIX_TIMEFRAME;
import static common.Constants.Appointments.FLEXIBLE;
import static common.Constants.Appointments.GROUPID;
import static common.Constants.Appointments.ID;
import static common.Constants.Appointments.INITIATOR;
import static common.Constants.Appointments.KEY_ATTENDANTS;
import static common.Constants.Appointments.LAST_UPDATE_TIME;
import static common.Constants.Appointments.LENGTH;
import static common.Constants.Appointments.LOCATION;
import static common.Constants.Appointments.MIN_ATTENDANTS;
import static common.Constants.Appointments.NAME;
import static common.Constants.Appointments.POSSIBLE_DATES;
import static common.Constants.Appointments.START_UNIX_TIME;
import static common.Constants.Appointments.START_UNIX_TIMEFRAME;
import static common.Constants.Appointments.WEATHER;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import common.Constants;
import common.Constants.News;
import common.Constants.Timeslots;
import common.Constants.User;
import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.ClientAttendantAppointment;
import proto.CalenderMessagesProto.Conditions;
import proto.CalenderMessagesProto.Person;

public class Appointment {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private MongoDatabase database;
	private String initiator;
	private ObjectId appointmentID;
	private List<Document> attendants = new ArrayList<Document>();
	private long start_unix_time;
	private int length;
	private String name;
	private String description;
	private long deadline_unix_time;
	private boolean flexible; // what does that imply???
	private String location;
	private int min_attendants;
	private List<Document> key_attendants = new ArrayList<Document>();
	private String group_id;
	private Conditions.Weather weather;
	private AppointmentMsg.Category category;
	private long last_update_time;
	private long start_unix_timeframe;
	private long end_unix_timeframe;
	private List<Long> possible_dates = new ArrayList<Long>(); // maybe generate custom datatype "date" including time, rank
	private double[][][][] time_slots;
	private int dim_people;
	private int dim_days;
	private int dim_slots;
	
	MongoCollection<Document> USER;
	MongoCollection<Document> APPOINTMENT;
	MongoCollection<Document> TIMESLOTS;
	MongoCollection<Document> NEWS;

	
	public Appointment(AppointmentMsg message, MongoDatabase database) {
		/*
		 * Constructor for creating new appointments
		 * TODO: only save fields that are set --> check them first!
		 */
		this.database = database;
		USER = database.getCollection(USER_COLLECTION);
		APPOINTMENT = database.getCollection(APPOINTMENT_COLLECTION);
		TIMESLOTS = database.getCollection(TIMESLOTS_COLLECTION);
		NEWS = database.getCollection(NEWS_COLLECTION);
		this.appointmentID = new ObjectId();
		this.initiator = message.getInitiator();
		addAppointmentIDToUserEntry(initiator);
		logger.debug("AppointmentID after creation: {}", appointmentID);
		for (Person attendee : message.getAttendantsList()) {
			String attendee_email = attendee.getEmail();
			this.attendants.add(new Document(ATTENDEE_KEY, attendee_email));
			addAppointmentIDToUserEntry(attendee_email);
		}
		this.setStart_unix_time(message.getStartUnixTime());
		this.setLength(message.getLength());
		this.setName(message.getName());
		this.setDescription(message.getDescription());
		this.setDeadline_unix_time(message.getDeadlineUnixTime());
		this.flexible = message.getFlexible();
		this.setLocation(message.getLocation());
		this.setMin_attendants(message.getMinAttendants());
		for (Person key_attendee : message.getKeyAttendantsList()) {
			String key_attendee_email = key_attendee.getEmail();
			this.key_attendants.add(new Document(ATTENDEE_KEY, key_attendee_email));
			addAppointmentIDToUserEntry(key_attendee_email);
		}
		this.group_id = message.getGroupId();
		this.setWeather(message.getConditions().getWeather());
		this.setCategory(message.getCategory());
		this.setLast_update_time(Calendar.getInstance().getTimeInMillis());
		this.setStart_unix_timeframe(message.getStartUnixTimeframe());
		this.setEnd_unix_timeframe(message.getEndUnixTimeframe());
	}
	
	public Appointment(ObjectId appointmentIDFromClient, MongoDatabase database) {
		this.database = database;
		USER = database.getCollection(USER_COLLECTION);
		APPOINTMENT = database.getCollection(APPOINTMENT_COLLECTION);
		TIMESLOTS = database.getCollection(TIMESLOTS_COLLECTION);
		NEWS = database.getCollection(NEWS_COLLECTION);
		this.appointmentID = appointmentIDFromClient;
		
		Document appointmentEntry = APPOINTMENT.find(eq(ID, appointmentID)).first();
		this.initiator = appointmentEntry.getString(INITIATOR);
		this.attendants = (List<Document>) appointmentEntry.get(ATTENDANTS);
		this.key_attendants = (List<Document>) appointmentEntry.get(KEY_ATTENDANTS);
		this.setStart_unix_time(appointmentEntry.getLong(START_UNIX_TIME));
		this.setLength(appointmentEntry.getInteger(LENGTH));
		this.setName(appointmentEntry.getString(NAME));
		this.setDescription(appointmentEntry.getString(DESCRIPTION));
		this.setDeadline_unix_time(appointmentEntry.getLong(DEADLINE_UNIX_TIME));
		this.flexible = appointmentEntry.getBoolean(FLEXIBLE, false);
		this.setLocation(appointmentEntry.getString(LOCATION));
		this.setMin_attendants(appointmentEntry.getInteger(MIN_ATTENDANTS, 0));
		this.group_id = appointmentEntry.getString(GROUPID);
		this.setStart_unix_timeframe(appointmentEntry.getLong(START_UNIX_TIMEFRAME));
		this.setEnd_unix_timeframe(appointmentEntry.getLong(END_UNIX_TIMEFRAME));
		
		logger.debug("Reached checkpoint in appointment constructor (ID, database)");
		
		// TODO: check if this works!
		this.setCategory(appointmentEntry.getInteger(CATEGORY));
		this.setWeather(appointmentEntry.getInteger(WEATHER));
		this.setLast_update_time(Calendar.getInstance().getTimeInMillis());
		this.possible_dates = (List<Long>) appointmentEntry.get(POSSIBLE_DATES);
	}
	
	protected void writeParamsInDatabase() {
		Document oldAppointmentEntry = APPOINTMENT.find(eq(ID, appointmentID)).first();
		logger.debug("AppointmentID when writing in database: {}", appointmentID);
		Document appointmentEntry = new Document(ID, appointmentID);
		appointmentEntry.append(INITIATOR, initiator);
		appointmentEntry.append(ATTENDANTS, attendants);
		appointmentEntry.append(KEY_ATTENDANTS, key_attendants);
		appointmentEntry.append(START_UNIX_TIME, getStart_unix_time());
		appointmentEntry.append(LENGTH, getLength());
		appointmentEntry.append(NAME, getName());
		appointmentEntry.append(DESCRIPTION, getDescription());
		appointmentEntry.append(DEADLINE_UNIX_TIME, getDeadline_unix_time());
		appointmentEntry.append(FLEXIBLE, flexible);
		appointmentEntry.append(LOCATION, location);
		appointmentEntry.append(MIN_ATTENDANTS, min_attendants);
		appointmentEntry.append(GROUPID, group_id);
		appointmentEntry.append(START_UNIX_TIMEFRAME, getStart_unix_timeframe());
		appointmentEntry.append(END_UNIX_TIMEFRAME, getEnd_unix_timeframe());
		appointmentEntry.append(LAST_UPDATE_TIME, Calendar.getInstance().getTimeInMillis());
		appointmentEntry.append(POSSIBLE_DATES, possible_dates);
		
		// save only hash code for enums
		appointmentEntry.append(CATEGORY, category.getNumber());
		appointmentEntry.append(WEATHER, weather.getNumber());
				
		if (oldAppointmentEntry == null) {
			// check if initiator is a user
			Document initiatorEntry = USER.find(eq(User.EMAIL, initiator)).first();
			if (initiatorEntry != null) {
				logger.debug("Inserting new appointmentEntry");
				APPOINTMENT.insertOne(appointmentEntry);				
			}
			else {
				logger.error("Trying to create an appointment that was most likely already deleted");
			}
		}
		else {
			logger.debug("Replacing old appointmentEntry");
			APPOINTMENT.replaceOne(eq(ID, appointmentID), appointmentEntry);
		}		
	}
	
	protected void deleteAppointment() {
		Document appointmentEntry = APPOINTMENT.find(eq(ID, appointmentID)).first();
		for (Document attendant : (List<Document>) appointmentEntry.get(ATTENDANTS)) {
			removeAppointmentIDFromUserAndNewsEntry(attendant.getString(ATTENDEE_KEY));
		}
		for (Document attendant : (List<Document>) appointmentEntry.get(KEY_ATTENDANTS)) {
			removeAppointmentIDFromUserAndNewsEntry(attendant.getString(ATTENDEE_KEY));
		}
		removeAppointmentIDFromUserAndNewsEntry(initiator);
		APPOINTMENT.deleteOne(appointmentEntry);
	}
	
	protected void getTimeslotsOfAttendants(boolean only_same_category){
		/*
		 * Dimensions of matrix of timeslots:
		 * 1) 0: 0/1 --> available/na, 1: 0-6 representing category
		 * 2) different people in order: initiator - key_attendants - attendants
		 * 3) different days
		 * 4) different smallest scale timeslots (corresponding to Timeslots.MIN_UNIT)
		 */
		int day_in_ms = 24*3600*1000;
		dim_people = 1 + key_attendants.size() + attendants.size();
		dim_days = (int) Math.ceil((end_unix_timeframe - start_unix_timeframe) / day_in_ms);
		dim_slots = 24 * 3600 * 1000 / Timeslots.MIN_UNIT; // 24*12
		time_slots = new double[2][dim_people][dim_days][dim_slots];
		
		addToTimeslotsMatrix(0, initiator);
		int idx_people = 1;
		for (Document attendee : key_attendants) {
			addToTimeslotsMatrix(idx_people, attendee.getString(ATTENDEE_KEY));
			idx_people++;
		}
		for (Document attendee : attendants) {
			addToTimeslotsMatrix(idx_people, attendee.getString(ATTENDEE_KEY));
			idx_people++;
		}
	}

	private void addToTimeslotsMatrix(int idx_people, String memberEmail) {
		int day_in_ms = 24*3600*1000;
		Document emailEntry = USER.find(eq(User.EMAIL, memberEmail)).first();
		String timeslotsID = emailEntry.getString(User.TIMESLOTSID);
		Document timeslotsEntry = TIMESLOTS.find(eq(Timeslots.ID, timeslotsID)).first();
		for (Document timeslot : (List<Document>) timeslotsEntry.get(Timeslots.TIMESLOTS)) {
			long start_to_slot = timeslot.getLong(Timeslots.SLOT_START_TIME) - start_unix_timeframe;
			int idx_days_start = (int) Math.floor(start_to_slot / day_in_ms);
			int idx_slots_start = (int) Math.floor((start_to_slot % day_in_ms) / Timeslots.MIN_UNIT);
			long in_slot = timeslot.getLong(Timeslots.SLOT_END_TIME) - timeslot.getLong(Timeslots.SLOT_START_TIME);
			int idx_days_end = (int) Math.floor(in_slot / day_in_ms);
			int idx_slots_end = (int) Math.ceil((in_slot % day_in_ms) / Timeslots.MIN_UNIT);
			
			for (int idx_days = idx_days_start; idx_days <= idx_days_end; idx_days++) {
				for (int idx_slots = idx_slots_start; idx_slots <= idx_slots_end; idx_slots++) {
					time_slots[0][idx_people][idx_days][idx_slots] = 1;
					time_slots[1][idx_people][idx_days][idx_slots] = ((AppointmentMsg.Category) timeslot.get(Timeslots.SLOT_CATEGORY)).getNumber();
				}
			}
		}
	}
	
	protected void calculateBestDates() {
		// TODO: switch to nd4j for better performance on tensors --> measure performance!
		// TODO: regard category for different weighting
		
		Array2DRowRealMatrix score = new Array2DRowRealMatrix(new double[dim_days][dim_slots]);
		for (int idx_people = 0; idx_people < dim_people; idx_people++) {
			for (int idx_days = 0; idx_days < dim_days; idx_days++) {
				for (int idx_slots = 0; idx_slots < dim_slots; idx_slots++) {
					time_slots[0][idx_people][idx_days][idx_slots] *= DAY_MASK[idx_slots];
					int idx_slots_end = idx_slots + (int) Math.ceil(length/Timeslots.MIN_UNIT);
					int idx_days_end = (int) Math.floor(idx_slots_end / dim_slots);
					idx_slots_end = idx_slots_end % dim_slots;
					time_slots[0][idx_people][idx_days][idx_slots] += time_slots[0][idx_people][idx_days_end][idx_slots_end];
				}
			}
			score.add(new Array2DRowRealMatrix(time_slots[0][idx_people]));
		}
		ArrayList<int[]> idx_list = new ArrayList<int[]>();
		idx_list.add(new int[] {0,0,0});
		double[][] score_double = score.getData();
		for (int idx_days = 0; idx_days < dim_days; idx_days++) {
			for (int idx_slots = 0; idx_slots < dim_slots; idx_slots++) {
				for (int idx = 0; idx < idx_list.size(); idx++)
				{
					if (score_double[idx_days][idx_slots] > idx_list.get(idx)[2]) {
						idx_list.add(idx, new int[] {idx_days, idx_slots, (int) score_double[idx_days][idx_slots]});
					}
				}
			}
		}
		for (int idx = 0; idx < AMOUNT_POSSIBLE_DATES; idx++) {
			possible_dates.add((long) (start_unix_timeframe + idx_list.get(idx)[0] * 24*3600*1000 + idx_list.get(idx)[1] * 24*12));
		}
	}
	
	
	/*
	 * functions for asking all participants by adding entries to their 
	 * respective news tab
	 */
	protected void askKeyAttendantsForAvailability(long chosen_date) {
		for (Document attendee : key_attendants) {
			String attendee_email = attendee.getString(ATTENDEE_KEY);
			addDateToNewsTab(chosen_date, attendee_email);
		}
	}
	
	protected void askAttendantsForAvailability(long chosen_date) {
		for (Document attendee : attendants) {
			String attendee_email = attendee.getString(ATTENDEE_KEY);
			addDateToNewsTab(chosen_date, attendee_email);
		}
	}


	
	/*
	 *  Answering request sent to all participants by above functions
	 */
	protected void processAnswerToAvailabilityRequest(String email, AppointmentMsg.Answer answer) {
		// add answer to participant's entry
		for (Document attendee : attendants) {
			if (attendee.getString(ATTENDEE_KEY).equals(email)) {
				if (attendee.containsKey(ATTENDEE_STATUS)) {
					// TODO: check whether this works
					attendee.replace(ATTENDEE_STATUS, answer);
				}
				else {
					attendee.append(ATTENDEE_STATUS, answer);
				}
			}
		}
		for (Document attendee : key_attendants) {
			if (attendee.getString(ATTENDEE_KEY).equals(email)) {
				if (attendee.containsKey(ATTENDEE_STATUS)) {
					// TODO: check whether this works
					attendee.replace(ATTENDEE_STATUS, answer);
				}
				else {
					attendee.append(ATTENDEE_STATUS, answer);
				}
			}
		}
		
		// put participant and his answer in news tab of initiator
		addAnswerToNewsTab(email, answer, initiator);
	}
	
	
	/*
	 * Creating messages
	 */
	protected ClientAttendantAppointment getInfoForAttendant() {
		ClientAttendantAppointment.Builder appBuilder = ClientAttendantAppointment.newBuilder();
		logger.debug("AppointmentID: as ObjectId {} - as String {}", appointmentID, appointmentID.toString());
		appBuilder.setId(appointmentID.toString());
		appBuilder.setInitiator(initiator);
		for (int i = 0; i < key_attendants.size(); i++) {
			appBuilder.addAttendants( 
					Person.newBuilder()
					.setEmail(key_attendants.get(i).getString(ATTENDEE_KEY))
					.build());
		}
		for (int i = key_attendants.size(); i < attendants.size(); i++) {
			appBuilder.addAttendants(
					Person.newBuilder()
					.setEmail(attendants.get(i).getString(ATTENDEE_KEY))
					.build());
		}
		if (start_unix_time != 0) {
			appBuilder.setStartUnixTime(start_unix_time);			
		}
		appBuilder.setLength(length);
		appBuilder.setName(name);
		appBuilder.setLocation(location);
		appBuilder.setDescription(description);
		appBuilder.setDeadlineUnixTime(deadline_unix_time);
		appBuilder.setFlexible(flexible);
		appBuilder.setMinAttendants(min_attendants);
		appBuilder.setGroupId(group_id);
		logger.debug("Get info: weather {} - category {}", weather, category);
		appBuilder.setConditions(Conditions.newBuilder().setWeather(weather).build());
		appBuilder.setCategory(category);
		
		ClientAttendantAppointment appMsg = appBuilder.build();
		logger.debug("get info msg:\n{}", appMsg);
		return appMsg;
	}
	
	
	/*
	 * simple functions
	 */
	protected boolean checkInitiator(String email) {
		if (initiator.equals(email)) {
			return true;
		}
		return false;
	}
	
	protected boolean checkAttendants(String email) {
		if (attendants.contains(new Document(ATTENDEE_KEY, email))) {
			return true;
		}
		return false;
	}
	
	protected boolean checkKeyAttendants(String email) {
		if (key_attendants.contains(new Document(ATTENDEE_KEY, email))) {
			return true;
		}
		return false;
	}
	
	
	/*
	 * Initiator functions
	 */
	protected void initAddToAttendants(List<Person> new_attendants) {
		if (new_attendants.isEmpty()) {
			return;
		}
		for (Person attendee : new_attendants) {
			String attendee_email = attendee.getEmail();
			attendants.add(new Document(ATTENDEE_KEY, attendee_email));
			addAppointmentIDToUserEntry(attendee_email);
			// add this new information to everybody's news tab
			Document newAttendeeEntry = new Document(ATTENDEE_KEY, attendee_email);
			for (Document previousAttendants : key_attendants) {
				addToAppointmentListOfSomeonesNewsTab(previousAttendants.getString(ATTENDEE_KEY), News.APPOINTMENT_NEW_ATTENDANTS_LIST, newAttendeeEntry);
			}
			for (Document previousAttendants : attendants) {
				addToAppointmentListOfSomeonesNewsTab(previousAttendants.getString(ATTENDEE_KEY), News.APPOINTMENT_NEW_ATTENDANTS_LIST, newAttendeeEntry);
			}
		}
	}
	
	protected void initAddToKeyAttendants(List<Person> new_attendants) {
		if (new_attendants.isEmpty()) {
			return;
		}
		for (Person attendee : new_attendants) {
			String attendee_email = attendee.getEmail();
			key_attendants.add(new Document(ATTENDEE_KEY, attendee_email));
			addAppointmentIDToUserEntry(attendee_email);
			// add this new information to everybody's news tab
			Document newAttendeeEntry = new Document(ATTENDEE_KEY, attendee_email);
			for (Document previousAttendants : key_attendants) {
				addToAppointmentListOfSomeonesNewsTab(previousAttendants.getString(ATTENDEE_KEY), News.APPOINTMENT_NEW_ATTENDANTS_LIST, newAttendeeEntry);
			}
			for (Document previousAttendants : attendants) {
				addToAppointmentListOfSomeonesNewsTab(previousAttendants.getString(ATTENDEE_KEY), News.APPOINTMENT_NEW_ATTENDANTS_LIST, newAttendeeEntry);
			}
		}
	}
	
	protected void initRemoveParticipants(List<Person> to_be_removed) {
		if (to_be_removed.isEmpty()) {
			return;
		}
		List<Document> begone_total = new ArrayList<Document>();
		for (Person participant_begone : to_be_removed) {
			String participant_begone_email = participant_begone.getEmail();
			Document begone = new Document(ATTENDEE_KEY, participant_begone_email);
			begone_total.add(begone);
			if (attendants.contains(begone)) {
				attendants.remove(begone);
			}
			if (key_attendants.contains(begone)) {
				key_attendants.remove(begone);
			}
			removeAppointmentIDFromUserAndNewsEntry(participant_begone_email);
		}
		// add this new information to everybody's news tab
		for (Document attendant : key_attendants) {
			addToAppointmentListOfSomeonesNewsTab(attendant.getString(ATTENDEE_KEY), News.APPOINTMENT_ATTENDANTS_GONE_LIST, begone_total);
		}
		for (Document attendant : attendants) {
			addToAppointmentListOfSomeonesNewsTab(attendant.getString(ATTENDEE_KEY), News.APPOINTMENT_ATTENDANTS_GONE_LIST, begone_total);
		}
	}
	
	protected void initRemoveParticipant(String to_be_removed_email) {
		if (to_be_removed_email.isEmpty()) {
			return;
		}
		Document begone = new Document(ATTENDEE_KEY, to_be_removed_email);
		if (attendants.contains(begone)) {
			attendants.remove(begone);
		}
		if (key_attendants.contains(begone)) {
			key_attendants.remove(begone);
		}
		removeAppointmentIDFromUserAndNewsEntry(to_be_removed_email);
		
		// add this new information to everybody's news tab
		for (Document attendant : key_attendants) {
			addToAppointmentListOfSomeonesNewsTab(attendant.getString(ATTENDEE_KEY), News.APPOINTMENT_ATTENDANTS_GONE_LIST, begone);
		}
		for (Document attendant : attendants) {
			addToAppointmentListOfSomeonesNewsTab(attendant.getString(ATTENDEE_KEY), News.APPOINTMENT_ATTENDANTS_GONE_LIST, begone);
		}
	}
	
	protected void initRemoveAppointment() {
		/*
		 * 1. delete appointment in participant's user collection's appointment list
		 * 2. add this new information to everybody's news tab
		 */
		for (Document attendant : key_attendants) {
			String attendant_email = attendant.getString(ATTENDEE_KEY);
			removeAppointmentIDFromOnlyUserEntry(attendant_email);
			setAppointmentBoolInSomeonesNewsTab(attendant_email, News.APPOINTMENT_DELETED, true);
		}
		for (Document attendant : attendants) {
			String attendant_email = attendant.getString(ATTENDEE_KEY);
			removeAppointmentIDFromOnlyUserEntry(attendant_email);
			setAppointmentBoolInSomeonesNewsTab(attendant_email, News.APPOINTMENT_DELETED, true);
		}
		APPOINTMENT.findOneAndDelete(eq(ID, appointmentID));
	}

	
	/*
	 * Checking for fields in constants' classes
	 */
	private boolean newsKeyIsPresentInNewsConstants(String news_key) {
		// check if news_key is equal to a key in Constants.News
		boolean isPresent = false;
		for (java.lang.reflect.Field news_field : News.class.getDeclaredFields()) {
			String field_value;
			try {
				if (news_field.getType().equals(String.class)) {
					field_value = (String) news_field.get(null);
					if(field_value.equals(news_key)) {
						isPresent = true;
					}					
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return isPresent;
	}
	
	
	/*
	 * Interacting with user and news collection
	 */
	private void addDateToNewsTab(long date, String email) {
		Document emailEntry = USER.find(eq(User.EMAIL, email)).first();
		if (!emailEntry.containsKey(User.NEWSID)) {
			ObjectId newsID = new ObjectId();
			emailEntry.append(User.NEWSID, newsID);
			NEWS.insertOne(new Document(News.ID, newsID));
			USER.replaceOne(eq(User.EMAIL, email), emailEntry);
		}
		ObjectId newsID = emailEntry.getObjectId(User.NEWSID);
		Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
		if (!newsEntry.containsKey(News.APPOINTMENTS)) {
			newsEntry.append(News.APPOINTMENTS, new ArrayList<Document>());
		}
		List<Document> news_appointment_list = (List<Document>) newsEntry.get(News.APPOINTMENTS);
		// checking for existing appointment entry in news list
		boolean[] entryExists = {false};
		news_appointment_list.forEach(
				element -> {
					if (element.getObjectId(News.APPOINTMENT_LIST_ID) == appointmentID) {
						element.append(News.APPOINTMENT_LIST_DATE, date);
						entryExists[0] = true;
					}
				});
		if (!entryExists[0]) {
			Document newsAppointmentEntry = new Document(News.APPOINTMENT_LIST_ID, appointmentID);
			newsAppointmentEntry.append(News.APPOINTMENT_LIST_DATE, date);
			news_appointment_list.add(newsAppointmentEntry);
		}
		newsEntry.replace(News.APPOINTMENTS, news_appointment_list);
		NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
	}
	
	private void setAppointmentBoolInSomeonesNewsTab(String target_email, String news_key, boolean bool_value) {
		Document emailEntry = USER.find(eq(User.EMAIL, target_email)).first();
		boolean isPresent = newsKeyIsPresentInNewsConstants(news_key);
		
		if (!isPresent || emailEntry == null) {
			logger.error("Insufficient parameters used for addToListOfSomeonesNewsTab");
			return;
		}
		if (!emailEntry.containsKey(User.NEWSID)) {
			ObjectId newsID = new ObjectId();
			emailEntry.append(User.NEWSID, newsID);
			NEWS.insertOne(new Document(News.ID, newsID));
			USER.replaceOne(eq(User.EMAIL, target_email), emailEntry);
		}
		ObjectId newsID = emailEntry.getObjectId(User.NEWSID);
		Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
		if (!newsEntry.containsKey(News.APPOINTMENTS)) {
			newsEntry.append(News.APPOINTMENTS, new ArrayList<Document>());
		}
		List<Document> news_appointment_list = (List<Document>) newsEntry.get(News.APPOINTMENTS);
		// checking for existing appointment entry in news list
		boolean[] entryExists = {false};
		news_appointment_list.forEach(
				element -> {
					if (element.getObjectId(News.APPOINTMENT_LIST_ID) == appointmentID) {
						if (element.containsKey(news_key)) {
							element.replace(news_key, bool_value);
						}
						else {
							element.append(news_key, bool_value);
						}
						entryExists[0] = true;
					}
				});
		if (!entryExists[0]) {
			Document newsAppointmentEntry = new Document(News.APPOINTMENT_LIST_ID, appointmentID);
			newsAppointmentEntry.append(news_key, bool_value);
			news_appointment_list.add(newsAppointmentEntry);
		}
		newsEntry.replace(News.APPOINTMENTS, news_appointment_list);
		NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
	}

	
	protected void addToAppointmentListOfSomeonesNewsTab(String target_email, String list, Document singleEntry) {
		List<Document> singleEntryList = new ArrayList<Document>();
		singleEntryList.add(singleEntry);
		addToAppointmentListOfSomeonesNewsTab(target_email, list, singleEntryList);
	}
	
	protected void addToAppointmentListOfSomeonesNewsTab(String target_email, String news_list, List<Document> multipleEntries) {
		/*
		 * Function for adding several entries to a list in the appointment list in target_email's news tab
		 */
		Document emailEntry = USER.find(eq(User.EMAIL, target_email)).first();
		if (emailEntry == null) {
			logger.error("Couldn't find account for requested user name");
			return;
		}
		boolean isPresent = newsKeyIsPresentInNewsConstants(news_list);
		if (!isPresent) {
			logger.error("Insufficient parameters used for addToListOfSomeonesNewsTab");
			return;
		}
		if (!emailEntry.containsKey(User.NEWSID)) {
			ObjectId newsID = new ObjectId();
			emailEntry.append(User.NEWSID, newsID);
			NEWS.insertOne(new Document(News.ID, newsID));
			USER.replaceOne(eq(User.EMAIL, target_email), emailEntry);
		}
		ObjectId newsID = emailEntry.getObjectId(User.NEWSID);
		Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
		if (!newsEntry.containsKey(News.APPOINTMENTS)) {
			newsEntry.append(News.APPOINTMENTS, new ArrayList<Document>());
		}
		List<Document> news_appointment_list = (List<Document>) newsEntry.get(News.APPOINTMENTS);
		// checking for existing appointment entry in news list
		boolean[] entryExists = {false};
		news_appointment_list.forEach(
				appointment_element -> {
					if (appointment_element.getObjectId(News.APPOINTMENT_LIST_ID) == appointmentID) {
						List<Document> appointment_some_list = (List<Document>) appointment_element.get(news_list);
						for (Document listEntry : multipleEntries) {
							appointment_some_list.add(listEntry);							
						}
						entryExists[0] = true;
					}
				});
		if (!entryExists[0]) {
			Document newsAppointmentEntry = new Document(News.APPOINTMENT_LIST_ID, appointmentID);
			List<Document> appointment_some_list = new ArrayList<Document>();
			for (Document listEntry : multipleEntries) {
				appointment_some_list.add(listEntry);							
			}
			newsAppointmentEntry.append(news_list, appointment_some_list);
			news_appointment_list.add(newsAppointmentEntry);
		}
		newsEntry.replace(News.APPOINTMENTS, news_appointment_list);
		NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
	}
	
	private void addAnswerToNewsTab(String participant_email, AppointmentMsg.Answer answer, String target_email) {
		if (answer == AppointmentMsg.Answer.NONE) {
			return;
		}
		Document emailEntry = USER.find(eq(User.EMAIL, target_email)).first();
		if (!emailEntry.containsKey(User.NEWSID)) {
			ObjectId newsID = new ObjectId();
			emailEntry.append(User.NEWSID, newsID);
			NEWS.insertOne(new Document(News.ID, newsID));
			USER.replaceOne(eq(User.EMAIL, target_email), emailEntry);
		}
		ObjectId newsID = emailEntry.getObjectId(User.NEWSID);
		Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
		if (!newsEntry.containsKey(News.APPOINTMENTS)) {
			newsEntry.append(News.APPOINTMENTS, new ArrayList<Document>());
		}
		List<Document> news_appointment_list = (List<Document>) newsEntry.get(News.APPOINTMENTS);
		// checking for existing appointment entry in news list
		boolean[] entryExists = {false, false};
		news_appointment_list.forEach(
				news_element -> {
					if (news_element.getObjectId(News.APPOINTMENT_LIST_ID) == appointmentID) {
						List<Document> part_answer_list = (List<Document>) news_element.get(News.APPOINTMENT_ANSWER_NEWS_LIST);
						part_answer_list.forEach(
								part_element -> {
									if (part_element.getString(News.APPOINTMENT_LIST_ATTENDEE_KEY) == participant_email) {
										// if status is not protobuf's default (None for Answer field)
										if (part_element.get(News.APPOINTMENT_LIST_ATTENDEE_STATUS) != AppointmentMsg.Answer.NONE) {
											part_element.replace(News.APPOINTMENT_LIST_ATTENDEE_STATUS, answer);											
										}
										else {
											// assuming no entry is made for None! --> filter at beginning
											part_element.append(News.APPOINTMENT_LIST_ATTENDEE_STATUS, answer);
										}
										entryExists[1] = true;
									}
								});
						if (entryExists[1]) {
							news_element.replace(News.APPOINTMENT_ANSWER_NEWS_LIST, part_answer_list);							
						}
						else {
							// create entry for answer
							Document part_answer = new Document(News.APPOINTMENT_LIST_ATTENDEE_KEY, participant_email);
							part_answer.append(News.APPOINTMENT_LIST_ATTENDEE_STATUS, answer);
							part_answer_list.add(part_answer);
						}
						entryExists[0] = true;
					}
				});
		if (!entryExists[0]) {
			Document newsAppointmentEntry = new Document(News.APPOINTMENT_LIST_ID, appointmentID);
			List<Document> answer_list = new ArrayList<Document>();
			Document answerlistEntry = new Document(News.APPOINTMENT_LIST_ATTENDEE_KEY, participant_email);
			answerlistEntry.append(News.APPOINTMENT_LIST_ATTENDEE_STATUS, answer);
			newsAppointmentEntry.append(News.APPOINTMENT_ANSWER_NEWS_LIST, answer_list);
			news_appointment_list.add(newsAppointmentEntry);
		}
		newsEntry.replace(News.APPOINTMENTS, news_appointment_list);
		NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
	}
	
	protected void addAppointmentIDToUserEntry(String email) {
		/*
		 * Assuming this function is called first after creation of appointment
		 */
		Document emailEntry = USER.find(eq(User.EMAIL, email)).first();
		if (!emailEntry.containsKey(User.APPOINTMENTS)) {
			emailEntry.append(User.APPOINTMENTS, new ArrayList<Document>());
		}
		List<Document> appointments_list = (List<Document>) emailEntry.get(User.APPOINTMENTS);
		appointments_list.add(new Document(User.APPOINTMENT_LIST_ID, appointmentID));
		emailEntry.replace(User.APPOINTMENTS, appointments_list);
		USER.replaceOne(eq(User.EMAIL, email), emailEntry);
		
		// same procedure with news list but in news collection
		if (!emailEntry.containsKey(User.NEWSID)) {
			ObjectId newsID = new ObjectId();
			emailEntry.append(User.NEWSID, newsID);
			NEWS.insertOne(new Document(News.ID, newsID));
			USER.replaceOne(eq(User.EMAIL, email), emailEntry);
		}
		ObjectId newsID = emailEntry.getObjectId(User.NEWSID);
		Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
		if (!newsEntry.containsKey(News.APPOINTMENTS)) {
			newsEntry.append(News.APPOINTMENTS, new ArrayList<Document>());
		}
		List<Document> news_appointment_list = (List<Document>) newsEntry.get(News.APPOINTMENTS);
		news_appointment_list.add(new Document(User.APPOINTMENT_LIST_ID, appointmentID));
		newsEntry.replace(News.APPOINTMENTS, news_appointment_list);
		NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
	}
	
	private void removeAppointmentIDFromUserAndNewsEntry(String email) {
		Document emailEntry = USER.find(eq(User.EMAIL, email)).first();
		if (emailEntry == null) {
			return;
		}
		Document to_be_removed = new Document(User.APPOINTMENT_LIST_ID, appointmentID);
		if (emailEntry.containsKey(User.APPOINTMENTS)) {
			List<Document> appointments_list = (List<Document>) emailEntry.get(User.APPOINTMENTS);
			appointments_list.remove(to_be_removed);
			emailEntry.replace(User.APPOINTMENTS, appointments_list);
			USER.replaceOne(eq(User.EMAIL, email), emailEntry);
		}
		if (emailEntry.containsKey(User.NEWSID)) {
			ObjectId newsID = emailEntry.getObjectId(User.NEWSID);
			Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
			if (newsEntry.containsKey(News.APPOINTMENTS)) {
				List<Document> news_appointment_list = (List<Document>) newsEntry.get(News.APPOINTMENTS);
				news_appointment_list.removeIf(element -> (element.getObjectId(News.APPOINTMENT_LIST_ID) == appointmentID));			
				newsEntry.replace(News.APPOINTMENTS, news_appointment_list);
				NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
			}
		}
	}
	
	private void removeAppointmentIDFromOnlyUserEntry(String email) {
		Document emailEntry = USER.find(eq(User.EMAIL, email)).first();
		if (emailEntry == null) {
			return;
		}
		Document to_be_removed = new Document(User.APPOINTMENT_LIST_ID, appointmentID);
		if (emailEntry.containsKey(User.APPOINTMENTS)) {
			List<Document> appointments_list = (List<Document>) emailEntry.get(User.APPOINTMENTS);
			appointments_list.remove(to_be_removed);
			emailEntry.replace(User.APPOINTMENTS, appointments_list);
			USER.replaceOne(eq(User.EMAIL, email), emailEntry);
		}
	}
	
	
	
	// getters and setters
	protected ObjectId getAppointmentID() {
		return appointmentID;
	}
	
	// USE WITH CARE!
	protected void setAppointmentID(String appointmentIDString) {
		appointmentID = new ObjectId(appointmentIDString);
	}
	
	protected String getInitiator() {
		return initiator;
	}
	
	protected List<Document> getAttendants() {
		return attendants;
	}
	
	protected long getStart_unix_time() {
		return start_unix_time;
	}

	protected void setStart_unix_time(long start_unix_time) {
		this.start_unix_time = start_unix_time;
	}

	protected int getLength() {
		return length;
	}

	protected void setLength(int length) {
		this.length = length;
	}

	protected String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}
	
	protected String getDescription() {
		return description;
	}

	protected void setDescription(String description) {
		this.description = description;
	}

	protected long getDeadline_unix_time() {
		return deadline_unix_time;
	}

	protected void setDeadline_unix_time(long deadline_unix_time) {
		this.deadline_unix_time = deadline_unix_time;
	}

	protected String getLocation() {
		return location;
	}

	protected void setLocation(String location) {
		this.location = location;
	}

	protected int getMin_attendants() {
		return min_attendants;
	}

	protected void setMin_attendants(int min_attendants) {
		this.min_attendants = min_attendants;
	}

	protected Conditions.Weather getWeather() {
		return weather;
	}

	protected void setWeather(Conditions.Weather weather) {
		this.weather = weather;
	}
	
	protected void setWeather(int weather_hash) {
		this.weather = Conditions.Weather.forNumber(weather_hash);
	}

	protected AppointmentMsg.Category getCategory() {
		return category;
	}

	protected void setCategory(AppointmentMsg.Category category) {
		this.category = category;
	}
	
	protected void setCategory(int category_hash) {
		this.category = AppointmentMsg.Category.forNumber(category_hash);
	}
	
	protected long getLast_update_time() {
		return last_update_time;
	}

	protected void setLast_update_time(long last_update_time) {
		this.last_update_time = last_update_time;
	}
	
	protected long getStart_unix_timeframe() {
		return start_unix_timeframe;
	}

	protected void setStart_unix_timeframe(long start_unix_timeframe) {
		this.start_unix_timeframe = start_unix_timeframe;
	}
	
	protected long getEnd_unix_timeframe() {
		return end_unix_timeframe;
	}

	protected void setEnd_unix_timeframe(long end_unix_timeframe) {
		this.end_unix_timeframe = end_unix_timeframe;
	}

	protected List<Long> getPossible_dates() {
		return possible_dates;
	}
	
	protected long getChosenDate(int idx) {
		return possible_dates.get(idx);
	}
	
}
