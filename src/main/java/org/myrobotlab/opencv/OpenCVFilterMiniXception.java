package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvFont;
import static org.bytedeco.opencv.global.opencv_imgproc.cvPutText;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * this opencv filter will load the keras model for emotion detection published on this open source project
 * https://github.com/omar178/Emotion-recognition
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterMiniXception extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterMiniXception.class.getCanonicalName());

  private transient Deeplearning4j dl4j;
  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

  public Map<String, Double> lastResult = null;
  private volatile IplImage lastImage = null;
  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  // the additional border around the face detection to include in the emotion classification. (in pixels)
  private int boxSlop = 10;
  // a confidence threshold typically from 0.0 to 1.0 on how confident the classification is.
  private double confidence = 0.25;
  
  public OpenCVFilterMiniXception(String name) {
    super(name);
    loadDL4j();
    log.info("Finished loading mini XCEPTION model.");
  }

  private void loadDL4j() {
    dl4j = (Deeplearning4j) Runtime.createAndStart("dl4j", "Deeplearning4j");
    log.info("Loading mini XCEPTION Model.");
    try {
      dl4j.loadMiniEXCEPTION();
    } catch (IOException | UnsupportedKerasConfigurationException | InvalidKerasConfigurationException e) {
      log.warn("Error loading mini xception emotion detection model!", e);
      return;
    }
    log.info("Done loading model..");
    // start classifier thread
    Thread classifier = new Thread(this, "DL4JMiniXceptionClassifierThread");
    classifier.start();
    log.info("DL4J Mini Xception Classifier thread started : {}", this.name);
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    if (lastResult != null) {
      // the thread running will be updating lastResult for it as fast as it
      // can.
      // log.info("Display result " );
      displayResult(image, lastResult);
    }
    
    
    // there's something currently being processed.. skip
    if (lastImage != null) {
      return image;
    }
    // here we want to update the lastImage as the one with the bounding box.
    
    List<Rectangle> boxes = data.getBoundingBoxArray();
    // we should grab the center of the first box.. 
    // crop a square around that center.. and set that as the last image to pass to the emotion detector.
    if (boxes != null) {
      for (Rectangle box : boxes) {
        // log.info("Processing Box : {}", box);
        int x = (int)(box.x + box.width/2);
        int y = (int)(box.y + box.height/2);
        // now we have the center point
        // create a new box
        int miniExceptionWidth = (int)Math.max(box.width , box.height) + boxSlop;
        //int miniExceptionWidth = 64;
        Rect miniBox = new Rect(x-miniExceptionWidth/2, y-miniExceptionWidth/2, miniExceptionWidth, miniExceptionWidth);
        // now.. we need to crap the image for this bounding box..
        lastImage = extractSubImage(OpenCV.toMat(image), miniBox);
        // Here
      }
    }
    
    return image;
  }

  private IplImage extractSubImage(Mat inputMat, Rect boundingBox) {
    Mat cropped = new Mat(inputMat, boundingBox);
    IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(cropped));
    return image;
  }

  
  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  public void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  private void displayResult(IplImage image, Map<String, Double> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    int i = 0;
    int percentOffset = 150;
    for (String label : result.keySet()) {
      i++;
      String val = df2.format(result.get(label) * 100) + "%";
      cvPutText(image, label + " : ", cvPoint(20, 60 + (i * 12)), font, CvScalar.YELLOW);
      cvPutText(image, val, cvPoint(20 + percentOffset, 60 + (i * 12)), font, CvScalar.YELLOW);
    }
  }

  private String formatResultString(Map<String, Double> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    StringBuilder res = new StringBuilder();
    for (String key : result.keySet()) {
      res.append(key + " : ");
      res.append(df2.format(result.get(key) * 100) + "% , ");
    }
    return res.toString();
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  @Override
  public void release() {
    running = false;
  }

  @Override
  public void run() {

    int count = 0;
    long start = System.currentTimeMillis();
    log.info("Starting the DL4J classifier thread...");
    running = true;
    // in a loop, grab the current image and classify it and update the result.
    while (running) {// FIXME - must be able to release !!
      
      if (!enabled) {
        // sleep to avoid cpu usage
        // TODO: come up with a better way of doing this (maybe shutdown this thread and restart when it enables/disbales?)
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          log.info("Dl4j classifier thread interrupted", e);
        }
        continue;
      }
      // log.info("Running!!!");
      // now we need to know which image we should classify
      // there likely needs to be some synchronization on this too.. o/w the
      // main thread will
      // be updating it while it's being classified maybe?!
      if (lastImage != null) {
        try {
          count++;
          lastResult = dl4j.classifyImageMiniEXCEPTION(lastImage, confidence);
          // Sort this lastResult based on it's value..
          if (count % 100 == 0) {
            double rate = 1000.0 * count / (System.currentTimeMillis() - start);
            log.info("DL4J Filter Rate: {}", rate);
          }
          invoke("publishClassification", lastResult);
          if (lastResult != null && lastResult.size() > 0)
            log.info(formatResultString(lastResult));
        } catch (IOException e) {
          log.warn("Exception classifying image!", e);
        }
        // at this point, presumably we've finished classifying this image.. we should null it out.
        lastImage = null;
      } else {
        // log.info("No Image to classify...");
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
  }
  
  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    // log.info("Set confidence {}", confidence);
    this.confidence = confidence;
  }

  public int getBoxSlop() {
    return boxSlop;
  }

  public void setBoxSlop(int boxSlop) {
    // log.info("Set slop {}", boxSlop);
    this.boxSlop = boxSlop;
  }

}
