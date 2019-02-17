package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imdecode;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvInitFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_face.EigenFaceRecognizer;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_face.FisherFaceRecognizer;
import org.bytedeco.javacpp.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.image.Util;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.service.OpenCV;

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
 *
 */
public class OpenCVFilterFaceRecognizer extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  // training mode stuff
  public Mode mode = Mode.RECOGNIZE;
  public RecognizerType recognizerType = RecognizerType.FISHER;
  // when in training mode, this is the name to associate with the face.
  public String trainName = null;
  private FaceRecognizer faceRecognizer;
  private boolean trained = false;
  // the directory to store the training images.
  private String trainingDir = "training";
  private int modelSizeX = 256;
  private int modelSizeY = 256;
  //
  // We read in the face filter when training the first time, and use it for all
  // subsequent
  // training and for masking images prior to comparison.
  //
  private Mat facemask = null;
  private String cascadeDir = "haarcascades";
  private CascadeClassifier faceCascade;
  private CascadeClassifier eyeCascade;
  private CascadeClassifier mouthCascade;
  // These are cv converts that help us convert between mat,frame and iplimage

  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);
  private CvFont fontWarning = cvFont(CV_FONT_HERSHEY_PLAIN);

  private boolean debug = false;
  // KW: I made up this word, but I think it's fitting.
  private boolean dePicaso = true;

  private boolean doAffine = true;

  // some padding around the detected face
  private int borderSize = 25;

  private boolean face = false;

  private String lastRecognizedName = null;

  public String faceModelFilename = "faceModel.bin";

  public OpenCVFilterFaceRecognizer(String name) {
    super(name);
    initAll();
  }

  public enum Mode {
    TRAIN, RECOGNIZE
  }

  public enum RecognizerType {
    FISHER, EIGEN, LBPH
  }

  public void initAll() {
    initHaarCas();
    initRecognizer();
    // FIXME - deprecate - fonts/graphics should be done in java-land
    cvInitFont(font, CV_FONT_HERSHEY_SIMPLEX, 0.5, 0.5, 2, 1, 10);
    cvInitFont(fontWarning, CV_FONT_HERSHEY_SIMPLEX, 0.4, 0.3);
  }

  public void initHaarCas() {
    faceCascade = new CascadeClassifier(cascadeDir + "/haarcascade_frontalface_default.xml");
    eyeCascade = new CascadeClassifier(cascadeDir + "/haarcascade_eye.xml");
    // This mouth classifier isn't that great.. so if we can find a better one,
    // cool
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
   * @return true if the training was successful.
   * @throws IOException
   *           if there is a problem reading the training images
   */
  public boolean train() throws IOException {
    //
    // The first time we train, find the image mask, if present, scale it to the
    // current image size,
    // and save it for later.
    //
    if (facemask == null) {
      File filterfile = new File(Util.getRessourceDir() + "/facerec/Filter.png");
      //
      // Face mask used to mask edges of face pictures to eliminate noise around
      // the edges
      //
      if (!filterfile.exists()) {
        log.warn("No image filter file found.  {}", filterfile.getAbsolutePath());
      } else {
        // Read the filter and rescale it to the current image size
        // BytePointer fbp = new
        // BytePointer(FileUtils.getFileAsBytes(filterfile.getAbsolutePath()));
        // Mat incomingfacemask = imread(fbp, CV_LOAD_IMAGE_GRAYSCALE);
        Mat incomingfacemask = imread(filterfile.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
        facemask = resizeImage(incomingfacemask);
        if (debug) {
          show(facemask, "Face Mask");
        }
      }
    }

    File root = new File(trainingDir);
    if (root.isFile()) {
      log.warn("Training directory was a file, not a directory.  {}", root.getAbsolutePath());
      return false;
    }
    if (!root.exists()) {
      log.info("Creating new training directory {}", root.getAbsolutePath());
      root.mkdirs();
    }
    log.info("Using {} for training data.", root.getAbsolutePath());
    ArrayList<File> imageFiles = listImageFiles(root);
    if (imageFiles.size() < 1) {
      log.info("No images found for training.");
      return false;
    }
    // Storage for the files that we load.
    MatVector images = new MatVector(imageFiles.size());
    // storage for the labels for the images
    Mat labels = new Mat(imageFiles.size(), 1, CV_32SC1);
    IntBuffer labelsBuf = labels.getIntBuffer();
    int counter = 0;

    // a map between the hashcode and the string label
    HashMap<Integer, String> idToLabelMap = new HashMap<Integer, String>();

    for (File image : imageFiles) {
      // load the image
      log.info("Loading training image file: {}", image.getAbsolutePath());

      // we know that imread doesn't work with non-ascii file paths.. so we want
      // to use a different
      // so, load the image into memory, warp it in a byte pointer and pass it
      // to imdecode to load the image from memory, instead of
      // from disk
      byte[] tmpImg = FileIO.toByteArray(image);
      Mat img = imdecode(new Mat(new BytePointer(tmpImg)), CV_LOAD_IMAGE_GRAYSCALE);

      // The directory name is the label.
      String personName = image.getParentFile().getName();
      // String personName = UnicodeFolder.get(image.getParentFile().getName());

      // TODO: we need an integer to represent this string .. for now we're
      // using a hashcode here.
      // this can definitely have a collision!
      // we really need a better metadata store for these images. (atleast this
      // is deterministic.)
      int label = personName.hashCode();
      // make sure all our test images are resized
      Mat resized = resizeImage(img);
      //
      // Mask out unwanted parts of the training image by applying the resized
      // mask
      //
      if (facemask != null) {
        Mat maskedface = facemask.clone();
        resized.copyTo(maskedface, facemask);
        resized = maskedface;
      }
      // so, now our input for the training set is always 256x256 image.
      // we should probably run face detect and center this resized image, so we
      // can see
      // if we detect a full face in the image or not..
      // If these images are generated by this filter, they'll already be
      // cropped so it's ok
      if (debug) {
        // add a debug method to show the image
        show(resized, personName);
      }
      // TODO: our training images are indexed by integer,
      images.put(counter, resized);
      labelsBuf.put(counter, label);
      // keep track of what string the hash code maps to.
      idToLabelMap.put(label, personName);
      counter++;
    }
    initRecognizer();
    // must be at least 2 things to classify, is it A or B ?
    if (idToLabelMap.keySet().size() > 1) {
      faceRecognizer.train(images, labels);
      trained = true;
    } else {
      log.info("No labeled images loaded. training skipped.");
      trained = false;
    }
    // populate the human readable labels on the trained model
    for (int k : idToLabelMap.keySet()) {
      faceRecognizer.setLabelInfo(k, idToLabelMap.get(k));
    }
    return true;
  }

  private void initRecognizer() {
    // Configure which type of recognizer to use
    if (RecognizerType.FISHER.equals(recognizerType)) {
      faceRecognizer = FisherFaceRecognizer.create();
    } else if (RecognizerType.EIGEN.equals(recognizerType)) {
      faceRecognizer = EigenFaceRecognizer.create();
    } else {
      faceRecognizer = LBPHFaceRecognizer.create();
    }
  }

  /**
   * Save the current model to the faceModelFilename
   * 
   * @throws IOException
   */
  public void save() throws IOException {
    save(faceModelFilename);
  }

  /**
   * @param filename
   *          the filename to save the current model to.
   */
  public void save(String filename) {
    faceRecognizer.save(filename);
  }

  /**
   * load the model from the default filename specified by faceModelFilename.
   */
  public void load() {
    load(faceModelFilename);
  }

  /**
   * Load a face recognizer model from the provided saved filename.
   * 
   * @param filename
   *          the filename that represents the saved model.
   */
  public void load(String filename) {
    // faceRecognizer.load(new File(filename));
    faceRecognizer.read(filename);
    // assume we're trained now..
    trained = true;
    // let's also flip it to recognize mode
    mode = Mode.RECOGNIZE;
  }

  private ArrayList<File> listImageFiles(File root) {
    //
    ArrayList<File> trainingFiles = new ArrayList<File>();
    // only jpg , png , pgm files. TODO: other formats? bmp/tiff/etc?
    FilenameFilter imgFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        name = name.toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
      }
    };

    String[] contents = root.list();
    for (String fn : contents) {
      File f = new File(root.getAbsolutePath() + File.separator + fn);
      if (f.isDirectory()) {
        for (File x : f.listFiles(imgFilter)) {
          trainingFiles.add(x);
        }
      }
    }
    return trainingFiles;
  }

  private Mat resizeImage(Mat img, int width, int height) {
    Mat resizedMat = new Mat();
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
    // faceCascade.detectMultiScale(mat, vec);
    // int minSize = 10;
    // int maxSize = 10;
    // faceCascade.detectMultiScale(mat,
    // vec,1.1,5,CV_HAAR_DO_ROUGH_SEARCH|CV_HAAR_DO_CANNY_PRUNING|CV_HAAR_FIND_BIGGEST_OBJECT
    // ,
    // new Size(minSize), new Size(maxSize));
    faceCascade.detectMultiScale(mat, vec);
    return vec;
  }

  public void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // convert to grayscale
    Frame grayFrame = makeGrayScale(image);
    int cols = grayFrame.imageWidth;
    int rows = grayFrame.imageHeight;

    // convert to a Mat
    Mat bwImgMat = toMat(grayFrame);
    //
    // Image detection is done on the grayscale image, so we can modify the
    // original frame once
    // we make a grayscale copy.
    //
    if (Mode.TRAIN.equals(mode)) {
      String status = "Training Mode: " + trainName;
      // cvPutText(image, status, cvPoint(20, 40), font, CvScalar.GREEN);
    } else if (Mode.RECOGNIZE.equals(mode)) {
      String status = "Recognize Mode:" + lastRecognizedName;
      // cvPutText(image, status, cvPoint(20, 40), font, CvScalar.YELLOW);

    }

    //
    // Find a bunch of faces and their features
    // extractDetectedFaces will only return a face if it has all the necessary
    // features (face, 2 eyes and 1 mouth)
    ArrayList<DetectedFace> dFaces = extractDetectedFaces(bwImgMat);

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
        if (borderRect == null) {
          log.warn("Invalid face border found... parts were negative!");
          continue;
        }
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
        // dFaceMat is the cropped and rotated face
        if (Mode.TRAIN.equals(mode)) {
          // we're in training mode.. so we should save the image
          log.info("Training Mode for {}.", trainName);
          if (!StringUtils.isEmpty(trainName)) {
            try {
              saveTrainingImage(trainName, dFaceMat);
              cvPutText(image, "Snapshot Saved: " + trainName, cvPoint(20, 60), font, CvScalar.CYAN);
            } catch (IOException e) {
              // TODO Auto-generated catch block
              cvPutText(image, "Error saving: " + trainName, cvPoint(20, 60), font, CvScalar.CYAN);
              e.printStackTrace();
            }
          }
        } else if (Mode.RECOGNIZE.equals(mode)) {
          // You bettah recognize!
          if (!trained) {
            // we are a young grasshopper.
            if (face) {
              invoke("publishNoRecognizedFace");
              face = false;
            }
            return image;
          } else {
            face = true;
            // Resize the face to pass it to the predicter
            Mat dFaceMatSized = resizeImage(dFaceMat);
            // Mat copytoMat = dFaceMatSized.clone();

            // If we're applying a mask, do it before the prediction
            if (facemask != null) {
              Mat maskedface = facemask.clone();
              dFaceMatSized.copyTo(maskedface, facemask);
              dFaceMatSized = maskedface;
              if (debug) {
                show(dFaceMatSized, "Masked Face");
              }
            }

            int predictedLabel = faceRecognizer.predict_label(dFaceMatSized);
            BytePointer bp = faceRecognizer.getLabelInfo(predictedLabel);
            // TODO: what char encoding is this?!
            String name = bp.getString();
            log.info("Recognized a Face {} - {}", predictedLabel, name);
            cvPutText(image, name, dF.resolveGlobalLowerLeftCorner(), font, CvScalar.CYAN);
            // If it's a new name. invoke it an publish.
            if (lastRecognizedName != name) {
              invoke("publishRecognizedFace", name);
            }
            lastRecognizedName = name;
          }
        }
      }
      // highlight each of the faces we find.
      drawFaceRects(image, dF);
    }
    // pass through/return the original image marked up.
    return image;
  }

  private void saveTrainingImage(String label, Mat dFaceMat) throws IOException {
    // create some sort of a unique value so the file names don't
    // conflict
    // TODO: use something more random like a
    // OK now we need to make a subdirectory for the label if it doesn't exist.
    // imwrite dont like unicode, we map unicode name to ascii
    File labelDir = new File(trainingDir + File.separator + label);
    if (!labelDir.exists()) {
      labelDir.mkdirs();
    }
    // TODO: should we give it something other than a random uuid ?
    UUID randValue = UUID.randomUUID();
    String filename = trainingDir + File.separator + label + File.separator + randValue + ".png";
    // TODO: I think this is a png file ? not sure.
    // TODO: we need to be able to write a unicode filename with a path here..
    // we probably just need to get the image as a byte array png encoded, and
    // write that out ourselves..
    // a work around because imwrite doesn't support unicode in the filename.
    // so we'll convert the image to something like a byte array, and write it
    // out ourselves.
    // imwrite(filename, dFaceMat);
    BufferedImage buffImg = toBufferedImage(dFaceMat);
    ImageIO.write(buffImg, "png", new File(filename));
  }

  private Frame makeGrayScale(IplImage image) {
    IplImage imageBW = IplImage.create(image.width(), image.height(), 8, 1);
    cvCvtColor(image, imageBW, CV_BGR2GRAY);
    return converterToMat.convert(imageBW);
  }

  private ArrayList<DetectedFace> extractDetectedFaces(Mat bwImgMat) {
    ArrayList<DetectedFace> dFaces = new ArrayList<DetectedFace>();
    // first lets pick up on the face. we'll assume the eyes and mouth are
    // inside.
    RectVector faces = detectFaces(bwImgMat);
    // For each detected face, we need to to find the eyes and mouths to make it
    // complete.
    for (int i = 0; i < faces.size(); i++) {
      Rect face = faces.get(i);
      if (debug) {
        Mat croppedFace = new Mat(bwImgMat, face);
        show(croppedFace, "Face Area");
      }
      // The eyes will only be located in the top half of the image. Even with a
      // tilted
      // image, the face detector won't recognize the face if the eyes aren't in
      // the upper half of the image.
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
      // If we don't find exactly one mouth and two eyes in this image, just
      // skip the whole thing
      // Or, if the eyes overlap (identification of the same eye), skip this one
      // as well
      if ((mouths.size() == 1) && (eyes.size() == 2) && !rectOverlap(eyes.get(0), eyes.get(1))) {
        DetectedFace dFace = new DetectedFace();
        // In the recognizer, the first eye detected will be the highest one in
        // the picture. Because it may detect a
        // larger area, it's quite possible that the right eye will be detected
        // before the left eye. Move the eyes
        // into the right order, if they're not currently in the right order.
        // First, set the face features,
        // then call dePicaso to re-arrange out-of-order eyes.
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
    }
    if (dFace.getRightEye() != null) {
      Rect offset = new Rect(dFace.getFace().x() + dFace.getRightEye().x(), dFace.getFace().y() + dFace.getRightEye().y(), dFace.getRightEye().width(),
          dFace.getRightEye().height());
      drawRect(image, offset, CvScalar.BLUE);
    }
    if (dFace.getMouth() != null) {
      Rect offset = new Rect(dFace.getFace().x() + dFace.getMouth().x(), dFace.getFace().y() + dFace.getMouth().y(), dFace.getMouth().width(), dFace.getMouth().height());
      drawRect(image, offset, CvScalar.GREEN);
    }

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

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public String getTrainName() {
    return trainName;
  }

  public void setTrainName(String trainName) {
    this.trainName = trainName;
  }

  public String getTrainingDir() {
    return trainingDir;
  }

  public void setTrainingDir(String trainingDir) {
    this.trainingDir = trainingDir;
  }

  public String getCascadeDir() {
    return cascadeDir;
  }

  public void setCascadeDir(String cascadeDir) {
    this.cascadeDir = cascadeDir;
  }

  public String getLastRecognizedName() {
    return lastRecognizedName;
  }

  // Thanks to @calamity for the suggestion to expose this
  // TODO: expose this in a more generic way for all OpenCVFilters that
  // can recognize objects and other data.
  public String publishRecognizedFace(String name) {
    return name;
  }

  public void publishNoRecognizedFace() {
    log.info("Classifier not trained yet.");
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {

    if (Mode.TRAIN.equals(mode)) {
      String status = "Training Mode: " + trainName;
      // cvPutText(image, status, cvPoint(20, 40), font, CvScalar.GREEN);
      graphics.drawString(status, 20, 40);
    } else if (Mode.RECOGNIZE.equals(mode)) {
      String status = "Recognize Mode:" + lastRecognizedName;
      // cvPutText(image, status, cvPoint(20, 40), font, CvScalar.YELLOW);
      graphics.drawString(status, 20, 40);
    }
    return image;
  }
}
