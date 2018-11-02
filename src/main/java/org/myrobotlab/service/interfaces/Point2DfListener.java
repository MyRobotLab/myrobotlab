package org.myrobotlab.service.interfaces;

import org.myrobotlab.math.geometry.Point2Df;

public interface Point2DfListener {

  public String getName();

  public Point2Df onPoint2Df(Point2Df point);
}
