package org.myrobotlab.service.data;

import java.io.Serializable;

public class Point2Df implements Serializable {

  private static final long serialVersionUID = 1L;

  public long timestamp;

  public float x;
  public float y;
  public float value;

  public Point2Df() {
  }

  public Point2Df(float x, float y) {
    timestamp = System.currentTimeMillis();
    this.x = x;
    this.y = y;
  }

  public Point2Df(float x, float y, float value) {
    timestamp = System.currentTimeMillis();
    this.x = x;
    this.y = y;
    this.value = value;
  }

  public Point2Df(float x, float y, int value) {
    timestamp = System.currentTimeMillis();
    this.x = x;
    this.y = y;
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("(%f,%f)", x, y);
  }
  
  public float get(String coord){
    if (coord.equals("x")){
      return x;
    }
    if (coord.equals("y")){
      return y;
    }
    return 0;
  }

}
