package server;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import message.MessageInterface;
import message.MessageInterface.MESSAGETYPE;
import message.ProfileMessage;

public class ProfileHandler extends Handler {

	private ProfileMessage message;

	public ProfileHandler(ProfileMessage message) {
		this.message = message;
		database = super.getDatabase();
	}

	public MessageInterface process() {
		 // TODO activate with correct collection name //MongoCollection<Document>
		 // profile = database.getCollection(common.Constants.PROFILE_COLLECTION);
		 
		if (this.message.getMessageType() == MESSAGETYPE.PROFILE_UPDATE_PRIVATE) {
			// TODO handle private profile update
			return new ProfileMessage(MESSAGETYPE.OPERATION_SUCCESS);
		}

		if (this.message.getMessageType() == MESSAGETYPE.PROFILE_UPDATE_PUBLIC) {
			// TODO handle public profile update
			return new ProfileMessage(MESSAGETYPE.OPERATION_SUCCESS);
		}

		return new ProfileMessage(MESSAGETYPE.OPERATION_FAILED);
	}

}
