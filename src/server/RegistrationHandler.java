package server;

import static com.mongodb.client.model.Filters.*;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import message.MessageInterface;
import message.RegistrationMessage;
import message.RegistrationMessageInterface.MESSAGETYPE;

public class RegistrationHandler extends Handler {

	private RegistrationMessage message;

	public RegistrationHandler(RegistrationMessage message) {
		this.message = (RegistrationMessage) message;
		database = super.getDatabase();
	}

	public MessageInterface process() {
		if (this.message.getMessageType() == MESSAGETYPE.REGISTRATION_REQUEST) {
			MongoCollection<Document> login = database.getCollection("login");

			if (login.find(eq("email", message.getEmail())) != null) {

				Document document = new Document("email", message.getEmail()).append("password", message.getPw());
				login.insertOne(document);

				return new RegistrationMessage(MESSAGETYPE.REGISTRATION_SUCCESS);
			}
		}
		return new RegistrationMessage(MESSAGETYPE.REGISTRATION_FAILED);
	}

}
