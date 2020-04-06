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

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

public class OpenCVFilterFaceDetectDNN extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceDetectDNN.class.getCanonicalName());
  // int x0, y0, x1, y1;

  private String FACE_LABEL = "face";
  private Net net;
  /**
   * bounding boxes of faces
   */
  final List<Rectangle> bb = new ArrayList<>();
  final Map<String, List<Classification>> classifications = new TreeMap<>();
  // if deps were checked in it would be like this
  /*
  public String model = FileIO.gluePaths(Service.getResourceDir(OpenCV.class),"models/facedetectdnn/res10_300x300_ssd_iter_140000.caffemodel");
  public String protoTxt = FileIO.gluePaths(Service.getResourceDir(OpenCV.class),"models/facedetectdnn/deploy.prototxt.txt");
  */
  // but 
  public String model = "resource/OpenCV/models/facedetectdnn/res10_300x300_ssd_iter_140000.caffemodel";
  public String protoTxt = "resource/OpenCV/models/facedetectdnn/deploy.prototxt.txt";
  double threshold = .2;

  transient private final OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
  transient private OpenCVFrameConverter.ToIplImage converterToIpl = new OpenCVFrameConverter.ToIplImage();

  public OpenCVFilterFaceDetectDNN() {
    this(null);
  }

  public OpenCVFilterFaceDetectDNN(String name) {
    super(name);
    loadModel();
  }

  public void loadModel() {
    // log.info("loading DNN caffee model for face recogntion..");
    if (!new File(protoTxt).exists()) {
      log.warn("Caffe DNN Face Detector ProtoTxt not found {}", protoTxt);
      return;
    }
    if (!new File(model).exists()) {
      log.warn("Caffe DNN Face Detector model not found {}", model);
      return;
    }
    net = readNetFromCaffe(protoTxt, model);
    log.info("Caffe DNN Face Detector model loaded.");
  }

  @Override
  public void imageChanged(IplImage image) {
    // TODO: noOp?
  }

  @Override
  public IplImage process(IplImage image) {
    int h = image.height();
    int w = image.width();
    // TODO: cv2.resize(image, (300, 300))
    Mat srcMat = grabberConverter.convertToMat(grabberConverter.convert(image));
    Mat inputMat = new Mat();
    resize(srcMat, inputMat, new Size(300, 300));// resize the image to match
    // the input size of the model
    // create a 4-dimensional blob from image with NCHW (Number of images in the
    // batch -for training only-, Channel, Height, Width)
    // dimensions order,
    // for more details read the official docs at
    // https://docs.opencv.org/trunk/d6/d0f/group__dnn.html#gabd0e76da3c6ad15c08b01ef21ad55dd8
    Mat blob = blobFromImage(inputMat, 1.0, new Size(300, 300), new Scalar(104.0, 177.0, 123.0, 0), false, false, CV_32F);
    // log.info("Input Blob : {}", blob);
    // set the input to network model
    if (blob == null) {
      return image;
    }
    net.setInput(blob);
    // feed forward the input to the network to get the output matrix
    Mat output = net.forward();
    Mat ne = new Mat(new Size(output.size(3), output.size(2)), CV_32F, output.ptr(0, 0));// extract
    // a
    // 2d
    // matrix
    // for
    // 4d
    // output
    // matrix
    // with
    // form
    // of
    // (number
    // of
    // detections
    // x
    // 7)
    FloatIndexer srcIndexer = ne.createIndexer(); // create indexer to access
    // elements of the matrix
    // log.info("Output Size: {}", output.size(3));
    bb.clear();
    classifications.clear();
    for (int i = 0; i < output.size(3); i++) {// iterate to extract elements
      float confidence = srcIndexer.get(i, 2);
      // log.info("Getting element {} confidence {}", i, confidence);
      float f1 = srcIndexer.get(i, 3);
      float f2 = srcIndexer.get(i, 4);
      float f3 = srcIndexer.get(i, 5);
      float f4 = srcIndexer.get(i, 6);

      if (confidence > threshold) {
        // log.info("Passes the threshold test.");
        float tx = f1 * w;// top left point's x
        float ty = f2 * h;// top left point's y
        float bx = f3 * w;// bottom right point's x
        float by = f4 * h;// bottom right point's y
        Rectangle rect = new Rectangle(tx, ty, bx - tx, by - ty);
        List<Classification> cl = null;
        Classification classification = new Classification(FACE_LABEL, confidence, rect);
        if (classifications.containsKey(FACE_LABEL)) {
          classifications.get(FACE_LABEL).add(classification);
        } else {
          cl = new ArrayList<>();
          cl.add(classification);
          classifications.put(FACE_LABEL, cl);
        }
        bb.add(rect);

      }
    }

    publishClassification(classifications);
    IplImage result = grabberConverter.convert(converterToIpl.convert(srcMat));
    ne.close();
    return result;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    for (String label : classifications.keySet()) {
      List<Classification> cl = classifications.get(label);
      for (Classification c : cl) {
        Rectangle rect = c.getBoundingBox();
        graphics.drawString(String.format("%s %.3f", c.getLabel(), c.getConfidence()), (int) rect.x, (int) rect.y);
        graphics.drawRect((int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
      }
    }
    return image;
  }

}
