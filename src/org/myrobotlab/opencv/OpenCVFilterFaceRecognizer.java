package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvInitFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.getAffineTransform;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

import javax.swing.WindowConstants;

import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import com.google.common.base.CharMatcher;

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
  // TODO: why the heck do we need to convert back and forth, and is this
  // effecient?!?!
  transient private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  private HashMap<Integer, String> idToLabelMap = new HashMap<Integer, String>();

 
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
  
  private static Hashtable<String, String> UnicodeFolder = new Hashtable<String, String>();
  
  public OpenCVFilterFaceRecognizer() {
    super();
    initHaarCas();
    initRecognizer();
  }

  public OpenCVFilterFaceRecognizer(String name) {
    super(name);
    initHaarCas();
    initRecognizer();
  }

  public OpenCVFilterFaceRecognizer(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    initHaarCas();
    initRecognizer();
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
   * @return true if the training was successful.
   */ 
  public boolean train() {
    //
    // The first time we train, find the image mask, if present, scale it to the
    // current image size,
    // and save it for later.
    //
    if (facemask == null) {
      File filterfile = new File("resource/facerec/Filter.png");
      //
      // Face mask used to mask edges of face pictures to eliminate noise around
      // the edges
      //
      if (!filterfile.exists()) {
        log.warn("No image filter file found.  {}", filterfile.getAbsolutePath());
      } else {
        // Read the filter and rescale it to the current image size
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
    for (File image : imageFiles) {
      // load the image
      Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);
      // Parse the filename label-foo.jpg everything up to the first - is the
      // label.
      // String personName = image.getName().split("\\-")[0];
      String personName = UnicodeFolder.get(image.getParentFile().getName());
      
      // TODO: we need an integer to represent this string .. for now we're
      // using a hashcode here.
      // this can definitely have a collision!
      // we really need a better metadata store for these images.
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
      // TODO: add a debug method to show the image
      if (debug) {
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
    
    return true;
  }

  private void initRecognizer() {
    // Configure which type of recognizer to use
    if (RecognizerType.FISHER.equals(recognizerType)) {
      faceRecognizer = createFisherFaceRecognizer();
    } else if (RecognizerType.EIGEN.equals(recognizerType)) {
      faceRecognizer = createEigenFaceRecognizer();
    } else {
      faceRecognizer = createLBPHFaceRecognizer();
    }
  }

 
  /**
   * Save the current model to the faceModelFilename
   * @throws IOException 
   */
  public void save() throws IOException {
    save(faceModelFilename);
  }
  
  /**
   * @param filename the filename to save the current model to.
   * @throws IOException 
   */
  public void save(String filename) throws IOException {
    
    String labelFilename = filename + ".labels";
    faceRecognizer.save(filename);
    // TODO: we also need to save the labels for the model so we can load them back in.
    FileWriter fw = new FileWriter(new File(labelFilename));
    for (Integer key : idToLabelMap.keySet()) {
      fw.write(key + ":" + idToLabelMap.get(key) + "\n");
    }
    fw.close();
  }
  
  
  /**
   * load the model from the default filename specified by faceModelFilename.
   * @throws IOException 
   */
  public void load() throws IOException {
    load(faceModelFilename);
  }
  /**
   * Load a face recognizer model from the provided saved filename.
   * @param filename the filename that represents the saved model.
   * @throws IOException 
   */
  public void load(String filename) throws IOException {
    String labelFilename = filename + ".labels";
    faceRecognizer.load(filename);
    
    // load the labels.
    BufferedReader fr = new BufferedReader(new FileReader(new File(labelFilename)));
    String line = null;
    
    // re-initialize the id to label map.
    idToLabelMap = new HashMap<Integer,String>();
    while ((line = fr.readLine()) != null) {
      int delimOffset = line.indexOf(":");
      String key = line.substring(0, delimOffset);
      String value = line.substring(delimOffset);
      idToLabelMap.put(Integer.valueOf(key), value);
    }
    fr.close();
    //assume we're trained now..
    trained = true;
  }
  
  private  ArrayList<File> listImageFiles(File root) {
    
    // 
    ArrayList<File> trainingFiles = new ArrayList<File>();
    
    // only jpg , png , pgm files.  TODO: other formats? bmp/tiff/etc?
    FilenameFilter imgFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        name = name.toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
      }
    };
    
    FilenameFilter labelFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        name = name.toLowerCase();
        return name.endsWith(".label");
      }
    };
    
    String[] contents = root.list();
    for (String fn : contents) {
      File f = new File(root.getAbsolutePath() + File.separator + fn);

      if (f.isDirectory()) {
        
        for (File x : f.listFiles(imgFilter)) {
          trainingFiles.add(x);
        }
        
        String labelUnicode = fn;

        
        // opencv cannot parse unicode folders, so we use an extra thing as unicode label ( file.label )
        Boolean foundUnicodeLabel = false;
        for (File l : f.listFiles(labelFilter)) {
          int pos = l.getName().lastIndexOf(".");
          labelUnicode = l.getName().substring(0, pos);
          foundUnicodeLabel=true;
          FeedUnicodeDictionary(labelUnicode, fn);
          FeedUnicodeDictionary(fn, labelUnicode);
          
        }
        if (!foundUnicodeLabel)
        {
          log.info("labelNotUnicode : "+labelUnicode);
          FeedUnicodeDictionary(fn, labelUnicode);
        }
      }
    }
    return trainingFiles;
  }
  
  /**
   * Feed a collection to map unicode text to ascii one
   * This take care about duplicates
   * @return mapped text
   */
  private static String FeedUnicodeDictionary(String unicodeName)  {
    
    if (UnicodeFolder.get(unicodeName) != null)
    {
      return UnicodeFolder.get(unicodeName);
    }
    else
    {
    String randValue = UUID.randomUUID().toString();
    UnicodeFolder.put(randValue, unicodeName);
    UnicodeFolder.put(unicodeName, randValue);
      return randValue;
    }
  }
  
  /**
   * Feed a collection to map unicode text to ascii one
   * This take care about duplicates
   */
  private static void FeedUnicodeDictionary(String key, String unicodeName)  {
    if (UnicodeFolder.get(key) == null){
    UnicodeFolder.put(key, unicodeName);
    log.info("key : "+key+" label : "+unicodeName);
    }
  }

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
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
    cvInitFont(font,CV_FONT_HERSHEY_SIMPLEX,0.5,0.5,2,1,10);
    cvInitFont(fontWarning,CV_FONT_HERSHEY_SIMPLEX,0.4,0.3);
    // convert to grayscale
    Frame grayFrame = makeGrayScale(image);
    // TODO: this seems super wonky! isn't there an easy way to go from IplImage
    // to opencv Mat?
    int cols = grayFrame.imageWidth;
    int rows = grayFrame.imageHeight;
    // convert to a Mat
    Mat bwImgMat = converterToIpl.convertToMat(grayFrame);

    //
    // Image detection is done on the grayscale image, so we can modify the
    // original frame once
    // we make a grayscale copy.
    //
    if (Mode.TRAIN.equals(mode)) {
      String status = "Training Mode: " + trainName;
      cvPutText(image, status, cvPoint(20, 40), font, CvScalar.GREEN);
    } else if (Mode.RECOGNIZE.equals(mode)) {
      String status = "Recognize Mode:" + lastRecognizedName;
      cvPutText(image, status, cvPoint(20, 40), font, CvScalar.YELLOW);
    }

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
        // dFaceMat is the cropped and rotated face
        if (Mode.TRAIN.equals(mode)) {
          // we're in training mode.. so we should save the image
          log.info("Training Mode for {}.", trainName);
          if (!StringUtils.isEmpty(trainName)) {
            saveTrainingImage(trainName, dFaceMat);
            cvPutText(image, "Snapshot Saved: " + trainName, cvPoint(20, 60), font, CvScalar.CYAN);
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

            int predictedLabel = faceRecognizer.predict(dFaceMatSized);
            String name = Integer.toString(predictedLabel);
            if (idToLabelMap.containsKey(predictedLabel)) {
              name = idToLabelMap.get(predictedLabel);
            } else {
              // you shouldn't ever see this.
              log.warn("Unknown predicted label returned! {}", predictedLabel);
            }
            log.info("Recognized a Face {} - {}", predictedLabel, name);
            if (!CharMatcher.ASCII.matchesAllOf(name))
            {
              cvPutText(image, "[unicode char detected !!! text not visible, but succesfully parsed !]", cvPoint(5, 60), fontWarning, CvScalar.RED);
            }
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

  private void saveTrainingImage(String label, Mat dFaceMat) {
    // create some sort of a unique value so the file names don't
    // conflict
    // TODO: use something more random like a
    // OK now we need to make a subdirectory for the label if it doesn't exist.
    // imwrite dont like unicode, we map unicode name to ascii
    String labelUnicode=label;
    if (!CharMatcher.ASCII.matchesAllOf(label))
    {
      labelUnicode=FeedUnicodeDictionary(labelUnicode);
    }
    
    File labelDir = new File(trainingDir + File.separator + labelUnicode);
    if (!labelDir.exists()) {
      labelDir.mkdirs();
    }
    // TODO: should we give it something other than a random uuid ?
    UUID randValue = UUID.randomUUID();
    String filename = trainingDir + File.separator + labelUnicode + File.separator + randValue + ".png";
    // TODO: I think this is a png file ? not sure.
    imwrite(filename, dFaceMat);
    File labelIdentifier = new File(trainingDir + File.separator + labelUnicode + File.separator + label + ".label");
    if (!CharMatcher.ASCII.matchesAllOf(label) && !labelIdentifier.exists())
    {
      try {
        new FileOutputStream(labelIdentifier).close();
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
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
}
