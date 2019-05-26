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

import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.CvSize;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_calib3d.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_features2d.*;
import static org.bytedeco.opencv.global.opencv_flann.*;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_ml.*;
import static org.bytedeco.opencv.global.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_photo.*;
import static org.bytedeco.opencv.global.opencv_shape.*;
import static org.bytedeco.opencv.global.opencv_stitching.*;
import static org.bytedeco.opencv.global.opencv_video.*;
import static org.bytedeco.opencv.global.opencv_videostab.*;

public class OpenCVFilterBoundingBoxToFile extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterBoundingBoxToFile.class);

  transient Java2DFrameConverter converterToJava = new Java2DFrameConverter();

  String targetDir;

  public OpenCVFilterBoundingBoxToFile(String name) {
    super(name);

  }

  @Override
  public void imageChanged(IplImage image) {
    targetDir = String.format("%s.%s", opencv.getName(), name);
    File f = new File(targetDir);
    f.mkdirs();
  }

  @Override
  public IplImage process(IplImage image) {

    List<Rectangle> bb = data.getBoundingBoxArray();
    if (bb != null) {
      for (int i = 0; i < bb.size(); ++i) {

        Rectangle r = bb.get(i);
        CvRect roiRect = new CvRect((int) r.x, (int) r.y, (int) r.width, (int) r.height);
        cvSetImageROI(image, roiRect);
        IplImage copy = cvCreateImage(new CvSize((int) r.width, (int) r.height), image.depth(), image.nChannels());
        cvCopy(image, copy, null); // roi vs mask ?

        saveToFile(String.format("%s" + File.separator + "%07d-%03d.png", targetDir, opencv.getFrameIndex(), i), copy);
        // cvSaveImage(String.format("%s"+File.separator+"%07d-%03d.png",
        // targetDir, opencv.getFrameIndex(), i), copy);
        roiRect.close();
        roiRect = new CvRect(0, 0, image.width(), image.width());
        cvSetImageROI(image, roiRect);
        roiRect.close();
      }
    }

    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

}
