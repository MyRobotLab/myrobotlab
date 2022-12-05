package org.myrobotlab.service;

import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.config.MotorPortConfig;
import org.myrobotlab.service.config.ServiceConfig;

/**
 * Motor - MotorController which uses a "Port". Examples are Adafruit Motor
 * Controller which uses ports M1 M2 M3 M4 Sabertooth has M1 &amp; M2 ports.
 * 
 * @author GroG
 * 
 *         Some ports are labeled by numbers some by string values, since a
 *         string value can handle either we use a String port.
 *
 */
public class MotorPort extends AbstractMotor {
  private static final long serialVersionUID = 1L;

  String port;

  public MotorPort(String n, String id) {
    super(n, id);
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getPort() {
    return port;
  }

  @Override
  public ServiceConfig getConfig() {
    MotorPortConfig config = (MotorPortConfig)super.getConfig();
    // FIXME - remove local field use config only
    config.port = port;
    return config;
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {    
    // FIXME - remove local field use config only
    MotorPortConfig config = (MotorPortConfig)super.apply(c);
    setPort(config.port);
    return c;
  }

}
