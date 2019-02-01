package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Updates;
import com.mongodb.client.MongoCollection;

import common.Constants;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.Profile;

public class ProfileHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Profile message;

	public ProfileHandler(Profile profile) {
		this.message = profile;
		database = super.getDatabase();
	}

	public Basic process() {
		
		MongoCollection<Document> profile = database.getCollection(common.Constants.PROFILE_COLLECTION);
		MongoCollection<Document> user = database.getCollection(Constants.USER_COLLECTION);
 
		Document emailEntry = user.find(eq("email", message.getEmail())).first();

		if (emailEntry != null) {
			logger.debug("Update profile for: {}", message.getEmail());
			ObjectId profileID = (ObjectId) emailEntry.get("profileID");

			profile.updateOne(eq("_id", profileID), 
							  Updates.combine(
									  Updates.set("name", this.message.getName()),  
									  Updates.set("location", this.message.getLocation()),
									  Updates.set("bio", this.message.getBio()), 
									  Updates.set("organisation", this.message.getOrganisation())));
				
			return Basic.newBuilder().setType(Basic.MessageType.SUCCESS).build();
		}
		
		return Basic.newBuilder().setType(Basic.MessageType.ERROR).build();

	}
}
