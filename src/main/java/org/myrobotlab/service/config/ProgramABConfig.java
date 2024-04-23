package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Plan;

public class ProgramABConfig extends ServiceConfig {

  @Deprecated /* unused text filters */
  public String[] textFilters;

  /**
   * explicit bot directories
   */
  public List<String> bots = new ArrayList<>();

  /**
   * current sessions bot name, it must match a botname that was scanned
   * currently with ProgramAB Alice, Dr.Who, Mr. Turing and Ency
   */
  public String currentBotName = "Alice";

  /**
   * User name currently interacting with the bot. Setting it here will
   * default it.
   */
  public String currentUserName = "human";

  /**
   * sleep current state of the sleep if globalSession is used true : ProgramAB
   * is sleeping and wont respond false : ProgramAB is not sleeping and any
   * response requested will be processed
   * current sleep/wake value
   */
  public boolean sleep = false;

  /**
   * Specific list of channels ProgramAB will respond to, if not defined, then
   * ProgramAB will respond to all channels
   */
  public List<String> channels = new ArrayList<>();

  /**
   * topic to start with, if null then topic will be loaded from predicates of
   * a new session if available, this means a config/{username}.predicates.txt
   * will need to exist with a topic field
   */
  public String startTopic = "unknown";

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "search", "Wikipedia");
    bots.add("resource/ProgramAB/Alice");
    bots.add("resource/ProgramAB/Dr.Who");
    bots.add("resource/ProgramAB/Ency");
    bots.add("resource/ProgramAB/Mr. Turing");
    return plan;
  }

}
