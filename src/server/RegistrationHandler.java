package server;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import message.MessageInterface;
import message.MessageInterface.MESSAGETYPE;
import message.RegistrationMessage;

public class RegistrationHandler extends Handler {

	private RegistrationMessage message;

	public RegistrationHandler(RegistrationMessage message) {
		this.message = (RegistrationMessage) message;
		database = super.getDatabase();
	}

	public MessageInterface process() {
		
		MongoCollection<Document> login = database.getCollection("login");
		
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_REQUEST) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();

			if (emailEntry == null) {

				Document document = new Document("email", message.getEmail()).append("password", message.getPw());
				login.insertOne(document);

				return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_DELETE_REQUEST) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();

			if (emailEntry != null) {

				login.deleteOne(eq("email", message.getEmail()));

				return new RegistrationMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		return new RegistrationMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
