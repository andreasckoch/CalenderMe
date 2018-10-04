package evaluation;

import java.util.HashMap;

import app_kvServer.KVServer;
import client.ClientConnection;
import ecs.Ecs;

public class Evaluation {

	public HashMap<EvaluationThread, Double> durationMapPut = new HashMap<EvaluationThread, Double>();

	public HashMap<EvaluationThread, Double> durationMapGet = new HashMap<EvaluationThread, Double>();
	
	public Evaluation() {
		
	}
	
	public void evaluate(int cacheSize, String cacheStrategy, int numberOfServers, int numberOfClients, int operationsPerClient) {
		Ecs EvaluationECS = new Ecs("config");
		EvaluationECS.start();
		
		//Start Server-Instances
		for(int i=0; i<numberOfServers; i++) {
			new KVServer(50000+i);
		}
		
		try {
			EvaluationECS.initService(numberOfServers, cacheSize, cacheStrategy);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		
		
		String[][] keymap = new String[operationsPerClient][2];
		
		for(int i = 0; i<keymap.length; i++){
			keymap[i][0] = "key"+i;
			keymap[i][1] = "value"+i;
		}
		
		//Initialize clients
		for(int i = 0; i<numberOfClients;i++) {
			EvaluationThread clientthread = new EvaluationThread(new ClientConnection("127.0.0.1", 50000), keymap, "Client"+i, this);
			durationMapPut.put(clientthread, 0.0);
			durationMapGet.put(clientthread, 0.0);
			new Thread(clientthread).start();
		}

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//add up all individual time values of every respective thread (PUT)
		double sumPut = 0.0;
		for (double f : durationMapPut.values()) {
		    sumPut += f;
		}
		/*
		//add up all individual time values of every respective thread (GET)
		double sumGet = 0.0;
		for (double f : durationMapGet.values()) {
		    sumGet += f;
		}
		*/
		System.out.println(sumPut/1000000000);
		//System.out.println(sumGet/1000000000);
	}
	
	public static void main(String args[]) {
		Evaluation e = new Evaluation();
		
		//e.evaluate(10, "FIFO", 1, 5, 20);
		e.evaluate(10, "FIFO", 10, 10, 50);
		System.out.print("Ende");
	}
	
}