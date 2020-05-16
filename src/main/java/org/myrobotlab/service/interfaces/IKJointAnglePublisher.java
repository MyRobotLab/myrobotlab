package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.AngleData;

public interface IKJointAnglePublisher {

  public String getName();

  public AngleData publishJointAngles(AngleData angle);

}
