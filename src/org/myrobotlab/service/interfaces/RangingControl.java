package org.myrobotlab.service.interfaces;

public interface RangingControl extends DeviceControl {

  public void startRanging();
  public void stopRanging();
  public void setUnitCm();
  public void setUnitInches();
  
}
