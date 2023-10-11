package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.service.Pid.PidData;

public class CalikoConfig extends ServiceConfig {

  public Map<String, PidData> data = new HashMap<>();
  
  public boolean use3dDemo;

  public int demoNumber;

  public boolean drawConstraints;

  public Object rotateBasesMode;

  public boolean drawLines;

  public boolean drawModels;

  public boolean fixedBaseMode;

  public boolean drawAxes;

  public boolean paused;

  public boolean leftMouseButtonDown;

  public int windowWidth;

  public int windowHeight;

}
