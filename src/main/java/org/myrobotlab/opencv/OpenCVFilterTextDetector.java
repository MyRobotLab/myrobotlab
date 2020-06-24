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

public class OpenCVFilterTextDetector extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  ArrayList<DetectedText> classifications = new ArrayList<DetectedText>();
  public TesseractOcr tesseract = null;
  int newWidth = 320;
  int newHeight = 320;
  // a little extra padding on the x axis
  int xPadding = 10;
  // some on the y axis.
  int yPadding = 10;
  // first we need our EAST detection model. 
  String modelFile = "resource/OpenCV/east_text_detector/frozen_east_text_detection.pb";
  float confThreshold = 0.5f;
  Net detector = null;
  String detectedText = null;

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
    // TODO Auto-generated method stub
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    detectedText = detectText(image);
    // return the original image un-altered.
    return image;
  } 


  private String detectText(IplImage image) {
    // 

    StringBuilder detectedText = new StringBuilder();
    Mat originalImageMat = OpenCV.toMat(image);
    // Resize the image to mat
    IplImage ret = IplImage.create(newWidth, newHeight, image.depth(), image.nChannels());
    cvResize(image, ret, Imgproc.INTER_AREA);
    Mat frame = OpenCV.toMat(ret);
    // Create the blob to put into the EAST text detector
    Mat blob = blobFromImage(frame,  1.0, new Size(newWidth, newHeight), new Scalar(123.68, 116.78, 103.94, 0.0), true, false, CV_32F);
    detector.setInput(blob);
    // enumerate the layers to return from the network forward call
    String[] outNamesA = new String[] {"feature_fusion/Conv_7/Sigmoid","feature_fusion/concat_3"};
    StringVector outNames = new StringVector(outNamesA);
    MatVector outs = new MatVector();   
    detector.forward(outs, outNames);
    // first layer is the scores/confidence values
    Mat scores = outs.get(0);
    // The second layer is the actual geometry of the region found.
    Mat geometry = outs.get(1);
    // Decode predicted bounding boxes.
    ArrayList<DetectedText> results = decodeBoundingBoxes(frame, scores, geometry, confThreshold);
    // here we can/should draw the results on the image i guess?
    Point2f ratio = new Point2f( (float)image.width() / newWidth ,  (float)image.height() / newHeight );
    // log.error("Image Size {} {} ", image.width(), image.height());
    // TODO: fill in the stuffs.
    
    for (DetectedText rr : results) {
      // Render the rect on the image..
      Rect bR = rr.box.boundingRect();
      // scaled back down.
      int x = (int) Math.max(0, bR.x()*ratio.x()-xPadding/2);
      int y = (int) Math.max(0, bR.y()*ratio.y()-yPadding/2);
      int w = (int) Math.max(0, bR.width()*ratio.x()+xPadding);
      int h = (int) Math.max(0, bR.height()*ratio.y()+yPadding);
      //log.error("Draw Rect : {} {} {} {}", x,y,w,h);
      // This should be correct
      Rect rect = new Rect(x,y,w,h);
      Mat cropped = cropAndRotate(originalImageMat, rr.box, rect, ratio);
      // can I just ocr that cropped mat now?
      String croppedResult = ocrMat(cropped);
      rr.text = croppedResult;
      if (croppedResult != null) {
        croppedResult = croppedResult.trim();
        if (croppedResult.length() > 0) {
          detectedText.append(croppedResult);
          detectedText.append(" ");
        }
      }
      
    }
    String trimmed = detectedText.toString().trim();
    if (trimmed.length() > 0) {
      System.err.println("Detected Text : " + detectedText.toString());
      // stuff this in the opencvdata.
      data.setDetectedText(trimmed);
    }
    
    return trimmed;
  }

  private Mat cropAndRotate(Mat frame, RotatedRect box, Rect rect, Point2f ratio) {
    // TODO Auto-generated method stub
    Point2f vertices = new Point2f(4);
    box.points(vertices);
    for (int i = 0 ; i < 4; i++) {
      vertices.position(i);
      vertices.x( vertices.x() * ratio.x() );
      vertices.y( vertices.y() * ratio.y() );
    }
    vertices.position(0);
    // In theory we have an array with the 4 scaled points representing the text area
    // TODO: better target size?
    Size outputSize = new Size(rect.width(),rect.height());
    // System.out.println("Output Size : " + outputSize);
    Mat cropped = new Mat();
    fourPointsTransform(frame, vertices, cropped, outputSize);
    // show(cropped, " cropped?");
    return cropped;
  }

  private String ocrMat(Mat input) {
    // log.error("INPUT MAT SIZE FOR OCR: {} x {}", input.cols(), input.rows());
    String result = null;
    BufferedImage candidate = OpenCV.toBufferedImage(OpenCV.toFrame(input));
    try {
      if (tesseract == null) {
        tesseract = (TesseractOcr)Runtime.start("tesseract", "TesseractOcr");
      }
      result = tesseract.ocr(candidate).trim();
      //System.err.println("Result from OCR WAS : " + result);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
    
  }
  
  private String ocrRegion(Mat originalImageMat, Rect box) {
    String result = null;
    IplImage subImage = extractSubImage(originalImageMat, box);
    BufferedImage candidate = OpenCV.toBufferedImage(subImage);
    // show(subImage , "detected region");
    try {
      if (tesseract == null) {
        tesseract = (TesseractOcr)Runtime.start("tesseract", "TesseractOcr");
      }
      result = tesseract.ocr(candidate).trim();
      //System.err.println("Result from OCR WAS : " + result);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return result;
  }

  private IplImage extractSubImage(Mat inputMat, Rect boundingBox) {
    // TODO: figure out if the width/height is too large!
    // bounds check on the input x / y..
    // if (boundingBox.x() > inputMat.cols()) {
    //   // out of bounds..       
    // }
    // truncate max width
    int remainingCols = inputMat.cols() - boundingBox.x();
    boundingBox.width( Math.min(remainingCols, boundingBox.width()));
    // truncate max height
    int remainingRows = inputMat.rows() - boundingBox.y();
    boundingBox.height( Math.min(remainingRows, boundingBox.height()));
    Mat cropped = new Mat(inputMat, boundingBox);
    IplImage image = OpenCV.toImage(cropped); 
    // This mat should be the cropped image!
    return image;
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

  private void otherStuff() {
    //    for (RotatedRect box : results) {
    //      Point2f vertices = new Point2f(4);
    //      box.points(vertices);
    //      // TODO: add a buffer around the actual positions?
    //      int minX = Integer.MAX_VALUE;
    //      int maxX = Integer.MIN_VALUE;
    //      int minY = Integer.MAX_VALUE;
    //      int maxY = Integer.MIN_VALUE;
    //      for (int j = 0; j < 4; ++j) {
    //        // walk the array?
    //        vertices.position(j);
    //        vertices.x( vertices.x() * ratio.x() );
    //        vertices.y( vertices.y() * ratio.y() );
    //        // log.info("Scaled: {} {} {} {}", vertices.x(), vertices.y());
    //        if (vertices.x() < minX) {
    //          minX = (int) vertices.x();
    //        }
    //
    //        if (vertices.x() > maxX) {
    //          maxX = (int) vertices.x();
    //        }
    //
    //        if (vertices.y() < minY) {
    //          minY = (int) vertices.y();
    //        }
    //        if (vertices.y() > maxY) {
    //          maxY = (int) vertices.y();
    //        }
    //
    //      }
    //      vertices.position(0);
    //      // In theory we have an array with the 4 scaled points representing the text area
    //
    //      Mat cropped = new Mat();
    //      // This should be the orig image...  not the frame.
    //      //fourPointsTransform(originalImageMat, vertices, cropped);
    //      // TODO: why is this "frame" isn't that the resized one.. i thought we would want the original image here..
    //
    //      // Something like the size of the original.
    //      int deltaX = maxX - minX;
    //      int deltaY = maxY - minY;
    //      //let's go for the max distance in x.. and max distance in y.  
    //      //Size outputSize = new Size(deltaX*20, deltaY*20);
    //      Size outputSize = new Size(100,32);
    //
    //      fourPointsTransform(frame, vertices, cropped, outputSize);
    //      // show(cropped , "detected region");
    //
    //      // TODO: increase contrast ?
    //
    //      // Ok... let's go from this cropped image to tesseract
    //      // maybe we can crop from the original image here// would be best
    //      Rect boundingB = box.boundingRect();
    //      boundingB.x((int)(boundingB.x() / ratio.x()));
    //      boundingB.y((int)(boundingB.y() / ratio.y()));
    //      boundingB.width((int)(boundingB.width() / ratio.y()));
    //      boundingB.height((int)(boundingB.height() / ratio.y()));
    //
    //      //  ocrRegion(originalImageMat, box);
    //
    //
    //      // TODO: Ok.. this cropped image is what we want to ocr... 
    //
    //      // TODO: use the scaled up vertices for the box instead.
    //      drawRect(frame,box);
    //    }
    //    ratio.close();
    //    IplImage newImg = OpenCV.toImage(frame);
    //    // show(newImg, "Regions");
    //    return newImg;

  }

  public CanvasFrame show(final IplImage image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(toFrame(image));
    return canvas;
  }

  // TODO: return a different type, to include the confidence
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
        RotatedRect rec = new RotatedRect(center, size, (float) (-1*angle * 180.0 / Math.PI));
        boxes.add(rec);
        confidences.add(score);
      }
    }
    ArrayList<DetectedText> maxRects = applyNMSBoxes(threshold, boxes, confidences);
    // This is the filtered list of rects that matched our threshold.
    classifications = orderRects(maxRects, frame.cols());
    return maxRects;
  }

  private ArrayList<DetectedText> orderRects(ArrayList<DetectedText> maxRects, int width) {
    // TODO Auto-generated method stub
    // we want to scan the rects top to bottom.. left to right.
    // TODO: we need to know the original image width
    
    Comparator<DetectedText> rectComparator = new Comparator<DetectedText>() {         
      @Override         
      public int compare(DetectedText rect1, DetectedText rect2) {
        // left to right.. top to bottom. 
        // TODO: this 100 is a vertical sort of resolution ... it should be more dynamic
        // and it should probably be configured somehow..
        int index1 = rect1.box.boundingRect().x() + (rect1.box.boundingRect().y()*width/100); 
        int index2 = rect2.box.boundingRect().x() + (rect2.box.boundingRect().y()*width/100);
        return (index2 > index1 ? -1 : (index2 == index1 ? 0 : 1));           
      }     
    };  
    
    maxRects.sort(rectComparator);
    return maxRects;
  }

  private static ArrayList<DetectedText> applyNMSBoxes(float threshold, ArrayList<RotatedRect> boxes, ArrayList<Float> confidences) {
    float nmsThreshold = (float) 0.3;
    RectVector boxesRV = new RectVector();
    for (RotatedRect rr : boxes) {
      boxesRV.push_back(rr.boundingRect());
    }
    FloatPointer confidencesFV = arrayListToFloatPointer(confidences);
    // IntPointer indicesIp = new IntPointer(confidences.size());
    IntPointer indicesIp = new IntPointer();
    NMSBoxes(boxesRV, confidencesFV, (float)threshold, nmsThreshold, indicesIp);
    // Ok.. so.. now what do we do with these ?!  persumably, the boxes for specific indicesIp values are good?
    ArrayList<DetectedText> goodOnes = new ArrayList<DetectedText>();
    for (int m=0;m<indicesIp.limit();m++) {
      int i = indicesIp.get(m);
      RotatedRect box = boxes.get(i); 
      confidencesFV.position(i);
      // we don't have text yet
      DetectedText dt = new DetectedText(box, confidencesFV.get(), null);
      goodOnes.add(dt);
    }
    return goodOnes;
  }

  private static FloatPointer arrayListToFloatPointer(ArrayList<Float> confidences) {
    FloatPointer confidencesFV = new FloatPointer(confidences.size());
    // fill an array..
    float[] confidenceA = new float[confidences.size()];
    for (int i = 0; i < confidenceA.length; i++) {
      confidenceA[i] = confidences.get(i);
    }
    confidencesFV.put(confidenceA);
    return confidencesFV;
  }


  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    //    //
    //    int width = image.getWidth();
    //    int height = image.getHeight();
    //    
    // we need to scale the boxes 
    Point2f ratio = new Point2f( (float)image.getWidth() / newWidth ,  (float)image.getHeight() / newHeight );
    // log.error("Image Size {} {} ", image.getWidth(), image.getHeight());
    // TODO: fill in the stuffs.
    for (DetectedText rr : classifications) {
      // Render the rect on the image..
      Rect bR = rr.box.boundingRect();
      // scaled back down.
      int x = (int) (bR.x()*ratio.x());
      int y = (int) (bR.y()*ratio.y());
      int w = (int) (bR.width()*ratio.x());
      int h = (int) (bR.height()*ratio.y());
      // log.error("Draw Rect : {} {} {} {}", x,y,w,h);
      graphics.setColor(Color.GREEN);
      graphics.drawRect(x,y,w,h);
      
      graphics.setColor(Color.BLUE);
      graphics.drawString(rr.text, x, y+10);
    }
    ratio.close();
    
    // 
    if (detectedText != null && detectedText.length() > 0) {
      graphics.setColor(Color.CYAN);
      graphics.drawString(detectedText, 20, 40);
    }
    
    return image;
  }

  private static void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  private static void drawRect(Mat frame, RotatedRect rec) {
    IplImage image = OpenCV.toImage(frame);
    drawRect(image, rec.boundingRect(), CvScalar.YELLOW);
  }

}
