package org.myrobotlab.service.config;

public class LeapMotionConfig extends ServiceConfig {
  
  public static class Map {
    public int minIn = 0;
    public int maxIn = 180;
    public int minOut = 0;
    public int maxOut = 180;
    
    public Map() {      
    }

    public Map(int minIn, int maxIn, int minOut, int maxOut) {
      this.minIn = minIn;
      this.maxIn = maxIn;
      this.minOut = minOut;
      this.maxOut = maxOut;
    }
  }

  public Map leftThumb = new Map();
  public Map leftIndex = new Map();
  public Map leftMiddle = new Map();
  public Map leftRing = new Map();
  public Map leftPinky = new Map();

  public Map rightThumb = new Map();
  public Map rightIndex = new Map();
  public Map rightMiddle = new Map();
  public Map rightRing = new Map();
  public Map rightPinky = new Map();
 
}
