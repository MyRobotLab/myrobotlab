package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class OakDConfig extends ServiceConfig {

  
  /**
   * install through py4j
   */
  public boolean py4jInstall = true;
  
  /**
   * the depthai clone
   */
  public String depthaiCloneUrl = "https://github.com/luxonis/depthai.git";
  
  /**
   * pin the repo
   */
  public String depthAiSha = "dde0ba57dba673f67a62e4fb080f22d6cfcd3224";
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "py4j", "Py4j");
    addDefaultPeerConfig(plan, name, "git", "Git");
    return plan;
  }


}
