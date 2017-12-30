package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface RangingControl extends NameProvider {

  public void startRanging();
  public void stopRanging();
  public void setUnitCm();
  public void setUnitInches();
  
}
