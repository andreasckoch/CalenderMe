package communicate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SmartyTestCommunicationWithCommandLine {

	public static void main(String[] args) {
		String mongod = "C:\\Program Files\\MongoDB\\Server\\4.0\\bin\\mongod.exe";
		String mongo = "C:\\Program Files\\MongoDB\\Server\\4.0\\bin\\mongo.exe";
		String insert = "db.shit.insert({whats:\"down\"})";

		Runtime run = Runtime.getRuntime();

		Process pr_mongod = null;
		Process pr_mongo = null;
		Process pr_insert = null;
		try {
			pr_mongod = run.exec(mongod);
			pr_mongo = run.exec(mongo);
			pr_insert = run.exec(insert);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			pr_mongod.waitFor();
			pr_mongo.waitFor();
			pr_insert.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BufferedReader buf_mongod = new BufferedReader(new InputStreamReader(pr_mongod.getInputStream()));
		BufferedReader buf_mongo = new BufferedReader(new InputStreamReader(pr_mongo.getInputStream()));
		BufferedReader buf_insert = new BufferedReader(new InputStreamReader(pr_insert.getInputStream()));
		
		String line = "";

		try {
			while ((line=buf_mongo.readLine())!=null || (line=buf_insert.readLine())!=null) {				
			System.out.println(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
