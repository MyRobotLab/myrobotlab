package org.myrobotlab.service.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.jme3.UserData;

public class JMonkeyEngineConfig extends ServiceConfig {

  public List<String> modelPaths;
  public Map<String, UserData> nodes;
  public Map<String, String[]> multiMapped;
  public String cameraLookAt;

}
