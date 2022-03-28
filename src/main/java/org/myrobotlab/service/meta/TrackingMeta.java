package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.config.ArduinoConfig;
import org.myrobotlab.service.config.OpenCVConfig;
import org.myrobotlab.service.config.PidConfig;
import org.myrobotlab.service.config.ServoConfig;
import org.myrobotlab.service.config.TrackingConfig;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class TrackingMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(TrackingMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public TrackingMeta() {

    // FIXME - could be debatable if base should 
    // have a peer servo controller definition
    // maybe it should use only 
    addPeer("controller", "Arduino");
    
    addPeer("cv", "OpenCV");
    addPeer("pan", "Servo");
    addPeer("tilt", "Servo");
    addPeer("pid", "Pid");
    // TODO - controller ? naw probably not

    addDescription("tracks objects through video stream given a simple pan, tilt servo camera rig");
    addCategory("sensors", "tracking", "vision");

  }

  /**
   * Default config of a service that has peers and the config for those peers
   * 
   * @param name
   *          - name of our service
   * @return map of peer names to their appropriate config
   */
  @Override
  public Plan getDefault(String name) {

    Plan plan = new Plan(name);
    // load default peers from meta here
    plan.putPeers(name, peers);
    // NOTE: you want to do any aliasing at the beginning
    // plan.setPeerName("tilt", name + ".head.neck");

    
    TrackingConfig tracking = new TrackingConfig();

    tracking.controller = name + ".controller";
    tracking.cv = name + ".cv";
    tracking.pan = name + ".pan";
    tracking.tilt = name + ".tilt";
    tracking.pid = name + ".pid";
    tracking.enabled = false;

    // pull out config this service default wants to modify
    ArduinoConfig controller = (ArduinoConfig) plan.getPeerConfig("controller");
    controller.connect = true; // FIXME - you can't connect to null port
    // controllerConfig.port = "/dev/ttyACM0"; wtf are you doing?
    controller.port = null;

    OpenCVConfig cv = (OpenCVConfig) plan.getPeerConfig("cv");
    cv.cameraIndex = 0;
    cv.capturing = false;
    cv.inputSource = "camera";
    cv.grabberType = "OpenCV";

    ServoConfig pan = (ServoConfig) plan.getPeerConfig("pan");
    pan.pin = "7";
    pan.autoDisable = true;
    pan.idleTimeout = 3000;
    pan.controller = tracking.controller;

    ServoConfig tilt = (ServoConfig) plan.getPeerConfig("tilt");
    tilt.pin = "5";
    tilt.autoDisable = true;
    tilt.idleTimeout = 3000;
    tilt.controller = tracking.controller;

    PidConfig pid = (PidConfig) plan.getPeerConfig("pid");
    PidData panData = new PidData();
    panData.kp = 0.015;
    panData.ki = 0.001;
    panData.kd = 0.0;

    pid.data.put(tracking.pan, panData);

    PidData tiltData = new PidData();
    tiltData.kp = 0.035;
    tiltData.ki = 0.001;
    tiltData.kd = 0.0;

    pid.data.put(tracking.tilt, tiltData);
    
    plan.addConfig(tracking);

    return plan;
  }

}
