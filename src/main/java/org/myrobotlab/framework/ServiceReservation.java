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

  // public String key;
  public String type;
  // public String comment;

  /**
   * service life-cycle state inactive | created | registered | running |
   * stopped | released a challenge will be keeping it sync'd with actual
   * service state :P
   */
  transient public String state = "INACTIVE";

//  public ServiceReservation(String typeName) {
//
//    if (typeName == null) {
//      log.error("typeName cannot be null");
//    }
//
//    this.type = typeName;
//  }

  @Override
  public String toString() {

    StringBuffer sb = new StringBuffer();
    sb.append(type);

    return sb.toString();
  }

}
