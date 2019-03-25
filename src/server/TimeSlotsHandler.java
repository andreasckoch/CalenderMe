package server;

import proto.CalenderMessagesProto.ClientBasic;
import proto.CalenderMessagesProto.TimeSlots;

public class TimeSlotsHandler extends Handler {

	private TimeSlots message;

	public TimeSlotsHandler(TimeSlots timeSlots) {
		database = super.getDatabase();
		this.message = timeSlots;
	}

	@Override
	protected ClientBasic process() {
		return ClientBasic.newBuilder().setType(ClientBasic.MessageType.SUCCESS).build();
	}

}
