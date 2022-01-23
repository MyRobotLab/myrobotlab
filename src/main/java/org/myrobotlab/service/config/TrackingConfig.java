package org.myrobotlab.service.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.service.Pid.PidData;

public class TrackingConfig extends ServiceConfig {

  public String pan;
  public String tilt;
  public String cv;
  public String pid;
  public boolean enabled;
  public long lostTrackingDelayMs = 1000;

  @Override
  public LinkedHashMap<String, ServiceConfig> getDefault(String name) {

    LinkedHashMap<String, ServiceConfig> config = new LinkedHashMap<>();

    // RuntimeConfig runtime = new RuntimeConfig();
    // runtime.registry = new String[] { controllerName, cvName, tiltName,
    // panName, pidName, trackingName };

    // set local names and config
    String controller = name + ".controller";
    cv = name + ".cv";
    pan = name + ".pan";
    tilt = name + ".tilt";
    pid = name + ".pid";
    enabled = true;

    // build a config with all peer defaults
    config.putAll(ServiceConfig.getDefault(controller, "Arduino"));
    config.putAll(ServiceConfig.getDefault(cv, "OpenCV"));
    config.putAll(ServiceConfig.getDefault(pan, "Servo"));
    config.putAll(ServiceConfig.getDefault(tilt, "Servo"));
    config.putAll(ServiceConfig.getDefault(pid, "Pid"));

    // pull out config this service default wants to modify
    ArduinoConfig controllerConfig = (ArduinoConfig) config.get(controller);
    controllerConfig.connect = true;
    controllerConfig.port = "/dev/ttyACM0";

    OpenCVConfig cvConfig = (OpenCVConfig) config.get(cv);
    cvConfig.cameraIndex = 0;
    cvConfig.capturing = true;
    cvConfig.inputSource = "camera";
    cvConfig.grabberType = "OpenCV";

    ServoConfig panConfig = (ServoConfig) config.get(pan);
    panConfig.pin = "7";
    panConfig.autoDisable = true;
    panConfig.idleTimeout = 3000; // AHAHAH - I did 3 originally
    panConfig.controller = controller;

    ServoConfig tiltConfig = (ServoConfig) config.get(tilt);
    tiltConfig.pin = "5";
    tiltConfig.autoDisable = true;
    tiltConfig.idleTimeout = 3000;
    tiltConfig.controller = controller;

    PidConfig pidConfig = (PidConfig) config.get(pid);
    PidData panData = new PidData();
    panData.kp = 0.015;
    panData.ki = 0.005;

    pidConfig.data.put(pan, panData);

    PidData tiltData = new PidData();
    tiltData.kp = 0.015;
    tiltData.ki = 0.005;
    pidConfig.data.put(tilt, tiltData);

    // put self in
    config.put(name, this);

    return config;
  }

}
