package org.myrobotlab.service.config;

import org.myrobotlab.math.MapperSimple;

public class GeneralMotorConfig extends ServiceConfig {

  public boolean locked = false;

  /**
   * a new "un-set" mapper for merging with default motorcontroller 1:1 map on
   * range -1,1 to -1, 1
   */
  public MapperSimple mapper = new MapperSimple(-100, 100, -100, 100);

  public String controller;

  public String axis;

}
