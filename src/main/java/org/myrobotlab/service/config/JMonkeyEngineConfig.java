package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.jme3.UserDataConfig;

public class JMonkeyEngineConfig extends ServiceConfig {

  public List<String> modelPaths = new ArrayList<>();
  public Map<String, UserDataConfig> nodes = new LinkedHashMap<>();
  public Map<String, String[]> multiMapped = new LinkedHashMap<>();
  public String cameraLookAt;

}
