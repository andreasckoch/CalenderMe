package logger;

public class Constants {

    static {
        System.setProperty("log4j.configurationFile", "log4j2.properties.xml");
    }

    public static String APP_NAME = "CloudDB";
    public static String SERVER_NAME= "kvServer";
    public static String TEST_NAME= "tests";
    public static String ECS_NAME="ecs";

}