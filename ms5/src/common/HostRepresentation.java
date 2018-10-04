package common;

import java.util.ArrayList;
import java.util.List;

public class HostRepresentation {

	private String address;
	private int port;
	// ascii ' 58 ' equals ' : ' , which does not appear in IP nor in PORT
	private static final byte colon = 58;

	public HostRepresentation(String address, int port) {
		this.address = address;
		this.port = port;
	}

	/**
	 * constructor to create a ServerRepresentation from a byte array (e.g. for
	 * the receiving side)
	 * 
	 * @param bytes
	 */
	public HostRepresentation(byte[] bytes) {
		List<Byte> readelement = new ArrayList<Byte>();

		for (int i = 0; i < bytes.length; i++) {

			if (bytes[i] == colon) {
				byte[] readelementbyte = new byte[readelement.size()];
				for (int j = 0; j < readelement.size(); j++) {
					readelementbyte[j] = readelement.get(j);
				}
				this.address = new String(readelementbyte);
				readelement.clear();
			} else if (i == bytes.length - 1) {
				readelement.add(bytes[i]);
				byte[] readelementbyte = new byte[readelement.size()];
				for (int j = 0; j < readelement.size(); j++) {
					readelementbyte[j] = readelement.get(j);
				}
				this.port = Integer.parseInt(new String(readelementbyte));
			} else {
				readelement.add(bytes[i]);
			}
		}

	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address
	 *            the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * String representation of the IP:PORT pair
	 */
	public String toString() {
		return this.address + ":" + this.port;
	}

	/**
	 * convert server representation to its hash value
	 * 
	 * @return hashed server value
	 */
	public String toHash() {
		return Hashing.hashIt(toString());
	}

	/**
	 * 
	 * @return byte array representation of the server
	 */
	public byte[] getBytes() {
		return (this.address + ":" + this.port).getBytes();
	}
	
	/**
	 * method for comparing two ServerRepresentation objects based
	 * on address and port
	 */
	public boolean equals(Object  s) {
	    if (s == null) return false;
	    if (s == this) return true;
	    if (!(s instanceof HostRepresentation)) return false;
	    HostRepresentation o = (HostRepresentation) s;
	    return (o.address.equals(this.address)) && (o.port == this.port);
	}

}
