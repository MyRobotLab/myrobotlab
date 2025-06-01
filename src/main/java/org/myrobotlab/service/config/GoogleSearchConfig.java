package org.myrobotlab.service.config;

public class GoogleSearchConfig extends ServiceConfig {

  /**
   * null == do nothing | true == make lower case | false == make upper case
   */
  public Boolean lowerCase = null;
  public int maxImages = 3;
  public boolean saveSearchToFile = false;

}
