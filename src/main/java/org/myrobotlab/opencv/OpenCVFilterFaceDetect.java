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

//import static org.bytedeco.opencv.global.opencv_core.cvLoad;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_DO_CANNY_PRUNING;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_DO_ROUGH_SEARCH;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_FIND_BIGGEST_OBJECT;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_SCALE_IMAGE;
//import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_STAGE_MAX;
//import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_FEATURE_MAX;
//import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_MAGIC_VAL;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.myrobotlab.framework.Service;
import org.myrobotlab.io.FileIO;
// import org.bytedeco.opencv.opencv_objdetect;
// import org.bytedeco.opencv.opencv_objdetect.CvHaarClassifierCascade;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

public class OpenCVFilterFaceDetect extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceDetect.class);

  public CascadeClassifier cascade = null; // TODO - was static

  /**
   * our default classifier - pre-trained
   */
  // cannot be this because - gets changed to src/main/resources/resource/OpenCV if src is present !!!!
  // public String cascadeDir = FileIO.gluePathsForwardSlash(Service.getResourceDir(OpenCV.class),"haarcascades");
  public String cascadeDir = "resource/OpenCV/haarcascades";
  public String cascadeFile = "haarcascade_frontalface_alt2.xml";
  // public String cascadeFile = "haarcascade_frontalface_default.xml";

  /**
   * bounding boxes of faces
   */
  ArrayList<Rectangle> bb = null;
  int i;
  double scaleFactor = 1.1;
  int minNeighbors = 1;

  public int minFaceFrames = 10;
  public int minEmptyFrames = 10;
  public int firstFaceFrame = 0;
  public int firstEmptyFrame = 0;
  public int faceCnt = 0;
  public int lastFaceCnt = 0;

  public static final String STATE_LOST_TRACKING = "STATE_LOST_TRACKING";
  public static final String STATE_LOSING_TRACKING = "STATE_LOSING_TRACKING";
  public static final String STATE_DETECTING_FACE = "STATE_DETECTING_FACE";
  public static final String STATE_DETECTED_FACE = "STATE_DETECTED_FACE";

  /**
   * Begin Recognition - which is just a sub-classification of "face"(detection)
   */
  public String trainingDir = "training" + File.separator + "_faces";

  private String state = STATE_LOST_TRACKING;
  int option = CASCADE_DO_CANNY_PRUNING | CASCADE_FIND_BIGGEST_OBJECT; // default
  // int option = 0; // default

  public OpenCVFilterFaceDetect(String name) {
    super(name);
  }

  /**
   * causes flat regions (no lines) to be skipped
   */
  public void addOptionCannyPruning() {
    option |= CASCADE_DO_CANNY_PRUNING;
  }

  public void addOptionRoughSearch() {
    option |= CASCADE_DO_ROUGH_SEARCH;
  }

//  public void addOptionFeatureMax() {
//    option |= CASCADE_FEATURE_MAX;
//  }

  /**
   * tells the detector to return the biggest - hence # of objects will be 1 or
   * none
   */
  public void addOptionFindBiggestObject() {
    option |= CASCADE_FIND_BIGGEST_OBJECT;
  }

//  public void addOptionMagicVal() {
//    option |= CASCADE_MAGIC_VAL;
//  }

  public void addOptionScaleImage() {
    option |= CASCADE_SCALE_IMAGE;
  }

//  public void addStageMax() {
//    option |= CASCADE_STAGE_MAX;
//  }

  /**
   * causes flat regions (no lines) to be skipped
   */
  public void removeOptionCannyPruning() {
    option &= 0xFF ^ CASCADE_DO_CANNY_PRUNING;
  }

  public void removeOptionRoughSearch() {
    option &= 0xFF ^ CASCADE_DO_ROUGH_SEARCH;
  }

//  public void removeOptionFeatureMax() {
//    option &= 0xFF ^ CASCADE_FEATURE_MAX;
//  }

  /**
   * tells the detector to return the biggest - hence # of objects will be 1 or
   * none
   */
  public void removeOptionFindBiggestObject() {
    option &= 0xFF ^ CASCADE_FIND_BIGGEST_OBJECT;
  }

