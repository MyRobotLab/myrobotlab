package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.jme3.UserData;

public class JMonkeyEngineConfig extends ServiceConfig {

  public List<String> modelPaths = new ArrayList<>();
  public Map<String, UserData> nodes = new HashMap<>();
  public Map<String, String[]> multiMapped = new HashMap<>();
  public String cameraLookAt;

}
