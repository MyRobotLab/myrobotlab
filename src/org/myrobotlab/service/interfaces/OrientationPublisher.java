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
  public void startOrientationTracking();
  
  /**
   * stop publishing orientation
   */
  public void stopOrientationTracking();
}