//  public void removeOptionMagicVal() {
//    option &= 0xFF ^ CASCADE_MAGIC_VAL;
//  }

  public void removeOptionScaleImage() {
    option &= 0xFF ^ CASCADE_SCALE_IMAGE;
  }

//  public void removeStageMax() {
//    option &= 0xFF ^ CASCADE_STAGE_MAX;
//  }

  public void setOption(int option) {
    this.option = option;
  }

  @Override
  public void imageChanged(IplImage image) {
    // Allocate the memory storage TODO make this globalData
//    if (storage == null) {
//      storage = cvCreateMemStorage(0);
//    }

    if (cascade == null) {
      // Preload the opencv_objdetect module to work around a known bug.
      // Loader.load(opencv_objdetect.class);

      log.info("Starting new classifier {}", cascadeFile);
      String filename = cascadeDir + File.separator +  cascadeFile;
      // cascade = new CvHaarClassifierCascade()
      cascade = new CascadeClassifier(filename);

      if (cascade == null) {
        log.error("Could not load classifier cascade");
      }
    }

  }

  @Override
  public IplImage process(IplImage image) {

    bb = new ArrayList<Rectangle>();

    // Clear the memory storage which was used before
    //cvClearMemStorage(storage);

    if (image == null) 
      return image;
    // Find whether the cascade is loaded, to find the faces. If yes, then:
    if (cascade != null) {
      RectVector vec = new RectVector();
      
      Mat imageMat = converterToImage.convertToMat(converterToMat.convert(image));
      cascade.detectMultiScale(imageMat, vec);
//      CvSeq faces = cvHaarDetectObjects(image, cascade, storage, scaleFactor, minNeighbors, option);
      if (vec != null) {
        
        faceCnt = (int)vec.size();
        for (i = 0; i < faceCnt; i++) {
          try {
            Rect r = vec.get(i);
            bb.add(new Rectangle(r.x(), r.y(), r.width(), r.height()));
            data.putBoundingBoxArray(bb);
            r.close();
          } catch (Exception e) {
          }
        }
      }
    } else {
      log.info("Creating and loading new classifier instance {}", cascadeFile);
      cascade = new CascadeClassifier(String.format("%s/%s", cascadeDir, cascadeFile));
    }

    switch (state) {
      case STATE_LOST_TRACKING:
        if (faceCnt > 0) {
          firstFaceFrame = opencv.getFrameIndex();
          state = STATE_DETECTING_FACE;
          broadcastFilterState();
        }
        break;
      case STATE_DETECTING_FACE:
        if (faceCnt > 0 && opencv.getFrameIndex() - firstFaceFrame > minFaceFrames) {
          state = STATE_DETECTED_FACE;
          // broadcastFilterState();
        } else if (faceCnt == 0) {
          firstFaceFrame = opencv.getFrameIndex();
        }
        break;
      case STATE_DETECTED_FACE:
        if (faceCnt == 0) {
          state = STATE_LOSING_TRACKING;
          firstFaceFrame = opencv.getFrameIndex();
          broadcastFilterState();
        }
        break;

      case STATE_LOSING_TRACKING:
        if (faceCnt == 0 && opencv.getFrameIndex() - firstEmptyFrame > minEmptyFrames) {
          state = STATE_LOST_TRACKING;
          // broadcastFilterState();
        } else if (faceCnt > 0) {
          firstEmptyFrame = opencv.getFrameIndex();
        }
        break;
      default:
        log.error("invalid state");
        break;
    }
    // face detection events
    if (faceCnt > 0 && opencv.getFrameIndex() - firstFaceFrame > minFaceFrames) {

    } else {

    }
    lastFaceCnt = faceCnt;
    return image;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (bb.size() > 0) {
      for (int i = 0; i < bb.size(); ++i) {
        Rectangle rect = bb.get(i);
        graphics.drawRect((int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
      }
    }
    return image;
  }

}
