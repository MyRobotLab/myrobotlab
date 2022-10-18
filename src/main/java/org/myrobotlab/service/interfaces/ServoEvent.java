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
  /**
   * name of the servo this event came from
   */
  final public String name;
  /**
   * I believe this is OUTPUT since it comes from the controller ??? IS IT
   * ALWAYS OUTPUT ?
   */
  final public Double pos;

  public ServoEvent(final String name, final Double pos) {
    this.name = name;
    this.pos = pos;
  }

  @Override
  public String toString() {
    if (pos == null) {
      return String.format("%s null", name);
    }
    return String.format("%s %.2f", name, pos);
  }

}
