package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.interfaces.ServoEvent.ServoStatus;

public interface ServoStatusPublisher {

  /**
   * 
   * @param eventType
   * @param currentPosUs
   * @return
   */
  ServoEvent publishServoEvent(ServoStatus eventType, Double currentPosUs);
  
}
