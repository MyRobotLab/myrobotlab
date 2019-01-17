package org.myrobotlab.math.geometry;

public class Line2df {
  Point2df p0 = null;
  Point2df p1 = null;

  public Line2df() {
  }

  public Line2df(float x0, float y0, float x1, float y1) {
    p0 = new Point2df(x0, y0);
    p1 = new Point2df(x1, y1);
  }

  public Line2df(Point2df p0, Point2df p1) {
    this.p0 = p0;
    this.p1 = p1;
  }

  public String toString() {
    return String.format("(%s,%s)", p0, p1);
  }

}
