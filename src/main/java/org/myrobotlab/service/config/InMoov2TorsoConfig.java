package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class InMoov2TorsoConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    addDefaultPeerConfig(plan, name, "topStom", "Servo");
    addDefaultPeerConfig(plan, name, "midStom", "Servo");
    addDefaultPeerConfig(plan, name, "lowStom", "Servo");

    // build a config with all peer defaults
    ServoConfig topStom = (ServoConfig) plan.get(getPeerName("topStom"));
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

    ServoConfig midStom = (ServoConfig) plan.get(getPeerName("midStom"));
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

    ServoConfig lowStom = (ServoConfig) plan.get(getPeerName("lowStom"));
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

    return plan;

  }

}
