package server;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import message.LoginMessage;
import message.MessageInterface;
import message.MessageInterface.MESSAGETYPE;
import message.RegistrationMessage;

public class LoginHandler extends Handler {

	private LoginMessage message;

	public LoginHandler(LoginMessage message) {
		this.message = (LoginMessage) message;
		database = super.getDatabase();
	}

	public MessageInterface process() {

		MongoCollection<Document> login = database.getCollection("login");

		if (this.message.getMessageType() == MESSAGETYPE.LOGIN_REQUEST) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();

			if (true) {
				return new LoginMessage(MESSAGETYPE.OPERATION_SUCCESS);
				// TODO handle case with matching credentials
			} else {
				return new LoginMessage(MESSAGETYPE.OPERATION_FAILED);
			}

		} else {
			return new LoginMessage(MESSAGETYPE.LOGIN_ERROR);
		}

	}
}
