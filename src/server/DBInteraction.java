package server;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import message.MessageInterface;

public class DBInteraction {

	public DBInteraction() {
		
	}
	
	public MessageInterface processMessage(MessageInterface message) {
		MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		return null;
	}
	
}
