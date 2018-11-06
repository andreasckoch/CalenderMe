package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.MongoCollection;

import logger.Constants;
import message.MessageInterface;
import message.LoginMessage;
import message.MessageInterface.MESSAGETYPE;

public class LoginHandler extends Handler {
	
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	
	private LoginMessage message;

	public LoginHandler(LoginMessage message) {
		this.message = message;
		database = super.getDatabase();
	}

	public MessageInterface process() {
		
		MongoCollection<Document> login = database.getCollection("login");
		
		if (this.message.getMessageType() == MESSAGETYPE.LOGIN_REQUEST) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first();

			if (emailEntry != null) {

				return new LoginMessage(MESSAGETYPE.OPERATION_SUCCESS);
			}
		}
		
		return new LoginMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
