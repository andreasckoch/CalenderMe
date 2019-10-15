package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import common.Constants;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.Login;

public class LoginHandler extends Handler {

	@SuppressWarnings({ "unused" })
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Login message;

	public LoginHandler(Login login) {
		this.message = login;
		database = super.getDatabase();
	}

	@Override
	protected ClientBasic process() {

		MongoCollection<Document> login = database.getCollection(common.Constants.LOGIN_COLLECTION);
		MongoCollection<Document> user = database.getCollection(common.Constants.USER_COLLECTION);


		Document emailEntry = user.find(eq("email", message.getEmail())).first();

		if (emailEntry != null) {
			
			ObjectId loginID = (ObjectId) emailEntry.get("loginID");
			Document loginEntry = login.find(eq("_id", loginID)).first();
			
			if (loginEntry.get("password").equals(message.getPassword())) {
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
		}

		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
	}

}
