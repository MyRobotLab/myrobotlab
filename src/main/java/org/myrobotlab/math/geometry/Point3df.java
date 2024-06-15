package org.myrobotlab.math.geometry;

import java.io.Serializable;

public class Point3df implements Serializable {

  private static final long serialVersionUID = 1L;

  public float x;
  public float y;
  public float z;

  public Point3df() {
  }

  public Point3df(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public String toString() {
    return String.format("(%f,%f,%s)", x, y, z);
  }

}
