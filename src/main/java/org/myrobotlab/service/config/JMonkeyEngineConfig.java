package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.jme3.UserDataConfig;

public class JMonkeyEngineConfig extends ServiceConfig {

  /**
   * must be unique entries - use addModelPath(path) helper
   */
  public List<String> modelPaths = new ArrayList<>();
  public Map<String, UserDataConfig> nodes = new LinkedHashMap<>();
  public Map<String, String[]> multiMapped = new LinkedHashMap<>();
  public String cameraLookAt;
  public Set<String> test = new HashSet<>();

  /**
   * JMonkeyEngine requires model paths to be unique - they are not idempotent
   * when adding to the graphtree - use this function to keep them unique, yet
   * the yaml will still be a simple array. This is to avoid Yaml's !!set
   * definition
   * 
   * @param path
   */
  public void addModelPath(String path) {
    for (String p : modelPaths) {
      if (p.equals(path)) {
        return;
      }
    }
    modelPaths.add(path);
  }

}
