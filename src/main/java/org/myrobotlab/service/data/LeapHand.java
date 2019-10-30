package org.myrobotlab.service.data;

import java.io.Serializable;

public class LeapHand implements Serializable {

  private static final long serialVersionUID = 1L;
  public String type;
  public int thumb;
  public int index;
  public int middle;
  public int ring;
  public int pinky;
  public double palmNormalX;
  public double palmNormalY;
  public double palmNormalZ;
  public double posX;
  public double posY;
  public double posZ;
}