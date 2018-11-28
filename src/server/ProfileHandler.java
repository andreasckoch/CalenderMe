package server;

import static com.mongodb.client.model.Filters.eq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.MongoCollection;

import common.Constants;
import message.MessageInterface;
import message.MessageInterface.MESSAGETYPE;
import message.ProfileMessage;

public class ProfileHandler extends Handler {

	
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	
	private ProfileMessage message;

	public ProfileHandler(ProfileMessage message) {
		this.message = message;
		database = super.getDatabase();
	}

	public MessageInterface process() {
		 // TODO activate with correct collection name //MongoCollection<Document>
		 MongoCollection<Document> profile = database.getCollection(common.Constants.PROFILE_COLLECTION);
		 MongoCollection<Document> user = database.getCollection(common.Constants.USER_COLLECTION);
		 
		 
		if (this.message.getMessageType() == MESSAGETYPE.PROFILE_UPDATE_PRIVATE) {
			//TODO update with root collection to link from
			/*Document emailEntry = user.find(eq("email", message.getEmail())).first();
			String profileId = emailEntry.get("profileId").toString();
			
			Document profileEntry = profile.find(eq("_id", profileId)).first();
			*/
			System.out.println("type" + message.getEmail());
			Document profileEntry = profile.find(eq("email",message.getEmail())).first();
			//error case if profile is not found
			if(profileEntry == null) {
				return new ProfileMessage(MESSAGETYPE.ERROR);
			}
		
			//profile found & update
			else {
				profileEntry.replace("name_private", profileEntry.get("name_private"), message.getName());
				profileEntry.replace("age_private", profileEntry.get("age_private"), message.getAge());
				profileEntry.replace("bio_private", profileEntry.get("bio_private"), message.getBio());
				//profile.replaceOne(eq("_id", profileId), profileEntry);
				//TODO
				profile.replaceOne(eq("email", message.getEmail()), profileEntry);
			}
	
			return new ProfileMessage(MESSAGETYPE.OPERATION_SUCCESS);
		}

		if (this.message.getMessageType() == MESSAGETYPE.PROFILE_UPDATE_PUBLIC) {
			//TODO update with root collection to link from
			/*Document emailEntry = user.find(eq("email", message.getEmail())).first();
			String profileId = emailEntry.get("profileId").toString();
			
			Document profileEntry = profile.find(eq("_id", profileId)).first();
			*/
			
			Document profileEntry = profile.find(eq("email",message.getEmail())).first();
			//error case if profile is not found
			if(profileEntry == null) {
				return new ProfileMessage(MESSAGETYPE.ERROR);
			}
		
			//profile found & update
			else {
				profileEntry.replace("name_public", profileEntry.get("name_public"), message.getName());
				profileEntry.replace("age_public", profileEntry.get("age_public"), message.getAge());
				profileEntry.replace("bio_public", profileEntry.get("bio_public"), message.getBio());
				//profile.replaceOne(eq("_id", profileId), profileEntry);
				//TODO
				profile.replaceOne(eq("email", message.getEmail()), profileEntry);
			}
	
			return new ProfileMessage(MESSAGETYPE.OPERATION_SUCCESS);

		}

		return new ProfileMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
