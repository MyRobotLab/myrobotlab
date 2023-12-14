package org.myrobotlab.framework;

/**
 * Configuration for starting.  If a start.yml is saved in the root mrl will 
 * use this on startup to determine which configuration set to use.  Also instance id is
 * maintainable here.
 * 
 * @author GroG
 *
 */
public class StartYml {
  /**
   * instance id of myrobotlab, default will be dynamically generated
   */
  public String id;  
    
  /**
   * configuration set to start under /data/config/{configName}
   */
  public String config = "default";

  /**
   * if this startup is enabled
   */
  public boolean enable = false;

}
