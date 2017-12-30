package org.myrobotlab.openni;

import java.io.Serializable;

public class Skeleton implements Serializable {
  private static final long serialVersionUID = 1L;
  public int frameNumber;
  public int userId;

  public PVector centerOfMass = new PVector();

  public PVector head = new PVector();

  public PVector rightShoulder = new PVector();

  public PVector neck = new PVector();

  public PVector leftShoulder = new PVector();

  public PVector torso = new PVector();

  public PVector leftElbow = new PVector();

  public PVector leftHand = new PVector();

  public PVector rightElbow = new PVector();

  public PVector rightHand = new PVector();

  public PVector rightHip = new PVector();

  public PVector rightKnee = new PVector();

  public PVector rightFoot = new PVector();

  public PVector leftHip = new PVector();

  public PVector leftKnee = new PVector();

  public PVector leftFoot = new PVector();

}
