package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_64F;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.meanStdDev;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.Laplacian;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.indexer.DoubleRawIndexer;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;

public class OpenCVFilterBlurDetector extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  // this threshold is only for the display label
  protected float threshold = 100.0f;

  /**
   * This filter will detect how blurry an image is. It does this by computing
   * the variance of the laplacian of the image. More Info on the approach here:
   * https://www.pyimagesearch.com/2015/09/07/blur-detection-with-opencv/
   * 
   * The blurriness score is computed over the entire image.
   * 
   * NOTE: Lower blurriness scores mean the image is more blurry! Scores below
   * 100 tend to be very blurry and scores above 100 tend to be sharper.
   * 
   * @param name
   *          the name of the filter
   * 
   */
  public OpenCVFilterBlurDetector(String name) {
    super(name);
  }

  public static double varianceOfLaplacian(IplImage image) {
    // compute the Laplacian of the image and then return the focus
    // measure, which is simply the variance of the Laplacian
    CloseableFrameConverter converter = new CloseableFrameConverter();
    Mat input = converter.toMat(image);
    Mat output = new Mat();
    Laplacian(input, output, CV_64F);
    Mat mean = new Mat();
    Mat stdDev = new Mat();
    meanStdDev(output, mean, stdDev);
    // float meanF = mean.getFloatBuffer().get();
    DoubleRawIndexer indexer = stdDev.createIndexer();
    double f = indexer.get(0, 0);
    indexer.close();
    converter.close();
    // stddev squared is the variance.
    return f * f;
  }

  @Override
  public void imageChanged(IplImage image) {
    // NoOp?
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // gray scale the image.
    IplImage gray = cvCreateImage(image.cvSize(), 8, CV_THRESH_BINARY);
    cvCvtColor(image, gray, CV_BGR2GRAY);
    // compute the variance of the laplacian.
    data.setBlurriness(varianceOfLaplacian(gray));
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    // The lower the blurriness score, the more blurry the image is (on average)
    Double blurriness = data.getBlurriness();
    if (blurriness == null) {
      return image;
    } else if (blurriness <= threshold) {
      String status = "Blurry     : " + blurriness;
      graphics.setColor(Color.RED);
      graphics.drawString(status, 20, 40);
    } else {
      String status = "Not Blurry : " + blurriness;
      graphics.setColor(Color.BLUE);
      graphics.drawString(status, 20, 40);
    }
    return image;
  }

}
