package org.myrobotlab.service.interfaces;

import java.util.HashMap;
import java.util.Map;

public interface IKJointAnglePublisher {

  public String getName();

  public Map<String, Double> publishJointAngles(HashMap<String, Double> angleMap);

}
