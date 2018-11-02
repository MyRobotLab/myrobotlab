package org.myrobotlab.service.interfaces;

import org.myrobotlab.math.geometry.Point2Df;

public interface Point2DfPublisher {

  public String getName();

  public Point2Df publishPoint2Df(Point2Df point);
}