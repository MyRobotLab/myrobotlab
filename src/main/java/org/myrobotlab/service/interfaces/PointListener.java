package org.myrobotlab.service.interfaces;

import org.myrobotlab.kinematics.Point;

/**
 * Receives a single Cartesian point.
 * <p>
 * {@link org.myrobotlab.service.JMonkeyEngine} publishes an OAK-D mesh
 * click as world meters (Y-up) on {@code publishClickPoint}. MyRobotLab
 * delivers that point to {@link #onPoint(Point)}.
 * {@link org.myrobotlab.service.Fabrik} implements this interface and moves
 * an arm to the clicked location.
 */
public interface PointListener {

  /**
   * Runtime name of this listener.
   * <p>
   * Publishers use the name to attach {@code publishClickPoint} to
   * {@link #onPoint(Point)}.
   *
   * @return service name
   */
  String getName();

  /**
   * Accept one Cartesian point.
   *
   * @param point
   *          target position. OAK-D overlay clicks are world meters with Y
   *          up. A {@code null} point means there is no target.
   */
  void onPoint(Point point);
}
