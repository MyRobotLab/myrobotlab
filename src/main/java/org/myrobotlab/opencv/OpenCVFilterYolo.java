package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromDarknet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.myrobotlab.document.Classification;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

public class OpenCVFilterYolo extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterYolo.class);

  protected Boolean running;

  // zero offset to where the confidence level is in the output matrix of the
  // darknet.
  private static final int CONFIDENCE_INDEX = 4;

  transient private final OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();

  private float confidenceThreshold = 0.25F;
  // the column in the detection matrix that contains the confidence level. (I
  // think?)
  // int probability_index = 5;
  // yolo file locations
  // private String darknetHome = "c:/dev/workspace/darknet/";

  // *** the 'correct' way ***
  // public String darknetHome =
  // FileIO.gluePaths(Service.getResourceDir(OpenCV.class),"yolo");
  public String darknetHome = "resource/OpenCV/yolo"; // FileIO.gluePaths(Service.getResourceDir(OpenCV.class),"yolo");
  public String modelConfig = "yolov2.cfg";
  public String modelWeights = "yolov2.weights";
  public String modelNames = "coco.names";

  int classifierThreadCount = 0;

  transient DecimalFormat df2 = new DecimalFormat("#.###");

  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  boolean debug = false;
  transient private Net net;
  ArrayList<String> classNames;
  public ArrayList<Classification> lastResult = null;
  transient private volatile IplImage lastImage = null;
  private volatile boolean pending = false;
  transient private Thread classifier;

  public OpenCVFilterYolo(String name) {
    super(name);
    enable(); // start classifier thread
  }

  public OpenCVFilterYolo() {
    this(null);
  }

  private void loadYolo() {
    log.info("loadYolo - begin");

    try {
      net = readNetFromDarknet(darknetHome + File.separator + modelConfig, darknetHome + File.separator + modelWeights);
      log.info("Loaded yolo darknet model to opencv");
    } catch (Exception e) {
      log.error("readNetFromDarknet could not read", e);
      return;
    }
    // load the class names
    try {
      classNames = loadClassNames(darknetHome + File.separator + modelNames);
    } catch (IOException e) {
      log.warn("Error unable to load class names from file {}", modelNames, e);
      return;
    }
    log.info("Done loading model..");
    log.info("loadYolo - end");
  }

  private ArrayList<String> loadClassNames(String filename) throws IOException {
    log.info("loadClassNames - begin");
    ArrayList<String> names = new ArrayList<String>();
    FileReader fileReader = new FileReader(filename);
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    String line;
    int i = 0;
    while ((line = bufferedReader.readLine()) != null) {
      names.add(line.trim());
      i++;
    }
    log.info("read {} names", i);
    fileReader.close();

    log.info("loadClassNames - end");
    return names;
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    if (lastResult != null) {
      // the thread running will be updating lastResult for it as fast as it
      // can.
      // displayResult(image, lastResult);
    }
    // ok now we just need to update the image that the current thread is
    // processing (if the current thread is idle i guess?)
    lastImage = image;
    if (enabled) {
      pending = true;
    }
    return image;
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public void run() {
    log.info("yolo run - begin");
    try {
      int count = 0;
      long start = System.currentTimeMillis();
      loadYolo();
      log.info("Starting the Yolo classifier thread...");
      // in a loop, grab the current image and classify it and update the
      // result.
      running = true;
      // loading the model takes a lot of time, we want to block enable/disable
      // until we are actually running - then we notifyAll

      while (running && enabled) {
        if (!pending) {
          log.debug("Skipping frame");
          Thread.sleep(10);
          continue;
        }

        log.info("process - begin");
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

          Map<String, List<Classification>> ret = new TreeMap<>();
          for (Classification c : lastResult) {
            List<Classification> nl = null;
            if (ret.containsKey(c.getLabel())) {
              nl = ret.get(c.getLabel());
            } else {
              nl = new ArrayList<>();
              ret.put(c.getLabel(), nl);
            }
            nl.add(c);
          }

          invoke("publishClassification", ret);
        } else {
          log.info("No Image to classify...");
        }
        // TODO: see why there's a race condition. i seem to need a little delay
        // here o/w the recognition never seems to start.
        // maybe lastImage needs to be marked as volatile ?

        Thread.sleep(1);
      } // while (running)

    } catch (Exception e) {
      log.error("yolo thread threw", e);
    }

    synchronized (lock) {
      classifier = null;
    }

    log.info("yolo exiting classifier thread");
    log.info("run - end");
  }

  private ArrayList<Classification> yoloFrame(IplImage frame) {
    log.debug("Starting yolo on frame...");
    log.info("yoloFrame - begin");
    // this is our list of objects that have been detected in a given frame.
    ArrayList<Classification> yoloObjects = new ArrayList<Classification>();
    // convert that frame to a matrix (Mat) using the frame converters in javacv

    log.info("yoloFrame - grabberConverter {}", frame);
    // log.info("Yolo frame start");
    Mat inputMat = grabberConverter.convertToMat(grabberConverter.convert(frame));
    // log.info("Input mat created");
    // TODO: I think yolo expects RGB color (which is inverted in the next step)
    // so if the input image isn't in RGB color, we might need a cvCutColor
    log.info("yoloFrame - blobFromImage");
    Mat inputBlob = blobFromImage(inputMat, 1 / 255.F, new Size(416, 416), new Scalar(), true, false, CV_32F);
    // put our frame/input blob into the model.
    // log.info("input blob created");
    log.info("yoloFrame - blob {}", inputBlob);
    net.setInput(inputBlob);

    log.debug("Feed forward!");
    // log.info("Input blob set on network.");
    // ask for the detection_out layer i guess? not sure the details of the
    // forward method, but this computes everything like magic!
    Mat detectionMat = net.forward("detection_out");
    // log.info("output detection matrix produced");
    log.debug("detection matrix computed");
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
          if (opencv != null) {
            String localized = opencv.localize(label);
            if (localized != null) {
              label = localized;
            }
          }
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

          log.debug(label + " (" + confidence + "%) [(" + xLeftBottom + "," + yLeftBottom + "),(" + xRightTop + "," + yRightTop + ")]");
          Rect boundingBox = new Rect(xLeftBottom, yLeftBottom, xRightTop - xLeftBottom, yRightTop - yLeftBottom);
          // grab just the bytes for the ROI defined by that rect..
          // get that as a mat, save it as a byte array (png?) other encoding?
          // TODO: have a target size?

          IplImage cropped = extractSubImage(inputMat, boundingBox);
          if (debug) {
            debug = false;
            show(cropped, "detected img");
          }
          Classification obj = new Classification(String.format("%s.%s-%d", data.getName(), name, data.getFrameIndex()));
          obj.setLabel(label);
          obj.setBoundingBox(xLeftBottom, yLeftBottom, xRightTop - xLeftBottom, yRightTop - yLeftBottom);
          obj.setConfidence(confidence);
          // obj.setImage(data.getDisplay());
          // for non-serializable "local" image objects
          obj.setObject(frame);
          yoloObjects.add(obj);
        }
      }
    }
    log.info("yoloFrame - end");
    return yoloObjects;
  }

  private IplImage extractSubImage(Mat inputMat, Rect boundingBox) {
    log.info("extractSubImage - begin");
    //
    log.debug(boundingBox.x() + " " + boundingBox.y() + " " + boundingBox.width() + " " + boundingBox.height());

    // TODO: figure out if the width/height is too large! don't want to go array
    // out of bounds
    Mat cropped = new Mat(inputMat, boundingBox);

    IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(cropped));
    // This mat should be the cropped image!

    log.info("extractSubImage - end");
    return image;
  }

  @Override
  public void release() {
    // synchronized (lock) {
    log.info("release - begin");
    disable(); // blocks until ready

    // while(isRunning){ sleep(30) .. check again }
    // bleed out the thread before deallocating

    if (net != null) {
      net.deallocate();
      net = null;
    }
    log.info("release - end");
    // }
  }

  volatile Object lock = new Object();

  @Override
  public void enable() {
    if (classifier != null) {
      // already enabled
      return;
    }
    log.info("enabling yolo");
    synchronized (lock) {
      log.info("enable - begin");
      super.enable();
      if (classifier == null) {
        classifier = new Thread(this, "YoloClassifierThread");
        classifier.start();
      }
      log.info("enable - end");
    }
  }

  @Override
  public void disable() {
    super.disable();
    if (classifier == null) {
      // already disabled
      return;
    }
    int waitTime = 0;    
    while (classifier != null && waitTime < 1000) {
      ++waitTime;
      Service.sleep(10);
    }
    log.info("capture - waited {} times", waitTime);
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (lastResult != null) {

      for (Classification obj : lastResult) {
        String label = obj.getLabel() + " (" + df2.format(obj.getConfidence() * 100) + "%)";

        Rectangle bb = obj.getBoundingBox();
        int x = (int) bb.x;
        int y = (int) bb.y;
        int width = (int) bb.width;
        int height = (int) bb.height;

        graphics.setColor(Color.BLACK);
        graphics.drawRect(x, y, width, height);
        graphics.fillRect(x, y - 20, 7 * label.length(), 20);
        graphics.setColor(Color.WHITE);
        graphics.drawString(label, x + 6, y - 6);
      }
    }
    return image;
  }

  public float getConfidenceThreshold() {
    return confidenceThreshold;
  }

  public void setConfidenceThreshold(float i) {
    confidenceThreshold = i;
    broadcastFilterState();
  }

}