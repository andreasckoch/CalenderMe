package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import common.Constants;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.ClientBasic;

public class MessageDecoder {

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(Constants.SERVER_NAME);

	private Handler handler;

	public MessageDecoder() {

	}

	public ClientBasic processMessage(Basic basic) {

		
		switch (basic.getType()) {
		// operation success/failure are not processed by server
		case REGISTRATION:
			handler = new RegistrationHandler(basic.getRegistration());
			break;
		case LOGIN:
			handler = new LoginHandler(basic.getLogin());
			break;
		case PROFILE:
			handler = new ProfileHandler(basic.getProfile());
			break;
		case GROUP:
			handler = new GroupHandler(basic.getGroup());
			break;
		case APPOINTMENT:
			handler = new AppointmentHandler(basic.getAppointment());
			break;
		case TIMESLOTS:
			handler = new TimeSlotsHandler(basic.getTimeSlots());
			break;
		case REQUEST:
			handler = new RequestHandler(basic.getRequest());
			break;
		default:
			return ClientBasic.newBuilder().setType(ClientBasic.MessageType.ERROR).build();
		}
		return handler.process();
	}

}
