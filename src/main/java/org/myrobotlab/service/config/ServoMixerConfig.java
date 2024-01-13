package org.myrobotlab.service.config;

public class ServoMixerConfig extends ServiceConfig {

  /**
   * set autoDisable on "all" servos .. true - will make all servos autoDisable
   * false - will make all servos autoDisable false null - will make no changes
   */
  public boolean autoDisable = true;
  
  /**
   * where gesture files are stored 
   */
  public String gesturesDir = "data/ServoMixer/gestures";
  
  /**
   * where pose files are stored
   */
  public String posesDir = "data/ServoMixer/poses";
  
  /**
   * speech service name
   */
  public String mouth;
  
}
