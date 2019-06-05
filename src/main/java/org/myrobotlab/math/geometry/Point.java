package org.myrobotlab.math.geometry;

import java.io.Serializable;

public class Point implements Serializable {

  private static final long serialVersionUID = 1L;

  public int x;
  public int y;
  public int value;

  public Point() {
  }

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Point(int x, int y, int value) {
    this.x = x;
    this.y = y;
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("(%d,%d)", x, y);
  }

}
