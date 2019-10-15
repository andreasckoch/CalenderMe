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
import common.Constants.Appointments;
import common.Constants.Groups;
import common.Constants.LoginDB;
import common.Constants.News;
import common.Constants.ProfileDB;
import common.Constants.Timeslots;
import common.Constants.User;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.Registration;

public class RegistrationHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Registration message;
	private boolean change_email;
	private boolean change_password;
	private boolean delete_account;
	private String email;

	public RegistrationHandler(Registration registration) {
		this.message = registration;
		this.email = message.getEmail();
		this.change_email = message.getChangeEmail();
		this.change_password = message.getChangePassword();
		this.delete_account = message.getDeleteAccount();
		database = super.getDatabase();
	}

	// TODO: adjust removing account and changing email for usage of appointments!
	
	@Override
	protected ClientBasic process() {

		MongoCollection<Document> loginColl = database.getCollection(Constants.LOGIN_COLLECTION);
		MongoCollection<Document> profileColl = database.getCollection(Constants.PROFILE_COLLECTION);
		MongoCollection<Document> timeslotsColl = database.getCollection(Constants.TIMESLOTS_COLLECTION);
		MongoCollection<Document> groupsColl = database.getCollection(Constants.GROUP_COLLECTION);
		MongoCollection<Document> userColl = database.getCollection(Constants.USER_COLLECTION);
		MongoCollection<Document> appointmentColl = database.getCollection(Constants.APPOINTMENT_COLLECTION);
		MongoCollection<Document> newsColl = database.getCollection(Constants.NEWS_COLLECTION);


		if (this.change_email == false && this.change_password == false
				&& this.delete_account == false) {

			Document emailEntry = userColl.find(eq(User.EMAIL, email)).first();

			if (emailEntry == null) {

				logger.debug("Create account for: {}", email);
				ObjectId loginID = new ObjectId();
				ObjectId profileID = new ObjectId();
				
				Document userEntry = new Document(User.EMAIL, email);
				userEntry.append(User.LOGIN, loginID);
				userEntry.append(User.PROFILE, profileID);
				userColl.insertOne(userEntry);
				
				Document loginEntry = new Document(LoginDB.ID, loginID);
				loginEntry.append(LoginDB.PW, message.getPassword());
				loginColl.insertOne(loginEntry);
				
				Document profileEntry = new Document(ProfileDB.ID, profileID);
				profileColl.insertOne(profileEntry);

				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
		}
		if (this.change_email == false && this.change_password == false
				&& this.delete_account == true) {

			Document emailEntry = userColl.find(eq(User.EMAIL, email)).first();

			if (emailEntry != null) {
				ObjectId loginID = emailEntry.getObjectId(User.LOGIN);
				
				Document loginEntry = loginColl.find(eq(LoginDB.ID, loginID)).first();
				if (loginEntry.get(LoginDB.PW).equals(message.getPassword())) {
	
					logger.debug("Delete: {}", emailEntry);
					userColl.deleteOne(emailEntry);

					loginColl.deleteOne(loginEntry);
					
					ObjectId profileID = emailEntry.getObjectId(User.PROFILE);
					Document profileEntry = profileColl.find(eq(ProfileDB.ID, profileID)).first();
					profileColl.deleteOne(profileEntry);
					
					if (emailEntry.containsKey(User.TIMESLOTSID)) {
						ObjectId timeslotsID = emailEntry.getObjectId(User.TIMESLOTSID);
						Document timeslotsEntry = timeslotsColl.find(eq(Timeslots.ID, timeslotsID)).first();
						timeslotsColl.deleteOne(timeslotsEntry);						
					}
					
					// groups
					List<Document> groups_list = new ArrayList<Document>();
					if (emailEntry.containsKey(User.GROUPS)) {
						groups_list = (List<Document>) emailEntry.get(User.GROUPS);					
					}
					if (!groups_list.isEmpty()) {
						for (Document group : groups_list) {
							ObjectId groupID = group.getObjectId(User.GROUPS_LIST_ID);
							Document groupEntry = groupsColl.find(eq(Groups.ID, groupID)).first();
							
							// remove person from this group
							List<Document> admins_list = (List<Document>) groupEntry.get(Groups.ADMINS);
							List<Document> members_list = (List<Document>) groupEntry.get(Groups.MEMBERS);
							Document admin_to_be_removed = new Document(Groups.ADMINS_LIST_EMAIL, email);
							Document member_to_be_removed = new Document(Groups.MEMBERS_LIST_EMAIL, email);
							if (admins_list.contains(admin_to_be_removed)) {
								admins_list.remove(admin_to_be_removed);
								
								// if there are no admins anymore, declare the first member as admin
								if (admins_list.isEmpty()) {
									if (members_list.isEmpty()) {
										// delete this group with no members
										groupsColl.deleteOne(eq(Groups.ID, groupID));
									}
									else {
										Document new_admin = members_list.get(0);
										admins_list.add(new_admin);
										members_list.remove(new_admin);
										groupEntry.replace(Groups.MEMBERS, members_list);
									}
								}
								groupEntry.replace(Groups.ADMINS, admins_list);
							}
							else {
								members_list.remove(member_to_be_removed);
								groupEntry.replace(Groups.MEMBERS, members_list);
							}
							groupsColl.replaceOne(eq(Groups.ID, groupID), groupEntry);
						}
					}
					
					// appointments
					if (emailEntry.containsKey(User.APPOINTMENTS)) {
						
						List<Document> appointments_list = (List<Document>) emailEntry.get(User.APPOINTMENTS);
						for (Document appointment_list_entry : appointments_list) {
							ObjectId appointmentID = appointment_list_entry.getObjectId(User.APPOINTMENT_LIST_ID);
							
							// check if appointment still exists
							Document appointmentEntry = appointmentColl.find(eq(Appointments.ID, appointmentID)).first();
							if (appointmentEntry == null) {
								continue;
							}
							Appointment appointment = new Appointment(appointmentID, database);
							if (appointment.checkAttendants(email) || appointment.checkKeyAttendants(email)) {
								logger.debug("Attendant is removed from appointment");
								// TODO: when written a quit + delete appointment function for participants, use it here instead
								appointment.initRemoveParticipant(email);
								String initiator = appointment.getInitiator();
								Document removedParticipant = new Document(Appointments.ATTENDEE_KEY, email);
								appointment.addToAppointmentListOfSomeonesNewsTab(initiator, News.APPOINTMENT_ATTENDANTS_GONE_LIST, removedParticipant);
								appointment.writeParamsInDatabase();
							}
							else if (appointment.checkInitiator(email)) {
								// remove appointment
								logger.debug("Initiator removes appointment");
								appointment.initRemoveAppointment();
							}
						}
					}
					
					// news
					if (emailEntry.containsKey(User.NEWSID)) {
						newsColl.findOneAndDelete(eq(News.ID, emailEntry.get(User.NEWSID)));
					}
					
					
					return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
				}
			}
		}
		
		if (this.change_email == true && this.change_password == false
				&& this.delete_account == false) {

			Document emailEntry = userColl.find(eq(User.EMAIL, email)).first();

			if (emailEntry != null) {
				
				logger.debug("Email entry before replace: {}, {}", emailEntry.get(User.ID), emailEntry.get(User.EMAIL));
				ObjectId loginID = (ObjectId) emailEntry.get(User.LOGIN);
				Document loginEntry = loginColl.find(eq(LoginDB.ID, loginID)).first();
				
				if (loginEntry.get(LoginDB.PW).equals(message.getPassword())) {
					
					emailEntry.replace(User.EMAIL, email, message.getChangedField());
					userColl.replaceOne(eq(User.EMAIL, email), emailEntry);
					
					return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
				}
			}
		}

		if (this.change_email == false && this.change_password == true
				&& this.delete_account == false) {

			Document emailEntry = userColl.find(eq(User.EMAIL, email)).first(); 
			
			if (emailEntry != null) {

				logger.debug("Email entry before pw change: {}, {}", emailEntry.get(User.ID), emailEntry.get(User.EMAIL));
				ObjectId loginID = (ObjectId) emailEntry.get(User.LOGIN);
				Document loginEntry = loginColl.find(eq(LoginDB.ID, loginID)).first();
				
				if (loginEntry.get(LoginDB.PW).equals(message.getPassword())) {
					
					loginEntry.replace(LoginDB.PW, message.getPassword(), message.getChangedField());
					loginColl.replaceOne(eq(LoginDB.ID, loginID), loginEntry);
					
					return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
				}
			}
		}
		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
	}

}
