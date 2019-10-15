package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;

import common.Constants;
import common.Constants.ProfileDB;
import common.Constants.User;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.Profile;


public class ProfileHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Profile message;

	public ProfileHandler(Profile profile) {
		this.message = profile;
		database = super.getDatabase();
	}

	@Override
	protected ClientBasic process() {
		
		MongoCollection<Document> profile = database.getCollection(Constants.PROFILE_COLLECTION);
		MongoCollection<Document> user = database.getCollection(Constants.USER_COLLECTION);
 
		Document emailEntry = user.find(eq(User.EMAIL, message.getEmail())).first();

		if (emailEntry != null) {
			logger.debug("Update profile for: {}", message.getEmail());
			ObjectId profileID = (ObjectId) emailEntry.get(User.PROFILE);

			profile.updateOne(eq(ProfileDB.ID, profileID), 
							  Updates.combine(
									  Updates.set(ProfileDB.NAME, this.message.getName()),  
									  Updates.set(ProfileDB.LOCATION, this.message.getLocation()),
									  Updates.set(ProfileDB.BIO, this.message.getBio()), 
									  Updates.set(ProfileDB.ORGANISATION, this.message.getOrganisation())));
				
			return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
		}
		
		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();

	}
}
