package server.Test;

import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.Basic;
import proto.CalenderMessagesProto.Basic.MessageType;
import proto.CalenderMessagesProto.Registration;

public class MsgFactory {

	protected static Basic getRegistrationMsg(String email, String pw) {
		return Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(email)
						.setPassword(pw).build()
						).build();
	}
	
	protected static Basic getRegistrationDeleteMsg(String email, String pw) {
		return Basic.newBuilder().setType(Basic.MessageType.REGISTRATION)
				.setRegistration(
						Registration.newBuilder()
						.setEmail(email)
						.setPassword(pw)
						.setDeleteAccount(true).build()
						).build();
	}
	
	protected static Basic getBasic(AppointmentMsg appMsg) {
		Basic.Builder builder = Basic.newBuilder();
		builder.setType(MessageType.APPOINTMENT);
		builder.setAppointment(appMsg);
		return builder.build();
	}
	
}
