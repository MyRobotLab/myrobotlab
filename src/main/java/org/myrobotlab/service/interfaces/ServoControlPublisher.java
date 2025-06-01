package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.ServoMove;
import org.myrobotlab.service.data.ServoSpeed;

/**
 * Servo will invoke these when various control methods are called.
 * ServoControlListeners will be informed of the control message.
 * 
 */
public interface ServoControlPublisher {

  public ServoMove publishServoMoveTo(ServoMove pos);

  public ServoControl publishMoveTo(ServoControl sc);

  // FIXME - IMPLEMENTED AS A STATIC WITH A BODY - NOT REACHABLE BY METHOD CACHE
  public ServoSpeed publishServoSetSpeed(ServoControl sc);
  /*
   * { return new ServoSpeed(sc.getName(), sc.getSpeed()); }
   */

  public String publishServoEnable(ServoControl sc);

  public String publishServoDisable(ServoControl sc);

  /*
   * FIXME these should be returning name - the event itself is enough info -
   * sending whole servo is excessive
   */
  public ServoControl publishServoStop(ServoControl sc);

  public String publishServoEnable(String name);

  public void attachServoControlListener(String name);

}
