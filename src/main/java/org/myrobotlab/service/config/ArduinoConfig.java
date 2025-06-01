package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class ArduinoConfig extends ServiceConfig {

  /**
   * Port (usb or ip:port) to connect)
   */
  public String port;
  
  /**
   * If you want the arduino to try to connect
   * port must not be null.
   * This is not a status field.
   */
  public boolean connect;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    SerialConfig serialConfig = (SerialConfig) addDefaultPeerConfig(plan, name, "serial", "Serial");
    serialConfig.port = port;
    return plan;
  }
}
