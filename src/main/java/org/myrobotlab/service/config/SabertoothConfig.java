package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class SabertoothConfig extends ServiceConfig {

  public String port;
  public boolean connect = false;

  // peer names
  public String serial;

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    // default peer names
    serial = name + ".serial";
    addPeer(plan, name, "serial", serial, "Serial", "Serial");

    return plan;
  }


}
