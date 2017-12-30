package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.kinematics.Point;

public interface PointPublisher {

  public String getName();

  public List<Point> publishPoints(List<Point> points);

}
