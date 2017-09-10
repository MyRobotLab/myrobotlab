package org.myrobotlab.image;

import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Date;

import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.service.Vision;

public class KinectImageNode implements Serializable {
  private static final long serialVersionUID = 1L;
  public int ID = 0;
  public Date timestamp = new Date();

  // won't serialize - need type conversion
  public transient IplImage cvCameraFrame = null;
  public transient IplImage cvMask = null;
  public transient IplImage cvCropped = null;
  public CvRect cvBoundingBox = null;
  // public transient IplImage cvGrayFrame = null;

  public SerializableImage cameraFrame = null;
  public SerializableImage mask = null;
  public SerializableImage cropped = null;
  // public Rectangle boudingBox = null;
  public Rectangle boundingBox = null;
  public SerializableImage template = null;
  public String imageFilePath = null;

  public int lastGoodFitIndex = 0;

  public void convertToSerializableTypes() {
    cameraFrame = new SerializableImage(Vision.IplImageToBufferedImage(cvCameraFrame), "camera");
    mask = new SerializableImage(Vision.IplImageToBufferedImage(cvMask), "frame");
  }

  public IplImage getTemplate() {
    cvSetImageROI(cvMask, cvBoundingBox); // 615-8 = to remove right hand
    // band
    IplImage template = cvMask.clone(); //
    cvResetImageROI(cvMask);
    return template;
  }

}
