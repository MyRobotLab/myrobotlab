package org.myrobotlab.service.config;

import org.myrobotlab.service.config.LeapMotionConfig.Map;

public class LeapMotion2Config extends ServiceConfig {

  public String websocketUrl = "ws://127.0.0.1:6437";
  public boolean tracking = true;
  
  public Map leftThumb = new Map();
  public Map leftIndex = new Map();
  public Map leftMiddle = new Map();
  public Map leftRing = new Map();
  public Map leftPinky = new Map();

  public Map rightThumb = new Map();
  public Map rightIndex = new Map();
  public Map rightMiddle = new Map();
  public Map rightRing = new Map();
  public Map rightPinky = new Map();

}
