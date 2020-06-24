package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.cvPoint;
import static org.bytedeco.opencv.global.opencv_dnn.NMSBoxes;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNet;
import static org.bytedeco.opencv.global.opencv_imgproc.cvDrawRect;
import static org.bytedeco.opencv.global.opencv_imgproc.cvResize;
import static org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform;
import static org.bytedeco.opencv.global.opencv_imgproc.warpPerspective;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.RotatedRect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.Size2f;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TesseractOcr;
import org.opencv.imgproc.Imgproc;

/**
 * This opencv filter will first use the EAST text detector to identify rotated rects that represent
 * areas that contain text within the image.  Then, that region is cropped, rotated, and slighty warped 
 * The resulting image is then passed to tesseractOcr to perform OCR on that region.  
 * The resulting data returned is the DetectedText object. 
 * This is based on work from 
 * 
 * https://www.pyimagesearch.com/2018/08/20/opencv-text-detection-east-text-detector/
 * And the original OpenCV example here:
 * https://github.com/opencv/opencv/blob/master/samples/dnn/text_detection.cpp
 * 
 * @author kwatters
 *
 */
public class OpenCVFilterTextDetector extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  ArrayList<DetectedText> classifications = new ArrayList<DetectedText>();
  private transient TesseractOcr tesseract = null;
  int fontSize = 40;
  int newWidth = 320;
  int newHeight = 320;
  // a little extra padding on the x axis
  int xPadding = 10;
  // some on the y axis.
  int yPadding = 10;
  // first we need our EAST detection model. 
  String modelFile = "resource/OpenCV/east_text_detector/frozen_east_text_detection.pb";
  float confThreshold = 0.5f;
  // non-maximum suppression threshold
  float nmsThreshold = (float) 0.3;
  Net detector = null;
  
  public OpenCVFilterTextDetector() {
    super();
    initModel();
  }

  public OpenCVFilterTextDetector(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    initModel();
  }

  public OpenCVFilterTextDetector(String name) {
    super(name);
    initModel();
  }

  private void initModel() {
    detector = readNet(modelFile);
  }

  @Override
  public void imageChanged(IplImage image) {
    // NoOp 
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    classifications = detectText(image);
    data.setDetectedText(classifications);
    // return the original image un-altered.
    return image;
  } 

  private ArrayList<DetectedText> detectText(IplImage image) {
    StringBuilder detectedTextLine = new StringBuilder();
    Mat originalImageMat = OpenCV.toMat(image);
    // This is a ratio between the size of the input image vs the input for the east detector.
    Point2f ratio = new Point2f( (float)image.width() / newWidth ,  (float)image.height() / newHeight );
    // Resize the image to mat
    IplImage ret = IplImage.create(newWidth, newHeight, image.depth(), image.nChannels());
    cvResize(image, ret, Imgproc.INTER_AREA);
    Mat frame = OpenCV.toMat(ret);
    // Create the blob to put into the EAST text detector
    Mat blob = blobFromImage(frame,  1.0, new Size(newWidth, newHeight), new Scalar(123.68, 116.78, 103.94, 0.0), true, false, CV_32F);
    detector.setInput(blob);
    // these two layers contain the scores and the rotated rects
    StringVector outNames = new StringVector("feature_fusion/Conv_7/Sigmoid","feature_fusion/concat_3");
    MatVector outs = new MatVector();   
    detector.forward(outs, outNames);
    // first layer is the scores/confidence values
    Mat scores = outs.get(0);
    // The second layer is the actual geometry of the region found.
    Mat geometry = outs.get(1);
    // Decode predicted bounding boxes.
    ArrayList<DetectedText> results = decodeBoundingBoxes(frame, scores, geometry, confThreshold);
    for (DetectedText dt : results) {
      // Render the rect on the image..
      Rect bR = dt.box.boundingRect();
      // scaled back down.
      int x = (int) Math.max(0, bR.x()*ratio.x()-xPadding/2);
      int y = (int) Math.max(0, bR.y()*ratio.y()-yPadding/2);
      int w = (int) Math.max(0, bR.width()*ratio.x()+xPadding);
      int h = (int) Math.max(0, bR.height()*ratio.y()+yPadding);
      // this rect should have a border around it.
      // TODO: this represents the target size.  This should probably not include the padding.
      // Crop the image and rotate it to the same size as rect ?  pass in the ratio here.
      // TODO: this is probably the wrong size.. 
      Size outputSize = new Size(w,h);
      Mat cropped = cropAndRotate(originalImageMat, dt.box, outputSize, ratio);
      // can I just ocr that cropped mat now?
      String croppedResult = ocrMat(cropped);
      if (croppedResult != null) {
        croppedResult = croppedResult.trim();
        // update the text on the detected text object.
        dt.text = croppedResult;
        if (croppedResult.length() > 0) {
          detectedTextLine.append(croppedResult);
          detectedTextLine.append(" ");
        }
      }
    }
    String trimmed = detectedTextLine.toString().trim();
    if (trimmed.length() > 0) {
      System.err.println("Detected Text : " + detectedTextLine.toString());
      // stuff this in the opencvdata.
    }
    return results;
  }

  private Mat cropAndRotate(Mat frame, RotatedRect box, Size outputSize, Point2f ratio) {
    // Input rotatedRect is on the neural network scaled image
    // this needs to be scaled up to the original resolution by the ratio.
    Point2f vertices = new Point2f(4);
    box.points(vertices);
    for (int i = 0 ; i < 4; i++) {
      vertices.position(i);
      vertices.x( vertices.x() * ratio.x() );
      vertices.y( vertices.y() * ratio.y() );
    }
    vertices.position(0);
    Mat cropped = new Mat();
    fourPointsTransform(frame, vertices, cropped, outputSize);
    // show(cropped, " cropped?");
    return cropped;
  }

  private String ocrMat(Mat input) {
    String result = null;
    BufferedImage candidate = OpenCV.toBufferedImage(OpenCV.toFrame(input));
    try {
      if (tesseract == null) {
        tesseract = (TesseractOcr)Runtime.start("tesseract", "TesseractOcr");
      }
      result = tesseract.ocr(candidate).trim();
    } catch (IOException e) {
      log.warn("Tesseract failure.", e);
    }
    return result;
  }

  private void fourPointsTransform(Mat frame, Point2f vertices, Mat result, Size outputSize) {
    // TODO: it'd be nice to actually size this according to the original size of the rotated rect
    // not grok'in whats 100x32 for?
    // TODO: a better resoulution is desired.
    Point2f targetVertices = new Point2f(4);
    // write the data into the array
    targetVertices.position(0);
    targetVertices.put(new Point2f(0, outputSize.height() - 1));
    targetVertices.position(1);
    targetVertices.put(new Point2f(0, 0));
    targetVertices.position(2);
    targetVertices.put(new Point2f(outputSize.width() - 1, 0));
    targetVertices.position(3);
    targetVertices.put(new Point2f(outputSize.width() - 1, outputSize.height() - 1));
    // reset the pointer to the beginning of the array.
    targetVertices.position(0);
    Mat rotationMatrix = getPerspectiveTransform(vertices, targetVertices);
    warpPerspective(frame, result, rotationMatrix, outputSize);
    // Ok.. now the result should have the cropped image?
    // show(OpenCV.toImage(result), "four points...");
  }

  public CanvasFrame show(final IplImage image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(toFrame(image));
    return canvas;
  }

  private ArrayList<DetectedText> decodeBoundingBoxes(Mat frame, Mat scores, Mat geometry, float threshold) {
    int height = scores.size(2);
    int width = scores.size(3);
    // For fast lookup into the Mats
    FloatIndexer scoresIndexer = scores.createIndexer(); 
    FloatIndexer geometryIndexer = geometry.createIndexer();
    ArrayList<RotatedRect> boxes = new ArrayList<RotatedRect>();
    ArrayList<Float> confidences = new ArrayList<Float>();
    for (int y = 0; y < height; y++) {
      for (int x = 0 ; x < width; x++) {
        // Get the score of this classification.
        float score = scoresIndexer.get(0,0,y,x);
        if (score < threshold) {
          continue;
        }
        // two points and an angle to determin the rotated rect i guess.
        float x0_data = geometryIndexer.get(0,0,y,x);
        float x1_data = geometryIndexer.get(0,1,y,x);
        float x2_data = geometryIndexer.get(0,2,y,x);
        float x3_data = geometryIndexer.get(0,3,y,x);
        float angle = geometryIndexer.get(0,4,y,x);
        // Calculate offset
        double offsetX = x * 4.0;
        double offsetY = y * 4.0;
        // Calculate cos and sin of angle
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        // The classification height and width?
        double h = x0_data + x2_data;
        double w = x1_data + x3_data;
        // Calculate offset
        double offset0 = offsetX + cosA * x1_data + sinA * x2_data;
        double offset1 = offsetY - sinA * x1_data + cosA * x2_data;
        // Find points for rectangle
        double p1_0 = -sinA * h + offset0;
        double p1_1 = -cosA * h + offset1;
        double p3_0 = -cosA * w + offset0;
        double p3_1 = sinA * w + offset1;
        // Center point of the rect
        float  centerX = (float) (0.5*(p1_0+p3_0));
        float  centerY =  (float) (0.5*(p1_1+p3_1));
        Point2f center = new Point2f(centerX, centerY);
        // The dimensions of the rect
        Size2f size = new Size2f((float)w,(float)h);
        // Create the rotated rect. 
        // TODO: It would be nice if this was scaled back up to the 
        // original resolution here.  (perhaps, that means scaling x0/1/2/3_data.. 
        // and what does that do to the angle if we scale with a non square aspect ratio? ... icky.
        RotatedRect rec = new RotatedRect(center, size, (float) (-1*angle * 180.0 / Math.PI));
        boxes.add(rec);
        confidences.add(score);
      }
    }
    // Apply non-maximum suppression to filter down boxes that mostly overlap
    ArrayList<DetectedText> maxRects = applyNMSBoxes(threshold, boxes, confidences, nmsThreshold);
    // This is the filtered list of rects that matched our threshold.
    classifications = orderRects(maxRects, frame.cols());
    return maxRects;
  }

  private ArrayList<DetectedText> orderRects(ArrayList<DetectedText> maxRects, int width) {
    Comparator<DetectedText> rectComparator = new Comparator<DetectedText>() {         
      @Override         
      public int compare(DetectedText rect1, DetectedText rect2) {
        // left to right.. top to bottom. 
        // TODO: this 100 is a vertical sort of resolution ... it should be more dynamic
        // and it should probably be configured somehow..  in reality this algorithm needs to be replaced/fixed
        int index1 = rect1.box.boundingRect().x() + (rect1.box.boundingRect().y()*width/100); 
        int index2 = rect2.box.boundingRect().x() + (rect2.box.boundingRect().y()*width/100);
        return (index2 > index1 ? -1 : (index2 == index1 ? 0 : 1));           
      }     
    };    
    maxRects.sort(rectComparator);
    return maxRects;
  }

  private static ArrayList<DetectedText> applyNMSBoxes(float threshold, ArrayList<RotatedRect> boxes, ArrayList<Float> confidences, float nmsThreshold) {    
    RectVector boxesRV = new RectVector();
    for (RotatedRect rr : boxes) {
      boxesRV.push_back(rr.boundingRect());
    }
    FloatPointer confidencesFV = arrayListToFloatPointer(confidences);
    IntPointer indicesIp = new IntPointer();
    NMSBoxes(boxesRV, confidencesFV, (float)threshold, nmsThreshold, indicesIp);
    ArrayList<DetectedText> goodOnes = new ArrayList<DetectedText>();
    for (int m=0;m<indicesIp.limit();m++) {
      int i = indicesIp.get(m);
      RotatedRect box = boxes.get(i); 
      confidencesFV.position(i);
      // we don't have text yet, that will be filled in later by the ocr step.
      DetectedText dt = new DetectedText(box, confidencesFV.get(), null);
      goodOnes.add(dt);
    }
    return goodOnes;
  }

  // utilty helper function to put an array of floats into a javacpp float pointer
  private static FloatPointer arrayListToFloatPointer(ArrayList<Float> confidences) {
    // create a float pointer of the correct size
    FloatPointer confidencesFV = new FloatPointer(confidences.size());
    for (int i = 0; i < confidences.size(); i++) {
      // update the pointer and put the float in 
      confidencesFV.position(i);
      confidencesFV.put(confidences.get(i));
    }
    // reset the pointer position back to the head.
    confidencesFV.position(0);
    return confidencesFV;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    StringBuilder fullText = new StringBuilder();
    // we need to scale the boxes
    Font previousFont = graphics.getFont();
    // increase the size of this font.
    graphics.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
    Point2f ratio = new Point2f( (float)image.getWidth() / newWidth ,  (float)image.getHeight() / newHeight );
    for (DetectedText rr : classifications) {
      // Render the rect on the image..
      // TODO: draw the rotated rect instead of a bounding box. graphics.drawLine(x1, y1, x2, y2);
      Rect bR = rr.box.boundingRect();
      // Scaled to the original size of the image.
      int x = (int) (bR.x()*ratio.x());
      int y = (int) (bR.y()*ratio.y());
      int w = (int) (bR.width()*ratio.x());
      int h = (int) (bR.height()*ratio.y());
      graphics.setColor(Color.GREEN);
      graphics.drawRect(x,y,w,h);
      graphics.setColor(Color.BLUE);
      // we should center the text in the middle of the box.
      int yText = y + h/2;
      graphics.drawString(rr.text, x, yText);
      fullText.append(rr.text).append(" ");
    }
    graphics.drawString(fullText.toString().trim(), 20, 80);
    ratio.close();
    //restore the font .. just in case?
    graphics.setFont(previousFont);
    return image;
  }

}
