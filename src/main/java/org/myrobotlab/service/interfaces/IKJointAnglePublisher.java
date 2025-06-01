package org.myrobotlab.service.interfaces;

import java.util.Map;

public interface IKJointAnglePublisher {

  public String getName();

  public Map<String, Double> publishJointAngles(Map<String, Double> angleMap);

}
