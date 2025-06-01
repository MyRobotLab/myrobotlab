package org.myrobotlab.service.config;

public class MouthControlConfig extends ServiceConfig {

  public int mouthClosedPos = 0;
  public int mouthOpenedPos = 180;
  public int delaytime = 75;
  public int delaytimestop = 150;
  public int delaytimeletter = 45;
  public String jaw;
  // to remember to attach to mouth - mouth is the one
  // with the notify entries
  public String mouth;
  public String neoPixel;
  public String animation = "Theater Chase";

}