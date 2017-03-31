package org.myrobotlab.service.interfaces;

import java.util.List;

import org.myrobotlab.kinematics.Point;

public interface PointsListener {

  public String getName();

  public void onPoints(List<Point> p);

}
