package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromDarknet;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvFont;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPutText;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.AbstractCvScalar;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.deeplearning4j.CustomModel;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.slf4j.Logger;

/**
 * Experimental filter to be used with Lloyd. All behaviors of this filter are
 * subject to change.
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterLloyd extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterLloyd.class.getCanonicalName());

  // zero offset to where the confidence level is in the output matrix of the
  // darknet.
  private static final int CONFIDENCE_INDEX = 4;
  private final OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();

  private float confidenceThreshold = 0.25F;
  // the column in the detection matrix that contains the confidence level. (I
  // think?)
  // int probability_index = 5;
  // yolo file locations
  // private String darknetHome = "c:/dev/workspace/darknet/";
  public String darknetHome = "yolo";
  public String modelConfig = "yolov2.cfg";
  public String modelWeights = "yolov2.weights";
  public String modelNames = "coco.names";

  // TODO: store these somewhere as a resource / dependency ..
  public String modelConfigUrl = "https://raw.githubusercontent.com/pjreddie/darknet/master/cfg/yolov2.cfg";
  public String modelWeightsUrl = "https://pjreddie.com/media/files/yolov2.weights";
  public String modelNamesUrl = "https://raw.githubusercontent.com/pjreddie/darknet/master/data/coco.names";

  transient private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  boolean debug = false;
  private Net net;
  ArrayList<String> classNames;
  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);
  public ArrayList<YoloDetectedObject> lastResult = null;
  private volatile IplImage lastImage = null;
  private volatile boolean pending = false;
  public Deeplearning4j dl4j;

  CustomModel personModel = null;
  boolean enabled = true;

  transient Thread classifier;
  boolean running = false;

  public OpenCVFilterLloyd() {
    super();
    // start classifier thread
    classifier = new Thread(this, "YoloClassifierThread");
    classifier.start();
    log.info("Yolo Classifier thread started : {}", this.name);
  }

  public OpenCVFilterLloyd(String name) {
    super(name);
    // start classifier thread
    classifier = new Thread(this, "YoloClassifierThread");
    classifier.start();
    log.info("Yolo Classifier thread started : {}", this.name);
  }

  private void downloadYoloModel() {

    File yoloHome = new File(darknetHome);
    if (!yoloHome.exists()) {
      yoloHome.mkdirs();
    }

    // now we need to check the files in the directory exist.
    // 3 files to check for
    File modelConfigFile = new File(darknetHome + File.separator + modelConfig);
    File modelWeightsFile = new File(darknetHome + File.separator + modelWeights);
    // TODO: localize this? why not!
    File modelNamesFile = new File(darknetHome + File.separator + modelNames);

    if (!modelConfigFile.exists()) {
      // download & cache!
      downloadAndCache(modelConfigUrl, modelConfigFile, null);
    }
    if (!modelWeightsFile.exists()) {
      // download & cache!
      downloadAndCache(modelWeightsUrl, modelWeightsFile, "Large download 200mb +/-");
    }
    if (!modelNamesFile.exists()) {
      // download & cache!
      downloadAndCache(modelNamesUrl, modelNamesFile, null);
    }

  }

  private void downloadAndCache(String uri, File location, String details) {
    // TODO: clean up the error handling here.

    log.info("Downloading {} to file location {}) {}", uri, location.getAbsolutePath(), details);
    URL url = null;
    try {
      url = new URL(uri);
    } catch (MalformedURLException e) {
      log.warn("Invalid url passed! {}", uri);
      e.printStackTrace();
      return;
    }
    InputStream in = null;
    try {
      in = url.openStream();
    } catch (IOException e) {
      log.warn("Error opening a connection to {} ", uri);
      e.printStackTrace();
      return;
    }
    DataInputStream dis = new DataInputStream(new BufferedInputStream(in));
    try {
      // open up the destination file for writing
      FileOutputStream fos = new FileOutputStream(location);
      IOUtils.copy(dis, fos);
      fos.close();
      dis.close();
    } catch (IOException e) {
      log.warn("Error downloading.");
      e.printStackTrace();
      // clean up a partially written file
      if (location.exists()) {
        log.warn("Partially downloaded file.. cleaning up");
        location.delete();
      }
      return;
    }
  }

  private void loadYolo() {
    // If the model isn't there, we should download it and cache it.
    log.info("Staritng yolo download verification");
    downloadYoloModel();
    log.info("Completed downloading yolo model");
    net = readNetFromDarknet(darknetHome + File.separator + modelConfig, darknetHome + File.separator + modelWeights);
    log.info("Loaded yolo darknet model to opencv");
    // load the class names
    try {
      classNames = loadClassNames(darknetHome + File.separator + modelNames);
    } catch (IOException e) {
      // e.printStackTrace();
      log.warn("Error unable to load class names from file {}", modelNames);
      return;
    }
    log.info("Done loading model..");

  }

  private ArrayList<String> loadClassNames(String filename) throws IOException {
    ArrayList<String> names = new ArrayList<String>();
    FileReader fileReader = new FileReader(filename);
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    String line;
    int i = 0;
    while ((line = bufferedReader.readLine()) != null) {
      names.add(line.trim());
      i++;
    }
    fileReader.close();
    return names;
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    if (lastResult != null) {
      // the thread running will be updating lastResult for it as fast as it
      // can.
      displayResult(image, lastResult);
    }
    // ok now we just need to update the image that the current thread is
    // processing (if the current thread is idle i guess?)
    lastImage = image;
    pending = true;
    return image;
  }

  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  private void displayResult(IplImage image, ArrayList<YoloDetectedObject> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    for (YoloDetectedObject obj : result) {
      String label = obj.label + " (" + df2.format(obj.confidence * 100) + "%)";
      String subLabel = obj.sublabel;
      // anchor point for text.
      cvPutText(image, label, cvPoint(obj.boundingBox.x(), obj.boundingBox.y()), font, AbstractCvScalar.YELLOW);
      if (!StringUtils.isEmpty(subLabel)) {
        cvPutText(image, subLabel, cvPoint(obj.boundingBox.x(), obj.boundingBox.y() + 20), font, AbstractCvScalar.YELLOW);
      }
      drawRect(image, obj.boundingBox, AbstractCvScalar.BLUE);
    }
  }

  public void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  @Override
  public void run() {

    loadYolo();

    int count = 0;
    long start = System.currentTimeMillis();
    log.info("Starting the Yolo classifier thread...");
    running = true;
    // in a loop, grab the current image and classify it and update the result.
    while (running) {

      if (!pending || !enabled) {
        log.debug("Skipping frame");
        try {
          // prevent thrashing of the cpu ...
          Thread.sleep(20);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          break;
        }
        continue;
      }
      // only classify this if we haven't already classified it.
      if (lastImage != null) {
        // lastResult = dl4j.classifyImageVGG16(lastImage);
        log.debug("Doing yolo...");
        lastResult = yoloFrame(lastImage);
        // log.info("Yolo done.");
        // we processed, next object we'll pick up.
        pending = false;
        count++;
        if (count % 10 == 0) {
          double rate = 1000.0 * count / (System.currentTimeMillis() - start);
          log.info("Yolo Classification Rate : {}", rate);
        }
        invoke("publishYoloClassification", lastResult);
      } else {
        log.info("No Image to classify...");
      }
      // TODO: see why there's a race condition. i seem to need a little delay
      // here o/w the recognition never seems to start.
      // maybe lastImage needs to be marked as volatile ?
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    log.info("Exited the run loop for Yolo!!! you shouldn't see this.");
  }

  private ArrayList<YoloDetectedObject> yoloFrame(IplImage frame) {
    // log.info("Starting yolo on frame...");
    // this is our list of objects that have been detected in a given frame.
    ArrayList<YoloDetectedObject> yoloObjects = new ArrayList<YoloDetectedObject>();
    // convert that frame to a matrix (Mat) using the frame converters in javacv

    // log.info("Yolo frame start");
    Mat inputMat = grabberConverter.convertToMat(grabberConverter.convert(frame));
    // log.info("Input mat created");
    // TODO: I think yolo expects RGB color (which is inverted in the next step)
    // so if the input image isn't in RGB color, we might
    // need a cvCutColor
    Mat inputBlob = blobFromImage(inputMat, 1 / 255.F, new Size(416, 416), new Scalar(), true, false, CV_32F); // Convert
    // Mat
    // to
    // batch
    // of
    // images
    // put our frame/input blob into the model.
    // log.info("input blob created");
    net.setInput(inputBlob);

    // log.info("Feed forward!");
    // log.info("Input blob set on network.");
    // ask for the detection_out layer i guess? not sure the details of the
    // forward method, but this computes everything like magic!
    Mat detectionMat = net.forward("detection_out");
    // log.info("output detection matrix produced");
    // log.info("detection matrix computed");
    // iterate the rows of the detection matrix.
    for (int i = 0; i < detectionMat.rows(); i++) {
      Mat currentRow = detectionMat.row(i);
      float confidence = currentRow.getFloatBuffer().get(CONFIDENCE_INDEX);
      if (confidence < confidenceThreshold) {
        // skip the noise
        continue;
      }

      // System.out.println("\nCurrent row has " + currentRow.size().width() +
      // "=width " + currentRow.size().height() + "=height.");
      // currentRow.position(probability_index);
      // int probability_size = detectionMat.cols() - probability_index;
      // detectionMat;

      // String className = getWithDefault(classNames, i);
      // System.out.print("\nROW (" + className + "): " +
      // currentRow.getFloatBuffer().get(4) + " -- \t\t");
      for (int c = CONFIDENCE_INDEX + 1; c < currentRow.size().get(); c++) {
        float val = currentRow.getFloatBuffer().get(c);
        // TODO: this filtering logic is probably wrong.
        if (val > 0.0) {
          String label = classNames.get(c - CONFIDENCE_INDEX - 1);
          // System.out.println("Index : " + c + "->" + val + " label : " +
          // classNames.get(c-probability_index) );
          // let's just say this is something we've detected..
          // ok. in theory this is something we think it might actually be.
          float x = currentRow.getFloatBuffer().get(0);
          float y = currentRow.getFloatBuffer().get(1);

          float width = currentRow.getFloatBuffer().get(2);
          float height = currentRow.getFloatBuffer().get(3);
          int xLeftBottom = (int) ((x - width / 2) * inputMat.cols());
          int yLeftBottom = (int) ((y - height / 2) * inputMat.rows());
          int xRightTop = (int) ((x + width / 2) * inputMat.cols());
          int yRightTop = (int) ((y + height / 2) * inputMat.rows());

          if (xLeftBottom < 0) {
            xLeftBottom = 0;
          }
          if (yLeftBottom < 0) {
            yLeftBottom = 0;
          }

          // crop the right top
          if (xRightTop > inputMat.cols()) {
            xRightTop = inputMat.cols();
          }

          if (yRightTop > inputMat.rows()) {
            yRightTop = inputMat.rows();
          }

          log.info(label + " (" + confidence + "%) [(" + xLeftBottom + "," + yLeftBottom + "),(" + xRightTop + "," + yRightTop + ")]");
          Rect boundingBox = new Rect(xLeftBottom, yLeftBottom, xRightTop - xLeftBottom, yRightTop - yLeftBottom);
          // grab just the bytes for the ROI defined by that rect..
          // get that as a mat, save it as a byte array (png?) other encoding?
          // TODO: have a target size?
          IplImage cropped = extractSubImage(inputMat, boundingBox);
          if (debug) {
            show(cropped, "detected img");
          }
          YoloDetectedObject obj = new YoloDetectedObject(boundingBox, confidence, label, getOpenCV().getFrameIndex(), cropped, null);
          yoloObjects.add(obj);
          // if the label is a person.. we want to look up the model for further
          // classificatoin of a person
          // then add that info to disambiguate who that person is.
          // followed by a solr search for the current recognized person.
          if (obj.label.equals("person")) {
            if (dl4j != null && personModel != null) {
              Map<String, Double> dl4JResult;
              try {
                dl4JResult = dl4j.classifyImageCustom(cropped, personModel.getModel(), personModel.getLabels());
              } catch (IOException e) {
                // TODO Auto-generated catch block
                log.warn("Error in running dl4j person classifier");
                e.printStackTrace();
                continue;
              }
              // print this out!
              for (String key : dl4JResult.keySet()) {
                double dlVal = dl4JResult.get(key);
                if (dlVal > 0.9) {
                  obj.sublabel = key + "(" + dlVal + ")";
                  // System.out.println("PERSON RECOGNITION RESULT : " + key + "
                  // val " + dlVal);
                  // show(cropped,key);
                }
              }
            }
          }
        }
      }
    }
    return yoloObjects;
  }

  private IplImage extractSubImage(Mat inputMat, Rect boundingBox) {
    //
    // System.out.println(boundingBox.x() + " " + boundingBox.y() + " " +
    // boundingBox.width() + " " + boundingBox.height());

    // TODO: figure out if the width/height is too large! don't want to go array
    // out of bounds
    Mat cropped = new Mat(inputMat, boundingBox);

    IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(cropped));
    // This mat should be the cropped image!

    return image;
  }

  public Deeplearning4j getDl4j() {
    return dl4j;
  }

  public void setDl4j(Deeplearning4j dl4j) {
    this.dl4j = dl4j;
  }

  public CustomModel getPersonModel() {
    return personModel;
  }

  public void setPersonModel(CustomModel personModel) {
    this.personModel = personModel;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

  @Override
  public void release() {
    running = false;
  }

}
