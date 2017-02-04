package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.Point2Df;

public interface Point2DfListener {

  public String getName();

  public Point2Df onPoint2Df(Point2Df point);
}
