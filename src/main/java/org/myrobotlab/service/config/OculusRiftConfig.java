package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class OculusRiftConfig extends ServiceConfig {

  public Peer leftOpenCV = new Peer("leftOpenCV", "OpenCV");
  public Peer rightOpenCV = new Peer("rightOpenCV", "OpenCV");

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "leftOpenCV", "OpenCV");
    addDefaultPeerConfig(plan, name, "rightOpenCV", "OpenCV");
    return plan;
  }

}
