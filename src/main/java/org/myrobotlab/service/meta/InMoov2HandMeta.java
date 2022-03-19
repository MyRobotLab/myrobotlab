package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2HandConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2HandMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2HandMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public InMoov2HandMeta() {
    addDescription("an easier way to create gestures for InMoov");
    addCategory("robot");

    addPeer("thumb", "Servo", "Thumb servo");
    addPeer("index", "Servo", "Index servo");
    addPeer("majeure", "Servo", "Majeure servo");
    addPeer("ringFinger", "Servo", "RingFinger servo");
    addPeer("pinky", "Servo", "Pinky servo");
    addPeer("wrist", "Servo", "Wrist servo");

  }

  @Override
  public Plan getDefault(String name, Boolean autoStart) {

    InMoov2HandConfig hand = new InMoov2HandConfig();

    Plan plan = new Plan(name);
    // load default peers from meta here
    plan.putPeers(name, peers, autoStart);

        
    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };

    // set local names and config
    hand.thumb = name + ".thumb";
    hand.index = name + ".index";
    hand.majeure = name + ".majeure";
    hand.ringFinger = name + ".ringFinger";
    hand.pinky = name + ".pinky";
    hand.wrist = name + ".wrist";
    String cname = null;

    if (name.endsWith("leftHand")) {
      cname = "i01.left"; // FIXME - still terrible to have a i01 here :(
    } else if (name.endsWith("rightHand")) {
      cname = "i01.right"; // FIXME - still terrible to have a i01 here :(
    }

    // build a config with all peer defaults

    ServoConfig thumb = (ServoConfig) plan.getPeerConfig("thumb");
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

    ServoConfig index = (ServoConfig) plan.getPeerConfig("index");
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

    ServoConfig majeure = (ServoConfig) plan.getPeerConfig("majeure");
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

    ServoConfig ringFinger = (ServoConfig) plan.getPeerConfig("ringFinger");
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

    ServoConfig pinky = (ServoConfig) plan.getPeerConfig("pinky");    
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

    ServoConfig wrist = (ServoConfig) plan.getPeerConfig("wrist");
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

    plan.addConfig(hand, autoStart);

    return plan;

  }
}
