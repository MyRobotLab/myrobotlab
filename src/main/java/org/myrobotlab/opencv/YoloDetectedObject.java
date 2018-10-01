package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.Rect;

public class YoloDetectedObject {

  public YoloDetectedObject(Rect boundingBox, float confidence, String label, int frameIndex) {
    super();
    this.boundingBox = boundingBox;
    this.confidence = confidence;
    this.label = label;
    this.frameIndex = frameIndex;
  }

  public Rect boundingBox;
  public float confidence;
  public String label;
  public int frameIndex;

  @Override
  public String toString() {
    String box = "X:" + boundingBox.x() + ",Y:" + boundingBox.y() + " W:" + boundingBox.width() + " H:" + boundingBox.height();
    return "YoloDetectedObject [boundingBox=" + box + ", confidence=" + confidence + ", label=" + label + ", frameIndex= " + frameIndex + "]";
  }

}
