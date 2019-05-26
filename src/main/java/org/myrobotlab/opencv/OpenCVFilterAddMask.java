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

import static org.bytedeco.opencv.global.opencv_core.cvAddWeighted;
import static org.bytedeco.opencv.global.opencv_core.cvScalar;
import static org.bytedeco.opencv.global.opencv_imgproc.cvRectangle;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacv.Parallel;
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
  IplImage mask = null;
  public String maskName;

  transient IplImage dst = null;
  transient IplImage negativeImage = null;

  public OpenCVFilterAddMask(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    dst = null;
  }

  double opacity = 0.4;

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    if (mask != null) {

      // Convert uint8 to float
      // foreground = foreground.astype(float);
      // background = background.astype(float);

      // Normalize the alpha mask to keep intensity between 0 and 1
      // alpha = alpha.astype(float)/255

      // background.convertTo(background, CV_32FC3);

      // mask.convertTo(mask, CV_32FC3, 1.0/255, 0.0);

      // Multiply the foreground with the alpha matte
      // MatExpr foreground = multiply(mask, background);

      // Multiply the background with ( 1 - alpha )
      // background = multiply(1.0 - alpha, background);

      // Add the masked foreground and background.
      // MatExpr outImage = add(mask, background);

      // THIS IS THE ONE !!!
      // http://bistr-o-mathik.org/2012/06/13/simple-transparency-in-opencv/
      // cv2.addWeighted(overlay, opacity, img, 1 - opacity, 0, img)
      // show(image, "output");

      // cvAdd(src1, src2, overlay);
      IplImage output = copy(image);
      // cvAdd(src1, src2, output);
      IplImage overlay = copy(image);
      // Nd4j.concat(0, image, image);
      /*
       * 
       * cvSetImageROI(output, cvRect(0, 0, mask.width(), mask.height()));
       * cvAdd(mask, mask, output); cvResetImageROI(overlay);
       */

      cvRectangle(overlay, new int[] { 420, 205 }, new int[] { 595, 385 }, cvScalar(0, 0, 255, 1));
      show(overlay, "output");
      // cvFill

      // must be the same size
      cvAddWeighted(overlay, opacity, output, 1 - opacity, 0.0, output);

      show(output, "output");
      // Display image
      // imshow("outImg", outImage/255);
      log.info("here");
      /*
       * // Convert Mat to float data type mask.convertTo(mask, CV_32FC3);
       * background.convertTo(background, CV_32FC3);
       * 
       * // Normalize the alpha mask to keep intensity between 0 and 1
       * alpha.convertTo(alpha, CV_32FC3, 1.0/255); //
       * 
       * // Storage for output image Mat ouImage = Mat::zeros(mask.size(),
       * mask.type());
       * 
       * // Multiply the mask with the alpha matte multiply(alpha, mask, mask);
       * 
       * // Multiply the background with ( 1 - alpha )
       * multiply(Scalar::all(1.0)-alpha, background, background);
       * 
       * // Add the masked mask and background. add(mask, background, ouImage);
       */

    }

    return image;
  }

  /** Does alpha blending with high-performance Indexer from JavaCPP. */
  static void blendFast(final Mat rgbaImg, final Mat bgrImg, final Mat dstImg) {
    final UByteIndexer rgbaIdx = rgbaImg.createIndexer();
    final UByteIndexer bgrIdx = bgrImg.createIndexer();
    final UByteIndexer dstIdx = dstImg.createIndexer();
    final int rows = rgbaImg.rows(), cols = rgbaImg.cols();

    Parallel.loop(0, rows, new Parallel.Looper() {
      public void loop(int from, int to, int looperID) {
        for (int i = from; i < to; i++) {
          for (int j = 0; j < cols; j++) {
            float a = rgbaIdx.get(i, j, 3) * (1.0f / 255.0f);
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
  }

  /*
   * void overlayImage(final Mat background, final Mat foreground, Mat output,
   * int posX, int posY){
   * 
   * background.copyTo(output);
   * 
   * BytePointer bbp = background.data(); BytePointer fbp = foreground.data();
   * bbp.asByteBuffer();
   * 
   * // start at the row indicated by location, or at row 0 if posY is negative.
   * for(int y = Math.max(posY , 0); y < background.rows(); ++y) { int fY = y -
   * posY; // because of the translation
   * 
   * // we are done of we have processed all rows of the foreground image. if(fY
   * >= foreground.rows()) break;
   * 
   * // start at the column indicated by location,
   * 
   * // or at column 0 if posX is negative. for(int x = Math.max(posX, 0); x <
   * background.cols(); ++x) { int fX = x - posX; // because of the translation.
   * 
   * // we are done with this row if the column is outside of the foreground
   * image. if(fX >= foreground.cols()) break;
   * 
   * 
   * 
   * // determine the opacity of the foregrond pixel, using its fourth (alpha)
   * channel. double opacity = ((double)foreground.data[fY *
   * (int)foreground.step() + fX * foreground.channels() + 3])
   * 
   * / 255.;
   * 
   * 
   * // and now combine the background and foreground pixel, using the opacity,
   * 
   * // but only if opacity > 0. for(int c = 0; opacity > 0 && c <
   * output.channels(); ++c) { char foregroundPx = foreground.data[fY *
   * foreground.step() + fX * foreground.channels() + c]; char backgroundPx =
   * background.data[y * background.step() + x * background.channels() + c];
   * output.data[y*output.step + output.channels()*x + c] = backgroundPx *
   * (1.-opacity) + foregroundPx * opacity; } } } }
   * 
   */

  public void test() {

    Mat rgbaImg = OpenCVFilter.loadMat("transparent-bubble.png");
    Mat bgrImg = OpenCVFilter.loadMat("hf512.jpg");
    Mat dstImg = bgrImg.clone();
    // IplImage src = cvCreateImage(cvGetSize(srcColor), IPL_DEPTH_8U, 1);
    blendFast(rgbaImg, bgrImg, dstImg);
    show(dstImg, "dstImg");
    // setMask("https://upload.wikimedia.org/wikipedia/commons/6/6b/Bubble_3.jpg");
    setMask("transparent-bubble.png");
  }

  public void setMask(String maskName) {
    mask = loadImage(maskName);
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
