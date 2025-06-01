package org.myrobotlab.opencv;

import java.awt.image.BufferedImage;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;

public class CloseableFrameConverter implements AutoCloseable {

  // These are the javacv frame converters
  private Java2DFrameConverter converter = new Java2DFrameConverter();
  private OpenCVFrameConverter.ToIplImage converterToImage = new OpenCVFrameConverter.ToIplImage();
  OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

  public CloseableFrameConverter() {
    // default constructor
  }

  /**
   * converting IplImages to BufferedImages
   * 
   * @param src
   *          the source image
   * @return converted to buffered image
   */
  public BufferedImage toBufferedImage(IplImage src) {
    Frame frame = converterToImage.convert(src);
    return converter.getBufferedImage(frame, 1);
  }

  public BufferedImage toBufferedImage(Frame inputFrame) {
    return converter.getBufferedImage(inputFrame);
  }

  public BufferedImage toBufferedImage(Mat image) {
    return converter.convert(converterToImage.convert(image));
  }

  public Frame toFrame(IplImage image) {
    return converterToImage.convert(image);
  }

  public Frame toFrame(Mat image) {
    return converterToImage.convert(image);
  }

  /**
   * convert BufferedImages to IplImages
   * 
   * @param src
   *          input buffered image
   * @return iplimage converted
   */
  public IplImage toImage(BufferedImage src) {
    return converterToImage.convert(converter.convert(src));
  }

  public IplImage toImage(Frame image) {
    return converterToImage.convertToIplImage(image);
  }

  public IplImage toImage(Mat image) {
    return converterToImage.convert(converterToMat.convert(image));
  }

  public Mat toMat(Frame image) {
    return converterToImage.convertToMat(image);
  }

  public Mat toMat(IplImage image) {
    return converterToMat.convert(converterToMat.convert(image));
  }

  @Override
  public void close() {
    // clean up and release memory!
    converter.close();
    converterToImage.close();
    converterToMat.close();
  }

}
