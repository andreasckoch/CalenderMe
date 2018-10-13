package server;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

import common.Constants;

public abstract class Handler {
	
	protected MongoDatabase database;
	private MongoClient mongoClient;
	
	protected MongoDatabase getDatabase(){
		mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		return mongoClient.getDatabase(Constants.DATABASE);
	}
	
	protected void closeMongoClient() {
		mongoClient.close();
	}
	
}