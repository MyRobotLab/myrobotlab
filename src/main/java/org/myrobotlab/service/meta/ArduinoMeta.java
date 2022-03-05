package org.myrobotlab.service.meta;

import java.util.LinkedHashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ArduinoConfig;
import org.myrobotlab.service.config.SerialConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ArduinoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ArduinoMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @param type
   *          n
   */
  public ArduinoMeta() {
    addDescription("controls an Arduino microcontroller as a slave, which allows control of all the devices the Arduino is attached to, such as servos, motors and sensors");
    addCategory("microcontroller");
    addPeer("serial", "Serial", "serial device for this Arduino");
  }

  public LinkedHashMap<String, ServiceConfig> getDefault(String name) {
        
    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();
    ArduinoConfig arduinoConfig = new ArduinoConfig();

    // set local names and config
    arduinoConfig.serial = name + ".serial";

    // build a config with all peer defaults
    config.putAll(MetaData.getDefault(arduinoConfig.serial, "Serial"));

    // pull out specific config and modify
    SerialConfig serialConfig = (SerialConfig) config.get(arduinoConfig.serial);
    serialConfig.port = arduinoConfig.port;

    // put self in
    config.put(name, arduinoConfig);

    return config;
  }
}
