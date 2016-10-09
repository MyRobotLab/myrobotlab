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

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGet2D;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCanny;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDilate;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawLine;

import java.util.ArrayList;
import java.util.HashMap;

import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/*
 * 
 * You can use cvFindContours to get the contours and once you 
 * have the contours then use the cvContourPerimeter to get the perimeter of the contour
 * 
 */

public class OpenCVFilterMouse extends OpenCVFilter {

  public final class Node {
    public int x;
    public int y;
    public int state;

    public Node(int inx, int iny, int instate) {
      x = inx;
      y = iny;
      state = instate;
    }
  }

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterMouse.class.getCanonicalName());
  int stepSize = 1;

  CvPoint startPoint = null;

  CvPoint mousePos = null;
  final public int NONE = -1;
  final public int NORTH = 1;
  final public int NORTHWEST = 2;
  final public int WEST = 3;
  final public int SOUTHWEST = 4;
  final public int SOUTH = 5;
  final public int SOUTHEAST = 6;
  final public int EAST = 7;

  final public int NORTHEAST = 8;
  int lastWallChecked = NONE;

  int lastWall = NONE;
  int width = 0;

  int height = 0;
  double BLACK = 0.0;
  boolean doneMoving = false;

  boolean doneSweeping = false;
  double lowThreshold = 90.0;
  double highThreshold = 210.0;
  int apertureSize = 3;
  transient IplImage gray = null;

  transient IplImage src = null;

  ArrayList<CvPoint> path = new ArrayList<CvPoint>();
  HashMap<String, CvPoint> unique = new HashMap<String, CvPoint>();

  CvPoint p0 = cvPoint(0, 0);

  CvPoint p1 = cvPoint(0, 0);

  CvScalar pathColor = cvScalar(0.0, 255.0, 0.0, 1.0);

  int nextDirection = 0;

  public OpenCVFilterMouse() {
    super();
  }

  public OpenCVFilterMouse(String name) {
    super(name);
  }

  public IplImage drawPath(IplImage image) {
    for (int i = 0; i < path.size(); ++i) {
      CvPoint p = path.get(i);
      p0.x(p.x());
      p0.y(p.y());
      p1.x(p.x());
      p1.y(p.y());
      cvDrawLine(image, p0, p1, pathColor, 1, 1, 0);
    }
    /*
     * Iterator<String> sgi = path.keySet().iterator(); while (sgi.hasNext()) {
     * Node n = path.get(sgi.next()); p0.x() = n.x(); p0.y() = n.y(); p1.x() =
     * n.x(); p1.y() = n.y(); cvDrawLine(image, p0, p1, pathColor, 1, 1, 0); }
     */
    return image;
  }

  public void eightFoldMouse() {

    doneMoving = false;
    while (!doneMoving) {

      doneSweeping = false;
      while (!doneSweeping) {
        // checking in a sweeping counter-clockwise (left hand rule)
        // pattern
        switch (lastWall) {
          case SOUTH: {
            // check SOUTHEAST
            // Log.error("SOUTHEAST");
            if ((mousePos.x() + 1 > width || mousePos.y() + 1 > height) || cvGet2D(src, mousePos.y() + 1, mousePos.x() + 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = SOUTHEAST;
            } else {
              // move SOUTHEAST
              mousePos.x(mousePos.x() + 1);
              mousePos.y(mousePos.y() + 1);
              lastWall = WEST;
              doneSweeping = true;
            }
          }
            break;

          case SOUTHEAST: {
            // check EAST
            // Log.error("EAST");
            if (mousePos.x() + 1 > width || cvGet2D(src, mousePos.y(), mousePos.x() + 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = EAST;
            } else {
              // move EAST
              mousePos.x(mousePos.x() + 1);
              lastWall = SOUTH;
              doneSweeping = true;
            }

          }
            break;

          case EAST: {
            // check NORTHEAST
            // Log.error("NORTHEAST");
            if ((mousePos.x() + 1 > width || mousePos.y() == 0) || cvGet2D(src, mousePos.y() - 1, mousePos.x() + 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = NORTHEAST;
            } else {
              // move NORTHEAST
              mousePos.x(mousePos.x() + 1);
              mousePos.y(mousePos.y() - 1);
              lastWall = SOUTH;
              doneSweeping = true;
            }
          }
            break;

          case NORTHEAST: {
            // check NORTH
            // Log.error("NORTH");
            if (mousePos.y() == 0 || cvGet2D(src, mousePos.y() - 1, mousePos.x()).getVal(0) != BLACK) {
              // wall - check next
              lastWall = NORTH;
            } else {
              // move NORTH
              mousePos.y(mousePos.y() - 1);
              lastWall = EAST;
              doneSweeping = true;
            }

          }
            break;

          case NORTH: {
            // check NORTHWEST
            // Log.error("NORTHWEST");
            if ((mousePos.x() == 0 || mousePos.y() == 0) || cvGet2D(src, mousePos.y() - 1, mousePos.x() - 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = NORTHWEST;
            } else {
              // move NORTHWEST
              mousePos.x(mousePos.x() - 1);
              mousePos.y(mousePos.y() - 1);
              lastWall = EAST;
              doneSweeping = true;
            }
          }
            break;

          case NORTHWEST: {
            // Log.error("WEST");
            // check WEST
            if (mousePos.x() == 0 || cvGet2D(src, mousePos.y(), mousePos.x() - 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = WEST;
            } else {
              // move WEST
              mousePos.x(mousePos.x() - 1);
              lastWall = NORTH;
              doneSweeping = true;
            }

          }
            break;

          case WEST: {
            // Log.error("SOUTHWEST " + mousePos);
            // check SOUTHWEST
            if ((mousePos.x() == 0 || mousePos.y() + 1 > height) || cvGet2D(src, mousePos.y() + 1, mousePos.x() - 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = SOUTHWEST;
            } else {
              // move SOUTHWEST
              mousePos.x(mousePos.x() - 1);
              mousePos.y(mousePos.y() + 1);
              lastWall = NORTH;
              doneSweeping = true;
            }
          }
            break;

          case SOUTHWEST: {
            // Log.error("SOUTH");
            // check SOUTH
            if (mousePos.y() + 1 > height || cvGet2D(src, mousePos.y() + 1, mousePos.x()).getVal(0) != BLACK) {
              // wall - check next
              lastWall = SOUTH;
            } else {
              // move SOUTH
              mousePos.y(mousePos.y() + 1);
              lastWall = NORTH;
              doneSweeping = true;
            }

          }
            break;

          default: {
            log.error("invalid direction " + lastWall);
          }

        } // switch

      } // while (!doneSweeping)
      CvPoint p = cvPoint(mousePos.x(), mousePos.y());
      path.add(p);
      /*
       * if (!unique.containsKey(p.toString())) { unique.put(p.toString(), p); }
       */
      if (mousePos.x() == startPoint.x() && mousePos.y() == startPoint.y()) {
        doneMoving = true;
      }
    } // while (!doneMoving)

  }

  public void fourFoldMouse() {

    doneMoving = false;
    while (!doneMoving) {

      doneSweeping = false;
      while (!doneSweeping) {
        // checking in a sweeping counter-clockwise (left hand rule)
        // pattern
        switch (lastWall) {
          case SOUTH: {
            // check EAST
            // Log.error("EAST");
            if (mousePos.x() + 1 > width || cvGet2D(src, mousePos.y(), mousePos.x() + 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = EAST;
            } else {
              // move EAST
              mousePos.x(mousePos.x() + 1);
              lastWall = SOUTH;
              doneSweeping = true;
            }
          }
            break;

          case EAST: {
            // check NORTH
            // Log.error("NORTH");
            if (mousePos.y() == 0 || cvGet2D(src, mousePos.y() - 1, mousePos.x()).getVal(0) != BLACK) {
              // wall - check next
              lastWall = NORTH;
            } else {
              // move NORTH
              mousePos.y(mousePos.y() - 1);
              lastWall = EAST;
              doneSweeping = true;
            }
          }
            break;

          case NORTH: {
            // Log.error("WEST");
            // check WEST
            if (mousePos.x() == 0 || cvGet2D(src, mousePos.y(), mousePos.x() - 1).getVal(0) != BLACK) {
              // wall - check next
              lastWall = WEST;
            } else {
              // move WEST
              mousePos.x(mousePos.x() - 1);
              lastWall = NORTH;
              doneSweeping = true;
            }
          }
            break;

          case WEST: {
            // Log.error("SOUTH");
            // check SOUTH
            if (mousePos.y() + 1 > height || cvGet2D(src, mousePos.y() + 1, mousePos.x()).getVal(0) != BLACK) {
              // wall - check next
              lastWall = SOUTH;
            } else {
              // move SOUTH
              mousePos.y(mousePos.y() + 1);
              lastWall = NORTH;
              doneSweeping = true;
            }
          }
            break;

          default: {
            log.error("invalid direction " + lastWall);
          }

        } // switch

      } // while (!doneSweeping)

      path.add(cvPoint(mousePos.x(), mousePos.y()));
      if (mousePos.x() == startPoint.x() && mousePos.y() == startPoint.y()) {
        doneMoving = true;
      }
    } // while (!doneMoving)

  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub

  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) {

    if (image == null) {
      log.error("image is null");
    }

    // path.clear();
    // ArrayList<CvPoint> path = new ArrayList<CvPoint>();
    lastWall = SOUTH;

    if (startPoint == null) {
      lastWall = SOUTH; // since we know the mousePos and startPoint are
      // on the bottom perimeter
      mousePos = cvPoint(image.width() / 2, image.height() - 1);
      startPoint = cvPoint(image.width() / 2 - 1, image.height() - 1); // put
      // start
      // point
      // left
      // of
      // mousePos
      width = image.width() - 1;
      height = image.height() - 1;
    }

    if (gray == null) {
      gray = cvCreateImage(cvGetSize(image), 8, 1);
    }
    if (src == null) {
      src = cvCreateImage(cvGetSize(image), 8, 1);
    }

    if (image.nChannels() == 3) {
      cvCvtColor(image, gray, CV_BGR2GRAY);
    } else {
      gray = image.clone();
    }

    cvCanny(gray, src, lowThreshold, highThreshold, apertureSize);
    cvDilate(src, src, null, 2);

    mousePos.x(startPoint.x());
    mousePos.y(startPoint.y());

    // fourFoldMouse();
    eightFoldMouse();

    drawPath(image);

    invoke("publish", (Object) path);

    log.error("{}", path.size());
    return image;
  }

}
