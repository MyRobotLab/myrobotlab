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
import java.util.ArrayList;
import java.util.Map;

import org.bytedeco.opencv.opencv_core.AbstractCvScalar;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_imgproc.CvFont;
import org.myrobotlab.deeplearning4j.CustomModel;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Solr;
import org.slf4j.Logger;

public class OpenCVFilterDL4JTransfer extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterDL4JTransfer.class);

  private transient Deeplearning4j dl4j;
  private transient CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

  public Map<String, Double> lastResult = null;

  protected Boolean running;

  public transient CustomModel model = null;

  private volatile IplImage lastImage = null;

  public OpenCVFilterDL4JTransfer() {
    super();
    init();
  }

  public OpenCVFilterDL4JTransfer(String name) {
    super(name);
    log.info("Constructor of dl4j filter");
    init();
  }

  private void init() {
    dl4j = (Deeplearning4j) Runtime.createAndStart("dl4j", "Deeplearning4j");
    // loadDL4j();
    // log.info("Finished loading vgg16 model.");
    Thread classifier = new Thread(this, "DL4JClassifierThread");
    classifier.start();
    log.info("DL4J Classifier thread started : {}", this.name);
  }

  public void loadCustomModel(String filename) {
    // TODO: test if file exists!
    try {
      // got to load up the custom model.
      log.info("Loading model.");
      model = dl4j.loadComputationGraph(filename);
    } catch (IOException e) {
      e.printStackTrace();
      log.warn("Error loading model!", e);
      return;
    }
    log.info("Done loading model..");
    // start classifier thread
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    if (lastResult != null) {
      // the thread running will be updating lastResult for it as fast as it
      // can.
      // log.info("Display result " );
      displayResult(image, lastResult);
    }
    // ok now we just need to update the image that the current thread is
    // processing (if the current thread is idle i guess?)
    lastImage = image;
    return image;
  }

  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  public void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  private void displayResultYolo(IplImage image, ArrayList<YoloDetectedObject> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    for (YoloDetectedObject obj : result) {
      String label = obj.label + " (" + df2.format(obj.confidence * 100) + "%)";
      // anchor point for text.
      cvPutText(image, label, cvPoint(obj.boundingBox.x(), obj.boundingBox.y()), font, AbstractCvScalar.YELLOW);
      // obj.boundingBox.
      drawRect(image, obj.boundingBox, AbstractCvScalar.BLUE);
    }
  }

  private void displayResult(IplImage image, Map<String, Double> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    int i = 0;
    int percentOffset = 150;
    for (String label : result.keySet()) {
      i++;
      String val = df2.format(result.get(label) * 100) + "%";
      cvPutText(image, label + " : ", cvPoint(20, 60 + (i * 12)), font, AbstractCvScalar.YELLOW);
      cvPutText(image, val, cvPoint(20 + percentOffset, 60 + (i * 12)), font, AbstractCvScalar.YELLOW);
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
    running = true;
    int count = 0;
    long start = System.currentTimeMillis();
    log.info("Starting the DL4J classifier thread...");
    // in a loop, grab the current image and classify it and update the result.
    while (running) {
      // log.info("Running!!!");
      // now we need to know which image we should classify
      // there likely needs to be some synchronization on this too.. o/w the
      // main thread will
      // be updating it while it's being classified maybe?!
      if (lastImage != null && model != null) {
        try {
          // dl4j.yoloImage(lastImage);
          lastResult = dl4j.classifyImageCustom(lastImage, model.getModel(), model.getLabels());
          count++;
          if (count % 100 == 0) {
            double rate = 1000.0 * count / (System.currentTimeMillis() - start);
            log.info("Rate {}", rate);
            log.info(formatResultString(lastResult));
          }
          // dl4j.classifyImageDarknet(lastImage);
          // lastResult = dl4j.classifyImageVGG16(lastImage);
          invoke("publishClassification", lastResult);
          if (lastResult != null) {
            // log.info(formatResultString(lastResult));
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          log.warn("Exception classifying image!");
          e.printStackTrace();
        }
      } else {
        // log.info("No Image to classify...");
      }
      // TODO: see why there's a race condition. i seem to need a little delay
      // here o/w the recognition never seems to start.
      // maybe lastImage needs to be marked as volatile ?
      try {
        // Let's limit the speed at which we try to classify at most 2 fps
        // should be fine
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  /*
   * public Map<String, Double> publishClassification(Map<String, Double>
   * classification) { return classification; }
   */

  public void attach(Solr solr) {

    //

  }

  public void unloadModel() {
    // TODO Auto-generated method stub
    this.model = null;
    this.lastResult = null;

  }

  public CustomModel getModel() {
    return model;
  }

  public void setModel(CustomModel model) {
    this.model = model;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }
}
