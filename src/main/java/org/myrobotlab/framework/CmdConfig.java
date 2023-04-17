package org.myrobotlab.framework;

/**
 * Configuration for starting.  If a start.yml is saved in the root mrl will 
 * use this on startup to determin which configuration set to use.  Also instance id is
 * maintainable here.
 * 
 * TODO - eventually all CmdOption vars should be here, but only exposing these two initially.
 * 
 * @author greg
 *
 */
public class CmdConfig {
  /**
   * instanct id of myrobotlab
   */
  public String id;  
  
  /**
   * configuration set to start under /data/config/{configName}
   */
  public String config;

}
