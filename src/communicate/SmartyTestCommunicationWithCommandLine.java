package communicate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SmartyTestCommunicationWithCommandLine {

	public static void main(String[] args) {
		String cmd = "ls -al";
		String test = "python ../../test/test.py";

		Runtime run = Runtime.getRuntime();

		Process pr = null;
		Process pr2 = null;
		try {
			pr = run.exec(test);
			pr2 = run.exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			pr.waitFor();
			pr2.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		BufferedReader buf2 = new BufferedReader(new InputStreamReader(pr2.getInputStream()));

		String line = "";
		String line2 = "";

		try {
			while ((line=buf.readLine())!=null || (line2=buf2.readLine())!=null) {				
			System.out.println(line);
			System.out.println(line2);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
