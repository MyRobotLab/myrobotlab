package org.myrobotlab.math.geometry;

import java.io.Serializable;

public class Point2df implements Serializable {

  private static final long serialVersionUID = 1L;

  public float x;
  public float y;
  public float value;

  public Point2df() {
  }

  public Point2df(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public Point2df(float x, float y, float value) {
    this.x = x;
    this.y = y;
    this.value = value;
  }

  public Point2df(float x, float y, int value) {
    this.x = x;
    this.y = y;
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("(%f,%f)", x, y);
  }

  public float get(String coord) {
    if (coord.equals("x")) {
      return x;
    }
    if (coord.equals("y")) {
      return y;
    }
    return 0;
  }

}
