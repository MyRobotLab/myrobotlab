package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_64F;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.meanStdDev;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.Laplacian;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.indexer.DoubleRawIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.junit.Test;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterBlurDetector extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  double blurriness = 0;
  float threshold = 400.0f;

  public OpenCVFilterBlurDetector(String name) {
    super(name);
  }

  public double varianceOfLaplacian(IplImage image) {
    // compute the Laplacian of the image and then return the focus
    // measure, which is simply the variance of the Laplacian
    Mat input = OpenCV.toMat(image);
    Mat output = new Mat();
    Laplacian(input, output, CV_64F);
    Mat mean = new Mat();
    Mat stdDev = new Mat();
    meanStdDev(output, mean, stdDev);
    // float meanF = mean.getFloatBuffer().get();
    DoubleRawIndexer indexer = stdDev.createIndexer();
    double f = indexer.get(0,0);
    indexer.close();
    // stddev squared is the variance.
    return f*f;
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO: what do we do here?
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // gray scale the image.
    IplImage gray = cvCreateImage(image.cvSize(), 8, CV_THRESH_BINARY);
    cvCvtColor(image, gray, CV_BGR2GRAY);
    // compute the variance of the laplacian.
    blurriness = varianceOfLaplacian(gray);
    // TODO: set the CV data to include a blur score
    if (blurriness < threshold) {
      System.out.println("Blurry!");
    } else {
      System.out.println("not blurry!");
    }
    System.out.println("Fizzyness: " + blurriness);
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (blurriness <= threshold) {
      String status = "Blurry     : " + blurriness;
      graphics.drawString(status, 20, 40);
    } else  {
      String status = "Not Blurry : " + blurriness;
      graphics.drawString(status, 20, 40);
    }
    return image;
  }

}
