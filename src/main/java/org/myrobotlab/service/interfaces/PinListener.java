package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.PinData;

public interface PinListener extends Listener {

  public void setPin(String pin);

  public String getPin();
  
  // public boolean isEnabled();

  public void onPin(PinData pindata);
}
