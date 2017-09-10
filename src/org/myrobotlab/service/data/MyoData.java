package org.myrobotlab.service.data;

import java.io.Serializable;

public class MyoData implements Serializable {

  private static final long serialVersionUID = 1L;

  public long timestamp;

  public double roll = 0;
  public double pitch = 0;
  public double yaw = 0;
  public String currentPose = null;

  public MyoData() {
  }

  // constructor with initial values for roll/pitch/yaw
  public MyoData(long timestamp, double roll, double pitch, double yaw, String currentPose) {
    this.timestamp = timestamp;
    this.roll = roll;
    this.pitch = pitch;
    this.yaw = yaw;
    this.currentPose = currentPose;
  }

  @Override
  public String toString() {
    return "MyoData [roll=" + roll + ", pitch=" + pitch + ", yaw=" + yaw + ", pose=" + currentPose + "]";
  }

}
