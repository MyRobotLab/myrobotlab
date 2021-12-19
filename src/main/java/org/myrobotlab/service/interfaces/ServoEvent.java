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
  final public String name;
  final public Double pos;

  public ServoEvent(final String name, final Double pos) {
    this.name = name;
    this.pos = pos;
  }
  
  public String toString() {
    if (pos == null) {
      return String.format("%s null", name);
    }
    return String.format("%s %.2f", name, pos);
  }

}
