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

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCircle;
import static org.bytedeco.opencv.global.opencv_imgproc.cvInitFont;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPutText;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.KeyPoint;
import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_features2d.SimpleBlobDetector;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.slf4j.Logger;

public class OpenCVFilterSimpleBlobDetector extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSimpleBlobDetector.class.getCanonicalName());

  public ArrayList<Point2df> pointsToPublish = new ArrayList<Point2df>();
  transient CvFont font = new CvFont();

  public OpenCVFilterSimpleBlobDetector() {
    super();
    cvInitFont(font, CV_FONT_HERSHEY_PLAIN, 1.0, 1.0);
  }

  public OpenCVFilterSimpleBlobDetector(String name) {
    super(name);
    // TODO: what / when should we initialize this ?
    cvInitFont(font, CV_FONT_HERSHEY_PLAIN, 1.0, 1.0);
  }

  @Override
  public IplImage process(IplImage image) {
    // TODO: track an array of blobs , not just one.
    SimpleBlobDetector o = new SimpleBlobDetector();

    // KeyPoint point = new KeyPoint();
    KeyPointVector pv = new KeyPointVector();

    // TODO: i'd like to detect all the points at once..
    // can i pass an array or something like that? hmm.
    // TODO: this is null?! we blow up! (after javacv upgrade)

    o.detect(new Mat(image), pv);

    // close this o/w you could leak something i guess?
    o.close();

    // System.out.println(point.toString());
    if (pv.size() == 0) {
      log.error("no key points");
      return image;
    }

    // FIXME - go through for loop to get them all ?
    KeyPoint point = pv.get(0);

    float x = point.pt().x();
    float y = point.pt().y();
    if (x == 0 && y == 0) {
      // ignore the zero / zero point
      return image;
    }
    // pointsToPublish.clear();
    // min distance to an existing point ?
    // up to 25 pixels away?
    double minDist = 20.0;
    // Is this a new blob? or an old blob?
    boolean dupPoint = false;
    for (Point2df p : pointsToPublish) {
      double dist = Math.sqrt((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y));
      if (dist < minDist) {
        // we already have this point ?
        dupPoint = true;
        break;
      }
    }

    if (!dupPoint) {
      pointsToPublish.add(new Point2df(x, y));
      log.info("There are " + pointsToPublish.size() + " blobs.");
    }
    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  public void clearPoints() {
    pointsToPublish.clear();
  }

  public int getNumberOfBlobs() {
    return pointsToPublish.size();
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {

    for (int i = 0; i < pointsToPublish.size(); ++i) {
      Point2df point = pointsToPublish.get(i);
      graphics.drawOval((int) point.x, (int) point.y, 5, 5);
    }
    graphics.drawString(String.format("Blobs Found: %d", pointsToPublish.size()), 20, 40);
    return image;
  }
}
