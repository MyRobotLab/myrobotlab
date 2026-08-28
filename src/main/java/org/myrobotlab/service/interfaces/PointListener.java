package org.myrobotlab.service.interfaces;

import org.myrobotlab.kinematics.Point;

/**
 * Receives a single Cartesian point. JMonkeyEngine publishes OAK-D mesh clicks
 * as world meters (Y-up) to {@code onPoint}.
 */
public interface PointListener {

  String getName();

  void onPoint(Point point);
}
