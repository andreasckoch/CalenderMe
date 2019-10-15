package server;

import static com.mongodb.client.model.Filters.eq;
import static common.Constants.APPOINTMENT_COLLECTION;
import static common.Constants.USER_COLLECTION;
import static common.Constants.Appointments.ID;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import common.Constants;
import proto.CalenderMessagesProto;
import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.ClientInitAppointment;

public class AppointmentHandler extends Handler {
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private MongoDatabase database;
	private AppointmentMsg message;
	private Appointment appointment;
	private ObjectId appointmentID;
	private String appointmentIDString;
	private String email;
	private String initiator;
	private boolean same_category;

	public AppointmentHandler(AppointmentMsg appointmentMsg) {
		database = super.getDatabase();
		this.message = appointmentMsg;
		this.appointmentIDString = appointmentMsg.getId();
		this.email = appointmentMsg.getEmail();
		this.initiator = appointmentMsg.getInitiator();
		this.same_category = appointmentMsg.getOnlySameCategory();
	}

	@Override
	protected ClientBasic process() {
		/*
		 * 1) Create new appointment when no email and valid id are provided, but an initiator is
		 * 2) Interact with existing appointment with actions:
		 * 		a) as initiator:
		 * 				- asking participants for confirmation of date
		 * 				- adding (key-) attendants
		 * 				- removing participants
		 * 		b) as participant:
		 * 				- confirm/cancel/maybe an appointment
		 * 3) TODO: when deadline approaches check amount of people that confirmed appointment and ask initiator how to proceed
		 *  --> operate in dedicated thread for monitoring appointment deadlines
		 * 		
		 */
		MongoCollection<Document> USER = database.getCollection(USER_COLLECTION);
		MongoCollection<Document> APPOINTMENT = database.getCollection(APPOINTMENT_COLLECTION);

		Document appointmentEntry = null;
		if (!appointmentIDString.isEmpty()) {
			appointmentID = new ObjectId(appointmentIDString);
			appointmentEntry = APPOINTMENT.find(eq(ID, appointmentID)).first();
		}
		
		logger.debug("AppointmentID: {} - Initiator: {} - Email: {}", appointmentID, initiator, email);
		
		if (appointmentEntry == null) {
			if (email.isEmpty()) {
				if (initiator.isEmpty()) {
					return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
							.setError(CalenderMessagesProto.Error.newBuilder()
									.setErrorMessage("No valid information in appointment message")
									.build()
									).build();
				}
				// create new appointment, inform attendants (send them messages) and write new entry in database
				appointment = new Appointment(message, database);
				List<Long> possible_dates;
				if (message.getStartUnixTime() == 0) {
					// find suitable time for appointment
					logger.info("Calculating suitable date for appointment");
					appointment.getTimeslotsOfAttendants(same_category);
					appointment.calculateBestDates();
					possible_dates = appointment.getPossible_dates();
					appointment.writeParamsInDatabase();
					
					// send possible dates to initiator
					ClientInitAppointment.Builder initAppointment = ClientInitAppointment.newBuilder();
					for (int i = 0; i < possible_dates.size(); i++) {
						initAppointment.setPossibleDates(i, possible_dates.get(i));						
					}
					return ClientBasic.newBuilder().setType(ClientBasic.MessageType.APPOINTMENT_INIT_RESPONSE)
							.setClientInitAppointment(initAppointment.build()).build();
					
				}
				else {
					appointment.writeParamsInDatabase();
				}
				// send appointmentID back to initiator
				ClientInitAppointment.Builder initAppointmentBuilder = ClientInitAppointment.newBuilder();
				initAppointmentBuilder.setId(appointment.getAppointmentID().toString());
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.APPOINTMENT_INIT_RESPONSE)
						.setClientInitAppointment(initAppointmentBuilder.build()).build();
			}
			else {
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
						.setError(CalenderMessagesProto.Error.newBuilder()
								.setErrorMessage("Wrong id provided in appointment message")
								.build()
								).build();
			}			
		}
		else {
			// TODO: check whether appointment object with this id already exists in memory --> wait for it to finish
			
			// Interact with existing appointment
			appointment = new Appointment(appointmentID, database);
						
			// check whether email (inquirer) is initiator, attendant or key_attendant
			if (appointment.checkInitiator(email)) {
				// process answer to possible dates
				if (message.hasIndexOfDatesList()) {
					long chosen_date = appointment.getChosenDate(message.getIndexOfDatesList());
					logger.debug("Got chosen date {}", chosen_date);
					appointment.setStart_unix_time(chosen_date);
					
					// ask participants for confirmation
					appointment.askKeyAttendantsForAvailability(chosen_date);
					appointment.askAttendantsForAvailability(chosen_date);
				}
				// add or remove participants
				if (message.getAddParticipants()) {
					appointment.initAddToAttendants(message.getAddAttendantsList());
					appointment.initAddToKeyAttendants(message.getAddKeyAttendantsList());					
				}
				if (message.getRemoveParticipants()) {
					appointment.initRemoveParticipants(message.getRemoveParticipantList());
				}
			}
			
			else if (appointment.checkKeyAttendants(email)  || appointment.checkAttendants(email)) {
				// add answer to request for availability
				AppointmentMsg.Answer answer = message.getConfirmAppointment();
				if (!answer.equals(AppointmentMsg.Answer.NONE)) {
					logger.debug("Received genuine answer to request for availability");
					appointment.processAnswerToAvailabilityRequest(email, answer);					
				}
				if (message.getGetInfo()) {
					ClientBasic.Builder builder = ClientBasic.newBuilder();
					builder.setType(ClientBasic.MessageType.APPOINTMENT_ATTENDANT_RESPONSE);
					builder.setClientAttendantAppointment(appointment.getInfoForAttendant());
					return builder.build();
				}
			}
			else {
				return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR)
						.setError(CalenderMessagesProto.Error.newBuilder()
								.setErrorMessage("Not registered as participant of appointment")
								.build()
								).build();
			}
		}
			
		
		
		
		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
	}

}
