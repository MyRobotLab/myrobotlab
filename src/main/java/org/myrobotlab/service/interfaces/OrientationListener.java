package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.Orientation;

public interface OrientationListener {

  public String getName();

  public Orientation onOrientation(Orientation data);
}
