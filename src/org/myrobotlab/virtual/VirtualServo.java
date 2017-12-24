package org.myrobotlab.virtual;

public interface VirtualServo extends VirtualObject {
  // Fixme - needs a 3d axis vector
  
  public void writeMicroseconds(int posUs);
  
  public void attach(int pin);
  
  public void detach();
}
