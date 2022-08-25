package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

public class ProgramABConfig extends ServiceConfig {

  public String currentBotName = "Alice";
  public String currentUserName;
  public String[] textListeners;
  public String[] utteranceListeners;
  public String botDir;
  public List<String> bots = new ArrayList<>();
  
  // search engine - currently wikipedia
  // search for sraix
  public String search;
  
  /**
   * current sleep/wake value
   */
  public boolean sleep = false;  

}
