package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNet;
import static org.bytedeco.opencv.global.opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_RGB2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.THRESH_BINARY;
import static org.bytedeco.opencv.global.opencv_imgproc.adaptiveThreshold;
import static org.bytedeco.opencv.global.opencv_imgproc.cvResize;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RotatedRect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.Size2f;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TesseractOcr;
import org.opencv.imgproc.Imgproc;

/**
 * This opencv filter will first use the EAST text detector to identify rotated
 * rects that represent areas that contain text within the image. Then, that
 * region is cropped, rotated, and slighty warped The resulting image is then
 * passed to tesseractOcr to perform OCR on that region. The resulting data
 * returned is the DetectedText object. This is based on work from
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
  int fontSize = 16;
  // these need to be a multiple of 32. They determine the input size to the
  // model.
  int newWidth = 320;
  int newHeight = 320;
  // a little extra padding on the x axis for our detected boxes
  int xPadding = 2;
  // some on the y axis.
  int yPadding = 2;
  // first we need our EAST detection model.
  String modelFile = "resource/OpenCV/east_text_detector/frozen_east_text_detection.pb";
  // text region detection threshold
  float confThreshold = 0.5f;
  // non-maximum suppression threshold for de-dup of overlapping results.
  float nmsThreshold = (float) 0.3;
  // the actual east text classification network
  transient Net detector = null;
  //
  boolean thresholdEnabled = false;
  // milage may vary..
  double blurrinessThreshold = 100.0;

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
    if (data.getBlurriness() != null) {
      // the image has had blurriness detector already run on it..
      if (data.getBlurriness() < blurrinessThreshold) {
        // the image is too blurry, don't bother trying to do text detection.
        return image;
      }
    }
    classifications = detectText(image);
    data.setDetectedText(classifications);
    // return the original image un-altered.
    return image;
  }

  private ArrayList<DetectedText> detectText(IplImage image) {
    StringBuilder detectedTextLine = new StringBuilder();
    CloseableFrameConverter converter1 = new CloseableFrameConverter();
    Mat originalImageMat = converter1.toMat(image);
    // This is a ratio between the size of the input image vs the input for the
    // east detector.
    Point2f ratio = new Point2f((float) image.width() / newWidth, (float) image.height() / newHeight);
    // Resize the image to mat
    IplImage ret = AbstractIplImage.create(newWidth, newHeight, image.depth(), image.nChannels());
    cvResize(image, ret, Imgproc.INTER_AREA);
    CloseableFrameConverter converter2 = new CloseableFrameConverter();
    Mat frame = converter2.toMat(ret);
    // Create the blob to put into the EAST text detector
    Mat blob = blobFromImage(frame, 1.0, new Size(newWidth, newHeight), new Scalar(123.68, 116.78, 103.94, 0.0), true, false, CV_32F);
    detector.setInput(blob);
    // these two layers contain the scores and the rotated rects
    StringVector outNames = new StringVector("feature_fusion/Conv_7/Sigmoid", "feature_fusion/concat_3");
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
      // the target height and width rect after we warp with the padding on the
      // original image
      int w = (int) ((dt.box.size().width() + xPadding) * ratio.x());
      int h = (int) ((dt.box.size().height() + yPadding) * ratio.y());
      Size outputSizeInt = new Size(w, h);
      // the target output size for a rotated rect slighty larger than the
      // original.. in the scaled image.
      Size2f origPaddedSize = new Size2f(dt.box.size().width() + xPadding, dt.box.size().height() + yPadding);
      RotatedRect largerBox = new RotatedRect(dt.box.center(), origPaddedSize, dt.box.angle());
      // crop and rotate based on the updated padded box.
      Mat cropped = Util.cropAndRotate(originalImageMat, largerBox, outputSizeInt, ratio);
      // Some thresholding on the cropped image.
      Mat ocrInputMat = cropped;
      if (thresholdEnabled) {
        ocrInputMat = applyAdaptiveThreshold(cropped);
      }
      String croppedResult = ocrMat(ocrInputMat);
      // If debugging.. it's useful to see this section ..
      // show(cropped, croppedResult);
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
    converter1.close();
    converter2.close();
    return results;
  }

  private static Mat applyAdaptiveThreshold(Mat cropped) {
    // int thresh = 127;
    // int maxval = 255;
    Mat dest = new Mat();
    // threshold(cropped, dest, thresh, maxval, type);
    // need to cut the mat down to grayscale for this i guess?
    cvtColor(cropped, dest, COLOR_RGB2GRAY);
    // Mat, Mat, double, int, int, int, double
    // TODO: what does changing these values do?
    adaptiveThreshold(dest, dest, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 5, 2.0);
    // show(cropped, "input");
    // show(dest, "output");
    return dest;
  }

  private String ocrMat(Mat input) {
    String result = null;
    CloseableFrameConverter converter = new CloseableFrameConverter();
    BufferedImage candidate = converter.toBufferedImage(input);
    try {
      if (tesseract == null) {
        tesseract = (TesseractOcr) Runtime.start("tesseract", "TesseractOcr");
      }
      result = tesseract.ocr(candidate).trim();
    } catch (IOException e) {
      log.warn("Tesseract failure.", e);
    }
    converter.close();
    // show(input, result);
    return result;
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
      for (int x = 0; x < width; x++) {
        // Get the score of this classification.
        float score = scoresIndexer.get(0, 0, y, x);
        if (score < threshold) {
          continue;
        }
        // two points and an angle to determine the rotated rect i guess.
        float x0_data = geometryIndexer.get(0, 0, y, x);
        float x1_data = geometryIndexer.get(0, 1, y, x);
        float x2_data = geometryIndexer.get(0, 2, y, x);
        float x3_data = geometryIndexer.get(0, 3, y, x);
        float angle = geometryIndexer.get(0, 4, y, x);
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
        float centerX = (float) (0.5 * (p1_0 + p3_0));
        float centerY = (float) (0.5 * (p1_1 + p3_1));
        Point2f center = new Point2f(centerX, centerY);
        // The dimensions of the rect
        Size2f size = new Size2f((float) w, (float) h);
        // Create the rotated rect.
        // TODO: It would be nice if this was scaled back up to the
        // original resolution here. (perhaps, that means scaling
        // x0/1/2/3_data..
        // and what does that do to the angle if we scale with a non square
        // aspect ratio? ... icky.
        RotatedRect rec = new RotatedRect(center, size, (float) (-1 * angle * 180.0 / Math.PI));
        boxes.add(rec);
        confidences.add(score);
      }
    }
    // Apply non-maximum suppression to filter down boxes that mostly overlap
    ArrayList<DetectedText> maxRects = Util.applyNMSBoxes(threshold, boxes, confidences, nmsThreshold);
    // This is the filtered list of rects that matched our threshold.
    classifications = orderRects(maxRects, frame.cols());
    return maxRects;
  }

  private ArrayList<DetectedText> orderRects(ArrayList<DetectedText> maxRects, int width) {
    Comparator<DetectedText> rectComparator = new Comparator<DetectedText>() {
      @Override
      public int compare(DetectedText rect1, DetectedText rect2) {
        // left to right.. top to bottom.
        // TODO: this 100 is a vertical sort of resolution ... it should be more
        // dynamic
        // and it should probably be configured somehow.. in reality this
        // algorithm needs to be replaced/fixed
        int index1 = rect1.box.boundingRect().x() + (rect1.box.boundingRect().y() * width / 100);
        int index2 = rect2.box.boundingRect().x() + (rect2.box.boundingRect().y() * width / 100);
        return (index2 > index1 ? -1 : (index2 == index1 ? 0 : 1));
      }
    };
    maxRects.sort(rectComparator);
    return maxRects;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    StringBuilder fullText = new StringBuilder();
    // we need to scale the boxes
    Font previousFont = graphics.getFont();
    // increase the size of this font.
    graphics.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
    Point2f ratio = new Point2f((float) image.getWidth() / newWidth, (float) image.getHeight() / newHeight);
    for (DetectedText rr : classifications) {
      // Render the rect on the image..
      // TODO: draw the rotated rect instead of a bounding box.
      // graphics.drawLine(x1, y1, x2, y2);
      Rect bR = rr.box.boundingRect();
      // Scaled to the original size of the image.
      int x = (int) (bR.x() * ratio.x());
      int y = (int) (bR.y() * ratio.y());
      int w = (int) (bR.width() * ratio.x());
      int h = (int) (bR.height() * ratio.y());
      graphics.setColor(Color.GREEN);
      graphics.drawRect(x, y, w, h);
      graphics.setColor(Color.RED);
      // we should center the text in the middle of the box.
      int yText = y + h / 2 + fontSize / 2;
      graphics.drawString(rr.text, x, yText);
      fullText.append(rr.text).append(" ");
    }
    graphics.setColor(Color.YELLOW);
    graphics.drawString(fullText.toString().trim(), 20, 60);
    ratio.close();
    // restore the font .. just in case?
    graphics.setFont(previousFont);
    return image;
  }

}
