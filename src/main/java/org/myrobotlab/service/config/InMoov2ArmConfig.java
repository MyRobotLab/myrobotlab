package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class InMoov2ArmConfig extends ServiceConfig {
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    // load default peers from meta here
    
    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };
    String cname = null;
    if (name.endsWith("leftArm")) {
      cname = "i01.left"; // FIXME - still terrible to have a i01 here :(
    } else if (name.endsWith("rightArm")) {
      cname = "i01.right"; // FIXME - still terrible to have a i01 here :(
    }

    ServoConfig omoplate = (ServoConfig) addDefaultPeerConfig(plan, name, "omoplate", "Servo");
    omoplate.autoDisable = true;
    omoplate.controller = cname;
    omoplate.clip = true;
    omoplate.idleTimeout = 3000;
    omoplate.inverted = false;
    omoplate.maxIn = 80.0;
    omoplate.maxOut = 80.0;
    omoplate.minIn = 10.0;
    omoplate.minOut = 10.0;
    omoplate.pin = "11";
    omoplate.rest = 10.0;
    omoplate.speed = 45.0;
    omoplate.sweepMax = null;
    omoplate.sweepMin = null;

    ServoConfig shoulder = (ServoConfig)addDefaultPeerConfig(plan, name, "shoulder", "Servo");
    shoulder.autoDisable = true;
    shoulder.controller = cname;
    shoulder.clip = true;
    shoulder.idleTimeout = 3000;
    shoulder.inverted = false;
    shoulder.maxIn = 180.0;
    shoulder.maxOut = 180.0;
    shoulder.minIn = 0.0;
    shoulder.minOut = 0.0;
    shoulder.pin = "10";
    shoulder.rest = 30.0;
    shoulder.speed = 45.0;
    shoulder.sweepMax = null;
    shoulder.sweepMin = null;
    
    ServoConfig rotate = (ServoConfig) addDefaultPeerConfig(plan, name, "rotate", "Servo");
    rotate.autoDisable = true;
    rotate.controller = cname;
    rotate.clip = true;
    rotate.idleTimeout = 3000;
    rotate.inverted = false;
    rotate.maxIn = 180.0;
    rotate.maxOut = 180.0;
    rotate.minIn = 40.0;
    rotate.minOut = 40.0;
    rotate.pin = "9";
    rotate.rest = 90.0;
    rotate.speed = 45.0;
    rotate.sweepMax = null;
    rotate.sweepMin = null;
    
    ServoConfig bicep = (ServoConfig) addDefaultPeerConfig(plan, name, "bicep", "Servo");
    bicep.autoDisable = true;
    bicep.controller = cname;
    bicep.clip = true;
    bicep.idleTimeout = 3000;
    bicep.inverted = false;
    bicep.maxIn = 90.0;
    bicep.maxOut = 90.0;
    bicep.minIn = 0.0;
    bicep.minOut = 0.0;
    bicep.pin = "8";
    bicep.rest = 0.0;
    bicep.speed = 45.0;
    bicep.sweepMax = null;
    bicep.sweepMin = null;

    return plan;

  }

}