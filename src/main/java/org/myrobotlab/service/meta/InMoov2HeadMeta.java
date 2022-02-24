package org.myrobotlab.service.meta;

import java.util.LinkedHashMap;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.InMoov2HeadConfig;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2HeadMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(InMoov2HeadMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   * 
   * @param name
   *          n
   * 
   */
  public InMoov2HeadMeta(String name) {

    super(name);
    Platform platform = Platform.getLocalInstance();
    addDescription("The inmoov2 head");
    addPeer("jaw", "Servo", "Jaw servo");
    addPeer("eyeX", "Servo", "Eyes pan servo");
    addPeer("eyeY", "Servo", "Eyes tilt servo");
    addPeer("rothead", "Servo", "Head pan servo");
    addPeer("neck", "Servo", "Head tilt servo");
    addPeer("rollNeck", "Servo", "rollNeck Mod servo");
    // addPeer("arduino", "Arduino", "Arduino controller for this arm");

    addPeer("eyelidLeft", "Servo", "eyelidLeft or both servo");
    addPeer("eyelidRight", "Servo", "Eyelid right servo");

  }

  static public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    InMoov2HeadConfig headConfig = new InMoov2HeadConfig();

    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };

    // set local names and config
    headConfig.jaw = name + ".jaw";
    headConfig.eyeX = name + ".eyeX";
    headConfig.eyeY = name + ".eyeY";
    headConfig.rothead = name + ".rothead";
    headConfig.neck = name + ".neck";
    headConfig.rollNeck = name + ".rollNeck";
    headConfig.eyelidLeft = name + ".eyelidLeft";
    headConfig.eyelidRight = name + ".eyelidRight";

    // build a config with all peer defaults
    config.putAll(ServiceInterface.getDefault(headConfig.jaw, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyeX, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyeY, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.rothead, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.neck, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.rollNeck, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyelidLeft, "Servo"));
    config.putAll(ServiceInterface.getDefault(headConfig.eyelidRight, "Servo"));

    ServoConfig jaw = (ServoConfig) config.get(headConfig.jaw);
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

    ServoConfig eyeX = (ServoConfig) config.get(headConfig.eyeX);
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

    ServoConfig eyeY = (ServoConfig) config.get(headConfig.eyeY);
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

    ServoConfig rothead = (ServoConfig) config.get(headConfig.rothead);
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
    
    ServoConfig neck = (ServoConfig) config.get(headConfig.neck);
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

    ServoConfig rollNeck = (ServoConfig) config.get(headConfig.rollNeck);
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

    ServoConfig eyelidLeft = (ServoConfig) config.get(headConfig.eyelidLeft);
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
    
    ServoConfig eyelidRight = (ServoConfig) config.get(headConfig.eyelidRight);
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

    return config;

  }  
  
}
