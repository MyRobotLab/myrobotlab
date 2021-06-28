package org.myrobotlab.service.config;

import java.util.Map;

import org.myrobotlab.framework.MRLListener;

public class ServiceConfig {
  
  public String name;
  public String type;   
  public String locale;
  public Map<String, MRLListener> listeners;
  
}
