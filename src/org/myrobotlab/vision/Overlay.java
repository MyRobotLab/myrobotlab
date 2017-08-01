package org.myrobotlab.vision;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;

public class Overlay {

  public String text;
  public CvPoint pos;
  public CvFont font;
  public CvScalar color;

  public Overlay(String text, CvPoint pos, CvScalar color, CvFont font) {
    this.text = text;
    this.pos = pos;
    // NOTE: in order for this font to be used, it must be initialized with
    // cvInitFont(font, CV_FONT_HERSHEY_PLAIN, 1.0,1.0)
    this.font = font;
    this.color = color;
  }

}
