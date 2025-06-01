package org.myrobotlab.service.data;

public class Pose {
  public String name = null;
  public Long ts =  null;
  public Position position = null;
  public Orientation orientation = null;

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("name:%s", name));
    if (position != null) {
      sb.append(String.format(" x:%.2f y:%.2f z:%.2f", position.x, position.y, position.z));
    }
    if (orientation != null) {
      sb.append(String.format(" roll:%.2f pitch:%.2f yaw:%.2f", orientation.roll, orientation.pitch, orientation.yaw));
    }
    return sb.toString();
  }
  
  
}
