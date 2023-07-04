package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Plan;

public class ProgramABConfig extends ServiceConfig {

  /**
   * current initial bot
   */
  public String currentBotName = "Alice";
  
  /**
   * current initial user
   */
  public String currentUserName;
  
  @Deprecated /* unused text filters */
  public String[] textFilters;
  
  /**
   * a directory ProgramAB will scan for new bots
   */
  public String botDir;

  /**
   * explicit bot directories
   */
  public List<String> bots = new ArrayList<>();

  /**
   * current sleep/wake value
   */
  public boolean sleep = false;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "search", "Wikipedia");
    return plan;
  }

}
