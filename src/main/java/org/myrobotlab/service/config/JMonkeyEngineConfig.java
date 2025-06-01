package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.jme3.UserDataConfig;

public class JMonkeyEngineConfig extends ServiceConfig {

  /**
   * Models for JMonkeyEngine to load - can be of format
   */
  public List<String> models = new ArrayList<>();
  
  /**
   * A spatial associated with some part of the scene graph
   */
  public Map<String, UserDataConfig> nodes = new LinkedHashMap<>();
  public Map<String, String[]> multiMapped = new LinkedHashMap<>();
  
  /**
   * The name of the node which the camera should look at
   */
  public String cameraLookAt;

}
