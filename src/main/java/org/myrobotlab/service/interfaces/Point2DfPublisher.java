package org.myrobotlab.service.interfaces;

import org.myrobotlab.math.geometry.Point2df;

public interface Point2DfPublisher {

  public String getName();

  public Point2df publishPoint2Df(Point2df point);
}