package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.LeapMotion.LeapData;

public interface LeapDataListener {

	public String getName();

	public LeapData onLeapData(LeapData data);
}
