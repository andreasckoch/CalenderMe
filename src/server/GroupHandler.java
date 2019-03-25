package server;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import common.Constants;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.ClientGroupResponse;
import proto.CalenderMessagesProto.Error;
import proto.CalenderMessagesProto.Group;
import proto.CalenderMessagesProto.Person;

public class GroupHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);
	
	private Group message;
	private String email;
	private String name;
	private List<Person> admins;
	private List<Person> members;
	private String description;
	private boolean quit;
	private boolean change_info;
	private List<Person> remove_members;
	private List<Person> promote_to_admins;
	private String id;
	
	private Document groupEntry;


	public GroupHandler(Group group) {
		database = super.getDatabase();
		this.message = group;
		this.email = message.getEmail();
		this.id = message.getId();
		this.name = message.getName();
		this.admins = message.getAdminsList();
		this.members = message.getMembersList();
		this.description = message.getDescription();
		this.quit = message.getQuit();
		this.change_info = message.getChangeInfo();
		this.remove_members = message.getRemoveMembersList();
		this.promote_to_admins = message.getPromoteToAdminsList();
	}

	@Override
	protected ClientBasic process() {
		
		MongoCollection<Document> user = database.getCollection(Constants.USER_COLLECTION);
		MongoCollection<Document> groups = database.getCollection(Constants.GROUP_COLLECTION);

		Document emailEntry = user.find(eq("email", email)).first();

		if (emailEntry == null || email.isEmpty()) {
			return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
		}
		
		// create new group if name, admins, members and description are set
		logger.debug("Group Message Fields: {} - {} - {} - {} - {}", id, name, description, admins, members);
		if (id.isEmpty() && !name.isEmpty()) {
			
			// user collection: add ID of new group in list
			ObjectId groupID = new ObjectId();	
			addGroupID(user, email, emailEntry, "groups", groupID);
			
			// group collection: add new Document for group
			groupEntry = new Document("_id", groupID);
			groupEntry.append("name", name);
			groupEntry.append("description", description);
			
			List<Document> admins_list = new ArrayList<Document>();
			admins_list.add(new Document("email", email));
			if (!admins.isEmpty()) {
				for (Person admin : admins) {
					admins_list.add(new Document("email", admin.getEmail()));
					Document adminEmailEntry = user.find(eq("email", admin.getEmail())).first();
					addGroupID(user, admin.getEmail(), adminEmailEntry, "groups", groupID);
					
					// notify all admins (store id in news list)
					adminEmailEntry = user.find(eq("email", admin.getEmail())).first();
					addGroupID(user, admin.getEmail(), adminEmailEntry, "news", groupID);
					
				}
			}
			groupEntry.append("admins", admins_list);
			
			if (!members.isEmpty()) {
				List<Document> members_list = new ArrayList<Document>();
				for (Person member : members) {
					members_list.add(new Document("email", member.getEmail()));
					Document memberEmailEntry = user.find(eq("email", member.getEmail())).first();
					addGroupID(user, member.getEmail(), memberEmailEntry, "groups", groupID);
					
					// notify all members, updated memberEmailEntry required
					memberEmailEntry = user.find(eq("email", member.getEmail())).first();
					addGroupID(user, member.getEmail(), memberEmailEntry, "news", groupID);
				}
				groupEntry.append("members", members_list);
			}
			
			groups.insertOne(groupEntry);
			
			// send client the id of the group
			return ClientBasic.newBuilder().setType(ClientBasic.MessageType.GROUPRESPONSE)
					.setGroupResponse(ClientGroupResponse.newBuilder()
							.setId(groupID.toHexString())).build();
		}
		
		
		
		
		// check whether id of group is correct and execute instructions
		if (!id.isEmpty()) {
			List<Document> groups_list = (List<Document>) emailEntry.get("groups");
			ObjectId groupID = new ObjectId(id);
			boolean groupExists = groups_list.contains(new Document("groupID", groupID));
			if (!groupExists) {
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
						.setError(Error.newBuilder().setErrorMessage("Wrong group ID"))
						.build();
			}
			groupEntry = groups.find(eq("_id", groupID)).first();
			
			// Quit group (check members and admins)
			if (quit) {
				
				// user collection
				groups_list.remove(new Document("groupID", groupID));
				emailEntry.replace("groups", groups_list);
				user.replaceOne(eq("email", email), emailEntry);
				
				// group collection
				Document to_be_removed = new Document("email", email);
									
				List<Document> members_list = (List<Document>) groupEntry.get("members");
				boolean removed = members_list.remove(to_be_removed);
				
				if (!removed) {
					List<Document> admins_list = (List<Document>) groupEntry.get("admins");
					admins_list.remove(to_be_removed);
					
					// if there are no admins anymore, declare the first member as admin
					if (admins_list.isEmpty()) {
						if (members_list.isEmpty()) {
							// delete this group with no members
							groups.deleteOne(eq("_id", groupID));
							return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
						}
						Document new_admin = members_list.get(0);
						admins_list.add(new_admin);
						members_list.remove(new_admin);
					}
					groupEntry.replace("admins", admins_list);
				}
				groupEntry.replace("members", members_list);
				groups.replaceOne(eq("_id", groupID), groupEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			// change name and description of group
			if (change_info && !name.isEmpty()) {
				groupEntry.replace("name", name);
				groupEntry.replace("description", description);
				groups.replaceOne(eq("_id", groupID), groupEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			// As an admin, remove members from group
			if (!remove_members.isEmpty()) {
				List<Document> admins_list = (List<Document>) groupEntry.get("admins");
				if (admins_list.contains(new Document("email", email))) {
					List<Document> members_list = (List<Document>) groupEntry.get("members");
					for (Person member : remove_members) {						
						boolean removed = members_list.remove(new Document("email", member.getEmail()));
						if (!removed) {
							return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
						}
					}
					groupEntry.replace("members", members_list);
					groups.replaceOne(eq("_id", groupID), groupEntry);
				}
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			// As an admin, promote members to admins
			if (!promote_to_admins.isEmpty()) {
				List<Document> admins_list = (List<Document>) groupEntry.get("admins");
				if (admins_list.contains(new Document("email", email))) {
					List<Document> members_list = (List<Document>) groupEntry.get("members");
					for (Person member : promote_to_admins) {
						Document member_doc = new Document("email", member.getEmail());
						if (!members_list.remove(member_doc)) {
							return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
						}
						if (!admins_list.contains(member_doc)) {
							admins_list.add(member_doc);							
						}
					}
					groupEntry.replace("members", members_list);
					groupEntry.replace("admins", admins_list);
					groups.replaceOne(eq("_id", groupID), groupEntry);
				}
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
		}
		

		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
	}

	private void addGroupID(MongoCollection<Document> user, String email, Document emailEntry, String emailEntryKey, ObjectId groupID) {
		if (!emailEntry.containsKey(emailEntryKey)) {
			emailEntry.append(emailEntryKey, new ArrayList<Document>());
		}
		List<Document> groups_list = (List<Document>) emailEntry.get(emailEntryKey);
		groups_list.add(new Document("groupID", groupID));
		emailEntry.replace(emailEntryKey, groups_list);
		user.replaceOne(eq("email", email), emailEntry);
	}

}
