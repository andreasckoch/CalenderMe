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
	public final static String PROFIL_COLLECTION = "profil";

}
