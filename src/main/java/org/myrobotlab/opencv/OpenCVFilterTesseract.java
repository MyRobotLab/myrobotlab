package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.TesseractOcr;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class OpenCVFilterTesseract extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterTesseract.class);

  private transient TesseractOcr tesseract;
  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);
  
  public String lastResult = null; 
  private IplImage lastImage = null;
  
  public OpenCVFilterTesseract() {
    super();
    loadTesseract();
  }
  
  public OpenCVFilterTesseract(String name) {
    super(name);
    loadTesseract();
  }
  
  private void loadTesseract() {
    tesseract = (TesseractOcr)Runtime.createAndStart("tesseract", "TesseractOcr");
    log.info("Started tesseract...");
    // start classifier thread
    Thread classifier = new Thread(this, "TesseractClassifierThread");
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
  
  private void displayResult(IplImage image, String result) {
    cvPutText(image, result, cvPoint(20, 60), font, CvScalar.YELLOW);
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
    
    log.info("Starting the Tesseract classifier thread...");
    // in a loop, grab the current image and classify it and update the result.
    while (true) {
      // log.info("Running!!!");
      // now we need to know which image we should classify
      // there likely needs to be some synchronization on this too.. o/w the main thread will
      // be updating it while it's being classified maybe?!
      if (lastImage != null) {
        BufferedImage buffImg = OpenCV.IplImageToBufferedImage(lastImage);
        lastResult = tesseract.ocr(buffImg);
        log.info(lastResult);
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
