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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.opencv.opencv_core.CvScalar;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Point2fVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.RotatedRect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.Size2f;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.myrobotlab.document.Classification;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.service.OpenCV;
import org.opencv.imgproc.Imgproc;

public class OpenCVFilterTextDetector extends OpenCVFilter {

  ArrayList<RotatedRect> classifications = new ArrayList<RotatedRect>();

  int newWidth = 320;
  int newHeight = 320;

  public OpenCVFilterTextDetector() {
    super();
    // TODO Auto-generated constructor stub
    initModel();
  }

  public OpenCVFilterTextDetector(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    // TODO Auto-generated constructor stub
    initModel();
  }

  public OpenCVFilterTextDetector(String name) {
    super(name);
    // TODO Auto-generated constructor stub
    initModel();
  }


  private static final long serialVersionUID = 1L;

  // first we need our EAST detection model. 
  String modelFile = "resource/OpenCV/east_text_detector/frozen_east_text_detection.pb";
  float confThreshold = 0.5f;
  Net detector = null;

  private void initModel() {
    detector = readNet(modelFile);
  }

  private IplImage detectText(IplImage image) {
    // 

    Mat originalImageMat = OpenCV.toMat(image);

    IplImage ret = IplImage.create(newWidth, newHeight, image.depth(), image.nChannels());
    cvResize(image, ret, Imgproc.INTER_AREA);
    Mat frame = OpenCV.toMat(ret);
    int inpWidth = ret.width();
    int inpHeight = ret.height();
    // Create the blob to put into the EAST text detector
    Mat blob = blobFromImage(frame,  1.0, new Size(newWidth, newHeight), new Scalar(123.68, 116.78, 103.94, 0.0), true, false, CV_32F);
    detector.setInput(blob);
    String[] outNamesA = new String[] {"feature_fusion/Conv_7/Sigmoid","feature_fusion/concat_3"};
    StringVector outNames = new StringVector(outNamesA);
    // This will store the output of the network predictions
    MatVector outs = new MatVector();   
    detector.forward(outs, outNames);
    // first layer is the scores/confidence values
    Mat scores = outs.get(0);
    // The second layer is the actual geometry of the region found.
    Mat geometry = outs.get(1);
    // Decode predicted bounding boxes.
    // std::vector<RotatedRect> boxes;
    // std::vector<float> confidences;
    ArrayList<RotatedRect> results = decodeBoundingBoxes(frame, scores, geometry, confThreshold);
    // here we can/should draw the results on the image i guess?
    // TODO: map these rects back to the orginal image coordinates!



    Point2f ratio = new Point2f((float)frame.cols() / inpWidth, (float)frame.rows() / inpHeight);
    for (RotatedRect box : results) {
      Point2f vertices = new Point2f(4);
      box.points(vertices);
      for (int j = 0; j < 4; ++j) {
        // walk the array?
        vertices.position(j);
        vertices.x( vertices.x() * ratio.x());
        vertices.y( vertices.y() * ratio.y());
        log.info("Scaled: {} {} {} {}", vertices.x(), vertices.y());
      }
      vertices.position(0);
      // In theory we have an array with the 4 scaled points representing the text area

      Mat cropped = new Mat();
      // This should be the orig image...  not the frame.
      fourPointsTransform(originalImageMat, vertices, cropped);
      //fourPointsTransform(frame, vertices, cropped);



      // TODO: use the scaled up vertices for the box instead.
      drawRect(frame,box);
    }
    ratio.close();
    IplImage newImg = OpenCV.toImage(frame);
    // show(newImg, "Regions");
    return newImg;

  }

  private void fourPointsTransform(Mat frame, Point2f vertices, Mat result) {
    // TODO Auto-generated method stub
    
    // TODO: it'd be nice to actually size this according to the original size of the rotated rect
    // not grok'in whats 100x32 for?
    
    Size outputSize = new Size(100, 32);
    Point2f targetVertices = new Point2f(4);

    targetVertices.position(0);
    targetVertices.put(new Point2f(0, outputSize.height() - 1));
    targetVertices.position(1);
    targetVertices.put(new Point2f(0, 0));
    targetVertices.position(2);
    targetVertices.put(new Point2f(outputSize.width() - 1, 0));
    targetVertices.position(3);
    targetVertices.put(new Point2f(outputSize.width() - 1, outputSize.height() - 1));
    targetVertices.position(0);
    
    Mat rotationMatrix = getPerspectiveTransform(vertices, targetVertices);
    warpPerspective(frame, result, rotationMatrix, outputSize);
    // Ok.. now the result should have the cropped image?
    
   //  show(OpenCV.toImage(result), "four points...");

  }


