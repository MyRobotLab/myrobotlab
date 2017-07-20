package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Deeplearning4j;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class OpenCVFilterDL4J extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterDL4J.class.getCanonicalName());

  private transient Deeplearning4j dl4j;
  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);
  
  public Map<String, Double> lastResult = null; 
  private IplImage lastImage = null;
  
  public OpenCVFilterDL4J() {
    super();
    loadDL4j();
  }
  
  public OpenCVFilterDL4J(String name) {
    super(name);
    loadDL4j();
  }
  
  private void loadDL4j() {
    dl4j = (Deeplearning4j)Runtime.createAndStart("dl4j", "Deeplearning4j");
    log.info("Loading VGG 16 Model.");
    try {
      dl4j.loadVGG16();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.warn("Error loading vgg16 model!");
      return;
      
    }
    log.info("Done loading model..");
    
    // start classifier thread
    
    Thread classifier = new Thread(this, "DL4JClassifierThread");
    classifier.start();
    
    
  }
  
  @Override
  public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {
    
    if (lastResult != null) {
      // the thread running will be updating lastResult for it as fast as it can.
      // log.info("Display result " );
      displayResult(image, lastResult);
    }
    // ok now we just need to update the image that the current thread is processing (if the current thread is idle i guess?)
    lastImage = image;
    return image;
  }

  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);  
  }
  
  private void displayResult(IplImage image, Map<String, Double> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    int i = 0;
    int percentOffset = 150;
    for (String label : result.keySet()) {
      i++;
      String val = df2.format(result.get(label)*100) + "%";
      cvPutText(image, label + " : " , cvPoint(20, 60+(i*12)), font, CvScalar.YELLOW);
      cvPutText(image, val, cvPoint(20+percentOffset, 60+(i*12)), font, CvScalar.YELLOW);
    }
  }

  private String formatResultString(Map<String, Double> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    StringBuilder res = new StringBuilder();
    for (String key : result.keySet()) {
      res.append(key + " : ");
      res.append(df2.format(result.get(key)*100) + "% , ");        
    }
    return res.toString();
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  @Override
  public void run() {
    
    log.info("Starting the DL4J classifier thread...");
    // in a loop, grab the current image and classify it and update the result.
    while (true) {
      // log.info("Running!!!");
      // now we need to know which image we should classify
      // there likely needs to be some synchronization on this too.. o/w the main thread will
      // be updating it while it's being classified maybe?!
      if (lastImage != null) {
        try {
          lastResult = dl4j.classifyImageVGG16(lastImage);
          log.info(formatResultString(lastResult));
        } catch (IOException e) {
          // TODO Auto-generated catch block
          log.warn("Exception classifying image!");
          e.printStackTrace();
        }
      } else {
        // log.info("No Image to classify...");
      }
      // TODO: see why there's a race condition. i seem to need a little delay here o/w the recognition never seems to start.
      // maybe lastImage needs to be marked as volitite?
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
