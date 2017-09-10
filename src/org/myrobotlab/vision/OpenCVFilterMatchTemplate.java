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

import static org.bytedeco.javacpp.helper.opencv_core.CV_RGB;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_32F;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvMinMaxLoc;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSize;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_TM_SQDIFF;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvMatchTemplate;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.cvRectangle;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

// TODO - http://opencv.willowgarage.com/wiki/FastMatchTemplate
// FIXME - get template from exterior source
// FIXME - named inputs output - defaults "created"  filterName_input  filternName_output - 
// must create a "input filter (non filter) and a non-filter output
// Think about the difference between publishing vs blocking on data from different thread
// its probably best that this thread run unhindered and therefore "publishing"
// publish list !  - source should be <opencv>_<filtername>_<output/input?>

public class OpenCVFilterMatchTemplate extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterMatchTemplate.class.getCanonicalName());

  double[] minVal = new double[1];
  double[] maxVal = new double[1];

  transient public IplImage template = null;

  transient IplImage res = null;
  transient CvPoint minLoc = new CvPoint();
  transient CvPoint maxLoc = new CvPoint();

  transient CvPoint tempRect0 = new CvPoint();
  transient CvPoint tempRect1 = new CvPoint();

  transient CvPoint centeroid = cvPoint(0, 0);

  int clickCount = 0;

  int x0, y0, x1, y1;

  public CvRect rect = new CvRect();
  public boolean makeTemplate = false;
  CvPoint textpt = cvPoint(10, 20);
  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

  public int matchRatio = Integer.MAX_VALUE;

  boolean isTracking = false;

  public OpenCVFilterMatchTemplate() {
    super();
  }

  public OpenCVFilterMatchTemplate(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, VisionData data) {
    // cvMatchTemplate(iamge, arg1, arg2, arg3);

    /*
     * if (res == null && template != null) // || dim size change { res =
     * cvCreateImage( cvSize( image.width() - template.width() + 1,
     * image.height() - template.height() + 1), IPL_DEPTH_32F, 1 ); }
     */

    // CV_TM_CCOEFF_NORMED
    // cv.cvMatchTemplate(arg0, arg1, arg2, arg3);
    if (template != null && res != null) {
      // TODO - DISPLAY RES SO THAT RESULTS FORM DIFFERENT FN's CAN BE
      // EXAMINED
      cvMatchTemplate(image, template, res, CV_TM_SQDIFF);
      // cvNormalize( ftmp[i], ftmp[i], 1, 0, CV_MINMAX );
      cvMinMaxLoc(res, new DoublePointer(minVal), new DoublePointer(maxVal), minLoc, maxLoc, null);

      tempRect0.x(minLoc.x());
      tempRect0.y(minLoc.y());
      tempRect1.x(minLoc.x() + template.width());
      tempRect1.y(minLoc.y() + template.height());
    }

    if (makeTemplate) {
      makeTemplate = false;
      template = cvCreateImage(cvSize(rect.width(), rect.height()), image.depth(), image.nChannels());
      /* copy ROI to subimg */
      cvSetImageROI(image, rect);
      cvCopy(image, template, null);
      cvResetImageROI(image);
      invoke("publishTemplate", name, OpenCVUtils.IplImageToBufferedImage(template), 0);
      invoke("publishIplImageTemplate", template); // FYI -
      // IplImage
      // is not
      // serializable
      res = cvCreateImage(cvSize(image.width() - template.width() + 1, image.height() - template.height() + 1), IPL_DEPTH_32F, 1);
    }

    if (template != null) {
      // String text = "" + minVal[0];

      // textpt.y(20);
      // cvPutText(image, text, textpt, font, CV_RGB(254, 254, 254));
      textpt.y(20);
      matchRatio = (int) (minVal[0] / ((tempRect1.x() - tempRect0.x()) * (tempRect1.y() - tempRect0.y())));
      cvPutText(image, "" + matchRatio, textpt, font, CV_RGB(254, 254, 254));

      if (matchRatio < 500) {
        // draw rectangle
        cvRectangle(image, tempRect0, tempRect1, cvScalar(255, 255, 255, 0), 1, 0, 0);

        textpt.y(30);
        cvPutText(image, "locked", textpt, font, CV_RGB(254, 254, 254));
        centeroid.x(tempRect0.x() + ((tempRect1.x() - tempRect0.x()) / 2));
        centeroid.y(tempRect0.y() + ((tempRect1.y() - tempRect0.y()) / 2));
        invoke("publish", centeroid);
        if (isTracking != true) // message clutter optimization
        {
          invoke("isTracking", true);
        }
        isTracking = true;

      } else {
        if (isTracking != false) {
          invoke("isTracking", false);
        }
        isTracking = false;
      }
    } // if template != null

    return image;

  }

  public void samplePoint(Float x, Float y) {
    samplePoint((int) (x * width), (int) (y * height));
  }

  public void samplePoint(Integer x, Integer y) {
    // MouseEvent me = (MouseEvent)params[0];
    // if (event.getButton() == 1) {
    if (clickCount % 2 == 0) {
      x0 = x;
      y0 = y;
    } else {
      x1 = x;
      y1 = y;
      rect.x(x0);
      rect.y(y0);
      rect.width(Math.abs(x1 - x0));
      rect.height(Math.abs(y1 - y0));
      makeTemplate = true;
    }
    // }
    ++clickCount;
  }

}
