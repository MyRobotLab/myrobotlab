package org.myrobotlab.service.interfaces;

public interface ServoControlListener {
  /**
   * publishing servo's move
   * 
   * @param sc
   * @return
   */
  /**
   * control message listener interface.  This handles events like servo moving
   * start/stop/ set speed ..
   * 
   * TODO: this interface overlaps a bit with ServoStatusListener 
   * 
   * @param sc
   * @return
   */
  void onServoMoveTo(ServoControl sc);

  void onMoveTo(ServoControl sc);

  void onServoSetSpeed(ServoControl sc);

  void onServoEnable(ServoControl sc);

  void onServoDisable(ServoControl sc);

  /**
   * Publishing topic for a servo stop event - returns position
   * 
   * @param sc
   * @return
   */
  void onServoStop(ServoControl sc);

}
