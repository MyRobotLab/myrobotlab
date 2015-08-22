package org.myrobotlab.service.interfaces;

import java.util.Map;

public interface IKJointAnglePublisher {

	public String getName();

	public Map<String, Float> publishJointAngles(Map<String, Float> angleMap);

}
