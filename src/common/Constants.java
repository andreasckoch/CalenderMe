package common;

public class Constants {

	// logger configuration and log files
	static {
		System.setProperty("log4j.configurationFile", "log4j2.properties.xml");
	}
	public static String APP_NAME = "CalenderMe";
	public static String SERVER_NAME = "CalenderServer";
	public static String TEST_NAME = "TEST";
	public static String COMMUNICATION_NAME = "Communication";
	
	// database and its collections (need to be created beforehand)
	public final static String DATABASE = "calenderDB";
	public final static String USER_COLLECTION = "user";
	public final static String LOGIN_COLLECTION = "login";
	public final static String PROFILE_COLLECTION = "profile";
	public final static String GROUP_COLLECTION = "groups";
	public final static String TIMESLOTS_COLLECTION = "timeslots";
	public final static String APPOINTMENT_COLLECTION = "appointments";
	public final static String NEWS_COLLECTION = "news";
	
	

	// MongeDB hierarchy parameters
	// appointment entry fields
	public class User {
		public final static String ID = "_id";
		public final static String EMAIL = "email";
		public final static String LOGIN = "loginID";
		public final static String PROFILE = "profileID";
		public final static String TIMESLOTSID = "timeslotsID";
		public final static String GROUPS = "groups";
		public final static String GROUPS_LIST_ID = "groupID";
		public final static String APPOINTMENTS = "appointments";
		public final static String APPOINTMENT_LIST_ID = "appointmentID";
		public final static String NEWSID = "newsID";  // new appointment/group
	}
	
	public class LoginDB {
		public final static String ID = "_id";
		public final static String PW = "password";
	}
	
	public class ProfileDB {
		public final static String ID = "_id";
		public final static String NAME = "name";
		public final static String LOCATION = "location";
		public final static String BIO = "bio";
		public final static String ORGANISATION = "organisation";
	}
	
	public class News {
		public final static String ID = "_id";
		public final static String GROUPS = "groups";
		public final static String GROUPS_LIST_ID = "groupID";
		public final static String APPOINTMENTS = "appointments";
		public final static String APPOINTMENT_LIST_ID = "appointmentID";
		public final static String APPOINTMENT_LIST_DATE = "chosen_date";
		public final static String APPOINTMENT_NEW_ATTENDANTS_LIST = "new_attendants";
		public final static String APPOINTMENT_ATTENDANTS_GONE_LIST = "attendants_gone";  // list of attendants removed or exited
		public final static String APPOINTMENT_DELETED = "deleted";  // deleted appointment
		public final static String APPOINTMENT_ANSWER_NEWS_LIST = "attendants_answers"; // list of attendants answers
		public final static String APPOINTMENT_LIST_ATTENDEE_KEY = "email";
		public final static String APPOINTMENT_LIST_ATTENDEE_STATUS = "status";
	}
	
	public class Groups {
		public final static String ID = "_id";
		public final static String EMAIL = "email";
		public final static String NAME = "name";
		public final static String DESCRIPTION = "description";
		public final static String ADMINS = "admins";
		public final static String ADMINS_LIST_EMAIL = "email";
		public final static String MEMBERS = "members";
		public final static String MEMBERS_LIST_EMAIL = "email";
	}
	
	public class Timeslots {
		public final static String ID = "_id";
		public final static String TIMESLOTS = "timeslots";
		public final static String SLOT_START_TIME = "start_time";
		public final static String SLOT_END_TIME = "end_time";
		public final static String SLOT_CATEGORY = "category";
		public final static String TEMPLATE = "template";
		public final static int TEMPLATE_LENGTH = 1209600000;  // 2 weeks in ms
		public final static int MIN_UNIT = 300000;  // 5 min in ms
		
	}
	
	public static class Appointments {
		public final static String ID = "_id";
		public final static String INITIATOR = "initiator";
		public final static String ATTENDANTS = "attendants";
		public final static String KEY_ATTENDANTS = "key_attendants";
		public final static String ATTENDEE_KEY = "email";
		public final static String ATTENDEE_STATUS = "status";
		public final static String START_UNIX_TIME = "start_unix_time";
		public final static String LENGTH = "length";
		public final static String NAME = "name";
		public final static String DESCRIPTION = "description";
		public final static String DEADLINE_UNIX_TIME = "deadline_unix_time";
		public final static String FLEXIBLE = "flexible";
		public final static String LOCATION = "location";
		public final static String MIN_ATTENDANTS = "min_attendants";
		public final static String GROUPID = "groupID";
		public final static String CATEGORY = "category";
		public final static String WEATHER = "weather";
		public final static String LAST_UPDATE_TIME = "last_update_time";
		public final static String START_UNIX_TIMEFRAME = "start_unix_timeframe";
		public final static String END_UNIX_TIMEFRAME = "end_unix_timeframe";
		public final static String POSSIBLE_DATES = "possible_dates";

		// parameters for matrix calculation
		public final static double KEY_ATTENDANTS_WEIGHT = 2.0;
		public final static long MAX_TIMEFRAME = 3628800000L; // 6 weeks in ms
		public final static int AMOUNT_POSSIBLE_DATES = 10;
		public final static double[] DAY_MASK = new double[] {
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 1.0, 0.1, 0.1, 0.5, 0.1, 0.1, 
				0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1
				
		};
	}
	
	
	// TODO: remaining MongoDB hierarchy as parameters
	
}
