package org.myrobotlab.framework.interfaces;

import java.io.IOException;

import org.myrobotlab.service.config.ServiceConfig;

public interface StateSaver {

  public ServiceConfig load() throws IOException;

  public boolean loadFromJson(String json);

  /**
   * save to current default location 
   * @return
   */
  public boolean save();
  
  /**
   * save to a specific location
   * @return
   */
  public boolean save(String filename);
}
