package org.myrobotlab.jme3;

public class Interpolator {
  
  // need to get FPS

  // external thread does a request
  public void request(String spatial, String axis, String type, Double speed) {

  }

//external thread does a request
//processed into a data object which generates the next Msg
  public void request(String spatial, String axis, String type, Long beginTs, Long endTs) {

  }
  
  public Jme3Msg getNext() {
    return null;
  }
}
