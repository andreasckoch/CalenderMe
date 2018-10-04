package common;

import java.util.List;
import java.util.ArrayList;

public class Range {

	private String lower_limit;
	private String upper_limit;

	/**
	 * empty constructor for uninitialized range object
	 */
	public Range() {

	}

	/**
	 * constructor for a known range, sets local object limits to parameters
	 * 
	 * @param lower_limit
	 *            the lower limit to be set
	 * @param upper_limit
	 *            the upper limit to be set
	 */
	public Range(String lower_limit, String upper_limit) {
		this.lower_limit = lower_limit;
		this.upper_limit = upper_limit;
	}

	/**
	 * constructor operating on a given byte array representation
	 * 
	 * @param bytes
	 *            byte array which shall be converted to a range object
	 */
	public Range(byte[] bytes) {
		int i = 0;
		List<Byte> readLowerLimit = new ArrayList<Byte>();
		List<Byte> readUpperLimit = new ArrayList<Byte>();

		// Reads the lower limit of the range
		while (i < 32) {
			readLowerLimit.add(bytes[i]);
			i++;
		}
		byte[] tempreadlowerlimit = new byte[readLowerLimit.size()];
		for (int j = 0; j < readLowerLimit.size(); j++) {
			tempreadlowerlimit[j] = readLowerLimit.get(j);
		}
		this.lower_limit = new String(tempreadlowerlimit);
		i++;
		// Reads the upper limit of the range
		while (i < bytes.length) {
			readUpperLimit.add(bytes[i]);
			i++;
		}
		byte[] tempreadupperlimit = new byte[readUpperLimit.size()];
		for (int j = 0; j < readUpperLimit.size(); j++) {
			tempreadupperlimit[j] = readUpperLimit.get(j);
		}
		this.upper_limit = new String(tempreadupperlimit);
	}

	/**
	 * @return the lower limit
	 */
	public String getLower_limit() {
		return lower_limit;
	}

	/**
	 * @param lower_limit
	 *            the lower limit to be set
	 */
	public void setLower_limit(String lower_limit) {
		this.lower_limit = lower_limit;
	}

	/**
	 * @return the upper limit
	 */
	public String getUpper_limit() {
		return upper_limit;
	}

	/**
	 * @param upper_limit
	 *            the upper limit to be set
	 */
	public void setUpper_limit(String upper_limit) {
		this.upper_limit = upper_limit;
	}

	/**
	 * 
	 * @return the byte array representation of the range
	 */
	public byte[] getBytes() {
		return (lower_limit + "-" + upper_limit).getBytes();
	}

}