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

package org.myrobotlab.opencv;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Parallel;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * transparent overlay -
 * https://stackoverflow.com/questions/36921496/how-to-join-png-with-alpha-transparency-in-a-frame-in-realtime/37198079#37198079
 * http://jepsonsblog.blogspot.com/2012/10/overlay-transparent-image-in-opencv.html
 * https://www.pyimagesearch.com/2016/03/07/transparent-overlays-with-opencv/
 * http://bistr-o-mathik.org/2012/06/13/simple-transparency-in-opencv/
 * https://stackoverflow.com/questions/40895785/using-opencv-to-overlay-transparent-image-onto-another-image
 * 
 * @author GroG
 *
 */
public class OpenCVFilterAddMask extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterAddMask.class);
  transient Mat transparency = OpenCVFilter.loadMat("transparent-bubble.png");
  public String transparencyFileName;

  double opacity = 0.4;

  transient private CloseableFrameConverter converter1 = new CloseableFrameConverter();
  transient private CloseableFrameConverter converter2 = new CloseableFrameConverter();

  public OpenCVFilterAddMask(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {

    if (transparency != null) {
      transparency = OpenCVFilter.loadMat("transparent-bubble.png");
      Mat targetImage = converter1.toMat(image.clone()); // toMat(image);
      Mat resultImage = targetImage.clone();
      // IplImage src = cvCreateImage(cvGetSize(srcColor), IPL_DEPTH_8U, 1);
      // blendFast(transparency, targetImage, resultImage);
      blendFast(transparency, targetImage, resultImage);

      // FIXME have a cvAddWeighted filter
      /**
       * <pre>
       *  overlay using geometric shapes
       IplImage output = copy(image);
       IplImage overlay = copy(image);
       cvRectangle(overlay, new int[] { 420, 205 }, new int[] { 595, 385 }, cvScalar(0, 0, 255, 1));
       show(overlay, "output");
       cvAddWeighted(overlay, opacity, output, 1 - opacity, 0.0, output);
       show(output, "output");
       log.info("here");
       * </pre>
       */
      return converter2.toImage(resultImage.clone());
    }

    return image;
  }

  @Override
  public void release() {
    // TODO Auto-generated method stub
    super.release();
    converter1.close();
    converter2.close();
  }

  int cnt = 0;

  /** Does alpha blending with high-performance Indexer from JavaCPP. */
  final void blendFast(final Mat rgbaImg, final Mat bgrImg, final Mat dstImg) {
    try {
      final UByteIndexer rgbaIdx = rgbaImg.createIndexer();
      final UByteIndexer bgrIdx = bgrImg.createIndexer();
      final UByteIndexer dstIdx = dstImg.createIndexer();
      final int rows = rgbaImg.rows(), cols = rgbaImg.cols();

      log.debug("cnt {}", ++cnt);
      Parallel.loop(0, rows, new Parallel.Looper() {
        @Override
        public void loop(int from, int to, int looperID) {
          for (int i = from; i < to; i++) {
            for (int j = 0; j < cols; j++) {
              float a = rgbaIdx.get(i, j, 3) * (1.0f / 255.0f);
              float x = bgrIdx.get(i, j, 0);
              float b = rgbaIdx.get(i, j, 2) * a + bgrIdx.get(i, j, 0) * (1.0f - a);
              float g = rgbaIdx.get(i, j, 1) * a + bgrIdx.get(i, j, 1) * (1.0f - a);
              float r = rgbaIdx.get(i, j, 0) * a + bgrIdx.get(i, j, 2) * (1.0f - a);
              dstIdx.put(i, j, 0, (byte) b);
              dstIdx.put(i, j, 1, (byte) g);
              dstIdx.put(i, j, 2, (byte) r);
            }
          }
        }
      });
      rgbaIdx.release();
      bgrIdx.release();
      dstIdx.release();
    } catch (IndexOutOfBoundsException ex) {
      log.error("blendFast threw a out of bound index");
    } catch (RuntimeException ey) {
      log.error("blendFast threw a runtime exception");
    } catch (Exception e) {
      log.error("blendFast threw", e);
    }
  }

  public void test() {

    Mat transparency = OpenCVFilter.loadMat("transparent-bubble.png");
    Mat targetImage = OpenCVFilter.loadMat("hf512.jpg");
    Mat resultImage = targetImage.clone();
    // IplImage src = cvCreateImage(cvGetSize(srcColor), IPL_DEPTH_8U, 1);
    blendFast(transparency, targetImage, resultImage);
    show(resultImage, "dstImg");
    // setMask("https://upload.wikimedia.org/wikipedia/commons/6/6b/Bubble_3.jpg");
    setMask("transparent-bubble.png");
  }

  public void setMask(String maskName) {
    transparency = OpenCVFilter.loadMat(maskName);
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

  public static void main(String[] args) {
    try {
      Runtime.start("gui", "SwingGui");
      OpenCV cv = (OpenCV) Runtime.start("cv", "OpenCV");

      OpenCVFilterAddMask mask = new OpenCVFilterAddMask("mask");
      cv.addFilter(mask);
      mask.test();

      boolean done = true;
      if (done) {
        return;
      }

      cv.capture("src\\test\\resources\\OpenCV\\multipleFaces.jpg");
      /*
       * mask.setMask("src/test/resources/OpenCV/transparent-bubble.jpg");
       */

      cv.addFilter(mask);
      cv.capture();
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
