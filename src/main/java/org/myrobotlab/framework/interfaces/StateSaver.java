package org.myrobotlab.framework.interfaces;

import java.io.IOException;

import org.myrobotlab.service.config.ServiceConfig;

public interface StateSaver {

  public ServiceConfig load() throws IOException;

  /**
   * save to current default location
   * 
   * @return true if successful
   */
  public boolean save();

}
