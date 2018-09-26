package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.IplImage;

public class YoloDetectedObject {

  public Rect boundingBox;
  public float confidence;
  public String label;
  public int frameIndex;
  public IplImage image;

  public YoloDetectedObject(Rect boundingBox, float confidence, String label, int frameIndex, IplImage cropped) {
    super();
    this.boundingBox = boundingBox;
    this.confidence = confidence;
    this.label = label;
    this.frameIndex = frameIndex;
    this.image = cropped;
  }
  
  
  @Override
  public String toString() {
    String box = "X:" + boundingBox.x() + ",Y:" + boundingBox.y() + " W:" + boundingBox.width() + " H:"+ boundingBox.height();
    return "YoloDetectedObject [boundingBox=" + box + ", confidence=" + confidence + ", label=" + label + ", frameIndex= "+frameIndex+"]";
  }
  
}
