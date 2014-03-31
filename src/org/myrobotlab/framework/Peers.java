package org.myrobotlab.framework;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Peers {

	transient public final static Logger log = LoggerFactory.getLogger(Peers.class);

	private final String name;
	private Index<ServiceReservation> templateDNA = new Index<ServiceReservation>();

	public Peers(String name) {
		this.name = name;
		// peers.put(getPeerKey(peer), new ServiceReservation(peer, peerType,
		// comment)); ???
	}

	/*
	 * // TODO - question on have to pas original root instance in ???? public
	 * void putAll(Index<ServiceReservation> dna, String peer, String
	 * inPeerType, String comment) { String myKey = getPeerKey(peer); String
	 * peerType; if (!inPeerType.contains(".")) { peerType =
	 * String.format("org.myrobotlab.service.%s", inPeerType); } else { peerType
	 * = inPeerType; }
	 * 
	 * // put his type in // log.warn(String.format("adding %s %s", myKey,
	 * peerType)); templateDNA.put(myKey, new ServiceReservation(myKey,
	 * peerType, comment));
	 * 
	 * 
	 * // load the dns from it's peers - and put them in // FIXME TODO -
	 * loadType and recurse put // FIXME try { Class<?> theClass =
	 * Class.forName(peerType); Method method = theClass.getMethod("getPeers",
	 * String.class); Peers peers = (Peers) method.invoke(null, new Object[] {
	 * myKey }); IndexNode<ServiceReservation> myNode =
	 * peers.getDNA().getNode(myKey); // LOAD CLASS BY NAME - and do a
	 * getReservations on it ! HashMap<String, IndexNode<ServiceReservation>>
	 * peerRequests = myNode.getBranches(); for (Entry<String,
	 * IndexNode<ServiceReservation>> o : peerRequests.entrySet()) { String
	 * peerKey = o.getKey(); IndexNode<ServiceReservation> p = o.getValue();
	 * 
	 * String fullKey = String.format("%s.%s", peer, peerKey);
	 * ServiceReservation peersr = p.getValue(); putAll(dna, fullKey,
	 * peersr.fullTypeName, peersr.comment); }
	 * 
	 * } catch (Exception e) {
	 * log.info(String.format("%s does not have a getPeers", peerType)); } }
	 */

	public void put(String key, String type, String comment) {
		put(key, null, type, comment);
	}

	// FIXME FIXME FIXME !!! - a single place for business logic merges...
	// THERE IS DUPLICATE CODE IN Service !!!
	// put should only insert - and avoid any updates or replacements
	// put in as static in Service
	public void put(String peer, String actualName, String peerType, String comment) {
		String fullKey = getPeerKey(peer);
		if (actualName == null) {
			actualName = fullKey;
		}

		ServiceReservation reservation = templateDNA.get(fullKey);
		if (reservation == null) {
			// log.warn(String.format("templateDNA adding new key %s %s %s %s",
			// fullKey, actualName, peerType, comment));
			templateDNA.put(fullKey, new ServiceReservation(peer, actualName, peerType, comment));
		} else {
			// log.warn(String.format("templateDNA collision - replacing null values !!! %s",
			// peer));
			StringBuffer sb = new StringBuffer();
			if (reservation.actualName == null) {
				sb.append(String.format(" updating actualName to %s ", actualName));
				reservation.actualName = actualName;
			}

			if (reservation.fullTypeName == null) {
				// FIXME check for dot ?
				sb.append(String.format(" updating peerType to %s ", peerType));
				reservation.fullTypeName = peerType;
			}

			if (reservation.comment == null) {
				sb.append(String.format(" updating comment to %s ", comment));
				reservation.comment = comment;
			}

			log.warn(sb.toString());
		}

	}

	public static String getPeerKey(String name, String key) {
		return String.format("%s.%s", name, key);
	}

	public String getPeerKey(String key) {
		return getPeerKey(name, key);
	}

	public String show() {
		return templateDNA.getRootNode().toString();
	}

	public Index<ServiceReservation> getDNA() {
		return templateDNA;
	}

	// suggestAs will insert only (no update) - but top level inserts bottom
	// won't override !!!
	/**
	 * @param key
	 * @param actualName
	 * @param type
	 * @param comment
	 * @return
	 */
	public boolean suggestAs(String key, String actualName, String type, String comment) {

		String fullkey = getPeerKey(key);
		log.info(String.format("suggesting %s now as %s", fullkey, actualName));
		put(key, getPeerKey(actualName), type, comment);

		return true;
	}

	public boolean suggestRootAs(String key, String actualName, String type, String comment) {

		String fullkey = getPeerKey(key);
		log.info(String.format("suggesting %s now as root %s", fullkey, actualName));
		put(key, actualName, type, comment);

		return true;
	}

}
