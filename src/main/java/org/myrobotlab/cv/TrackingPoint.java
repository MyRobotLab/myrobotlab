package org.myrobotlab.cv;

import org.myrobotlab.math.geometry.Point;

public class TrackingPoint {
  public Float error = null;
  public Integer found = null;
  // identifier/index of point being tracked
  public String id;

  public Point p0;
  public Point p1;

  public TrackingPoint(int id, int x0, int y0, int x1, int y1) {
    this.id = String.format("%d", id);
    p0 = new Point(x0, y0);
    p1 = new Point(x1, y1);
  }

  @Override
  public String toString() {
    return String.format("%s %s->%s found %s error %.2f", id, p0, p1, found, error);
  }
}