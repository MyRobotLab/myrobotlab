package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class WorkEConfig extends ServiceConfig {

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);

    addDefaultPeerConfig(plan, name, "git", "Git", false);
    addDefaultPeerConfig(plan, name, "controller", "Arduino", false);
    addDefaultPeerConfig(plan, name, "motorLeft", "MotorPort", false);
    addDefaultPeerConfig(plan, name, "motorRight", "MotorPort", false);
    addDefaultPeerConfig(plan, name, "simulator", "JMonkeyEngine", false);
    addDefaultGlobalConfig(plan, "python", "python", "Python");
    addDefaultGlobalConfig(plan, "webgui", "webgui", "WebGui");
    addDefaultPeerConfig(plan, name, "eye", "OpenCV", false);
    addDefaultPeerConfig(plan, name, "mouth", "Polly", false);
    addDefaultPeerConfig(plan, name, "brain", "ProgramAB", false);
    addDefaultPeerConfig(plan, name, "emoji", "Emoji", false);

    plan.removeRegistry(name + ".");
    // RuntimeConfig runtime = (RuntimeConfig)plan.get("runtime");
    // runtime

    return plan;
  }

}
