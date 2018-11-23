package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import common.Constants;
import message.MessageInterface;
import message.LoginMessage;
import message.MessageInterface.MESSAGETYPE;

public class LoginHandler extends Handler {

	@SuppressWarnings({ "unused" })
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private LoginMessage message;

	public LoginHandler(LoginMessage message) {
		this.message = message;
		database = super.getDatabase();
	}

	public MessageInterface process() {

		MongoCollection<Document> login = database.getCollection(common.Constants.LOGIN_COLLECTION);
		MongoCollection<Document> user = database.getCollection(common.Constants.USER_COLLECTION);

		if (this.message.getMessageType() == MESSAGETYPE.LOGIN) {

			Document emailEntry = user.find(eq("email", message.getEmail())).first();

			if (emailEntry != null) {
				
				ObjectId loginID = (ObjectId) emailEntry.get("loginID");
				Document loginEntry = login.find(eq("_id", loginID)).first();
				
				if (loginEntry.get("password").equals(message.getPw())) {
					return new LoginMessage(MESSAGETYPE.OPERATION_SUCCESS);
				}
			}
		}

		return new LoginMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
