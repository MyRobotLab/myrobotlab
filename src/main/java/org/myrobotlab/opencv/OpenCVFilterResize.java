/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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

import static org.bytedeco.javacpp.opencv_imgproc.resize;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
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
    resized = IplImage.createCompatible(image);
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
  public IplImage process(IplImage image, OpenCVData data) {
    Mat resizedMat = converterToMat.convertToMat(converterToIpl.convert(image));
    Mat res = resizeImage(resizedMat, destWidth, destHeight);
    return converterToMat.convertToIplImage(converterToIpl.convert(res));
  }

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

}