  public CanvasFrame show(final IplImage image, final String title) {
    CanvasFrame canvas = new CanvasFrame(title);
    // canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(toFrame(image));
    return canvas;
  }

  
  private ArrayList<RotatedRect> decodeBoundingBoxes(Mat frame, Mat scores, Mat geometry, float threshold) {

    ArrayList<Classification> results = new ArrayList<Classification>();
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
          // System.out.println("skip " + x );
          continue;
        }
        System.out.println(x + " " + score);
        // 0,1,2,3 and angle
        float x0_data = geometryIndexer.get(0,0,y,x);
        float x1_data = geometryIndexer.get(0,1,y,x);
        float x2_data = geometryIndexer.get(0,2,y,x);
        float x3_data = geometryIndexer.get(0,3,y,x);
        float angle = geometryIndexer.get(0,4,y,x);
        System.out.println("Here we are..." + score + " " + x0_data + " " + x1_data + " " + x2_data + " " + x3_data + " " + angle);
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
        System.out.println("Center " + centerX + "," + centerY);
        System.out.println("RoatedRec: " + rec);
        // drawRect(frame, rec);
      }

    }
    // ;
    //decodeBoundingBoxes(scores, geometry, threshold, boxes, confidences);
    // Apply non-maximum suppression procedure.
    // std::vector<int> indices;
    ArrayList<RotatedRect> maxRects = applyNMSBoxes(threshold, boxes, confidences);
    // System.out.println("Here we are.. what's in our indicesIp?!?");
    classifications = maxRects;
    return maxRects;
  }

  private static ArrayList<RotatedRect> applyNMSBoxes(float threshold, ArrayList<RotatedRect> boxes, ArrayList<Float> confidences) {
    float nmsThreshold = (float) 0.3;
    ArrayList<Integer> indices = new ArrayList<Integer>();
    RectVector boxesRV = new RectVector();
    for (RotatedRect rr : boxes) {
      boxesRV.push_back(rr.boundingRect());
    }
    FloatPointer confidencesFV = arrayListToFloatPointer(confidences);
    // IntPointer indicesIp = new IntPointer(confidences.size());
    IntPointer indicesIp = new IntPointer();
    NMSBoxes(boxesRV, confidencesFV, (float)threshold, nmsThreshold, indicesIp);
    // System.out.println("NMS BOXES!!!!");
    // Ok.. so.. now what do we do with these ?!  persumably, the boxes for specific indicesIp values are good?
    ArrayList<RotatedRect> goodOnes = new ArrayList<RotatedRect>();
    for (int m=0;m<indicesIp.limit();m++) {
      int i = indicesIp.get(m);
      System.out.println(i + "---" + m);
      RotatedRect box = boxes.get(i);    
      System.out.println(box);
      goodOnes.add(box);
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

  public static void drawRect(IplImage image, Rect rect, CvScalar color) {
    cvDrawRect(image, cvPoint(rect.x(), rect.y()), cvPoint(rect.x() + rect.width(), rect.y() + rect.height()), color, 1, 1, 0);
  }

  private static void drawRect(Mat frame, RotatedRect rec) {
    IplImage image = OpenCV.toImage(frame);
    drawRect(image, rec.boundingRect(), CvScalar.YELLOW);
    // show(image, "Our image..");
    // System.out.println("Here...");
  }


  @Override
  public void imageChanged(IplImage image) {
    // TODO Auto-generated method stub
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    // 
    // TODO: do the rendering in process display.
    IplImage newImg = detectText(image);
    return newImg;
  }


  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
//    //
//    int width = image.getWidth();
//    int height = image.getHeight();
//    
    // we need to scale the boxes 
    Point2f ratio = new Point2f( (float)image.getWidth() / newWidth ,  (float)image.getHeight() / newHeight );
    log.error("Image Size {} {} ", image.getWidth(), image.getHeight());
    // TODO: fill in the stuffs.
    for (RotatedRect rr : classifications) {
      // Render the rect on the image..
      Rect bR = rr.boundingRect();
      // scaled back down.
      int x = (int) (bR.x()*ratio.x());
      int y = (int) (bR.y()*ratio.y());
      int w = (int) (bR.width()*ratio.x());
      int h = (int) (bR.height()*ratio.y());
      log.error("Draw Rect : {} {} {} {}", x,y,w,h);
      graphics.setColor(Color.BLACK);

      graphics.drawRect(x,y,w,h);
    }
    ratio.close();
    return image;
  }

}
