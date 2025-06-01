package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class OakDConfig extends ServiceConfig {

  /**
   * install through py4j
   */
  public boolean py4jInstall = true;

  /**
   * publish images to web
   */
  public boolean displayWeb = true;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "py4j", "Py4j");
    listeners.add(new Listener("publishProcessMessage", getPeerName("py4j"), "onPythonMessage"));
    return plan;
  }

}
