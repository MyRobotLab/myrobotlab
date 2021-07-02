package org.myrobotlab.service.config;

import java.util.Map;

import org.myrobotlab.framework.MRLListener;

public class ServiceConfig {
  
  public String name;
  public String type;   
  public String locale;
  // TODO: come up with a better name than 'load'  (enabled is already used by ServoConfig)
  public boolean load;
  public Map<String, MRLListener> listeners;
  
  
}
