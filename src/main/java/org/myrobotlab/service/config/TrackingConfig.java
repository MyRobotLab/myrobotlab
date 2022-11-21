package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.service.Pid.PidData;

public class TrackingConfig extends ServiceConfig {

  public boolean enabled;
  public long lostTrackingDelayMs = 1000;
  /**
   * Default config of a service that has peers and the config for those peers
   * 
   * @param name
   *          - name of our service
   * @return map of peer names to their appropriate config
   */
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    enabled = false;

    addDefaultPeerConfig(plan, name, "controller", "Arduino");
    addDefaultPeerConfig(plan, name, "cv", "OpenCV");
    addDefaultPeerConfig(plan, name, "pan", "Servo");
    addDefaultPeerConfig(plan, name, "tilt", "Servo");
    addDefaultPeerConfig(plan, name, "pid", "Pid");

    // pull out config this service default wants to modify
    ArduinoConfig controller = (ArduinoConfig) plan.get(getPeerName("controller"));
    controller.connect = true; // FIXME - you can't connect to null port
    // controllerConfig.port = "/dev/ttyACM0"; wtf are you doing?
    controller.port = null;

    OpenCVConfig cv = (OpenCVConfig) plan.get(getPeerName("cv"));
    cv.cameraIndex = 0;
    cv.capturing = false;
    cv.inputSource = "camera";
    cv.grabberType = "OpenCV";

    ServoConfig pan = (ServoConfig) plan.get(getPeerName("pan"));
    pan.pin = "7";
    pan.autoDisable = true;
    pan.idleTimeout = 3000;
    pan.controller = getPeerName("controller");

    ServoConfig tilt = (ServoConfig) plan.get(getPeerName("tilt"));
    tilt.pin = "5";
    tilt.autoDisable = true;
    tilt.idleTimeout = 3000;
    tilt.controller = getPeerName("controller");

    PidConfig pid = (PidConfig) plan.get(getPeerName("pid"));
    PidData panData = new PidData();
    panData.kp = 30;
    panData.ki = 1;
    panData.kd = 0.0;

    pid.data.put(getPeerName("pan"), panData);

    PidData tiltData = new PidData();
    tiltData.kp = 30;
    tiltData.ki = 1;
    tiltData.kd = 0.0;

    pid.data.put(getPeerName("tilt"), tiltData);

    return plan;
  }

}
