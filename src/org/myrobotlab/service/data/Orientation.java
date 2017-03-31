package org.myrobotlab.service.data;

/**
 * @author GroG
 * 
 * 
 * 
 */
public class Orientation {

  public Double roll = null;
  public Double pitch = null;
  public Double yaw = null;

  // default constructor (values will be null until set)
  public Orientation() {
  }

  // constructor with initial values for roll/pitch/yaw
  public Orientation(Double roll, Double pitch, Double yaw) {
    this.roll = roll;
    this.pitch = pitch;
    this.yaw = yaw;
  }

  public Double getRoll() {
    return roll;
  }

  public Double getPitch() {
    return pitch;
  }

  public Double getYaw() {
    return yaw;
  }

  public void setRoll(Double roll) {
    this.roll = roll;
  }

  public void setPitch(Double pitch) {
    this.pitch = pitch;
  }

  public void setYaw(Double yaw) {
    this.yaw = yaw;
  }

  @Override
  public String toString() {
    return "OculusData [roll=" + roll + ", pitch=" + pitch + ", yaw=" + yaw + "]";
  }

}
