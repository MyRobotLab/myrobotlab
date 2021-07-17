package org.myrobotlab.service.interfaces;

/**
 * Servo will invoke these when various control methods are
 * called.  ServoControlListeners will be informed of the control
 * message.
 * 
 * @param sc 
 * @return
 */
public interface ServoControlPublisher {

  public ServoControl publishServoMoveTo(ServoControl sc);

  public ServoControl publishMoveTo(ServoControl sc);

  public ServoControl publishServoSetSpeed(ServoControl sc);

  public ServoControl publishServoEnable(ServoControl sc);

  public ServoControl publishServoDisable(ServoControl sc);

  public ServoControl publishServoStop(ServoControl sc);
  
  public String publishServoEnable(String name);
  
  public void attachServoControlListener(String name);

}
