package org.myrobotlab.service.meta.abstracts;

import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.framework.ServiceReservation;

abstract public class Meta {

  /**
   * override filter - where users can override names and types of services
   * before they are created
   */
  static protected Map<String, ServiceReservation> overrides = new TreeMap<>();

  protected AbstractMetaData metaData = null;

  abstract public AbstractMetaData getMetaData();

}
