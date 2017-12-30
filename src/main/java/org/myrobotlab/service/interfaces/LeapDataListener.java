package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.LeapData;

public interface LeapDataListener {

  public String getName();

  public LeapData onLeapData(LeapData data);
}
