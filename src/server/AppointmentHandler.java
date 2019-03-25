package server;

import proto.CalenderMessagesProto.Appointment;
import proto.CalenderMessagesProto.ClientBasic;

public class AppointmentHandler extends Handler {

	private Appointment message;

	public AppointmentHandler(Appointment appointment) {
		database = super.getDatabase();
		this.message = appointment;
	}

	@Override
	protected ClientBasic process() {
		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
	}

}
