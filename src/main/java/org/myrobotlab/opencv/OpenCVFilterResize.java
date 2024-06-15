/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_imgproc.cvResize;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.myrobotlab.logging.LoggerFactory;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;

public class OpenCVFilterResize extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  transient IplImage resized;

  private int destWidth = 480;
  private int destHeight = 640;
  // TODO: why the heck do we need to convert back and forth, and is this
  // effecient?!?!
  private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
  private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterResize.class.getCanonicalName());

  public OpenCVFilterResize() {
    super();
  }

  public OpenCVFilterResize(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    resized = AbstractIplImage.createCompatible(image);
  }

  private Mat resizeImage(Mat img, int w, int h) {
    // TODO: move this to a base class.
    Mat resizedMat = new Mat();
    // IplImage resizedImage = IplImage.create(modelSizeX, modelSizeY,
    // img.depth(), img.channels());
    Size sz = new Size(w, h);
    resize(img, resizedMat, sz);
    return resizedMat;
  }

  @Override
  public IplImage process(IplImage image) {
    Mat resizedMat = converterToMat.convertToMat(converterToIpl.convert(image));
    Mat res = resizeImage(resizedMat, destWidth, destHeight);
    return converterToMat.convertToIplImage(converterToIpl.convert(res));
  }

  public static IplImage resizeImage(final IplImage img, float percent) {
    int newWidth = (int) (img.width() * percent);
    int newHeight = (int) (img.height() * percent);
    IplImage ret = AbstractIplImage.create(newWidth, newHeight, img.depth(), img.nChannels());
    cvResize(img, ret, Imgproc.INTER_AREA);
    return ret;
  }

  public IplImage resizeNoAspect(final IplImage image, int width, int height) {
    destWidth = width;
    destHeight = height;
    Mat resizedMat = converterToMat.convertToMat(converterToIpl.convert(image));
    Mat res = resizeImage(resizedMat, destWidth, destHeight);
    return converterToMat.convertToIplImage(converterToIpl.convert(res));
  }

  public static IplImage resizeImageMaintainAspect(final IplImage img, int maxWidth, int maxHeight) {

    int scaledWidth = img.width();
    int scaledHeight = img.height();

    int deltaWidth = maxWidth - img.width();
    int deltaHeight = maxHeight - img.height();

    boolean widthConstrained = (deltaWidth <= deltaHeight);
    boolean heightConstrained = (deltaWidth >= deltaHeight);

    if (widthConstrained) {
      scaledWidth = maxWidth;
      scaledHeight = (scaledWidth * img.height()) / img.width();
    }

    if (heightConstrained) {
      scaledHeight = maxHeight;
      scaledWidth = (scaledHeight * img.width()) / img.height();
    }

    IplImage ret = AbstractIplImage.create(scaledWidth, scaledHeight, img.depth(), img.nChannels());

    // Imgproc.INTER_CUBIC

    cvResize(img, ret, Imgproc.INTER_AREA);
    // IplImage img2 = IplImage.create(maxWidth, maxHeight, img.depth(),
    // img.nChannels());

    // copy into the center

    return ret;
  }
  /*
   * public IplImage resizeWithAspect(IplImage img, int maxWidth, int maxHeight)
   * { int maxArea = maxWidth * maxHeight;
   * 
   * // find the dimension (w or h) where when the original image is scaled //
   * the first dimension which "fits" determines the percentage of what // the
   * image should scale to int dw = Math.abs(maxWidth - img.width()); int dh =
   * Math.abs(maxHeight - img.height());
   * 
   * boolean alignToWidth = (dw )
   * 
   * // if the maxWidth &
   * 
   * int newWidth = (int)(img.width() * percent); int newHeight =
   * (int)(img.height() * percent); IplImage ret = IplImage.create(800, 60,
   * img.depth(), img.nChannels()); cvResize(img, ret, Imgproc.INTER_AREA);
   * 
   * // resize with black padding ....
   * 
   * // then center & copy the image
   * 
   * return ret; }
   */

  public int getDestWidth() {
    return destWidth;
  }

  public void setDestWidth(int destWidth) {
    this.destWidth = destWidth;
  }

  public int getDestHeight() {
    return destHeight;
  }

  public void setDestHeight(int destHeight) {
    this.destHeight = destHeight;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
