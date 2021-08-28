package org.myrobotlab.framework;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * ServiceRegistration is a data object containing information regarding a
 * "peer" service within a "composite" service. The Composite service utilizes
 * or controls multiple peer services. In order to do so it needs an internal
 * key used only by the composite service. With this key the composite gets a
 * reference to the peer. In some situations it will be necessary to use a peer
 * with a different name. reserveAs is a method which will re-bind the composite
 * to a differently named peer service.
 * 
 */
public class ServiceReservation implements Serializable {
  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(ServiceReservation.class);

  public String key;
  public String actualName;
  public String type;
  public String comment;
  public Boolean autoStart;
  /**
   * service life-cycle state inactive | created | registered | running |
   * stopped | released a challenge will be keeping it sync'd with actual
   * service state :P
   */
  public String state = "inactive";

  /**
   * key type and comment are all that is needed to define a peer
   * 
   * @param key
   *          key
   * @param typeName
   *          type
   * @param comment
   *          a comment about it...
   * 
   */
  public ServiceReservation(String key, String typeName, String comment) {
    this(key, null, typeName, comment, null);
  }

  /**
   * when actual name is specified whatever key is then mapped to the actual
   * name
   * 
   * @param key
   *          the key
   * @param actualName
   *          the actual name
   * @param typeName
   *          the type
   * @param comment
   *          a comment
   * 
   */
  public ServiceReservation(String key, String actualName, String typeName, String comment) {
    this(key, actualName, typeName, comment, null);
  }

  public ServiceReservation(String key, String actualName, String typeName, String comment, Boolean autoStart) {
    if (key == null) {
      log.error("key cannot be null");
    }

    if (typeName == null) {
      log.error("typeName cannot be null");
    }

    this.key = key;
    this.actualName = actualName;
    this.type = typeName;
    this.comment = comment;
    this.autoStart = (autoStart == null) ? true : autoStart;
  }

  @Override
  public String toString() {

    StringBuffer sb = new StringBuffer();
    // sb.append(key).append("=");
    if (actualName != null) {
      sb.append("[");
      sb.append(actualName);
      sb.append("] ");
    }
    sb.append(type);

    return sb.toString();
  }

}
