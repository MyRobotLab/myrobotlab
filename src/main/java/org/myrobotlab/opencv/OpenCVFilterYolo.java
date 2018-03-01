package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromDarknet;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.cvFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvPutText;
import static org.bytedeco.javacpp.opencv_imgproc.cvDrawRect;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_dnn.Net;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Solr;
import org.slf4j.Logger;

public class OpenCVFilterYolo extends OpenCVFilter implements Runnable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterYolo.class.getCanonicalName());
  private OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
  
  
  private float confidenceThreshold = 0.0F;
  // the column in the detection matrix that contains the confidence level.  (I think?)
  int probability_index = 5;
  // yolo file locations
  // private String darknetHome = "c:/dev/workspace/darknet/";
  public String darknetHome = "./yolo";
  private String modelConfiguration = darknetHome + "cfg/yolo.cfg";
  private String modelBinary = darknetHome + "yolo.weights";
  private String classNamesFile = darknetHome + "data/coco.names";
  
  private Net net;
  //load the class names
  ArrayList<String> classNames;

  private CvFont font = cvFont(CV_FONT_HERSHEY_PLAIN);

  public ArrayList<YoloDetectedObject> lastResult = null; 
  private volatile IplImage lastImage = null;

  public OpenCVFilterYolo() {
    super();
    loadYolo();
  }

  public OpenCVFilterYolo(String name) {
    super(name);
    loadYolo();
  }

  private void loadYolo() {
    net = readNetFromDarknet(modelConfiguration, modelBinary);
    //load the class names
    try {
      classNames = loadClassNames(classNamesFile);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.warn("Error unable to load class names");
      return;
    }
    
    log.info("Done loading model..");
    // start classifier thread
    Thread classifier = new Thread(this, "YoloClassifierThread");
    classifier.start();
    log.info("Yolo Classifier thread started : {}", this.name);
  }

  
  private ArrayList<String> loadClassNames(String filename) throws IOException {
    ArrayList<String> names = new ArrayList<String>();
    FileReader fileReader = new FileReader(filename);
    BufferedReader bufferedReader = new BufferedReader(fileReader);
    String line;
    int i =0;
    while ((line = bufferedReader.readLine()) != null) {
      names.add(line.trim());
      i++;
    }
    fileReader.close();
    return names;
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

  private void displayResult(IplImage image, ArrayList<YoloDetectedObject> result) {
    DecimalFormat df2 = new DecimalFormat("#.###");
    for (YoloDetectedObject obj : result) {
      String label =  obj.label + " (" + df2.format(obj.confidence*100) + "%)";
      // anchor point for text.
      cvPutText(image, label , cvPoint(obj.boundingBox.x(), obj.boundingBox.y()), font, CvScalar.YELLOW);
      // obj.boundingBox.
      drawRect(image,obj.boundingBox, CvScalar.BLUE);
    }
  }

  
  public void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
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

    log.info("Starting the Yolo classifier thread...");
    // in a loop, grab the current image and classify it and update the result.
    while (true) {
      if (lastImage != null) {
          // lastResult = dl4j.classifyImageVGG16(lastImage);
          yoloFrame(lastImage);
          // invoke("publishClassification", lastResult);
          // log.info(formatResultString(lastResult));

      } else {
        // log.info("No Image to classify...");
      }
      // TODO: see why there's a race condition. i seem to need a little delay here o/w the recognition never seems to start.
      // maybe lastImage needs to be marked as volatile ?
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void yoloFrame(IplImage frame) {
    
    ArrayList<YoloDetectedObject> yoloObjects = new ArrayList<YoloDetectedObject>();
    // System.out.println("Image / Frame Size : " +frame.width() + " x " + frame.height() + " Channels: " + frame.nChannels());
    // convert that frame to a matrix (Mat) using the frame converters in javacv
    Mat inputMat = grabberConverter.convertToMat(grabberConverter.convert(frame));
    //show(inputMat, "input image");
    // System.out.println("Input Mat has " + inputMat.rows() + " rows and " + inputMat.cols() + " columns.");
    // we probably need to do a cut color of the input frame to make sure it's suitable for input to the yolo network
    //  cvtColor(frame, frame, COLOR_BGRA2BGR);
    // convert the frame matrix to a blob... the resulting images should be 416 x 416 in resolution because this is what yolo expects.
    Mat inputBlob = blobFromImage(inputMat, 1 / 255.F, new Size(416, 416), new Scalar(), true, false); //Convert Mat to batch of images
    //System.out.println("Input blob has " + inputBlob.size().area() +  " area.");
    // put our frame/input blob into the model.
    net.setInput(inputBlob, "data");
    // ask for the detection_out layer i guess?  not sure the details of the forward method.
    Mat detectionMat = net.forward("detection_out");
    // System.out.println("Matrix has " + detectionMat.rows() + " rows.");
    // with any luck. our names line up.
    //float maxConfidence = 0.0f;
    //int maxIndex = 0;
     
    for (int i = 0; i < detectionMat.rows(); i++) {
      // int probability_index = 5;
      Mat currentRow = detectionMat.row(i);
      float confidence = currentRow.getFloatBuffer().get(4);
      if (confidence < confidenceThreshold) {
        // skip the noise
        continue;
      }

      // System.out.println("\nCurrent row has " + currentRow.size().width() + "=width " + currentRow.size().height() + "=height.");
      // currentRow.position(probability_index);
      // int probability_size = detectionMat.cols() - probability_index;
      // detectionMat;

      //String className = getWithDefault(classNames, i); 
      // System.out.print("\nROW (" + className + "): " + currentRow.getFloatBuffer().get(4) + " -- \t\t");
      for (int c = probability_index ; c < currentRow.size().get(); c++) {
        float val = currentRow.getFloatBuffer().get(c);
        // TODO: this filtering logic is probably wrong.
        if (val > 0.0) {
          String label = classNames.get(c-probability_index);
          System.out.println("Index : " + c + "->" + val + " label : " + classNames.get(c-probability_index) );
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
          //          //Rect object(xLeftBottom, yLeftBottom, xRightTop - xLeftBottom, yRightTop - yLeftBottom);
          //          //rectangle(frame, object, Scalar(0, 255, 0));
          //          System.out.println("Class: " + objectClass );
          System.out.println(classNames.get(c-probability_index)  + " Confidence: " + confidence + " " + xLeftBottom + " " + yLeftBottom + " " + xRightTop + " " + yRightTop);

          Rect boundingBox = new Rect(xLeftBottom, yLeftBottom, xRightTop - xLeftBottom, yRightTop - yLeftBottom);
          YoloDetectedObject obj = new YoloDetectedObject(boundingBox, confidence, label);
          yoloObjects.add(obj);
          // rectangle(inputMat, boundingBox, Scalar.CYAN);
          // TODO: update the lastResult with the bounding box, confidence level, and label for all objects found.
          // now we have a box !  let's set this as the last image?
          // drawRect(frame, r, CvScalar.BLUE);
          //cvDrawRect(frame, )
          // todo draw the rectangle.
          //show(inputMat, "detected?");


        }
      }
    }
    // TODO: does this need to be synchronized/volitile or anything?
    this.lastResult = yoloObjects;
  }

  public Map<String, Double> publishClassification(Map<String, Double> classification) {	
    return classification;
  }

  public void attach(Solr solr) {

    // 

  }

}
