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
import static common.Constants.Groups.*;

import common.Constants.News;
import common.Constants.User;
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
	
	MongoCollection<Document> USER;
	MongoCollection<Document> NEWS;
	MongoCollection<Document> GROUPS;

	public GroupHandler(Group group) {
		database = super.getDatabase();
		USER = database.getCollection(Constants.USER_COLLECTION);
		NEWS = database.getCollection(Constants.NEWS_COLLECTION);
		GROUPS = database.getCollection(Constants.GROUP_COLLECTION);
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
		
		

		Document emailEntry = USER.find(eq(User.EMAIL, email)).first();

		if (emailEntry == null || email.isEmpty()) {
			return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
		}
		
		// create new group if name, admins, members and description are set
		logger.debug("Group Message Fields: {} - {} - {} - {} - {}", id, name, description, admins, members);
		if (id.isEmpty() && !name.isEmpty()) {
			
			// user collection: add ID of new group in list
			ObjectId groupID = new ObjectId();	
			addGroupIDToUserColl(email, emailEntry, User.GROUPS, groupID);
			
			// group collection: add new Document for group
			groupEntry = new Document(ID, groupID);
			groupEntry.append(NAME, name);
			groupEntry.append(DESCRIPTION, description);
			
			List<Document> admins_list = new ArrayList<Document>();
			admins_list.add(new Document(ADMINS_LIST_EMAIL, email));
			if (!admins.isEmpty()) {
				for (Person admin : admins) {
					admins_list.add(new Document(ADMINS_LIST_EMAIL, admin.getEmail()));
					Document adminEmailEntry = USER.find(eq(User.EMAIL, admin.getEmail())).first();
					addGroupIDToUserColl(admin.getEmail(), adminEmailEntry, User.GROUPS, groupID);
					
					// notify all admins (store id in news list)
					adminEmailEntry = USER.find(eq(User.EMAIL, admin.getEmail())).first();
					if (!adminEmailEntry.containsKey(User.NEWSID)) {
						ObjectId newsID = new ObjectId();
						adminEmailEntry.append(User.NEWSID, newsID);
						NEWS.insertOne(new Document(News.ID, newsID));
						USER.replaceOne(eq(User.EMAIL, admin.getEmail()), adminEmailEntry);
					}
					ObjectId newsID = adminEmailEntry.getObjectId(User.NEWSID);
					Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
					addGroupIDToNewsColl(newsID, newsEntry, News.GROUPS, groupID);
					
				}
			}
			groupEntry.append(ADMINS, admins_list);
			
			List<Document> members_list = new ArrayList<Document>();
			if (!members.isEmpty()) {
				for (Person member : members) {
					members_list.add(new Document(MEMBERS_LIST_EMAIL, member.getEmail()));
					Document memberEmailEntry = USER.find(eq(User.EMAIL, member.getEmail())).first();
					addGroupIDToUserColl(member.getEmail(), memberEmailEntry, User.GROUPS, groupID);
					
					// notify all members, updated memberEmailEntry required
					memberEmailEntry = USER.find(eq(User.EMAIL, member.getEmail())).first();
					if (!memberEmailEntry.containsKey(User.NEWSID)) {
						ObjectId newsID = new ObjectId();
						memberEmailEntry.append(User.NEWSID, newsID);
						NEWS.insertOne(new Document(News.ID, newsID));
						USER.replaceOne(eq(User.EMAIL, member.getEmail()), memberEmailEntry);
					}
					ObjectId newsID = memberEmailEntry.getObjectId(User.NEWSID);
					Document newsEntry = NEWS.find(eq(News.ID, newsID)).first();
					addGroupIDToNewsColl(newsID, newsEntry, News.GROUPS, groupID);
				}
			}
			groupEntry.append(MEMBERS, members_list);
			
			GROUPS.insertOne(groupEntry);
			
			// send client the id of the group
			return ClientBasic.newBuilder().setType(ClientBasic.MessageType.GROUP_RESPONSE)
					.setGroupResponse(ClientGroupResponse.newBuilder()
							.setId(groupID.toHexString())).build();
		}
		
		
		
		
		// check whether id of group is correct and execute instructions
		if (!id.isEmpty()) {
			List<Document> groups_list = (List<Document>) emailEntry.get(User.GROUPS);
			ObjectId groupID = new ObjectId(id);
			boolean groupExists = groups_list.contains(new Document(User.GROUPS_LIST_ID, groupID));
			if (!groupExists) {
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
						.setError(Error.newBuilder().setErrorMessage("Wrong group ID"))
						.build();
			}
			groupEntry = GROUPS.find(eq(ID, groupID)).first();
			
			// Quit group (check members and admins)
			if (quit) {
				
				// user collection
				groups_list.remove(new Document(User.GROUPS_LIST_ID, groupID));
				emailEntry.replace(User.GROUPS, groups_list);
				USER.replaceOne(eq(User.EMAIL, email), emailEntry);
				
				// group collection
				Document to_be_removed = new Document(EMAIL, email);
									
				List<Document> members_list = (List<Document>) groupEntry.get(MEMBERS);
				boolean removed = members_list.remove(to_be_removed);
				
				if (!removed) {
					List<Document> admins_list = (List<Document>) groupEntry.get(ADMINS);
					admins_list.remove(to_be_removed);
					
					// if there are no admins anymore, declare the first member as admin
					if (admins_list.isEmpty()) {
						if (members_list.isEmpty()) {
							// delete this group with no members
							GROUPS.deleteOne(eq(ID, groupID));
							return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
						}
						Document new_admin = members_list.get(0);
						admins_list.add(new_admin);
						members_list.remove(new_admin);
					}
					groupEntry.replace(ADMINS, admins_list);
				}
				groupEntry.replace(MEMBERS, members_list);
				GROUPS.replaceOne(eq(ID, groupID), groupEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			// change name and description of group
			if (change_info && !name.isEmpty()) {
				groupEntry.replace(NAME, name);
				groupEntry.replace(DESCRIPTION, description);
				GROUPS.replaceOne(eq(ID, groupID), groupEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			// As an admin, remove members from group
			if (!remove_members.isEmpty()) {
				List<Document> admins_list = (List<Document>) groupEntry.get(ADMINS);
				if (admins_list.contains(new Document(ADMINS_LIST_EMAIL, email))) {
					List<Document> members_list = (List<Document>) groupEntry.get(MEMBERS);
					for (Person member : remove_members) {						
						boolean removed = members_list.remove(new Document(MEMBERS_LIST_EMAIL, member.getEmail()));
						if (!removed) {
							return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
						}
						
						// remove groupID from the removed member's groups_list
						Document memberEntry = USER.find(eq(User.EMAIL, member.getEmail())).first();
						List<Document> member_groups_list = (List<Document>) memberEntry.get(User.GROUPS);
						member_groups_list.remove(new Document(User.GROUPS_LIST_ID, groupID));
						memberEntry.replace(User.GROUPS, member_groups_list);
						USER.replaceOne(eq(User.EMAIL, member.getEmail()), memberEntry);
					}
					groupEntry.replace(MEMBERS, members_list);
					GROUPS.replaceOne(eq(ID, groupID), groupEntry);
				}
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			// As an admin, promote members to admins
			if (!promote_to_admins.isEmpty()) {
				List<Document> admins_list = (List<Document>) groupEntry.get(ADMINS);
				if (admins_list.contains(new Document(ADMINS_LIST_EMAIL, email))) {
					List<Document> members_list = (List<Document>) groupEntry.get(MEMBERS);
					for (Person member : promote_to_admins) {
						Document member_doc = new Document(MEMBERS_LIST_EMAIL, member.getEmail());
						Document admin_doc = new Document(ADMINS_LIST_EMAIL, member.getEmail());
						if (!members_list.remove(member_doc)) {
							return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
						}
						if (!admins_list.contains(admin_doc)) {
							admins_list.add(admin_doc);							
						}
					}
					groupEntry.replace(MEMBERS, members_list);
					groupEntry.replace(ADMINS, admins_list);
					GROUPS.replaceOne(eq(ID, groupID), groupEntry);
				}
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
		}
		

		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
	}

	private void addGroupIDToUserColl(String email, Document emailEntry, String emailEntryKey, ObjectId groupID) {
		if (!emailEntry.containsKey(emailEntryKey)) {
			emailEntry.append(emailEntryKey, new ArrayList<Document>());
		}
		List<Document> groups_list = (List<Document>) emailEntry.get(emailEntryKey);
		groups_list.add(new Document(User.GROUPS_LIST_ID, groupID));
		emailEntry.replace(emailEntryKey, groups_list);
		USER.replaceOne(eq(User.EMAIL, email), emailEntry);
	}
	
	private void addGroupIDToNewsColl(ObjectId newsID, Document newsEntry, String newsEntryKey, ObjectId groupID) {
		if (!newsEntry.containsKey(newsEntryKey)) {
			newsEntry.append(newsEntryKey, new ArrayList<Document>());
		}
		List<Document> groups_list = (List<Document>) newsEntry.get(newsEntryKey);
		groups_list.add(new Document(News.GROUPS_LIST_ID, groupID));
		newsEntry.replace(newsEntryKey, groups_list);
		NEWS.replaceOne(eq(News.ID, newsID), newsEntry);
	}

}
