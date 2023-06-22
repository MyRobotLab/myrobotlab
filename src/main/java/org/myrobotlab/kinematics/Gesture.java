package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.List;

/** represent a set of servo positions at a given point in time */
public class Gesture {

  /**
   * sequence of poses and offset times
   */
  protected List<GesturePart> parts = new ArrayList<>();

  protected boolean repeat = false;
  
  public List<GesturePart> getParts(){
    return parts;
  }
  
  public boolean getRepeat() {
    return repeat;
  }
  
  public void setParts(List<GesturePart> parts){
    this.parts = parts;
  }
  
  public void setRepeat(boolean repeat) {
    this.repeat = repeat;
  }
  
  

}
