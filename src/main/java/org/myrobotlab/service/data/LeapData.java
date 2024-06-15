package org.myrobotlab.service.data;

import java.io.Serializable;

import com.leapmotion.leap.Frame;

public class LeapData implements Serializable {
  private static final long serialVersionUID = 1L;
  transient public Frame frame;
  public LeapHand leftHand;
  public LeapHand rightHand;
}
