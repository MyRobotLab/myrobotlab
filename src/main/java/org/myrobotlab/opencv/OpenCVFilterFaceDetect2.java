package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.WindowConstants;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

/**
 * This is the OpenCV Face Recognition. It must be trained with a set of images
 * and their labels. These images should be of people faces and their names are
 * the labels.
 * 
 * It computes the "distance" from the reference new image to existing images
 * that it's been trained on and provides a prediction of what label applies
 * 
 * Based on:
 * https://github.com/bytedeco/javacv/blob/master/samples/OpenCVFaceRecognizer.
 * java
 * 
 * @author kwatters
 * @author scruffy-bob
 * modified by alessandruino
 */
public class OpenCVFilterFaceDetect2 extends OpenCVFilter {
  private static final long serialVersionUID = 1L;
  // training mode stuff
  public RecognizerType recognizerType = RecognizerType.FISHER;
  private int modelSizeX = 256;
  private int modelSizeY = 256;


  private String cascadeDir = "haarcascades";
  private CascadeClassifier faceCascade;
  private CascadeClassifier eyeCascade;
  private CascadeClassifier mouthCascade;
  // TODO: why the heck do we need to convert back and forth, and is this
  // effecient?!?!
  transient private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  private HashMap<Integer, String> idToLabelMap = new HashMap<Integer, String>();

  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

  private boolean debug = false;
  // KW: I made up this word, but I think it's fitting.
  private boolean dePicaso = true;

  private boolean doAffine = true;

  // some padding around the detected face
  private int borderSize = 25;

  public OpenCVFilterFaceDetect2() {
    super();
    initHaarCas();

  }

  public OpenCVFilterFaceDetect2(String name) {
    super(name);
    initHaarCas();
  }

  public OpenCVFilterFaceDetect2(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    initHaarCas();
  }

  public enum Mode {
    TRAIN, RECOGNIZE
  }

  public enum RecognizerType {
    FISHER, EIGEN, LBPH
  }

  public void initHaarCas() {
    faceCascade = new CascadeClassifier(cascadeDir + "/haarcascade_frontalface_default.xml");
    eyeCascade = new CascadeClassifier(cascadeDir + "/haarcascade_eye.xml");
    // TODO: find a better mouth classifier! this one kinda sucks.
    mouthCascade = new CascadeClassifier(cascadeDir + "/haarcascade_mcs_mouth.xml");
    // mouthCascade = new
    // CascadeClassifier(cascadeDir+"/haarcascade_mouth.xml");
    // noseCascade = new CascadeClassifier(cascadeDir+"/haarcascade_nose.xml");
  }

  /**
   * This method will load all of the image files in a directory. The filename
   * will be parsed for the label to apply to the image. At least 2 different
   * labels must exist in the training set.
   * 
   * @return
   */


  private Mat resizeImage(Mat img, int width, int height) {
    Mat resizedMat = new Mat();
    // IplImage resizedImage = IplImage.create(modelSizeX, modelSizeY,
    // img.depth(), img.channels());
    Size sz = new Size(width, height);
    resize(img, resizedMat, sz);
    return resizedMat;
  }

  private Mat resizeImage(Mat img) {
    return resizeImage(img, modelSizeX, modelSizeY);
  }

  public RectVector detectEyes(Mat mat) {
    RectVector vec = new RectVector();
    eyeCascade.detectMultiScale(mat, vec);
    return vec;
  }

  public RectVector detectMouths(Mat mat) {
    RectVector vec = new RectVector();
    mouthCascade.detectMultiScale(mat, vec);
    return vec;
  }

  public RectVector detectFaces(Mat mat) {
    RectVector vec = new RectVector();
    // TODO: see about better tuning and passing these parameters in.
    // RectVector faces =
    // faceCascade.detectMultiScale(gray,scaleFactor=1.1,minNeighbors=5,minSize=(50,
    // 50),flags=cv2.cv.CV_HAAR_SCALE_IMAGE)
    faceCascade.detectMultiScale(mat, vec);
    return vec;
  }

