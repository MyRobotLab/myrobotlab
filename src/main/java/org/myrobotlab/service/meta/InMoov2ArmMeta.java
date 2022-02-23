package org.myrobotlab.service.meta;

import java.util.LinkedHashMap;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2ArmConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2ArmMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2ArmMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public InMoov2ArmMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("the InMoov Arm Service");
    addCategory("robot");

    addPeer("bicep", "Servo", "Bicep servo");
    addPeer("rotate", "Servo", "Rotate servo");
    addPeer("shoulder", "Servo", "Shoulder servo");
    addPeer("omoplate", "Servo", "Omoplate servo");
    // addPeer("arduino", "Arduino", "Arduino controller for this arm");

  }

  static public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    InMoov2ArmConfig armConfig = new InMoov2ArmConfig();

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
    armConfig.omoplate = name + ".omoplate";
    armConfig.shoulder = name + ".shoulder";
    armConfig.rotate = name + ".rotate";
    armConfig.bicep = name + ".bicep";

    // build a config with all peer defaults
    config.putAll(ServiceInterface.getDefault(armConfig.omoplate, "Servo"));
    config.putAll(ServiceInterface.getDefault(armConfig.shoulder, "Servo"));
    config.putAll(ServiceInterface.getDefault(armConfig.rotate, "Servo"));
    config.putAll(ServiceInterface.getDefault(armConfig.bicep, "Servo"));

    ServoConfig omoplate = (ServoConfig) config.get(armConfig.omoplate);
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

    ServoConfig shoulder = (ServoConfig) config.get(armConfig.shoulder);
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

    ServoConfig rotate = (ServoConfig) config.get(armConfig.rotate);
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

    ServoConfig bicep = (ServoConfig) config.get(armConfig.bicep);
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

    return config;

  }
}
