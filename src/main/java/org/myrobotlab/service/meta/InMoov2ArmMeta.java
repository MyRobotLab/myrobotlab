package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2ArmConfig;
import org.myrobotlab.service.config.SerialConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2ArmMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2ArmMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public InMoov2ArmMeta() {
    addDescription("the InMoov Arm Service");
    addCategory("robot");

    addPeer("bicep", "Servo", "Bicep servo");
    addPeer("rotate", "Servo", "Rotate servo");
    addPeer("shoulder", "Servo", "Shoulder servo");
    addPeer("omoplate", "Servo", "Omoplate servo");
    // addPeer("arduino", "Arduino", "Arduino controller for this arm");

  }

  @Override
  public Plan getDefault(String name, Boolean autoStart) {
    
    Plan plan = new Plan(name);
    // load default peers from meta here
    plan.putPeers(name, peers, autoStart);


    InMoov2ArmConfig arm = new InMoov2ArmConfig();

    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };
    String cname = null;
    if (name.endsWith("leftArm")) {
      cname = "i01.left"; // FIXME - still terrible to have a i01 here :(
    } else if (name.endsWith("rightArm")) {
      cname = "i01.right"; // FIXME - still terrible to have a i01 here :(
    }

    // set local names and config
    arm.omoplate = name + ".omoplate";
    arm.shoulder = name + ".shoulder";
    arm.rotate = name + ".rotate";
    arm.bicep = name + ".bicep";

    ServoConfig omoplate = (ServoConfig) plan.addPeerConfig("omoplate", autoStart);
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

    ServoConfig shoulder = (ServoConfig) plan.addPeerConfig("shoulder", autoStart);
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

    ServoConfig rotate = (ServoConfig) plan.addPeerConfig("rotate", autoStart);
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

    ServoConfig bicep = (ServoConfig) plan.addPeerConfig("bicep", autoStart);
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

    plan.addConfig(arm, autoStart);

    return plan;

  }
}
