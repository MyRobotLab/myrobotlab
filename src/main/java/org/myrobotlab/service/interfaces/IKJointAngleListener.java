package org.myrobotlab.service.interfaces;

import java.util.Map;

public interface IKJointAngleListener {

  public String getName();

  public void onJointAngles(Map<String, Double> angleMap);

}
