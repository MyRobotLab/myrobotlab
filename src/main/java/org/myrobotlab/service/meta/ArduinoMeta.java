package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ArduinoConfig;
import org.myrobotlab.service.config.SerialConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class ArduinoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(ArduinoMeta.class);

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   */
  public ArduinoMeta() {
    addDescription("controls an Arduino microcontroller as a slave, which allows control of all the devices the Arduino is attached to, such as servos, motors and sensors");
    addCategory("microcontroller");
    addPeer("serial", "Serial", "serial device for this Arduino");
  }

  @Override
  public Plan getDefault(String name) {

    Plan plan = new Plan(name);
    plan.putPeers(name, peers);
    // NOTE: you want to do any aliasing at the beginning
    // plan.setPeerName("serial", "serialx");

    ArduinoConfig arduinoConfig = new ArduinoConfig();
    arduinoConfig.serial = name + ".serial";

    // == Peer serial - TODO - automagically add it when you add peers (meta's
    // peers have type info)
    SerialConfig serialConfig = (SerialConfig) plan.addPeerConfig("serial");

    // pull out specific config and modify
    serialConfig.port = arduinoConfig.port;

    // add self last - desired order or construction
    plan.addConfig(arduinoConfig);

    return plan;
  }

}
