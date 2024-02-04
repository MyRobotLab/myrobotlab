package org.myrobotlab.service;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.abstracts.AbstractMotor;
import org.myrobotlab.service.config.MotorPortConfig;

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
public class MotorPort extends AbstractMotor<MotorPortConfig> {
  private static final long serialVersionUID = 1L;

  public MotorPort(String n, String id) {
    super(n, id);
  }

  public void setPort(String port) {
    MotorPortConfig config = (MotorPortConfig)this.config;
    config.port = port;
  }

  public String getPort() {
    MotorPortConfig config = (MotorPortConfig)this.config;
    return config.port;
  }

  
  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      
      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      // webgui.setSsl(true);
      webgui.autoStartBrowser(false);
      webgui.setPort(8888);
      // webgui.setSsl(true);
      webgui.startService();
      
//      Runtime.start("python", "Python");
//      //Runtime.start("motor", "MotorPort");
      
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  @Override
  public void attachMotorController(String controller) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void detachMotorController(String controller) {
    // TODO Auto-generated method stub
    
  }
  
  

}
