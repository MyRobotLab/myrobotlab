package org.myrobotlab.kinematics;

public class GesturePart {

  /**
   * name of pose
   */
  public String name;
  
  /**
   * type determines how to handle the value 
   * depending on what is desired ...
   */
  public String type; // Pose | Text | Delay | Message
  
  /**
   * delay type when type is Delay, String when type is Text
   */
  public Object value;
  
  /**
   * if blocking true will wait until sequence part finished
   */
  public boolean blocking = false;


  @Override
  public String toString() {
    return String.format("part %s %s %s", name, type, (value != null)?value.toString():null);
  }
}
