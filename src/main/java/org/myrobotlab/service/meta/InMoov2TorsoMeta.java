package org.myrobotlab.service.meta;

import java.util.LinkedHashMap;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2TorsoConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2TorsoMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2TorsoMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public InMoov2TorsoMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();

    addDescription("InMoov Torso");
    addCategory("robot");

    addPeer("topStom", "Servo", "Top Stomach servo");
    addPeer("midStom", "Servo", "Mid Stomach servo");
    addPeer("lowStom", "Servo", "Low Stomach servo");

  }

  static public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    InMoov2TorsoConfig torsoConfig = new InMoov2TorsoConfig();

    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };

    // set local names and config
    torsoConfig.topStom = name + ".topStom";
    torsoConfig.midStom = name + ".midStom";
    torsoConfig.lowStom = name + ".lowStom";

    // build a config with all peer defaults
    config.putAll(ServiceInterface.getDefault(torsoConfig.topStom, "Servo"));
    config.putAll(ServiceInterface.getDefault(torsoConfig.midStom, "Servo"));
    config.putAll(ServiceInterface.getDefault(torsoConfig.lowStom, "Servo"));

    ServoConfig topStom = (ServoConfig) config.get(torsoConfig.topStom);
    topStom.autoDisable = true;
    topStom.clip = true;
    topStom.controller = "i01.left";
    topStom.idleTimeout = 3000;
    topStom.inverted = false;
    topStom.maxIn = 180.0;
    topStom.maxOut = 120.0;
    topStom.minIn = 0.0;
    topStom.minOut = 60.0;
    topStom.pin = "27";
    topStom.rest = 90.0;
    topStom.speed = 20.0;
    topStom.sweepMax = null;
    topStom.sweepMin = null;

    ServoConfig midStom = (ServoConfig) config.get(torsoConfig.midStom);
    midStom.autoDisable = true;
    midStom.clip = true;
    midStom.controller = "i01.left";
    midStom.idleTimeout = 3000;
    midStom.inverted = false;
    midStom.maxIn = 180.0;
    midStom.maxOut = 120.0;
    midStom.minIn = 0.0;
    midStom.minOut = 60.0;
    midStom.pin = "28";
    midStom.rest = 90.0;
    midStom.speed = 20.0;
    midStom.sweepMax = null;
    midStom.sweepMin = null;

    ServoConfig lowStom = (ServoConfig) config.get(torsoConfig.lowStom);
    lowStom.autoDisable = true;
    lowStom.clip = true;
    lowStom.controller = "i01.left";
    lowStom.idleTimeout = 3000;
    lowStom.inverted = false;
    lowStom.maxIn = 180.0;
    lowStom.maxOut = 180.0;
    lowStom.minIn = 0.0;
    lowStom.minOut = 0.0;
    lowStom.pin = "29";
    lowStom.rest = 90.0;
    lowStom.speed = 20.0;
    lowStom.sweepMax = null;
    lowStom.sweepMin = null;

    return config;

  }

}
