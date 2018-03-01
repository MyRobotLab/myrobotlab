package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.Rect;

public class YoloDetectedObject {

  public YoloDetectedObject(Rect boundingBox, float confidence, String label) {
    super();
    this.boundingBox = boundingBox;
    this.confidence = confidence;
    this.label = label;
  }
  
  public Rect boundingBox;
  public float confidence;
  public String label;
  
}
