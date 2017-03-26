package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.LeapData;

public interface LeapDataPublisher {

  public String getName();

  public LeapData publishLeapData(LeapData data);

}
