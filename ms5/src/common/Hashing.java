package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashing {

	/**
	 * hashes a given key and returns its hash value, using the standard MD5
	 * hashing algorithm
	 * 
	 * @param key
	 *            the key which shall be hashed
	 * @return the resulting String of the hashing function
	 */
	public static String hashIt(String key) {

		MessageDigest md;
		try {
			byte[] input = key.getBytes();
			md = MessageDigest.getInstance("MD5");
			byte[] hashBytes = md.digest(input);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < hashBytes.length; i++) {
				// converts the hashBytes to a String representation, using the
				// base 16
				sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			String hashValue = sb.toString();
			return hashValue;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

}
