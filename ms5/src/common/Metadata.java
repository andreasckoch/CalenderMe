package common;

import java.util.*;

public class Metadata {

	private SortedMap<String, HostRepresentation> metadata;
	

	private final byte semicolon = 59;

	public Metadata() {
		this.metadata = new TreeMap<String, HostRepresentation>();
	}

	/**
	 * Unserialization constructor, takes a byte and creates a TreeMap structure
	 * with the hash as key, and the server representation as element
	 * 
	 * @param bytes
	 */
	public Metadata(byte[] bytes) {
		this.metadata = new TreeMap<String, HostRepresentation>();
		List<Byte> readserverrepbytes = new ArrayList<Byte>();
		for (int i = 0; i < bytes.length; i++) {

			if (bytes[i] == semicolon) {
				byte[] readstatustype = new byte[readserverrepbytes.size()];
				for (int j = 0; j < readserverrepbytes.size(); j++) {
					readstatustype[j] = readserverrepbytes.get(j);
				}
				add(new HostRepresentation(readstatustype));
				readserverrepbytes = new ArrayList<Byte>();
			} else {
				readserverrepbytes.add(bytes[i]);
			}
		}
	}

	/**
	 * We simply add the key, which will go in the right position of the tree
	 * since it's a SortedMap
	 * 
	 * @param serverRep
	 *            server representation to add
	 */

	public void add(HostRepresentation serverRep) {
		String key = Hashing.hashIt(serverRep.toString());
		this.metadata.put(key, serverRep);
	}

	/**
	 * @param serverRep
	 *            server representation to remove, which will be hashed and then
	 *            used as key to remove
	 */

	public void remove(HostRepresentation serverRep) {
		String key = serverRep.toHash();
		this.metadata.remove(key);
	}

	/**
	 * Returns the server representation responsible for handling the key.
	 * Because tailMap returns all the keys bigger than the given key, if the
	 * map is empty it means that there are no bigger keys (we are the biggest)
	 * so the first one (in the circular logic) is taken, otherwise the first
	 * key bigger than our key is taken
	 * 
	 * @param key
	 *            the key of the server representation one aims to get
	 * @return server representation of the responsible server
	 */
	public HostRepresentation getResponsibleServer(String key) {
		if (metadata.isEmpty()) {
			return null;
		}

		if (!metadata.containsKey(key)) {
			SortedMap<String, HostRepresentation> tailMap = metadata.tailMap(key);
			if (tailMap.isEmpty()) {
				key = metadata.firstKey();
			} else {
				key = tailMap.firstKey();
			}
		}
		return metadata.get(key);
	}

	/**
	 * Returns the predecessor server in the ring topology, using the same logic
	 * of getResponsibleServer() but inverted
	 * 
	 * @param key
	 *            the key of the server representation one aims to get the
	 *            predecessor of
	 * @return server representation of the predecessor of the currently
	 *         responsible server (e.g. the one responsible after a deletion of
	 *         a node)
	 */
	public HostRepresentation getPredecessor(String key) {
		if (metadata.isEmpty()) {
			return null;
		}
		SortedMap<String, HostRepresentation> headMap = metadata.headMap(key);
		key = headMap.isEmpty() ? metadata.lastKey() : headMap.lastKey();

		return metadata.get(key);
	}

	/**
	 * 
	 * 
	 * @param key
	 * @return
	 */
	public HostRepresentation getSuccessor(String key) {
		if (metadata.isEmpty()) {
			return null;
		}
		SortedMap<String, HostRepresentation> tempMap = new TreeMap<String, HostRepresentation>(metadata);
		tempMap = tempMap.tailMap(key);
		tempMap.remove(tempMap.firstKey());
		if (tempMap.isEmpty()) {
			return metadata.get(metadata.firstKey());
		} else {
			return tempMap.get(tempMap.firstKey());
		}
	}

	/**
	 * Serialization function intended for preparing to send over the network
	 * 
	 * @return a byte representation array
	 */
	public byte[] getBytes() {
		List<Byte> metadatalist = new ArrayList<Byte>();
		for (Map.Entry<String, HostRepresentation> entry : metadata.entrySet()) {
			HostRepresentation serverRep = entry.getValue();

			byte[] representation = serverRep.getBytes();
			for (byte b : representation) {
				metadatalist.add(b);
			}
			metadatalist.add(semicolon);
		}

		byte[] returnbytes = new byte[metadatalist.size()];
		for (int j = 0; j < metadatalist.size(); j++) {
			returnbytes[j] = metadatalist.get(j);
		}

		return returnbytes;
	}

	/**
	 * toString representation of the metadata tree, for example useful for
	 * debugging
	 * 
	 * @return the String representation of the metadata tree structure
	 */

	public String toString() {
		String retvalue = "";
		for (Map.Entry<String, HostRepresentation> entry : metadata.entrySet()) {
			HostRepresentation serverRep = entry.getValue();
			String key = entry.getKey();
			retvalue = retvalue + "|" + key + ";" + new String(serverRep.getBytes());
		}

		return retvalue;
	}
	
	public SortedMap<String, HostRepresentation> getMetadata() {
		return metadata;
	}
}
