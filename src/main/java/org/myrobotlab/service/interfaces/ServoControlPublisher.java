package org.myrobotlab.service.interfaces;

public interface ServoControlPublisher {
  /**
   * publishing servo's move
   * 
   * @param sc
   * @return
   */
  /**
   * control message publishing moveTo
   * 
   * @param sc
   * @return
   */
  ServoControl publishServoMoveTo(ServoControl sc);

  ServoControl publishMoveTo(ServoControl sc);

  ServoControl publishServoSetSpeed(ServoControl sc);

  ServoControl publishServoEnable(ServoControl sc);

  ServoControl publishServoDisable(ServoControl sc);

  /**
   * Publishing topic for a servo stop event - returns position
   * 
   * @param sc
   * @return
   */
  ServoControl publishServoStop(ServoControl sc);
  
  String publishServoEnable(String name); 

}
