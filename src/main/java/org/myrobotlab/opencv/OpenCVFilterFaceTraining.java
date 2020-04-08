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


import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;

import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
// import static org.bytedeco.opencv.opencv_objdetect.cvHaarDetectObjects;
// import org.bytedeco.opencv.opencv_objdetect.CvHaarClassifierCascade;
import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_core.cvClearMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvCopy;
import static org.bytedeco.opencv.global.opencv_core.cvCreateImage;
import static org.bytedeco.opencv.global.opencv_core.cvCreateMemStorage;
import static org.bytedeco.opencv.global.opencv_core.cvGetSeqElem;
//import static org.bytedeco.opencv.opencv_core.cvLoad;
//import static org.bytedeco.opencv.helper.opencv_core.cvLoad;
import static org.bytedeco.opencv.global.opencv_core.cvSetImageROI;
import static org.bytedeco.opencv.global.opencv_core.cvSetZero;
// import static org.bytedeco.opencv.global.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_DO_ROUGH_SEARCH;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_FIND_BIGGEST_OBJECT;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_SCALE_IMAGE;
//import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_FEATURE_MAX;
//import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_MAGIC_VAL;
//import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_STAGE_MAX;
import static org.bytedeco.opencv.global.opencv_objdetect.CASCADE_DO_CANNY_PRUNING;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.CvMemStorage;
import org.bytedeco.opencv.opencv_core.CvRect;
import org.bytedeco.opencv.opencv_core.CvSize;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.EigenFaceRecognizer;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

