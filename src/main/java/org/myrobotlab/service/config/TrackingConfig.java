package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.service.Pid.PidData;

public class TrackingConfig extends ServiceConfig {

  public String pan;
  public String tilt;
  public String cv;
  public String pid;
  public boolean enabled;
  public long lostTrackingDelayMs = 1000;
  public String controller;
  
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
    
    // default peer names
    controller = name + ".controller";
    cv = name + ".cv";
    pan = name + ".pan";
    tilt = name + ".tilt";
    pid = name + ".pid";

    enabled = false;
        
    addPeer(plan, name, "controller", controller, "Arduino", "Arduino");
    addPeer(plan, name, "cv", cv, "OpenCV", "OpenCV");
    addPeer(plan, name, "pan", pan, "Servo", "Servo");
    addPeer(plan, name, "tilt", tilt, "Servo", "Servo");
    addPeer(plan, name, "pid", pid, "Pid", "Pid");


    // pull out config this service default wants to modify
    ArduinoConfig controller = (ArduinoConfig) plan.get(this.controller);
    controller.connect = true; // FIXME - you can't connect to null port
    // controllerConfig.port = "/dev/ttyACM0"; wtf are you doing?
    controller.port = null;

    OpenCVConfig cv = (OpenCVConfig) plan.get(this.cv);
    cv.cameraIndex = 0;
    cv.capturing = false;
    cv.inputSource = "camera";
    cv.grabberType = "OpenCV";

    ServoConfig pan = (ServoConfig) plan.get(this.pan);
    pan.pin = "7";
    pan.autoDisable = true;
    pan.idleTimeout = 3000;
    pan.controller = this.controller;

    ServoConfig tilt = (ServoConfig) plan.get(this.tilt);
    tilt.pin = "5";
    tilt.autoDisable = true;
    tilt.idleTimeout = 3000;
    tilt.controller = this.controller;

    PidConfig pid = (PidConfig)  plan.get(this.pid);
    PidData panData = new PidData();
    panData.kp = 30;
    panData.ki = 1;
    panData.kd = 0.0;

    pid.data.put(this.pan, panData);

    PidData tiltData = new PidData();
    tiltData.kp = 30;
    tiltData.ki = 1;
    tiltData.kd = 0.0;

    pid.data.put(this.tilt, tiltData);

    return plan;
  }

}