  public void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  // helper method to show an image. (todo; convert it to a Mat )
  public void show(final Mat imageMat, final String title) {
    IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(imageMat));
    final IplImage image1 = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, image.nChannels());
    cvCopy(image, image1);
    CanvasFrame canvas = new CanvasFrame(title, 1);
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
    canvas.showImage(converter.convert(image1));
  }

  @Override
  public IplImage process(IplImage image, OpenCVData data) 
		  throws InterruptedException {
    // convert to grayscale
    Frame grayFrame = makeGrayScale(image);
    // TODO: this seems super wonky! isn't there an easy way to go from IplImage
    // to opencv Mat?
    int cols = grayFrame.imageWidth;
    int rows = grayFrame.imageHeight;
    // convert to a Mat
    Mat bwImgMat = converterToIpl.convertToMat(grayFrame);


    //
    // Find a bunch of faces and their features
    // extractDetectedFaces will only return a face if it has all the necessary
    // features (face, 2 eyes and 1 mouth)
    ArrayList<DetectedFace> dFaces = extractDetectedFaces(bwImgMat, cols, rows);

    // Ok, for each of these detected faces we should try to classify them.
    for (DetectedFace dF : dFaces) {
      if (dF.isComplete()) {
        // and array of 3 x,y points.
        // create the triangle from left->right->mouth center
        Point2f srcTri = dF.resolveCenterTriangle();
        Point2f dstTri = new Point2f(3);
        // populate dest triangle.
        dstTri.position(0).x((float) (dF.getFace().width() * .3)).y((float) (dF.getFace().height() * .45));
        dstTri.position(1).x((float) (dF.getFace().width() * .7)).y((float) (dF.getFace().height() * .45));
        dstTri.position(2).x((float) (dF.getFace().width() * .5)).y((float) (dF.getFace().height() * .85));
        // create the affine rotation/scale matrix
        Mat warpMat = getAffineTransform(srcTri.position(0), dstTri.position(0));
        // Mat dFaceMat = new Mat(bwImgMat, dF.getFace());

        Rect borderRect = dF.faceWithBorder(borderSize, cols, rows);
        Mat dFaceMat = new Mat(bwImgMat, borderRect);
        // TODO: transform the original image , then re-crop from that
        // so we don't loose the borders after the rotation
        if (doAffine) {
          warpAffine(dFaceMat, dFaceMat, warpMat, borderRect.size());
        }
        try {
          // TODO: why do i have to close these?!
          srcTri.close();
          dstTri.close();
        } catch (Exception e) {
          log.warn("Error releasing some OpenCV memory, you shouldn't see this: {}", e);
          // should we continue ?!
        }
          
      // highlight each of the faces we find.
      drawFaceRects(image, dF);
      data.setEyesDifference(dF.getRightEye().y() - dF.getLeftEye().y());
    }}
    // pass through/return the original image marked up.
    return image;
  }

  private Frame makeGrayScale(IplImage image) {
    IplImage imageBW = IplImage.create(image.width(), image.height(), 8, 1);
    cvCvtColor(image, imageBW, CV_BGR2GRAY);
    return converterToMat.convert(imageBW);
  }

  private ArrayList<DetectedFace> extractDetectedFaces(Mat bwImgMat, int width, int height) {
    ArrayList<DetectedFace> dFaces = new ArrayList<DetectedFace>();
    // first lets pick up on the face. we'll assume the eyes and mouth are
    // inside.
    RectVector faces = detectFaces(bwImgMat);

    //
    // For each detected face, we need to to find the eyes and mouths to make it
    // complete.
    //
    for (int i = 0; i < faces.size(); i++) {
      Rect face = faces.get(i);
      if (debug) {
        Mat croppedFace = new Mat(bwImgMat, face);
        show(croppedFace, "Face Area");
      }

      //
      // The eyes will only be located in the top half of the image. Even with a
      // tilted
      // image, the face detector won't recognize the face if the eyes aren't in
      // the
      // upper half of the image.
      //
      Rect eyesRect = new Rect(face.x(), face.y(), face.width(), face.height() / 2);
      Mat croppedEyes = new Mat(bwImgMat, eyesRect);
      RectVector eyes = detectEyes(croppedEyes);
      if (debug) {
        show(croppedEyes, "Eye Area");
      }

      // The mouth will only be located in the lower 1/3 of the picture, so only
      // look there.
      Rect mouthRect = new Rect(face.x(), face.y() + face.height() / 3 * 2, face.width(), face.height() / 3);
      Mat croppedMouth = new Mat(bwImgMat, mouthRect);
      if (debug) {
        show(croppedMouth, "Mouth Area");
      }
      RectVector mouths = detectMouths(croppedMouth);

      if (debug) {
        log.info("Found {} mouth and {} eyes.", mouths.size(), eyes.size());
      }

      //
      // If we don't find exactly one mouth and two eyes in this image, just
      // skip the whole thing
      // Or, if the eyes overlap (identification of the same eye), skip this one
      // as well
      //

      if ((mouths.size() == 1) && (eyes.size() == 2) && !rectOverlap(eyes.get(0), eyes.get(1))) {
        DetectedFace dFace = new DetectedFace();

        //
        // In the recognizer, the first eye detected will be the highest one in
        // the picture. Because it may detect a
        // larger area, it's quite possible that the right eye will be detected
        // before the left eye. Move the eyes
        // into the right order, if they're not currently in the right order.
        // First, set the face features,
        // then call dePicaso to re-arrange out-of-order eyes.
        //

        dFace.setFace(face);

        //
        // Remember, the mouth is offset from the top of the picture, so we have
        // to
        // account for this change before we store it. The eyes don't matter, as
        // they
        // start at the top of the image already.
        //

        mouthRect = new Rect(mouths.get(0).x(), mouths.get(0).y() + face.height() / 3 * 2, mouths.get(0).width(), mouths.get(0).height());

        dFace.setMouth(mouthRect);
        dFace.setLeftEye(eyes.get(0));
        dFace.setRightEye(eyes.get(1));
        if (dePicaso) {
          dFace.dePicaso();
        }

        // At this point, we've found the complete face and everything appears
        // normal.
        // Add this to the list of recognized faces
        dFaces.add(dFace);
        if (debug) {
          Mat croppedFace = new Mat(bwImgMat, face);
          show(croppedFace, "Cropped Face");
        }
      }
    }
    return dFaces;
  }

  private void drawFaceRects(IplImage image, DetectedFace dFace) {
    // helper function to draw rectangles around the detected face(s)
    drawRect(image, dFace.getFace(), CvScalar.MAGENTA);
    if (dFace.getLeftEye() != null) {
      // Ok the eyes are relative to the face
      Rect offset = new Rect(dFace.getFace().x() + dFace.getLeftEye().x(), dFace.getFace().y() + dFace.getLeftEye().y(), dFace.getLeftEye().width(), dFace.getLeftEye().height());
      drawRect(image, offset, CvScalar.BLUE);
      String positionY = "Y of Left Eye is: " + dFace.getLeftEye().y();
      cvPutText(image, positionY, cvPoint(20, 40), font, CvScalar.BLACK);
    }
    if (dFace.getRightEye() != null) {
      Rect offset = new Rect(dFace.getFace().x() + dFace.getRightEye().x(), dFace.getFace().y() + dFace.getRightEye().y(), dFace.getRightEye().width(),
          dFace.getRightEye().height());
      drawRect(image, offset, CvScalar.BLUE);
      String positionY = "Y of Right Eye is: " + dFace.getRightEye().y();
      String difference = "Difference between eyes is " + (dFace.getRightEye().y() - dFace.getLeftEye().y()) ; 
      cvPutText(image, positionY, cvPoint(20, 80), font, CvScalar.BLACK);
      cvPutText(image, difference , cvPoint(20, 100), font, CvScalar.BLACK);
    }
    if (dFace.getMouth() != null) {
      Rect offset = new Rect(dFace.getFace().x() + dFace.getMouth().x(), dFace.getFace().y() + dFace.getMouth().y(), dFace.getMouth().width(), dFace.getMouth().height());
      drawRect(image, offset, CvScalar.GREEN);
    }

  }

  private void drawRects(IplImage image, RectVector rects, CvScalar color) {
    for (int i = 0; i < rects.size(); i++) {
      Rect face = rects.get(i);
      drawRect(image, face, color);
    }
  }

  private boolean isInside(RectVector rects, Rect test) {
    for (int i = 0; i < rects.size(); i++) {
      boolean res = isInside(rects.get(i), test);
      if (res) {
        return true;
      }
    }
    return false;
  }

  public static boolean isInside(Rect r1, Rect r2) {
    // if r2 is inside of r1 return true
    int x1 = r1.x();
    int y1 = r1.y();
    int x2 = x1 + r1.width();
    int y2 = y1 + r1.height();
    int x3 = r2.x();
    int y3 = r2.y();
    int x4 = r2.x() + r2.width();
    int y4 = r2.y() + r2.height();
    // if r2 xmin/xmax is within r1s
    if (x1 < x3 && x2 > x4) {
      if (y1 < y3 && y2 > y4) {
        return true;
      }
    }
    return false;
  }

  public static boolean rectOverlap(Rect r, Rect test) {

    if (test == null || r == null) {
      return false;
    }
    return (((r.x() >= test.x()) && (r.x() < (test.x() + test.width()))) || ((test.x() >= r.x()) && (test.x() < (r.x() + r.width()))))
        && (((r.y() >= test.y()) && (r.y() < (test.y() + test.height()))) || ((test.y() >= r.y()) && (test.y() < (r.y() + r.height()))));
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO: what should we do here?
  }

  public int getModelSizeX() {
    return modelSizeX;
  }

  public void setModelSizeX(int modelSizeX) {
    this.modelSizeX = modelSizeX;
  }

  public int getModelSizeY() {
    return modelSizeY;
  }

  public void setModelSizeY(int modelSizeY) {
    this.modelSizeY = modelSizeY;
  }

  public String getCascadeDir() {
    return cascadeDir;
  }

  public void setCascadeDir(String cascadeDir) {
    this.cascadeDir = cascadeDir;
  }

}
