package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class InMoov2HeadConfig extends ServiceConfig {
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
            
    addDefaultPeerConfig(plan, name, "jaw", "Servo");
    addDefaultPeerConfig(plan, name, "eyeX", "Servo");
    addDefaultPeerConfig(plan, name, "eyeY", "Servo");
    addDefaultPeerConfig(plan, name, "rothead", "Servo");
    addDefaultPeerConfig(plan, name, "neck", "Servo");
    addDefaultPeerConfig(plan, name, "rollNeck", "Servo");
    addDefaultPeerConfig(plan, name, "eyelidLeft", "Servo");
    addDefaultPeerConfig(plan, name, "eyelidRight", "Servo");
    
    ServoConfig jaw = (ServoConfig) plan.get(getPeerName("jaw"));
    jaw.autoDisable = true;
    jaw.clip = true;
    jaw.controller = "i01.left";
    jaw.idleTimeout = 3000;
    jaw.inverted = false;
    jaw.maxIn = 180.0;
    jaw.maxOut = 25.0;
    jaw.minIn = 0.0;
    jaw.minOut = 10.0;
    jaw.pin = "26";
    jaw.rest = 10.0;
    jaw.speed = 500.0;
    jaw.sweepMax = null;
    jaw.sweepMin = null;

    ServoConfig eyeX = (ServoConfig) plan.get(getPeerName("eyeX"));
    eyeX.autoDisable = true;
    eyeX.clip = true;
    eyeX.controller = "i01.left";
    eyeX.idleTimeout = 3000;
    eyeX.inverted = false;
    eyeX.maxIn = 180.0;
    eyeX.maxOut = 120.0;
    eyeX.minIn = 0.0;
    eyeX.minOut = 60.0;
    eyeX.pin = "22";
    eyeX.rest = 90.0;
    eyeX.speed = null;
    eyeX.sweepMax = null;
    eyeX.sweepMin = null;

    ServoConfig eyeY = (ServoConfig) plan.get(getPeerName("eyeY"));
    eyeY.autoDisable = true;
    eyeY.clip = true;
    eyeY.controller = "i01.left";
    eyeY.idleTimeout = 3000;
    eyeY.inverted = false;
    eyeY.maxIn = 180.0;
    eyeY.maxOut = 120.0;
    eyeY.minIn = 0.0;
    eyeY.minOut = 60.0;
    eyeY.pin = "24";
    eyeY.rest = 90.0;
    eyeY.speed = null;
    eyeY.sweepMax = null;
    eyeY.sweepMin = null;

    ServoConfig rothead = (ServoConfig) plan.get(getPeerName("rothead"));
    rothead.autoDisable = true;
    rothead.clip = true;
    rothead.controller = "i01.left";
    rothead.enabled = false;
    rothead.idleTimeout = 3000;
    rothead.inverted = false;
    rothead.maxIn = 180.0;
    rothead.maxOut = 150.0;
    rothead.minIn = 0.0;
    rothead.minOut = 30.0;
    rothead.pin = "13";
    rothead.rest = 90.0;
    rothead.speed = 45.0;
    rothead.sweepMax = null;
    rothead.sweepMin = null;

    ServoConfig neck = (ServoConfig) plan.get(getPeerName("neck")); 
    neck.autoDisable = true;
    neck.clip = true;
    neck.controller = "i01.left";
    neck.enabled = false;
    neck.idleTimeout = 3000;
    neck.inverted = false;
    neck.maxIn = 180.0;
    neck.maxOut = 160.0;
    neck.minIn = 0.0;
    neck.minOut = 20.0;
    neck.pin = "12";
    neck.rest = 90.0;
    neck.speed = 45.0;
    neck.sweepMax = null;
    neck.sweepMin = null;

    ServoConfig rollNeck = (ServoConfig) plan.get(getPeerName("rollNeck"));
    rollNeck.autoDisable = true;
    rollNeck.clip = true;
    rollNeck.controller = "i01.right";
    rollNeck.enabled = false;
    rollNeck.idleTimeout = 3000;
    rollNeck.inverted = false;
    rollNeck.maxIn = 180.0;
    rollNeck.maxOut = 160.0;
    rollNeck.minIn = 0.0;
    rollNeck.minOut = 20.0;
    rollNeck.pin = "12";
    rollNeck.rest = 90.0;
    rollNeck.speed = 45.0;
    rollNeck.sweepMax = null;
    rollNeck.sweepMin = null;

    ServoConfig eyelidLeft = (ServoConfig) plan.get(getPeerName("eyelidLeft"));
    eyelidLeft.autoDisable = true;
    eyelidLeft.clip = true;
    eyelidLeft.controller = "i01.right";
    eyelidLeft.enabled = false;
    eyelidLeft.idleTimeout = 3000;
    eyelidLeft.inverted = false;
    eyelidLeft.maxIn = 180.0;
    eyelidLeft.maxOut = 180.0;
    eyelidLeft.minIn = 0.0;
    eyelidLeft.minOut = 0.0;
    eyelidLeft.pin = "24";
    eyelidLeft.rest = 0.0;
    eyelidLeft.speed = 50.0;
    eyelidLeft.sweepMax = null;
    eyelidLeft.sweepMin = null;

    ServoConfig eyelidRight = (ServoConfig) plan.get(getPeerName("eyelidRight"));
    eyelidRight.autoDisable = true;
    eyelidRight.clip = true;
    eyelidRight.controller = "i01.right";
    eyelidRight.enabled = false;
    eyelidRight.idleTimeout = 3000;
    eyelidRight.inverted = false;
    eyelidRight.maxIn = 180.0;
    eyelidRight.maxOut = 180.0;
    eyelidRight.minIn = 0.0;
    eyelidRight.minOut = 0.0;
    eyelidRight.pin = "22";
    eyelidRight.rest = 0.0;
    eyelidRight.speed = 50.0;
    eyelidRight.sweepMax = null;
    eyelidRight.sweepMin = null;


    return plan;

  }
  

}
