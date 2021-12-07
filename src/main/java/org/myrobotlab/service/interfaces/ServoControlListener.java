package org.myrobotlab.service.interfaces;

/**
 * control message listener interface. These methods get called when these
 * control events are seen. for example. If the ServoControl has it's moveTo
 * method called, the onMoveTo will be invoked to inform listeners that the
 * servo was moved
 * 
 */
public interface ServoControlListener {

  public void onServoMoveTo(ServoControl sc);

  public void onMoveTo(ServoControl sc);

  // FIXME - static, unimplemented body ? because most
  // rely on the TimeEncoder (except Arduino) so it would be a NOOP ?
  public void onServoSetSpeed(ServoControl sc);

  public void onServoEnable(ServoControl sc);

  public void onServoDisable(ServoControl sc);

  public void onServoStop(ServoControl sc);

  public void onServoEnable(String name);

}
