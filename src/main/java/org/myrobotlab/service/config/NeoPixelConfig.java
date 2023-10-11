package org.myrobotlab.service.config;

public class NeoPixelConfig extends ServiceConfig {

  public Integer pin = null;
  public Integer pixelCount = null;
  public int pixelDepth = 3;
  public int speed = 10;
  public int red = 0;
  public int green = 0;
  public int blue = 0;
  public String controller = null;
  public String currentAnimation = null;
  public Integer brightness = 255;
  public boolean fill = false;
  // auto clears flashes
  public boolean autoClear = false;
  public int idleTimeout = 1000;

}
