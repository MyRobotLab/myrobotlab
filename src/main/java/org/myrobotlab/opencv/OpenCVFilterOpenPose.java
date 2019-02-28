/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;

import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.line;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.bytedeco.javacpp.opencv_dnn.blobFromImage;
import static org.bytedeco.javacpp.opencv_dnn.readNetFromCaffe;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_dnn.Net;


import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;

import javax.swing.WindowConstants;
import org.slf4j.Logger;

/**
 * Open Pose Estimation from OpenCV
 * 
 * Largely derived from  https://docs.opencv.org/3.4.3/d7/d4f/samples_2dnn_2openpose_8cpp-example.html
 * @author kwatters
 *
 */

public class OpenCVFilterOpenPose extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterOpenPose.class.getCanonicalName());
  // int x0, y0, x1, y1;
  private Net net;

//  public String model = "openpose/pose_iter_584000.caffemodel";
//  public String protoTxt = "openpose/pose_deploy.prototxt";

//  public String model = "openpose/pose/body_25/pose_iter_584000.caffemodel";
//  public String protoTxt = "openpose/pose/body_25/pose_deploy.prototxt";

  public String model = "openpose/pose/coco/pose_iter_440000.caffemodel";
  public String protoTxt = "openpose/pose/coco/pose_deploy_linevec.prototxt";
  
  transient private final OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();
  
  int[][][] POSE_PAIRS = new int[][][]{
    {   // COCO body
      {1, 2}, // left shoulder
      {1, 5}, // right shoulder
      {2, 3}, // left arm
      {3, 4}, // left forearm
      {5, 6}, // right arm
      {6, 7}, // right forearm
      {1, 8}, // left body
      {8, 9}, // left thigh
      {9, 10}, // left calf
      {1, 11}, // right body
      {11, 12}, // right thigh
      {12, 13}, // right calf
      {1, 0}, // neck
      {0, 14}, // left nose
      {14, 16}, // left eye
      {0, 15}, // right nose
      {15, 17}  // right eye
  },
  {   // MPI body
      {0,1}, {1,2}, {2,3},
      {3,4}, {1,5}, {5,6},
      {6,7}, {1,14}, {14,8}, {8,9},
      {9,10}, {14,11}, {11,12}, {12,13}
  },
  {   // hand
      {0,1}, {1,2}, {2,3}, {3,4},         // thumb
      {0,5}, {5,6}, {6,7}, {7,8},         // pinkie
      {0,9}, {9,10}, {10,11}, {11,12},    // middle
      {0,13}, {13,14}, {14,15}, {15,16},  // ring
      {0,17}, {17,18}, {18,19}, {19,20}   // small
  }};
  
  
  public OpenCVFilterOpenPose() {
    super();
    loadModel();
  }

  public OpenCVFilterOpenPose(String name) {
    super(name);
    loadModel();
  }

  public void loadModel() {
    log.info("loading DNN caffee model Open Pose");
    if (!new File(protoTxt).exists()) {
      log.warn("ProtoTxt not found {}", protoTxt);
      return;
    }
    if (!new File(model).exists()) {
      log.warn("model not found {}", model);
      return;
    }
    net = readNetFromCaffe(protoTxt, model);
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO: noOp?
  }

  @Override
  public IplImage process(IplImage image) {
    
    //IplImage image = cvCreateImage(cvGetSize(im), 8, 1);
    //cvCvtColor(im, image, CV_BGR2GRAY);
    int h = image.height();
    int w = image.width();
    log.info("Process .. Image To Process {}", image);
    //h = 368;
    //w = 368;
    
    double thresh = 0.5;
    Mat src = grabberConverter.convertToMat(grabberConverter.convert(image));
    Mat srcMat = new Mat();
    resize(src, srcMat, new Size(320, 240));
    log.info("Process .. we've got a mat! {}", srcMat);
    //show(srcMat, "Source image");
    // TODO: this scalar is probably wrong.
    Scalar scalar = new Scalar(0, 0, 0, 0);
    // img, 1.0 / 255, Size(W_in, H_in), Scalar(0, 0, 0), false, false
    Mat inputBlob = blobFromImage(srcMat, 1.0/255, new Size(w, h), scalar, false, false, CV_32F);
    log.info("Input blob! {} ", inputBlob);
    net.setInput(inputBlob);
    Mat result = net.forward();
    // the result is an array of "heatmaps", the probability of a body part being in location x,y
    // ?? result.row(1).size()
    
  
    Mat ne = new Mat(result.size(), CV_32F, result.ptr(0, 0));
    FloatIndexer srcIndexer = ne.createIndexer();
    log.info("Src Indexer : {}", srcIndexer);
//    for (long l : srcIndexer.sizes()) {
//      log.info ("Result Size {} ", l);
//    }
//    log.info("Indexer rows : {}  cols : {}", srcIndexer.rows(), srcIndexer.cols());
//    
//    float f1 = srcIndexer.get(1, 0);
//    float f2 = srcIndexer.get(1, 1);
//    float f3 = srcIndexer.get(1, 2);
    
    int midx;
    int npairs;
    
    // reshape?!  TODO: seems like this is wonky? why divide by 3? 
    // there's got to be something else with this
    int nparts = result.size(1)/3;
    //log.info("Channels:{}  ", result.channels());
    log.info("Rows :  {}  Cols : {}", result.rows() , result.cols());
    log.info("Rows :  {}  Cols : {}", result.arrayWidth() , result.arrayHeight());
    log.info("Result MAT: {}", result);
    
    
    log.info("Result has {} depth on row 1", result.rows(1).arrayDepth());
    log.info("Result has {} height on row 1", result.rows(1).arrayHeight());
    log.info("Result has {} width on row 1", result.rows(1).arrayWidth());
    // how many rows.
    log.info("Result has {} nparts.", result.size(1));
    log.info("Result has {} h", result.size(2));
    log.info("Result has {} w", result.size(3));
    // String filename = "pose.jpg";
    for ( int i = 0 ; i < result.rows(); i++) {
      log.info("{} : {}",i, result.row(i).size());
    }
    
//    Mat ne = new Mat(new Size(result.size(3), result.size(2)), CV_32F, result.ptr(0, 0));//extract a 2d matrix for 4d output matrix with form of (number of detections x 7)
//    FloatIndexer srcIndexer = ne.createIndexer(); // create indexer to access elements of the matrix
    
   // log.info("3 : {}",result.row(3).size());
    int H = result.size(2);
    int W = result.size(3);

    //    // find out, which model we have
    if (nparts == 19) {   // COCO body
      midx   = 0;
      npairs = 17;
      nparts = 18; // skip background
    } else if (nparts == 16) {   // MPI body
      midx   = 1;
      npairs = 14;
    } else if (nparts == 22) {   // hand
      midx   = 2;
      npairs = 20;
    } else {
      log.warn("There should be 19 parts for the COCO model, 16 for MPI, or 22 for the hand one, but this model has {} parts.", nparts);
     
      return (image);
    }
    //    // find the position of the body parts
    float SX = (float)(srcMat.cols()) / W;
    float SY = (float)(srcMat.rows()) / H;
    Point[] points = new Point[22];
    for (int n=0; n<nparts; n++) {
      // Slice heatmap of corresponding body's part.
      Mat heatMap = new Mat(H, W, CV_32F, result.ptr(0,n));// Mat heatMap(H, W, CV_32F, result.ptr(0,n));
      // 1 maximum per heatmap
      Point p = new Point(-1,-1);
      Point pm = new Point(0,0);
      DoublePointer conf = new DoublePointer(0.0);
      DoublePointer doublePointer = new DoublePointer(0.0);
      // a mat gets passed in the end?
      // TODO: this is probably wrong.
      minMaxLoc(heatMap, doublePointer, conf, p, pm, null);
      //minMaxLoc(heatMap, doublePointer);
      //minMaxLoc(heatMap, doublePointer);
      
      // log.info("Point {} x: {} y: {} conf: {}", n, p.x(), p.y(), conf.get());
      //circle(srcMat, p, 10, Scalar.RED);
      //show(srcMat, "Point "+ n);
      log.info("PM: {}", pm);
      if (conf.get() > thresh) {
        p = pm;
      }
      log.info("Adding point N: {} Value : ({},{})", n, p.x(), p.y());
      points[n] = new Point((int)(p.x()*SX), (int)(p.y()*SY));
    }
    // connect body parts and draw it !

    for (int n=0; n<npairs; n++) {
     //  log.info("SX {} SY {}", SX,SY);
      // lookup 2 connected body/hand parts
      //Point a = new Point()
      Point a = points[POSE_PAIRS[midx][n][0]];
      Point b = points[POSE_PAIRS[midx][n][1]];
      
      log.info("ORIGINAL N:{} - A: ({},{}) B:  ({},{})", n, a.x(), a.y(), b.x(), b.y());
      
      // we did not find enough confidence before
      if (a.x()<=0 || a.y()<=0 || b.x()<=0 || b.y()<=0) {
        log.info("Skipping N {} ", n);
        continue;
      }
      // scale to image size
//      a.x((int)(a.x() * SX)); 
//      a.y((int)(a.y() * SY));
//      b.x((int)(b.x()*SX)); 
//      b.y((int)(b.y()*SY));
      
      log.info("SCALED N:{} - A: ({},{}) B:  ({},{})", n, a.x(), a.y(), b.x(), b.y());
      
      line(srcMat, a, b, Scalar.RED);
      circle(srcMat, a, 3, Scalar.GREEN);
      circle(srcMat, b, 3, Scalar.GREEN);
    }

    IplImage resImg = grabberConverter.convert(converterToIpl.convert(srcMat));
    return resImg;

    
  }



  // helper method to show an image. (todo; convert it to a Mat )
  public void show(final Mat imageMat, final String title) {
    IplImage image = converterToIpl.convertToIplImage(converterToIpl.convert(imageMat));
    final IplImage image1 = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, image.nChannels());
    cvCopy(image, image1);
    CanvasFrame canvas = new CanvasFrame(title, 1);
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    canvas.showImage(converterToIpl.convert(image1));
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    // TODO Auto-generated method stub
    return image;
  }

}
