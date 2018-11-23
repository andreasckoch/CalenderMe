package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import common.Constants;
import message.MessageInterface;
import message.MessageInterface.MESSAGETYPE;
import message.RegistrationMessage;

public class RegistrationHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private RegistrationMessage message;

	public RegistrationHandler(RegistrationMessage message) {
		this.message = (RegistrationMessage) message;
		database = super.getDatabase();
	}

	public MessageInterface process() {

		MongoCollection<Document> login = database.getCollection(Constants.LOGIN_COLLECTION);
		MongoCollection<Document> profile = database.getCollection(Constants.PROFILE_COLLECTION);
		MongoCollection<Document> user = database.getCollection(Constants.USER_COLLECTION);

		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION) {

			Document emailEntry = user.find(eq("email", message.getEmail())).first();

			if (emailEntry == null) {

				logger.debug("Create: {}", message.getEmail());
				ObjectId loginID = new ObjectId();
				ObjectId profileID = new ObjectId();
				
				Document userEntry = new Document("email", message.getEmail());
				userEntry.append("loginID", loginID);
				userEntry.append("profileID", profileID);
				user.insertOne(userEntry);
				
				Document loginEntry = new Document("_id", loginID);
				loginEntry.append("password", message.getPw());
				login.insertOne(loginEntry);
				
				Document profileEntry = new Document("_id", profileID);
				profile.insertOne(profileEntry);

				return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_DELETE) {

			Document emailEntry = user.find(eq("email", message.getEmail())).first();

			if (emailEntry != null) {

				logger.debug("Delete: {}", emailEntry);
				
				ObjectId loginID = emailEntry.getObjectId("loginID");
				ObjectId profileID = emailEntry.getObjectId("profileID");
				
				Document loginEntry = login.find(eq("_id", loginID)).first();
				logger.debug("Delete: {}", loginEntry);
				login.deleteOne(loginEntry);
				
				Document profileEntry = profile.find(eq("_id", profileID)).first();
				logger.debug("Delete: {}", profileEntry);
				profile.deleteOne(profileEntry);
				
				user.deleteOne(emailEntry);
				
				return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_MODIFICATION_EMAIL) {

			Document emailEntry = user.find(eq("email", message.getEmail())).first();
			logger.debug("Email entry before replace: {}, {}, {}", emailEntry.get("_id"), emailEntry.get("email"), emailEntry.get("password"));

			if (emailEntry != null) {
				
				ObjectId loginID = (ObjectId) emailEntry.get("loginID");
				Document loginEntry = login.find(eq("_id", loginID)).first();
				
				if (loginEntry.get("password").equals(message.getPw())) {
					
					emailEntry.replace("email", message.getEmail(), message.getChangedField());
					user.replaceOne(eq("email", message.getEmail()), emailEntry);
					
					return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
				}
			}
		}

		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_MODIFICATION_PW) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first(); 
	
			if (emailEntry != null) {

				ObjectId loginID = (ObjectId) emailEntry.get("loginID");
				Document loginEntry = login.find(eq("_id", loginID)).first();
				
				if (loginEntry.get("password").equals(message.getPw())) {
					
					loginEntry.replace("password", message.getPw(), message.getChangedField());
					login.replaceOne(eq("email", message.getEmail()), emailEntry);
					
					return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
				}
			}
		}
		return new RegistrationMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
