package org.myrobotlab.service.interfaces;

/**
 * 
 * @author GroG
 * 
 *         ServoData is automatically published if there is a backing encoder.
 *         TimeEncoder works for all Servos
 *
 */
public class ServoData {
  final public ServoStatus state;
  final public String name;
  final public Double pos;

  static public enum ServoStatus {
    SERVO_START, SERVO_POSITION_UPDATE, SERVO_STOPPED;
  }

  public ServoData(final ServoStatus state, final String name, final Double pos) {
    this.state = state;
    this.name = name;
    this.pos = pos;
  }

}
