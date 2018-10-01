package start;

//port log4j;

public class Startup {
	
	public static int startupSelectionDummy;

	public static void main(String[] args) {
		/*
		 * input intent to register or login (default = login)
		 */
		startupInput(args[0]);
	}

	public static int startupInput(String arg) {
		if (arg.equals("login")) {
			return loginScreen();
			
		} else if (arg.equals("register")) {
			return registerScreen();
		}
		else {
			System.out.println("Close application.");
			return 0;
		}
	}

	// enters login screen
	protected static int loginScreen() {
		return 1;
	}

	// enters registration screen
	protected static int registerScreen() {
		return 2;

	}

}
