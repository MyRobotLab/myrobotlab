package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvPoint;

import org.bytedeco.opencv.opencv_core.CvPoint;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class DetectedFace {

  public final static Logger log = LoggerFactory.getLogger(DetectedFace.class);
  public Rect face;
  public Rect leftEye;
  public Rect rightEye;
  // TODO: we no longer are doing mouth detection in the face.
  public Rect mouth;
  // TODO: create some better label id to string mapping
  public int detectedLabelId;

  public void dePicaso() {
    if (mouth == null) {
      // NoOp
      // log.warn("Mouth was null.. can't dePicaso");
      return;
    }
    // the face might be slightly scrambled. make sure the left eye
    // is in the left socket.. and the right eye in the right socket.
    if (mouth.y() < leftEye.y() + leftEye.height()) {
      // the mouth
      // that's not right!
      Rect tmp = mouth;
      mouth = leftEye;
      leftEye = tmp;
    }
    if (mouth.y() < rightEye.y() + rightEye.height()) {
      // the mouth
      // that's not right!
      Rect tmp = mouth;
      mouth = rightEye;
      rightEye = tmp;
    }

    if (leftEye.x() > rightEye.x()) {
      // swap eyes!
      Rect tmp = leftEye;
      leftEye = rightEye;
      rightEye = tmp;
    }

  }

  public Point2f resolveCenterTriangle() {

    // and array of 3 x,y points.
    int[][] ipts1 = new int[3][2];
    int centerleftx = getLeftEye().x() + getLeftEye().width() / 2;
    int centerlefty = getLeftEye().y() + getLeftEye().height() / 2;
    // right side center
    int centerrightx = getRightEye().x() + getRightEye().width() / 2;
    int centerrighty = getRightEye().y() + getRightEye().height() / 2;
    // mouth center.
    int centermouthx = getMouth().x() + getMouth().width() / 2;
    int centermouthy = getMouth().y() + getMouth().height() / 2;
    // point 1
    ipts1[0][0] = centerleftx;
    ipts1[0][1] = centerlefty;
    // point 2
    ipts1[1][0] = centerrightx;
    ipts1[1][1] = centerrighty;
    // point 3
    ipts1[2][0] = centermouthx;
    ipts1[2][1] = centermouthy;

    Point2f srcTri = new Point2f(3);

    // populate source triangle
    srcTri.position(0).x(centerleftx).y(centerlefty);
    srcTri.position(1).x(centerrightx).y(centerrighty);
    srcTri.position(2).x(centermouthx).y(centermouthy);

    return srcTri;
  }

  public Size size() {
    return new Size(getFace().width(), getFace().height());
  }

  public CvPoint resolveGlobalLowerLeftCorner() {
    return cvPoint(getFace().x(), getFace().y() + getFace().height());
  }

  public Rect getFace() {
    return face;
  }

  public Rect faceWithBorder(int size, int cols, int rows) {
    int x = Math.max(0, face.x() - size / 2);
    int y = Math.max(0, face.y() - size / 2);
    // make sure the cropped area doesn't exceed the borders of the original
    // image.
    int w = Math.min(cols - x, face.width() + size);
    int h = Math.min(rows - y, face.height() + size);

    if (x < 0 || y < 0 || w < 0 || h < 0) {
      // return new Rect(0,0,0,0);
      return null;
    }
    Rect faceWithBorder = new Rect(x, y, w, h);
    return faceWithBorder;
  }

  public void setFace(Rect face) {
    this.face = face;
  }

  public Rect getLeftEye() {
    return leftEye;
  }

  public void setLeftEye(Rect leftEye) {
    this.leftEye = leftEye;
  }

  public Rect getRightEye() {
    return rightEye;
  }

  public void setRightEye(Rect rightEye) {
    this.rightEye = rightEye;
  }

  public Rect getMouth() {
    return mouth;
  }

  public void setMouth(Rect mouth) {
    this.mouth = mouth;
  }

  public boolean isComplete() {
    // helper method to tell us if everything is set.
    // now we're going to consider a complete face to have 2 eyes.
    //
    return !((face == null) || (leftEye == null) || (rightEye == null) || (mouth == null));
  }

  public boolean isGoodCandidate() {
    // helper method to tell us if everything is set.
    // now we're going to consider a complete face to have 2 eyes.
    // || (mouth == null)
    // got to have a face and at least one eye.
    // our definition for this is that we have to have a face and an eye.
    return (face != null) && ((leftEye != null) || (rightEye != null));
  }

  public int getDetectedLabelId() {
    return detectedLabelId;
  }

  public void setDetectedLabelId(int detectedLabelId) {
    this.detectedLabelId = detectedLabelId;
  }
}
