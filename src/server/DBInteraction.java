package server;

import com.mongodb.DB;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import message.MessageInterface;
import message.RegistrationMessage;
import common.Constants;

public class DBInteraction {
	
	private DB database;

	public DBInteraction() {
		
	}
	
	public MessageInterface processMessage(MessageInterface message) {
		MessageInterface returnMessage = null;
		MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		database = mongoClient.getDB(Constants.DATABASE);
		
		if (message instanceof RegistrationMessage) {
			RegistrationHandler registrationHandler = new RegistrationHandler(message);
			returnMessage = registrationHandler.process();
			
		}
		
		return returnMessage;
	}
	
}
