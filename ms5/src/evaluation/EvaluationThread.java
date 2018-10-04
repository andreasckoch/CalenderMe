package evaluation;

import java.io.IOException;

import client.ClientConnection;
import client.KVStore;

public class EvaluationThread implements Runnable {
	
	ClientConnection clientConnection;
	KVStore kvstore;
	String key;
	String value;
	String[][] keymap;
	String ID;
	Evaluation e;

	EvaluationThread(ClientConnection clientConnection, String[][] keymap, String ID, Evaluation e) {
		this.clientConnection = clientConnection;
		this.kvstore = new KVStore(clientConnection);
		this.keymap = keymap;
		this.ID = ID;
		this.e = e;
		try {
			this.clientConnection.connect();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void run() {
		//start evaluation of put calls
		long start = System.nanoTime();
		for (int j = 0; j < keymap.length; j++) {
			try {
				kvstore.put(keymap[j][0] + ID, keymap[j][1]);
			} catch (Exception e) {
				System.out.println("keymap ist falsch!");
				e.printStackTrace();
			}
		}
		long end = System.nanoTime();
		long duration = end - start;
		e.durationMapPut.replace(this, e.durationMapPut.get(this).doubleValue() + (double) duration);
		
		//start evaluation of get calls
		start = System.nanoTime();
		for (int j = 0; j < keymap.length; j++) {
			try {
				kvstore.get(keymap[j][0] + ID);
			} catch (Exception e) {
				System.out.println("keymap ist falsch!");
				e.printStackTrace();
			}
		}
		end = System.nanoTime();
		long duration2 = end - start;
		e.durationMapPut.replace(this, e.durationMapGet.get(this).doubleValue() + (double) duration2);
	}

}
