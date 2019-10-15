package server;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;

import common.Constants.User;

import static common.Constants.*;
import static common.Constants.Timeslots.*;
import proto.CalenderMessagesProto;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.TimeSlots;
import proto.CalenderMessagesProto.TimeSlot;

public class TimeSlotsHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(SERVER_NAME);

	private TimeSlots message;
	private String email;
	private List<TimeSlot> timeslots;
	private boolean update_template;
	private boolean remove_timeslots;
	private boolean remove_template;


	public TimeSlotsHandler(TimeSlots timeSlots) {
		database = super.getDatabase();
		this.message = timeSlots;
		this.email = message.getEmail();
		this.timeslots = message.getSlotsList();
		this.update_template = message.getUpdateTemplate();
		this.remove_timeslots = message.getRemoveTimeslots();
		this.remove_template = message.getRemoveTemplate();
	}

	@Override
	protected ClientBasic process() {
		
		MongoCollection<Document> timeslotsColl = database.getCollection(TIMESLOTS_COLLECTION);
		MongoCollection<Document> user = database.getCollection(USER_COLLECTION);
 
		Document emailEntry = user.find(eq(User.EMAIL, email)).first();

		if (emailEntry != null) {
			logger.debug("Update timeslots for: {}", email);
			ObjectId timeslotsID;
			Document timeslotsEntry;
			if (!emailEntry.containsKey(User.TIMESLOTSID)) {
				timeslotsID = new ObjectId();
				logger.debug("Creating timeslotsID: {}", timeslotsID); 	
				emailEntry.append(User.TIMESLOTSID, timeslotsID);
				user.replaceOne(eq(User.EMAIL, email), emailEntry);
				timeslotsEntry = new Document(ID, timeslotsID);
				timeslotsColl.insertOne(timeslotsEntry);
			}
			else {
				timeslotsID = (ObjectId) emailEntry.get(User.TIMESLOTSID);	
				timeslotsEntry = timeslotsColl.find(eq(ID, timeslotsID)).first();
			}
			
			if (remove_timeslots && timeslots.size() == 1) {
				long start_remove = timeslots.get(0).getStartUnixTime();
				long end_remove = timeslots.get(0).getEndUnixTime();
				if (timeslotsEntry.containsKey(TIMESLOTS)) {
					List<Document> timeslots_list = (List<Document>) timeslotsEntry.get(TIMESLOTS);					
					for (int i = 0; i < timeslots_list.size(); i++) {
						long start = timeslots_list.get(i).getLong(SLOT_START_TIME);
						long end = timeslots_list.get(i).getLong(SLOT_END_TIME);
						if (start >= start_remove && start < end_remove ||
								end > start_remove && end <= end_remove) {
							timeslots_list.remove(i);
						}
					}
					timeslotsEntry.replace(TIMESLOTS, timeslots_list);
					timeslotsColl.replaceOne(eq(ID, timeslotsID), timeslotsEntry);
				}
				
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			if (remove_template) {
				timeslotsEntry.remove(TEMPLATE);
				timeslotsColl.replaceOne(eq(ID, timeslotsID), timeslotsEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			
			
			// check whether to update the template
			// When using template, make sure that all its timeslots are inside 2 weeks [t < 14*24*3600*1000ms] 
			// and relative to the start of the first day
			if (update_template && timeslots.get(timeslots.size()-1).getEndUnixTime() <= TEMPLATE_LENGTH) {
				List<Document> timeslots_list = new ArrayList<Document>();
				for (TimeSlot timeslot : timeslots) {
					long start = timeslot.getStartUnixTime();
					long end = timeslot.getEndUnixTime();
					if (start % MIN_UNIT != 0 || end % MIN_UNIT != 0) {
						return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
								.setError(CalenderMessagesProto.Error.newBuilder().setErrorMessage(String.format("Timeslot stamps are not divisible by {} min!", MIN_UNIT/60000))
										.build()
										).build();
					}
					Document listEntry = new Document(SLOT_START_TIME, start);
					listEntry.append(SLOT_END_TIME, end);
					listEntry.append(SLOT_CATEGORY, timeslot.getCategory().toString());
					timeslots_list.add(listEntry);
				}
				if (!timeslotsEntry.containsKey(TEMPLATE)){
					timeslotsEntry.append(TEMPLATE, timeslots_list);					
				}
				else {
					timeslotsEntry.replace(TEMPLATE, timeslots_list);					
				}
				timeslotsColl.replaceOne(eq(ID, timeslotsID), timeslotsEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			}
			else if (update_template) {
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
						.setError(CalenderMessagesProto.Error.newBuilder().setErrorMessage(String.format("Last timeslot of template needs to be included in {} days from the first day (where template begins)!", TEMPLATE_LENGTH/(24*3600*1000)))
								.build()
								).build();
			}
			
			// With each update of timeslots, replace previous timeslots the same way as above the template
			
			// TODO: check that timeslots do NOT interfere with any appointments!!!
			// - check that timeslots are in the future and that timestamps have a min unit of 5min
			
			// TODO: check that slots do not interfere with each other --> error
			
			Calendar cali = Calendar.getInstance();
			long time_now = cali.getTimeInMillis();
			if (!update_template) {
				List<Document> timeslots_list = new ArrayList<Document>();
				for (TimeSlot timeslot : timeslots) {
					long start = timeslot.getStartUnixTime();
					long end = timeslot.getEndUnixTime();
					//logger.debug("start - end - time_now: {} - {} - {}", start, end, time_now);
					if (start <= time_now || end <= time_now) {
						return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
								.setError(CalenderMessagesProto.Error.newBuilder().setErrorMessage("Timeslots are in the past")
										.build()
										).build();
					}
					if (start % MIN_UNIT != 0 || end % MIN_UNIT != 0) {
						return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
								.setError(CalenderMessagesProto.Error.newBuilder().setErrorMessage(String.format("Timeslot stamps are not divisible by {} min!", MIN_UNIT/60000))
										.build()
										).build();
					}
					Document listEntry = new Document(SLOT_START_TIME, start);
					listEntry.append(SLOT_END_TIME, end);
					listEntry.append(SLOT_CATEGORY, timeslot.getCategory().toString());
					timeslots_list.add(listEntry);
				}
				if (!timeslotsEntry.containsKey(TIMESLOTS)){
					timeslotsEntry.append(TIMESLOTS, timeslots_list);					
				}
				else {
					timeslotsEntry.replace(TIMESLOTS, timeslots_list);					
				}
				//logger.debug("timeslotsEntry: {}", timeslotsEntry);
				timeslotsColl.replaceOne(eq(ID, timeslotsID), timeslotsEntry);
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
			
			}
			
		}
		
		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
		
	}

}
	