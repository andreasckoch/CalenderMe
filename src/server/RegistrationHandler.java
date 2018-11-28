package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

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

		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();

			if (emailEntry == null) {

				Document document = new Document("email", message.getEmail()).append("password", message.getPw());
				login.insertOne(document);

				return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_DELETE) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();

			if (emailEntry != null) {

				logger.info("Delete: {}", emailEntry);
				login.deleteOne(emailEntry);
				return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_MODIFICATION_EMAIL) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();
			logger.debug("Email entry before replace: {}, {}, {}", emailEntry.get("_id"), emailEntry.get("email"), emailEntry.get("password"));

			if (emailEntry != null) {
				if (emailEntry.get("password").equals(message.getPw())) {
					
					emailEntry.replace("email", message.getEmail(), message.getChangedField());
					login.replaceOne(eq("email", message.getEmail()), emailEntry);
					
					return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
				}
			}
		}

		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_MODIFICATION_PW) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first(); 
	
			if (emailEntry != null) {

				if (emailEntry.get("password").equals(message.getPw())) {
					
					emailEntry.replace("password", message.getPw(), message.getChangedField());
					login.replaceOne(eq("email", message.getEmail()), emailEntry);
					
					return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
				}
			}
		}
		return new RegistrationMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
