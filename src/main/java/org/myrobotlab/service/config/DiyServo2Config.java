package org.myrobotlab.service.config;

import org.myrobotlab.framework.Plan;
import org.myrobotlab.service.Pid.PidData;

public class DiyServo2Config extends ServiceConfig {
  // TODO: add config here
  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    
    // Ok.. what do we need to add here for the DIYServo?  
    // we need an encoder... 
    // we need a motor
    // we need a Pid
    addDefaultPeerConfig(plan, name, "motor", "MotorDualPwm");
    addDefaultPeerConfig(plan, name, "encoder", "As5048AEncoder");
    addDefaultPeerConfig(plan, name, "pid", "Pid");
    
    
    MotorDualPwmConfig motor = (MotorDualPwmConfig) plan.get(getPeerName("motor"));
    // TODO: controller?!
    // motor.controller
    motor.leftPwmPin = "6";
    motor.rightPwmPin = "7";
    
    // TODO: how do we handle the controller for the encoder?  (could be different than the motor)
    As5048AEncoderConfig encoder = (As5048AEncoderConfig) plan.get(getPeerName("encoder"));
    
    PidConfig pid = (PidConfig) plan.get(getPeerName("pid"));
    pid.data.put(name, new PidData());
    
    // default settings?  
    return plan;
  }
}

