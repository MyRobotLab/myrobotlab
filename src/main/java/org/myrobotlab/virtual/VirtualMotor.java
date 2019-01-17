package org.myrobotlab.virtual;

public interface VirtualMotor {

  public void moveTo(int currentPosUs);

  public void move(int initPosUs);

}
