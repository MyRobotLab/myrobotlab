package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;

public class WorkEConfig extends ServiceConfig {
  public String git;
  public String controller;
  public String motorLeft;
  public String motorRight;
  public String simulator;
  public String python;
  public String webgui;
  public String eye;
  public String mouth;
  public String brain;
  public String emoji;
  
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    
    // default peer names
    git = name + ".git";
    controller = name + ".controller";
    motorLeft = name + ".motorLeft";
    motorRight = name + ".motorRight";
    simulator = name + ".simulator";
    python = name + ".python";
    webgui = name + ".webgui";
    eye = name + ".eye";
    mouth = name + ".mouth";
    brain = name + ".brain";
    emoji = name + ".emoji";

    addPeer(plan, name, "git", git, "Git", "Git");
    addPeer(plan, name, "controller", controller, "Arduino", "Arduino");
    addPeer(plan, name, "motorLeft", motorLeft, "MotorPort", "MotorPort");
    addPeer(plan, name, "motorRight", motorRight, "MotorPort", "MotorPort");
    addPeer(plan, name, "simulator", simulator, "JMonkeyEngine", "JMonkeyEngine");
    addPeer(plan, name, "python", python, "Python", "Python");
    addPeer(plan, name, "webgui", webgui, "WebGui", "WebGui");
    addPeer(plan, name, "eye", eye, "OpenCV", "OpenCV");
    addPeer(plan, name, "mouth", mouth, "Polly", "Polly");
    addPeer(plan, name, "brain", brain, "ProgramAB", "ProgramAB");
    addPeer(plan, name, "emoji", emoji, "Emoji", "Emoji");

    return plan;
  }
  
}
