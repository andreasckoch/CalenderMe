package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import common.Constants;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.Registration;

public class RegistrationHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Registration message;

	private boolean change_email;

	private boolean change_password;

	private boolean delete_account;

	public RegistrationHandler(Registration registration) {
		this.message = registration;
		this.change_email = registration.getChangeEmail();
		this.change_password = registration.getChangePassword();
		this.delete_account = registration.getDeleteAccount();
		database = super.getDatabase();
	}

	public Basic process() {

		MongoCollection<Document> login = database.getCollection(Constants.LOGIN_COLLECTION);
		MongoCollection<Document> profile = database.getCollection(Constants.PROFILE_COLLECTION);
		MongoCollection<Document> user = database.getCollection(Constants.USER_COLLECTION);

		if (this.change_email == false && this.change_password == false
				&& this.delete_account == false) {

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
				loginEntry.append("password", message.getPassword());
				login.insertOne(loginEntry);
				
				Document profileEntry = new Document("_id", profileID);
				profile.insertOne(profileEntry);

				return Basic.newBuilder().setType(Basic.MessageType.SUCCESS).build();
			}
		}
		if (this.change_email == false && this.change_password == false
				&& this.delete_account == true) {

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
				
				return Basic.newBuilder().setType(Basic.MessageType.SUCCESS).build();
			}
		}
		
		if (this.change_email == true && this.change_password == false
				&& this.delete_account == false) {

			Document emailEntry = user.find(eq("email", message.getEmail())).first();
			logger.debug("Email entry before replace: {}, {}, {}", emailEntry.get("_id"), emailEntry.get("email"), emailEntry.get("password"));

			if (emailEntry != null) {
				
				ObjectId loginID = (ObjectId) emailEntry.get("loginID");
				Document loginEntry = login.find(eq("_id", loginID)).first();
				
				if (loginEntry.get("password").equals(message.getPassword())) {
					
					emailEntry.replace("email", message.getEmail(), message.getChangedField());
					user.replaceOne(eq("email", message.getEmail()), emailEntry);
					
					return Basic.newBuilder().setType(Basic.MessageType.SUCCESS).build();
				}
			}
		}

		if (this.change_email == false && this.change_password == true
				&& this.delete_account == false) {

			Document emailEntry = login.find(eq("email", message.getEmail())).first(); 
	
			if (emailEntry != null) {

				ObjectId loginID = (ObjectId) emailEntry.get("loginID");
				Document loginEntry = login.find(eq("_id", loginID)).first();
				
				if (loginEntry.get("password").equals(message.getPassword())) {
					
					loginEntry.replace("password", message.getPassword(), message.getChangedField());
					login.replaceOne(eq("email", message.getEmail()), emailEntry);
					
					return Basic.newBuilder().setType(Basic.MessageType.SUCCESS).build();
				}
			}
		}
		return Basic.newBuilder().setType(Basic.MessageType.ERROR).build();
	}

}
