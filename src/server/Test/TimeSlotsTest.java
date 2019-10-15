package server.Test;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
import java.util.stream.IntStream;

import proto.CalenderMessagesProto.AppointmentMsg;
import proto.CalenderMessagesProto.TimeSlot;
import proto.CalenderMessagesProto.TimeSlots;

public class TimeSlotsTest {
	
	private final static long HOUR = 3600000;
	private final static long DAY = 24 * HOUR;
	private final static long MIN = 60000;
	private static Random rand = new Random();
	private static double[] hours = {8, 8.5, 9, 9.5, 10, 11, 12, 13, 14, 14.5, 15, 15.5, 16, 17, 18, 20};
	private static int[] lengthMIN = {20, 30, 45, 60, 90, 120, 180};
	private static int[] slots_per_day = {1, 2, 3, 4, 5};
	
	protected static TimeSlots.Builder addToTemplateMsgFromDayTemplate(TimeSlots.Builder timeBuilder, long[][] template, int[] template_days) {
		for (int day : template_days) {
			for (int slot = 0; slot < template.length; slot++) {
				timeBuilder.addSlots(TimeSlot.newBuilder()
						.setStartUnixTime(day*DAY + template[slot][0])
						.setEndUnixTime(day*DAY + template[slot][1])
						.setCategory(AppointmentMsg.Category.forNumber(rand.nextInt(7)))
						.build());	
			}
		}
		return timeBuilder;
	}
	
	protected static TimeSlots getRandomTimeSlotsMsg(String email, long start_unix_timeframe, long end_unix_timeframe) {
		/*
		 * Create random distribution of slots on each day for indicated time frame
		 * Start day is 1 day after start_unix_timeframe!
		 * Algorithm should generate ordered, non-overlapping time slots with above parameters (start times = hours, etc.)
		 */
		TimeSlots.Builder timeBuilder = TimeSlots.newBuilder();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long timeMS = cal.getTimeInMillis();
		
		// start 1 day from today until end_unix_timeframe
		int total_days = (int) Math.floor((end_unix_timeframe-start_unix_timeframe) / (DAY));
		Iterator<Integer> day_it = IntStream.range(1, total_days).iterator();
		while (day_it.hasNext()) {
			int num_slots = slots_per_day[rand.nextInt(slots_per_day.length)];
			int step = (int) Math.floor(hours.length / num_slots);
			long previous = 0; 
			for (int i = 0; i < num_slots; i++) {
				int rand_step = rand.nextInt(step);
				int hour_idx = i * step + rand_step;
				while (hours[hour_idx] < previous) {
					hour_idx++;
					if (hour_idx == hours.length) {
						break;
					}
				}
				int length_idx = rand.nextInt(lengthMIN.length);
				
				if (hour_idx >= hours.length) {
					break;
				}
				/*
				 * Check whether slot could interfere with next slot --> narrow options for next one
				 * As step is calculated by rounding down, hours should never be eclipsed by all steps together 
				 * '--> hours[(i+1)*step)] if next iteration possible
				 */
				previous = (long) (hours[hour_idx] * HOUR);
				long end_slot_time = (long) (hours[hour_idx] * HOUR + lengthMIN[length_idx] * MIN);
				if (i + 1 < num_slots) {
					if (end_slot_time > hours[(i+1)*step] * HOUR) {
						previous = end_slot_time;
					}					
				}
				int day = day_it.next();
				timeBuilder.addSlots(TimeSlot.newBuilder()
						.setStartUnixTime(timeMS + day*DAY + (long) hours[hour_idx]*HOUR)
						.setEndUnixTime(timeMS + day*DAY + end_slot_time)
						.setCategory(AppointmentMsg.Category.forNumber(rand.nextInt(7)))
						.build());	
			}
		}
		
		return timeBuilder.build();
	}
}
