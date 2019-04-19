package org.myrobotlab.service.interfaces;

/**
 * 
 * @author GroG
 * 
 * ServoData is automatically published if there is a backing encoder.
 *
 */
public class ServoData {
  
  static public enum ServoStatus {
    SERVO_START,
    SERVO_POSITION_UPDATE,
    SERVO_STOPPED;
  }
  
  public ServoStatus state;  
  public String name;
  public Double pos;
  public Double targetPos;
  public Double speed;
  
  public ServoData() {
    // TODO: remove this and make the members final.
  }
  
  public ServoData(ServoStatus state, String name, Double pos, Double targetPos, Double speed) {
    this.state = state;
    this.name = name;
    this.pos = pos;
    this.targetPos = targetPos;
    this.speed = speed;
  }

  
}
