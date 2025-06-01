package org.myrobotlab.service.interfaces;

import org.myrobotlab.math.geometry.Point2df;

public interface Point2DfListener {

  public String getName();

  public Point2df onPoint2Df(Point2df point);
}
