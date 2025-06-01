package org.myrobotlab.math.geometry;

public class Line3df {
  Point3df p0 = null;
  Point3df p1 = null;

  public Line3df() {
  }

  public Line3df(float x0, float y0, float z0, float x1, float y1, float z1) {
    p0 = new Point3df(x0, y0, z0);
    p1 = new Point3df(x1, y1, z1);
  }

  public Line3df(Point3df p0, Point3df p1) {
    this.p0 = p0;
    this.p1 = p1;
  }

  @Override
  public String toString() {
    return String.format("(%s,%s)", p0, p1);
  }

}
