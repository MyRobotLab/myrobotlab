package org.myrobotlab.service.interfaces;

/**
 * 
 * @author GroG
 * 
 *         ServoEvent is automatically published if there is a backing encoder.
 *         TimeEncoder works for all Servos
 *
 */
public class ServoEvent {
  final public ServoStatus state;
  final public String name;
  final public Double pos;

  static public enum ServoStatus {
    SERVO_STARTED, SERVO_STOPPED;
  }

  public ServoEvent(final ServoStatus state, final String name, final Double pos) {
    this.state = state;
    this.name = name;
    this.pos = pos;
  }

}
