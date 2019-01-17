package org.myrobotlab.memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import org.myrobotlab.image.KinectImageNode;

public class NodeDeprecate implements Serializable {

  private static final long serialVersionUID = 1L;
  public int ID = 0;
  public Date timestamp = null;
  public String word = null;
  public ArrayList<KinectImageNode> imageData = null;

  public NodeDeprecate() {
  }
}
