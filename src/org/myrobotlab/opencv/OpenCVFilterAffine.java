package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.cvCreateMat;
import static org.bytedeco.javacpp.opencv_core.cvPoint2D32f;
import static org.bytedeco.javacpp.opencv_core.cvSize2D32f;
import static org.bytedeco.javacpp.opencv_imgproc.cv2DRotationMatrix;
import static org.bytedeco.javacpp.opencv_imgproc.cvBoxPoints;
import static org.bytedeco.javacpp.opencv_imgproc.cvWarpAffine;

import org.bytedeco.javacpp.opencv_core.CvBox2D;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.CvPoint2D32f;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterAffine extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  transient IplImage dst;
  public int flipCode = 1;

  // angle of rotation
  private float angle;
  // translation along x axis (pixels)
  private double dx = 0;
  // tranlsation along y axis (pixels)
  private double dy = 0;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterTranspose.class.getCanonicalName());

  private Point lastClicked = null;

  public OpenCVFilterAffine() {
    super();
  }

  public OpenCVFilterAffine(String name) {
    super(name);
  }

  public OpenCVFilterAffine(String filterName, String sourceKey) {
    super(filterName, sourceKey);
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
    // TODO : Create the affine filter and return the new image
    // Find the center of the image

    CvPoint2D32f center = cvPoint2D32f(image.width() / 2.0F, image.height() / 2.0F);
    // TODO: test this...
    // CvBox2D box = new CvBox2D(center, cvSize2D32f(image.width() - 1,
    // image.height() - 1), angle);
    CvBox2D box = new CvBox2D();
    box.center(center);
    box.size(cvSize2D32f(image.width() - 1, image.height() - 1));
    box.angle(angle);

    CvPoint2D32f points = new CvPoint2D32f(4);
    cvBoxPoints(box, points);
    // CvMat pointMat = cvCreateMat(1, 4, CV_32FC2);
    // pointMat.put(0, 0, 0, points.position(0).x());
    // pointMat.put(0, 0, 1, points.position(0).y());
    // pointMat.put(0, 1, 0, points.position(1).x());
    // pointMat.put(0, 1, 1, points.position(1).y());
    // pointMat.put(0, 2, 0, points.position(2).x());
    // pointMat.put(0, 2, 1, points.position(2).y());
    // pointMat.put(0, 3, 0, points.position(3).x());
    // pointMat.put(0, 3, 1, points.position(3).y());
    // CvRect boundingRect = cvBoundingRect(pointMat, 0);
    // CvMat dst = cvCreateMat(boundingRect.height(), boundingRect.width(),
    // image.type());
    // CvMat dst = cvCreateMat(boundingRect.height(), boundingRect.width(),
    // CV_32FC1);
    CvMat rotMat = cvCreateMat(2, 3, CV_32FC1);
    cv2DRotationMatrix(center, angle, 1, rotMat);
    // Add the transpose matrix
    double x = rotMat.get(0, 2) + dx;
    rotMat.put(0, 2, x);
    // Add the transpose matrix
    double y = rotMat.get(1, 2) + dy;
    rotMat.put(1, 2, y);
    // double y_1 = ((boundingRect.width() - image.width()) / 2.0F) +
    // rotMat.get(0, 2);
    // double y_2 = ((boundingRect.height() - image.height()) / 2.0F +
    // rotMat.get(1, 2));
    // rotMat.put(0, 2, y_1);
    // rotMat.put(1, 2, y_2);
    // CvScalar fillval = cvScalarAll(0);
    // IplImage dst_frame = cvCloneImage(image);
    // cvWarpAffine(image, dst_frame, rotMat);

    // System.out.println(rotMat);
    cvWarpAffine(image, image, rotMat);
    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
    dst = IplImage.create(image.height(), image.width(), image.depth(), image.nChannels());
  }

  public float getAngle() {
    return angle;
  }

  public void setAngle(float angle) {
    this.angle = angle;
  }

  public double getDx() {
    return dx;
  }

  public void setDx(double dx) {
    this.dx = dx;
  }

  public double getDy() {
    return dy;
  }

  public void setDy(double dy) {
    this.dy = dy;
  }

  @Override
  public void samplePoint(Integer x, Integer y) {
    //
    log.info("Affine clicked point called " + x + " " + y);
    lastClicked = new Point(x, y, 0, 0, 0, 0);
  }

  public Point getLastClicked() {
    return lastClicked;
  }

}
