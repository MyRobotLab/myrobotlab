package org.myrobotlab.service.config;

import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.service.Pid.PidData;

public class PidConfig extends ServiceConfig {

  public Map<String, PidData> data = new HashMap<>();

}
