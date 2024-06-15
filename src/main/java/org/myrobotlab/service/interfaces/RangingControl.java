package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface RangingControl extends NameProvider {

  /**
   * This method starts the ranging process. 
   * Ranging can be done as a single ping or as a continuous stream of pings.
   */
  public void startRanging();

  /**
   * This method stops the ranging process. 
   * Ranging can be done as a single ping or as a continuous stream of pings.
   */
  public void stopRanging();

  /**
   * The measured distance can be either Metric (centimeters) or imperial (inches).
   * This method sets the returned ranging value to Metric centimeters.
   */
  public void setUnitCm();

  /**
   * The measured distance can be either Metric (centimeters) or imperial (inches).
   * This method sets the returned ranging value to Imperial Inches.
   */
  public void setUnitInches();

}
