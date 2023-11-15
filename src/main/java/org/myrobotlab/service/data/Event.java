package org.myrobotlab.service.data;

import java.util.Map;

/**
 * Generalized Event POJO class, started by WebXR but intended to be used
 * in other event generating services
 * 
 * @author GroG
 *
 */
public class Event {
  /**
   * Identifier of the event typically its the identifier of some
   * detail of the source of the event e.g. in WebXR it is the uuid
   */
  public String id;

  /**
   * type of event WebXR has sqeezestart sqeezeend and others
   */
  public String type;

  
  /**
   * Value of the event, could be string or numeric or boolean
   */
  public Object value;
  
  
  /**
   * Meta data regarding the event, could be additional values like
   * "handedness" in WebXR
   */
  public Map<String,Object> meta;
  

}
