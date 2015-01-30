package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.LeapMotion2.LeapData;

public interface LeapDataPublisher {
	
	public LeapData publishLeapData(LeapData data);
	
	public String getName();

}
