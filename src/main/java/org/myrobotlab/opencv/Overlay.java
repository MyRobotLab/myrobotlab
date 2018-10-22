package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.service.OpenCV;

public class Overlay {

  public String text;
  public CvPoint pos;
  public CvFont font = new CvFont();
  public CvScalar color;
  
  public Overlay(int x, int y, String color, String format) {
    pos = new CvPoint(x, y);
    text = format;  
    this.color = OpenCV.getColor(color);
    text = format;
  }

  public Overlay(String text, CvPoint pos, CvScalar color, CvFont font) {
    this.text = text;
    this.pos = pos;
    // NOTE: in order for this font to be used, it must be initialized with
    // cvInitFont(font, CV_FONT_HERSHEY_PLAIN, 1.0,1.0)
    this.font = font;
    this.color = color;
  }

}
