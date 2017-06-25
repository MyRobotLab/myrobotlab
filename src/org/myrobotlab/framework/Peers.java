package org.myrobotlab.framework;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.TreeMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Peers {

  transient public final static Logger log = LoggerFactory.getLogger(Peers.class);

  private final String name;

  private Index<ServiceReservation> templateDNA = new Index<ServiceReservation>();

  public static String getPeerKey(String name, String key) {
    return String.format("%s.%s", name, key);
  }

  static public Peers getPeers(String type) {
    return getPeers("", type);
  }

  static public Peers getPeers(String namePrefix, String inType) {
    String type = CodecUtils.getServiceType(inType);
    try {
      Class<?> theClass = Class.forName(type);
      Method method = theClass.getMethod("getPeers", String.class);
      Peers peers = (Peers) method.invoke(null, new Object[] { namePrefix });
      return peers;
    } catch (Exception e) { // dont care
    }
    return null;
  }

  public static void main(String[] args) {
    LoggingFactory.getInstance().configure();

    // Peers dna = Peers.getPeers("InMoov");
    // Peers dna = Peers.getPeers("Plantoid");

    Peers peers = Peers.getPeers("InMoov");
    ArrayList<ServiceReservation> peerList = peers.getDNA().flatten();
    // Repo repo = Repo.getLocalInstance();

    for (int i = 0; i < peerList.size(); ++i) {
      ServiceReservation sr = peerList.get(i);
      log.info("SR: {}", sr);
    }

    // log.info(tdna.toString());

    TreeMap<String, ServiceReservation> dna = Service.buildDna("i01", "InMoov");
    log.info(dna.toString());
  }

  public Peers(String name) {
    this.name = name;
    // peers.put(getPeerKey(peer), new ServiceReservation(peer, peerType,
    // comment)); ???
  }

  public Index<ServiceReservation> getDNA() {
    return templateDNA;
  }

  public String getPeerKey(String key) {
    return getPeerKey(name, key);
  }

  public void put(String key, String type, String comment) {
    put(key, null, type, comment);
  }

  // FIXME FIXME FIXME !!! - a single place for business logic merges...
  // THERE IS DUPLICATE CODE IN Service !!!
  // put should only insert - and avoid any updates or replacements
  // put in as static in Service
  public void put(String peer, String actualName, String peerType, String comment) {
    peerType = CodecUtils.getServiceType(peerType);
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
      // log.warn(String.format("templateDNA collision - replacing null values
      // !!! %s",
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

  public String show() {
    return templateDNA.getRootNode().toString();
  }

  // suggestAs will insert only (no update) - but top level inserts bottom
  // won't override !!!
  /*
   * @param key
   * @param actualName
   * @param type
   * @param comment
   */
  public boolean suggestAs(String key, String actualName, String type, String comment) {

    type = CodecUtils.getServiceType(type);
    String fullkey = getPeerKey(key);
    log.info(String.format("suggesting %s now as %s", fullkey, actualName));
    put(key, getPeerKey(actualName), type, comment);

    return true;
  }

  public boolean suggestRootAs(String key, String actualName, String type, String comment) {
    type = CodecUtils.getServiceType(type);
    String fullkey = getPeerKey(key);
    log.info(String.format("suggesting %s now as root %s", fullkey, actualName));
    put(key, actualName, type, comment);

    return true;
  }

}
