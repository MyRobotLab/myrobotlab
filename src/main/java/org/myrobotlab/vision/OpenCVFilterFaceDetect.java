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

package org.myrobotlab.vision;

/*
 import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2HSV;
 import static org.bytedeco.javacpp.opencv_imgproc.CV_HAAR_DO_CANNY_PRUNING;
 import static org.bytedeco.javacpp.opencv_imgproc.cvHaarDetectObjects;
 import static org.bytedeco.javacpp.opencv_core.CV_RGB;
 import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
 import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
 import static org.bytedeco.javacpp.opencv_core.cvDrawLine;
 import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
 import static org.bytedeco.javacpp.opencv_core.cvLoad;
 import static org.bytedeco.javacpp.opencv_core.cvRectangle;
 import static org.bytedeco.javacpp.opencv_core.cvSize;
 import org.bytedeco.javacpp.opencv_imgproc.CvHaarClassifierCascade;
 import org.bytedeco.javacpp.opencv_core.CvMemStorage;
 import org.bytedeco.javacpp.opencv_core.CvPoint;
 import org.bytedeco.javacpp.opencv_core.CvRect;
 import org.bytedeco.javacpp.opencv_core.CvSeq;
 import org.bytedeco.javacpp.opencv_core.IplImage;
 */
import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCreateMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_FIND_BIGGEST_OBJECT;

import java.util.ArrayList;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_objdetect;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Rectangle;
import org.slf4j.Logger;

public class OpenCVFilterFaceDetect extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceDetect.class.getCanonicalName());

  CvMemStorage storage = null;
  public CvHaarClassifierCascade cascade = null; // TODO - was static
  public String cascadeDir = "haarcascades";
  public String cascadeFile = "haarcascade_frontalface_alt2.xml";
  // public String cascadePath = "haarcascades/haarcascade_mcs_lefteye.xml";
  // public String cascadePath =
  // "haarcascades/haarcascade_mcs_eyepair_big.xml";

  int i;

  // public int stablizedFrameCount = 10;
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

  private String state = STATE_LOST_TRACKING;

  int x0, y0, x1, y1;

  public OpenCVFilterFaceDetect() {
    super();
  }

  public OpenCVFilterFaceDetect(String name) {
    super(name);
  }

  @Override
  public IplImage display(IplImage image, VisionData data) {

    if (data != null) {
      ArrayList<Rectangle> bb = data.getBoundingBoxArray();
      if (bb != null) {
        for (int i = 0; i < bb.size(); ++i) {
          Rectangle rect = bb.get(i);

          if (useFloatValues) {
            x0 = (int) (rect.x * width);
            y0 = (int) (rect.y * height);
            x1 = x0 + (int) (rect.width * width);
            y1 = y0 + (int) (rect.height * height);
            cvDrawRect(image, cvPoint(x0, y0), cvPoint(x1, y1), CvScalar.RED, 1, 1, 0);
          } else {
            x0 = (int) rect.x;
            y0 = (int) rect.y;
            x1 = x0 + (int) rect.width;
            y1 = y0 + (int) rect.height;
            cvDrawRect(image, cvPoint(x0, y0), cvPoint(x1, y1), CvScalar.RED, 1, 1, 0);
          }
        }

        return image;
      }
    }

    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
    // Allocate the memory storage TODO make this globalData
    if (storage == null) {
      storage = cvCreateMemStorage(0);
    }

    if (cascade == null) {
      // Preload the opencv_objdetect module to work around a known bug.
      Loader.load(opencv_objdetect.class);

      cascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, cascadeFile)));
      // cascade = new
      // CvHaarClassifierCascade(cvLoad("haarcascades/haarcascade_eye.xml"));

      if (cascade == null) {
        log.error("Could not load classifier cascade");
      }
    }

  }

  @Override
  public IplImage process(IplImage image, VisionData data) {

    // Clear the memory storage which was used before
    cvClearMemStorage(storage);

    // Find whether the cascade is loaded, to find the faces. If yes, then:
    if (cascade != null) {

      // CV_HAAR_DO_CANNY_PRUNING - causes flat regions (no lines) to be
      // skipped
      // CV_HAAR_SCALE_IMAGE
      // CV_HAAR_FIND_BIGGEST_OBJECT - tells the detector to return the
      // biggest - hence # of objects will be 1 or none
      // CV_HAAR_DO_ROUGH_SEARCH

      // faces = cvHaarDetectObjects(grayImage, classifier, storage, 1.1,
      // 3, CV_HAAR_DO_ROUGH_SEARCH | CV_HAAR_FIND_BIGGEST_OBJECT);
      // faces = cvHaarDetectObjects(grayImage, classifier_eyes, storage,
      // 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
      CvSeq faces = cvHaarDetectObjects(image, cascade, storage, 1.1, 1, CV_HAAR_DO_CANNY_PRUNING | CV_HAAR_FIND_BIGGEST_OBJECT);

      if (faces != null) {
        ArrayList<Rectangle> bb = new ArrayList<Rectangle>();
        faceCnt = faces.total();
        // Loop the number of faces found.
        for (i = 0; i < faces.total(); i++) {

          CvRect r = new CvRect(cvGetSeqElem(faces, i));

          Rectangle rect;
          if (useFloatValues) {
            rect = new Rectangle((float) r.x() / width, (float) r.y() / height, (float) r.width() / width, (float) r.height() / height);
          } else {
            rect = new Rectangle(r.x(), r.y(), r.width(), r.height());
          }
          bb.add(rect);

          try {
            // close resource
            r.close();
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        data.put(bb);
      }
    } else {
      cascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, cascadeFile)));
    }

    // WOOHOO LOOK AT THAT A STRING SWITCH !!!
    // 16 years later ! :D
    // converted from compiler into 2 stage hash switch :) cool !
    switch (state) {
      case STATE_LOST_TRACKING:
        if (faceCnt > 0) {
          firstFaceFrame = frameIndex;
          state = STATE_DETECTING_FACE;
          broadcastFilterState();
        }
        break;
      case STATE_DETECTING_FACE:
        if (faceCnt > 0 && frameIndex - firstFaceFrame > minFaceFrames) {
          state = STATE_DETECTED_FACE;
          // broadcastFilterState();
        } else if (faceCnt == 0) {
          firstFaceFrame = frameIndex;
        }
        break;
      case STATE_DETECTED_FACE:
        if (faceCnt == 0) {
          state = STATE_LOSING_TRACKING;
          firstFaceFrame = frameIndex;
          broadcastFilterState();
        }
        break;

      case STATE_LOSING_TRACKING:
        if (faceCnt == 0 && frameIndex - firstEmptyFrame > minEmptyFrames) {
          state = STATE_LOST_TRACKING;
          // broadcastFilterState();
        } else if (faceCnt > 0) {
          firstEmptyFrame = frameIndex;
        }
        break;
      default:
        log.error("invalid state");
        break;
    }
    // face detection events
    if (faceCnt > 0 && frameIndex - firstFaceFrame > minFaceFrames) {

    } else {

    }

    lastFaceCnt = faceCnt;
    return image;
  }

}
