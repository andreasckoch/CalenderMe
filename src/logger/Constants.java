package logger;

public class Constants {

    static {
        System.setProperty("log4j.configurationFile", "log4j2.properties.xml");
    }

    public static String APP_NAME = "CalenderMe";
    public static String SERVER_NAME= "CalenderServer";
    public static String TEST_NAME= "TEST";
    public static String COMMUNICATION_NAME = "Communication";

}