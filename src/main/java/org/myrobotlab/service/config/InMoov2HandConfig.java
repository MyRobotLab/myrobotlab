package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class InMoov2HandConfig extends ServiceConfig {
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    addDefaultPeerConfig(plan, name, "thumb", "Servo");
    addDefaultPeerConfig(plan, name, "index", "Servo");
    addDefaultPeerConfig(plan, name, "majeure", "Servo");
    addDefaultPeerConfig(plan, name, "ringFinger", "Servo");
    addDefaultPeerConfig(plan, name, "pinky", "Servo");
    addDefaultPeerConfig(plan, name, "wrist", "Servo");

    String cname = null;

    if (name.endsWith("leftHand")) {
      cname = "i01.left"; // FIXME - still terrible to have a i01 here :(
    } else if (name.endsWith("rightHand")) {
      cname = "i01.right"; // FIXME - still terrible to have a i01 here :(
    }

    // build a config with all peer defaults

    ServoConfig thumb = (ServoConfig) plan.get(getPeerName("thumb"));
    thumb.autoDisable = true;
    thumb.controller = cname;
    thumb.clip = true;
    thumb.idleTimeout = 3000;
    thumb.inverted = false;
    thumb.maxIn = 180.0;
    thumb.maxOut = 180.0;
    thumb.minIn = 0.0;
    thumb.minOut = 0.0;
    thumb.pin = "2";
    thumb.rest = 0.0;
    thumb.speed = 45.0;
    thumb.sweepMax = null;
    thumb.sweepMin = null;

    ServoConfig index = (ServoConfig) plan.get(getPeerName("index"));
    index.autoDisable = true;
    index.controller = cname;
    index.clip = true;
    index.idleTimeout = 3000;
    index.inverted = false;
    index.maxIn = 180.0;
    index.maxOut = 180.0;
    index.minIn = 0.0;
    index.minOut = 0.0;
    index.pin = "3";
    index.rest = 0.0;
    index.speed = 45.0;
    index.sweepMax = null;
    index.sweepMin = null;

    ServoConfig majeure = (ServoConfig) plan.get(getPeerName("majeure"));
    majeure.autoDisable = true;
    majeure.controller = cname;
    majeure.clip = true;
    majeure.idleTimeout = 3000;
    majeure.inverted = false;
    majeure.maxIn = 180.0;
    majeure.maxOut = 180.0;
    majeure.minIn = 0.0;
    majeure.minOut = 0.0;
    majeure.pin = "4";
    majeure.rest = 0.0;
    majeure.speed = 45.0;
    majeure.sweepMax = null;
    majeure.sweepMin = null;

    ServoConfig ringFinger = (ServoConfig) plan.get(getPeerName("ringFinger"));
    ringFinger.autoDisable = true;
    ringFinger.controller = cname;
    ringFinger.clip = true;
    ringFinger.idleTimeout = 3000;
    ringFinger.inverted = false;
    ringFinger.maxIn = 180.0;
    ringFinger.maxOut = 180.0;
    ringFinger.minIn = 0.0;
    ringFinger.minOut = 0.0;
    ringFinger.pin = "5";
    ringFinger.rest = 0.0;
    ringFinger.speed = 45.0;
    ringFinger.sweepMax = null;
    ringFinger.sweepMin = null;

    ServoConfig pinky = (ServoConfig) plan.get(getPeerName("pinky"));
    pinky.autoDisable = true;
    pinky.controller = cname;
    pinky.clip = true;
    pinky.idleTimeout = 3000;
    pinky.inverted = false;
    pinky.maxIn = 180.0;
    pinky.maxOut = 180.0;
    pinky.minIn = 0.0;
    pinky.minOut = 0.0;
    pinky.pin = "6";
    pinky.rest = 0.0;
    pinky.speed = 45.0;
    pinky.sweepMax = null;
    pinky.sweepMin = null;

    ServoConfig wrist = (ServoConfig) plan.get(getPeerName("wrist"));
    wrist.autoDisable = true;
    wrist.controller = cname;
    wrist.clip = true;
    wrist.idleTimeout = 3000;
    wrist.inverted = false;
    wrist.maxIn = 180.0;
    wrist.maxOut = 180.0;
    wrist.minIn = 0.0;
    wrist.minOut = 0.0;
    wrist.pin = "7";
    wrist.rest = 0.0;
    wrist.speed = 45.0;
    wrist.sweepMax = null;
    wrist.sweepMin = null;

    return plan;

  }

}