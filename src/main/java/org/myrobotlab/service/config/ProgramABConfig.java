package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Plan;

public class ProgramABConfig extends ServiceConfig {

  public String currentBotName = "Alice";
  public String currentUserName;
  public String[] textFilters;
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

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // default peer names
    search = name + ".search";
    addPeer(plan, name, "search", search, "Wikipedia", "Wikipedia");
    
    return plan;

  }

}
