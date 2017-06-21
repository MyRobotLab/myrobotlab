package org.myrobotlab.service.interfaces;

public interface RangingControl extends Attachable {

  public void startRanging();
  public void stopRanging();
  public void setUnitCm();
  public void setUnitInches();
  
}
