package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.Orientation;

public interface OrientationPublisher {

  public String getName();

  public Orientation publishOrientation(Orientation data);

  public void attach(OrientationListener listener);

  public void detach(OrientationListener listener);

  /**
   * start publishing orientation
   */
  public void start();

  /**
   * stop publishing orientation
   */
  public void stop();

  @Deprecated /* use start */
  public void startOrientationTracking();

  @Deprecated /* use stop */
  public void stopOrientationTracking();
}