public class OpenCVFilterFaceTraining extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceTraining.class);

  CvMemStorage storage = null;
  public CascadeClassifier cascade = null; // TODO - was static

  final public static String MODE_TRAINING = "training";
  final public static String MODE_PREDICTING = "predicting";

  String mode = MODE_TRAINING;

  /**
   * our default classifier - pre-trained
   */
  // public String cascadeDir = FileIO.gluePathsForwardSlash(Service.getResourceDir(OpenCV.class),"haarcascades");
  public String cascadeDir = "resource/OpenCV/haarcascades";
  public String cascadeFile = "haarcascade_frontalface_alt2.xml";

  // a map between the hashcode and the string label
  HashMap<Integer, String> idToLabelMap = new HashMap<>();

  /**
   * bounding boxes of faces
   */
  ArrayList<Rectangle> bb = null;
  int i;
  double scaleFactor = 1.1;
  int minNeighbors = 1;

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

  public static final String CACHE_DIR = "_cache-resized-template";
  int templateWidth = 256;
  int templateHeight = 256;
  OpenCVFilterCopy copier = new OpenCVFilterCopy("copier");

  private String state = STATE_LOST_TRACKING;
  int option = CASCADE_DO_CANNY_PRUNING | CASCADE_FIND_BIGGEST_OBJECT; // default
  // int option = 0; // default

  /**
   * Begin Recognition - which is just a sub-classification of "face"(detection)
   * 
   * This is 'supervised' or semi-supervised training done with directories on
   * the filesystem and "help/supervision" from someone moving files into the
   * correct sub-directories
   * 
   * the sub-directories are sub-classes and would be indication of this filter
   * creating a new sub-class classifier..
   * 
   * e.g. if their is a directory of "fruit" a person can quickly make
   * subdirectories of apples, banannas, and oranges
   * 
   * the filter will see these subdirectores - make and train a classifier then
   * dump incoming classified data back into the directory with an underscore
   * '_' at the begining of the file.
   * 
   * The underscore will always be the machine guessing/classifying, while files
   * without underscored are put their by a human. The distinction is important,
   * as the human un-mangled named files are "always" considered correct. And
   * directories with _{label} are the machines guess. This allows the
   * supervisor to quickly assist in moving correct and incorrect machine
   * guesses
   * 
   */

  // all machine dirs with have underscore "_"{subclass}
  File rootTrainingDir = new File("training");
  File facesSubclass = new File(String.format(rootTrainingDir + File.separator + "faces"));

  public boolean saveFaces = true;

  /**
   * our set of classifiers for all (sub directories) or (sub classes) its
   * OpenCVClassifier vs BoofCv its "really" a FaceRecognizer - (I wish it was
   * more 'general')
   */
  public Map<File, OpenCVClassifier> classifiers = new TreeMap<File, OpenCVClassifier>();

  /**
   * root is the location on the file system where this classifier is supposed
   * to work and classify into sub directories.
   * 
   * A subclass is the same as a directory who's name is a "label"
   * 
   * it keeps track of the set of subdirectories and the number of image files
   * to train from. If the number of files change, or the number of directories
   * without underscore - it will likely cause a 're-train'
   */
  public class OpenCVClassifier {
    File root;
    FaceRecognizer recognizer;
    int fileCnt = 0;
    Map<Integer, String> intToLabel = new TreeMap<>();
    Set<File> subclassDirs = new TreeSet<>();

    OpenCVFilterResize resizer = new OpenCVFilterResize("resizer");

    /**
     * <pre>
     * Map<File, Set<File>> imgFiles = new TreeMap<File, Set<File>>();
     * is a map of subclass directories to the image files they contain
     * e.g. 
     *      neo/img01.png
     *      neo/img02.png
     *      neo/img03.png
     *      neo/img04.png
     *      morpheus/img01.png
     *      morpheus/img02.png
     *      morpheus/img03.png
     *      morpheus/img04.png
     * 
     * </pre>
     */
    Map<File, Set<File>> imgFiles = new TreeMap<File, Set<File>>();
    MatVector images = new MatVector(imgFiles.size());
    Mat labels = new Mat(imgFiles.size(), 1, CV_32SC1);
    IntBuffer labelsBuf;

    int totalImageFiles = 0;
    boolean debug = false;

    public OpenCVClassifier(File root) {
      this.root = root;
      if (imgFiles.size() != 0) {
        labelsBuf = labels.createBuffer();
      }
      resizer.height = 200;
      resizer.width = 200;
      // resizer.setPad(0)

    }

    public boolean retrain() {
      boolean retrain = false;
      // scan for new directories
      File[] dirs = root.listFiles(File::isDirectory);
      for (File dir : dirs) {
        // "UNDERSCORE" directory means it was created by the machine
        if (dir.getName().startsWith("_")) {
          continue;
        }
        if (!subclassDirs.contains(dir)) {
          log.info("new directory !!! {}", dir.getAbsoluteFile());
          retrain = true;
          imgFiles.put(dir, new TreeSet<File>());
          subclassDirs.add(dir);
          File[] scan = dir.listFiles(imgFilter);
          for (File img : scan) {
            if (!imgFiles.containsKey(img)) { // NOT STARTS WITH UNDERSCORE !!!
              ++totalImageFiles;
              imgFiles.get(dir).add(img);
              log.info("adding file !!! {}", dir.getAbsoluteFile());
            }
          }

        } else {
          File[] scan = dir.listFiles(imgFilter);
          Set<File> subclassImages = imgFiles.get(dir);
          for (File img : scan) {
            if (!subclassImages.contains(img)) { // NOT STARTS WITH UNDERSCORE
              // !!!
              subclassImages.add(img);
              log.info("new file !!! {}", dir.getAbsoluteFile());
              retrain = true;
            }
          }
        }
      }
      return retrain;
    }

    public void scanAndProcess() {

      // do we retrain ?
      if (!retrain()) {
        log.info("no retraining necessary"); // TODO - new files for Supervisor
        return;
      }

      // FIXME - DE-INITIALIZE CURRENT MEMORY !!!!!
      MatVector images = new MatVector(totalImageFiles);
      Mat labels = new Mat(totalImageFiles, 1, CV_32SC1);
      IntBuffer labelsBuf = labels.createBuffer();

      int counter = 0;

      for (File subclass : subclassDirs) {
        String label = subclass.getName();
        intToLabel.put(label.hashCode(), label);

        new File(subclass + File.separator + CACHE_DIR).mkdirs();

        for (File imageFile : imgFiles.get(subclass)) {
          Mat img = imread(imageFile.getAbsolutePath(), IMREAD_GRAYSCALE);

          int w = img.rows();
          int h = img.cols();

          // we'll need width/height info
          IplImage ipImg = new IplImage(img);

          if (debug) {
            show(ipImg, subclass.getName() + " " + ipImg.width() + "x" + ipImg.height() + " " + imageFile.getName());
          }

          w = ipImg.width();
          h = ipImg.height();
          // ipImg.close(); ???

          // int label = Integer.parseInt(image.getName().split("\\-")[0]);

          // TODO - pre-process here !!!
          // can pre-processing be cached ?
          // resizing, and other adjustmets ? masking ? feedback ?
          int wDelta = Math.abs(resizer.width - w);
          int hDelta = Math.abs(resizer.height - h);
          boolean widthClosestDimension = (wDelta < hDelta) ? true : false;
          if (widthClosestDimension) {
            // scale to width proportionally

          } else {

          }
          int z0 = Math.abs(width - w);

          // FIXME - have the resize filter do the resize (with options)
          // IplImage resizedImage = IplImage.create(800, 60, ipImg.depth(),
          // ipImg.nChannels());

          // cvSmooth(origImg, origImg);
          // cvResize(ipImg, resizedImage);
          // cvResize(ipImg, resizedImage, Imgproc.INTER_CUBIC);
          // cvResize(ipImg, resizedImage, Imgproc.INTER_MAX);
          // cvResize(ipImg, resizedImage, Imgproc.INTER_NEAREST);
          // resize(convertToMat(ipImg), convertToMat(resizedImage), new
          // Size(0,0), 1.0, 1.0, Imgproc.INTER_CUBIC);
          // cvResize(ipImg, resizedImage, Imgproc.INTER_AREA);
          // saveToFile("stretched", resizer.resizeNoAspect(ipImg,
          // templateWidth, templateHeight));

          // BEGIN STANDARDIZE SUPERVISORS IMAGES INTO CACHE_DIR
          IplImage resizedImage = OpenCVFilterResize.resizeImageMaintainAspect(ipImg, templateWidth, templateHeight);

          IplImage copy = cvCreateImage(new CvSize(templateWidth, templateHeight), ipImg.depth(), ipImg.nChannels());
          // cvCopy(resizedImage, copy, null);
          cvSetZero(copy);
          IplImage merged = copier.copy(resizedImage, copy);

          saveToFile(imageFile.getParent() + File.separator + CACHE_DIR + File.separator + imageFile.getName(), merged);

          // END STANDARDIZE SUPERVISORS IMAGES INTO CACHE_DIR

          images.put(counter, toMat(merged));
          log.warn("{} {}-{}", totalImageFiles, label, counter);
          labelsBuf.put(counter, label.hashCode());
          idToLabelMap.put(label.hashCode(), label);

          counter++;
        }
      }

      // recognizer = FisherFaceRecognizer.create(); // requires same size
      // training images
      recognizer = EigenFaceRecognizer.create(); // requires same size
      // training images
      // recognizer = LBPHFaceRecognizer.create();

      long startTrainingTs = System.currentTimeMillis();
      recognizer.train(images, labels);
      log.error("training done in {}", (System.currentTimeMillis() - startTrainingTs) / 1000L);
      log.info("here");
    }
  }

  FilenameFilter imgFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      name = name.toLowerCase();
      return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
    }
  };

  /**
   * a class to look for new directories created by the "supervisor" and retrain
   * classifiers or create new ones.
   * 
   * it might also examine if files have been added and if classifiers need to
   * be re-trained
   */
  class ScanTimer extends TimerTask {
    File rootDir = null;
    Set<Files> subclasses = new TreeSet<>();
    OpenCVClassifier classifier = null;

    public ScanTimer(File root) {
      rootDir = root;
      classifier = new OpenCVClassifier(rootDir);
    }

    @Override
    public void run() {
      // periodically crawl the training directory
      // trainer.sc
      classifiers.get(rootDir).scanAndProcess();
    }
  }

  @Override
  public void release() {
    if (scanTimer != null) {
      scanTimer.cancel();
      scanTimer = null;
    }
    if (timer != null) {
      timer.cancel();
    }
  }

  // at the moment have only 1 trainer .. could certainly change
  ScanTimer scanTimer = null;
  Timer timer = new Timer(String.format("%s-training-timer", name));

  /**
   * FIXME - haarcascade face finder "works" but should be put in a
   * interface/framework which treats it like a "generalized" classifier
   * 
   * @param name
   */
  public OpenCVFilterFaceTraining(String name) {
    super(name);
    File root = facesSubclass;
    root.mkdirs();

    // make "machine" dir
    String machineSubclass = facesSubclass.getParent() + File.separator + "_" + facesSubclass.getName();
    new File(machineSubclass).mkdirs();

    // first subclass in the training dir - since we are doing face classifiers
    // - it will
    // be "faces" - but in theory if the classifiers are flexible or we change
    // our model
    // it could be "rooms" or "objects" etc..

    OpenCVClassifier classifier = new OpenCVClassifier(root);
    classifiers.put(root, classifier);

    scanTimer = new ScanTimer(root);
    timer.schedule(scanTimer, 0, 3000);

    /**
     * <pre>
     * // potentially "other" things could be trained besides faces
     * // and seperate classifiers could be running continuously
     * // I would be intersted in a "room" classifier - which takes pictures of
     * // a room and recognizes what room
     * // the robot is currently in
     * // _room/office
     * // _room/kitchen
     * // _room/hallway
     * OpenCVClassifier classifier = new OpenCVClassifier("training/_room");
     * </pre>
     */

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

  /**
   * TODO - face classifier should be handled in the same way as other
   * "sub"-classifiers - e.g. classifier = new Classifier(subclass)
   */
  @Override
  public void imageChanged(IplImage image) {
    // Allocate the memory storage TODO make this globalData
    if (storage == null) {
      storage = cvCreateMemStorage(0);
    }

    if (cascade == null) {
      // Preload the opencv_objdetect module to work around a known bug.
      Loader.load(opencv_objdetect.class);

      log.info("Starting new classifier {}", cascadeFile);
      cascade = new CascadeClassifier(String.format("%s/%s", cascadeDir, cascadeFile));

      if (cascade == null) {
        log.error("Could not load classifier cascade");
      }
    }

  }

  @Override
  public IplImage process(IplImage image) {

    bb = new ArrayList<Rectangle>();

    // Clear the memory storage which was used before
    cvClearMemStorage(storage);

    // Find whether the cascade is loaded, to find the faces. If yes, then:
    if (cascade != null) {
      //CvSeq faces = cvHaarDetectObjects(image, cascade, storage, scaleFactor, minNeighbors, option);
      Mat imageMat = converterToImage.convertToMat(converterToMat.convert(image));
      RectVector vec = new RectVector();
      cascade.detectMultiScale(imageMat, vec);
      
      if (vec != null) {
        faceCnt = (int)vec.size();
        for (i = 0; i < faceCnt; i++) {
          try {

            CvRect r = new CvRect(vec.get(i));

            bb.add(new Rectangle(r.x(), r.y(), r.width(), r.height()));
            data.putBoundingBoxArray(bb);

            if (saveFaces) {

              // TODO - in theory output should be highest quality pictures -
              // and leave it to the
              // classifiers/trainers/recognizers to process the input to their
              // requirements
              // pre-process / isolate detected face

              // ====== BEGIN AUGMENT RESIZE FILTER TO DUMP CROPPED BOUNDING
              // BOXES
              // this is the full data copy (color - original size - etc...)
              cvSetImageROI(image, r);
              IplImage origBB = cvCreateImage(new CvSize(r.width(), r.height()), image.depth(), image.nChannels());
              cvCopy(image, origBB, null); // roi vs mask ?

              /**
               * <pre>
               * FIXME !!!! COMPLETE WITH CONFIDENCE
               */
              // save only if failed to use the classifier to recognize
              // if (confidence > 80) => save to subclass directory

              // String filename = String.format(facesSubclass.getAbsolutePath()
              // + File.separator + "%07d-%03d.png", opencv.getFrameIndex(), i);
              String filename = getMachineFileName(facesSubclass, opencv.getFrameIndex(), i);
              saveToFile(filename, origBB); //
              // cvSaveImage(String.format("%s"+File.separator+"%07d-%03d.png",targetDir,
              // opencv.getFrameIndex(), i), copy);
              r.close();

              // must "reset" ROI - seems to be "global" memory - affect
              // subsequent searches
              r = new CvRect(0, 0, image.width(), image.width());
              cvSetImageROI(image, r);

              // ====== BEGIN AUGMENT RESIZE FILTER TO DUMP CROPPED BOUNDING
              // BOXES

              // ====== BEGIN STANDARD TEMPLATE
              // BEGIN STANDARDIZE SUPERVISORS IMAGES INTO CACHE_DIR
              // convert to grey !
              IplImage gray = cvCreateImage(origBB.cvSize(), 8, 1);

              IplImage resizedImage = OpenCVFilterResize.resizeImageMaintainAspect(gray, templateWidth, templateHeight);

              IplImage template = cvCreateImage(new CvSize(templateWidth, templateHeight), gray.depth(), gray.nChannels());
              // cvCopy(resizedImage, copy, null);
              cvSetZero(template);
              IplImage merged = copier.copy(resizedImage, template);

              // TODO- COMPARE - IF A LABEL WITH ENOUGH CONFIDENCE COMES UP IT
              // GOES TO _{label} directory !

              // END STANDARDIZE SUPERVISORS IMAGES INTO CACHE_DIR
              // ====== BEGIN STANDARD TEMPLATE

              /// *<pre> predict !!
              IntPointer label = new IntPointer(1);
              DoublePointer confidence = new DoublePointer(1);
              OpenCVClassifier classifier = classifiers.get(facesSubclass);
              if (classifier.recognizer == null) {
                log.warn("no recognizer");
                confidence.close();
                label.close();
                return image;
              }
              classifier.recognizer.predict(toMat(merged), label, confidence);

              // IF HIGH ENOUGH CONFIDENCE GO TO APPROPRIATE DIRECTORIES
              if (confidence.get() > 50) {
                String labelStr = idToLabelMap.get(label.get());
                // we're making "confidence" guess of a person
                // FIXME - REFACTOR - THIS NEEDS TO BE ABLE TO RECURSE ... we're
                // at faces level - but it may be arbitrarily deeper
                // We've already traveled to the depth of faces .. future may be
                // deeper
                File dir = new File(facesSubclass + File.separator + labelStr);
                dir.mkdirs();

                // saving the "original" non-pre-processed image to the _{class}
                // "guess" directory - prolly should encode confidence
                // in
                // filename ?
                // this needs to be fixed to recursively build all classifiers
                // and key/directory paths
                String faceGuess = getMachineFileName(dir, opencv.getFrameIndex(), i);
                File faceGuessFile = new File(faceGuess);
                faceGuessFile.getParentFile().mkdirs();
                // File guessFace = dir.getParent() + File.separator +
                saveToFile(faceGuess, origBB);

                // TODO saved the pre-processed guess to the cache file
                // saveToFile(imageFile.getParent() + File.separator + CACHE_DIR
                // + File.separator + imageFile.getName(), merged);
              }

              // int predictedLabel =
              // classifier.recognizer.predict_label(convertToMat(copy));
              // BytePointer bp =
              // classifier.recognizer.getLabelInfo(predictedLabel);
              // TODO: what char encoding is this?!
              // String name = bp.getString();
              log.warn("Recognized a Face {} - {}", 0, name);
              // </pre>*/

              r.close();
            }

            r.close();
          } catch (Exception e) {
            log.error("recognizing threw", e);
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

  private String getMachineFileName(File subclass, int frameIndex, int faceCnt) {
    String parent = subclass.getParent();
    String machineSubclass = "_" + subclass.getName();
    return String.format(parent + File.separator + machineSubclass + File.separator + String.format("%07d-%03d.png", frameIndex, faceCnt));
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (bb.size() > 0) {
      for (int i = 0; i < bb.size(); ++i) {
        Rectangle rect = bb.get(i);
        graphics.drawRect((int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
      }
    }
    graphics.drawString(mode, 20, 20);
    return image;
  }

}
