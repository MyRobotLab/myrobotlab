package org.myrobotlab.opencv;

import org.bytedeco.opencv.opencv_core.RotatedRect;

public class DetectedText {

  public RotatedRect box;
  public float confidence;
  public String text;

  public DetectedText(RotatedRect box, float confidence, String text) {
    super();
    this.box = box;
    this.confidence = confidence;
    this.text = text;
  }

  @Override
  public String toString() {
    return "DetectedText [box=" + box + ", confidence=" + confidence + ", text=" + text + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((box == null) ? 0 : box.hashCode());
    result = prime * result + Float.floatToIntBits(confidence);
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DetectedText other = (DetectedText) obj;
    if (box == null) {
      if (other.box != null)
        return false;
    } else if (!box.equals(other.box))
      return false;
    if (Float.floatToIntBits(confidence) != Float.floatToIntBits(other.confidence))
      return false;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    return true;
  }

}
