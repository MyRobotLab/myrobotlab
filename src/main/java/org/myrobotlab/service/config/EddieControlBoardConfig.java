package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class EddieControlBoardConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    addDefaultPeerConfig(plan, name, "serial", "Serial");
    addDefaultPeerConfig(plan, name, "keyboard", "Keyboard");
    addDefaultPeerConfig(plan, name, "webgui", "WebGui");
    addDefaultPeerConfig(plan, name, "joystick", "Joystick");
    return plan;
  }

}
