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
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RotatedRect;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.PointVector;
import org.bytedeco.opencv.opencv_core.PointVectorVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.Size2f;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_dnn.TextDetectionModel_DB;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TesseractOcr;
import org.myrobotlab.service.data.OcrResult;
import org.myrobotlab.service.data.OcrResult.OcrWord;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;

/**
 * Scene-text OCR filter: detect regions (EAST or DBNet, or the full frame),
 * then recognize with a named {@link TesseractOcr} peer.
 * <p>
 * Detection weights: EAST may already be present from the OpenCV Ivy zip
 * ({@code resource/OpenCV/east_text_detector}). DBNet ONNX files are not
 * shipped; with {@link #autoDownloadModel} they are fetched into
 * {@code data/OpenCV/ocr_models/}. Select the variant with
 * {@link #detectionModel} (WebGui dropdown).
 * <p>
 * Unlike {@link OpenCVFilterTesseract} this does not OCR every millisecond on a
 * runaway thread. {@link #minIntervalMs} throttles work, and blurry frames can
 * be skipped when a {@link OpenCVFilterBlurDetector} has already run.
 */
public class OpenCVFilterOcr extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterOcr.class);

  /**
   * Name of the {@link TesseractOcr} service to use for recognition.
   */
  public String ocrService = "tesseract";

  /**
   * Catalog id from {@link OcrDetectionModels}: {@code east}, {@code none},
   * {@code db_ic15_r18}, {@code db_ic15_r50}, {@code db_td500_r18},
   * {@code db_td500_r50}.
   */
  public String detectionModel = OcrDetectionModels.EAST;

  /**
   * Legacy detector name kept in sync with {@link #detectionModel}:
   * {@code east}, {@code db}, or {@code none}.
   */
  public String detector = "east";

  /**
   * Download missing EAST / DBNet weights into {@code data/OpenCV/ocr_models/}.
   */
  public boolean autoDownloadModel = true;

  /**
   * Minimum milliseconds between OCR passes. Live video should stay well above
   * Tesseract's runtime (often 200–800ms per frame).
   */
  public int minIntervalMs = 500;

  /**
   * When true, skip OCR if {@link OpenCVData#getBlurriness()} is below
   * {@link #blurrinessThreshold}.
   */
  public boolean skipBlurry = true;

  public double blurrinessThreshold = 100.0;

  public float confThreshold = 0.5f;

  public float nmsThreshold = 0.3f;

  public int newWidth = 320;

  public int newHeight = 320;

  public int xPadding = 2;

  public int yPadding = 2;

  public boolean thresholdEnabled = false;

  public int fontSize = 16;

  /**
   * Optional override path for the EAST protobuf. Empty / missing falls back to
   * the Ivy zip, then the download cache.
   */
  public String eastModelFile = "resource/OpenCV/east_text_detector/frozen_east_text_detection.pb";

  /**
   * Optional override path for a DBNet ONNX file. Empty uses the catalog cache.
   */
  public String dbModelFile = "";

  public boolean autoStartOcr = true;

  /**
   * Last stitched OCR string, also shown in the OpenCV WebGui filter panel.
   */
  public String lastText = "";

  /**
   * Human-readable model load / download status for the WebGui.
   */
  public String modelStatus = "";

  /**
   * Absolute path of the detector currently loaded (empty if none).
   */
  public String loadedModelPath = "";

  /**
   * Catalog ids that are present locally (Ivy zip or download cache).
   */
  public List<String> installedDetectionModels = new ArrayList<>();

  transient private TesseractOcr tesseract = null;
  transient private Net eastDetector = null;
  transient private TextDetectionModel_DB dbDetector = null;
  transient private String loadedModelId = null;
  transient private boolean downloadFailed = false;
  transient private long lastOcrTs = 0;
  transient private ArrayList<DetectedText> classifications = new ArrayList<>();

  public OpenCVFilterOcr() {
    super();
    refreshInstalledModels();
  }

  public OpenCVFilterOcr(String name) {
    super(name);
    refreshInstalledModels();
  }

  public OpenCVFilterOcr(String filterName, String sourceKey) {
    super(filterName, sourceKey);
    refreshInstalledModels();
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    if (skipBlurry && data.getBlurriness() != null && data.getBlurriness() < blurrinessThreshold) {
      data.setDetectedText(classifications);
      return image;
    }
    long now = System.currentTimeMillis();
    if (lastOcrTs != 0 && now - lastOcrTs < Math.max(0, minIntervalMs)) {
      data.setDetectedText(classifications);
      return image;
    }
    classifications = detectAndOcr(image);
    lastOcrTs = System.currentTimeMillis();
    data.setDetectedText(classifications);
    return image;
  }

  private ArrayList<DetectedText> detectAndOcr(IplImage image) {
    OcrDetectionModels.Spec spec = selectedSpec();
    detectionModel = spec.id;
    detector = spec.detector;
    refreshInstalledModels();
    ArrayList<DetectedText> regions;
    if (OcrDetectionModels.NONE.equals(spec.detector)) {
      modelStatus = "full-frame (no detector)";
      loadedModelPath = "";
      regions = fullFrameRegion(image);
    } else if ("db".equals(spec.detector)) {
      regions = detectDb(image, spec);
      if (regions == null) {
        log.warn("DBNet model unavailable, falling back to EAST");
        modelStatus = "DBNet missing, using EAST";
        regions = detectEast(image, OcrDetectionModels.get(OcrDetectionModels.EAST));
      }
    } else {
      regions = detectEast(image, spec);
    }
    if (regions == null) {
      log.warn("detector model unavailable, falling back to full-frame OCR");
      modelStatus = "detector missing, using full-frame";
      regions = fullFrameRegion(image);
    }
    OcrResult combined = new OcrResult();
    combined.source = name;
    StringBuilder line = new StringBuilder();
    for (DetectedText dt : regions) {
      String text = ocrRegion(image, dt);
      dt.text = text;
      if (text != null && text.length() > 0) {
        line.append(text).append(" ");
        OcrWord word = new OcrWord();
        word.text = text;
        word.confidence = dt.confidence;
        if (dt.box != null) {
          Rect br = dt.box.boundingRect();
          word.x = br.x();
          word.y = br.y();
          word.width = br.width();
          word.height = br.height();
        }
        combined.words.add(word);
      }
    }
    combined.text = line.toString().trim();
    lastText = combined.text;
    publishCombined(combined);
    return regions;
  }

  private void publishCombined(OcrResult combined) {
    TesseractOcr ocr = getTesseract();
    if (ocr != null && combined.text != null && combined.text.length() > 0) {
      ocr.publishResult(combined);
    }
  }

  private ArrayList<DetectedText> fullFrameRegion(IplImage image) {
    ArrayList<DetectedText> regions = new ArrayList<>();
    Point2f center = new Point2f(image.width() / 2.0f, image.height() / 2.0f);
    Size2f size = new Size2f(image.width(), image.height());
    regions.add(new DetectedText(new RotatedRect(center, size, 0), 1.0f, null));
    return regions;
  }

  private String ocrRegion(IplImage image, DetectedText dt) {
    TesseractOcr ocr = getTesseract();
    if (ocr == null) {
      return "";
    }
    CloseableFrameConverter converter = new CloseableFrameConverter();
    try {
      BufferedImage candidate;
      if (dt.box == null || isFullFrame(dt, image)) {
        candidate = converter.toBufferedImage(image);
      } else {
        Mat original = converter.toMat(image);
        Point2f ratio = new Point2f(1.0f, 1.0f);
        int w = Math.max(8, (int) (dt.box.size().width() + xPadding));
        int h = Math.max(8, (int) (dt.box.size().height() + yPadding));
        Size outputSizeInt = new Size(w, h);
        Size2f origPaddedSize = new Size2f(dt.box.size().width() + xPadding, dt.box.size().height() + yPadding);
        RotatedRect largerBox = new RotatedRect(dt.box.center(), origPaddedSize, dt.box.angle());
        Mat cropped = Util.cropAndRotate(original, largerBox, outputSizeInt, ratio);
        Mat ocrInput = thresholdEnabled ? applyAdaptiveThreshold(cropped) : cropped;
        candidate = converter.toBufferedImage(ocrInput);
      }
      OcrResult result = ocr.recognize(candidate, false);
      if (result == null || result.text == null) {
        return "";
      }
      return result.text.trim();
    } catch (Exception e) {
      log.warn("OCR region failed", e);
      return "";
    } finally {
      converter.close();
    }
  }

  private static boolean isFullFrame(DetectedText dt, IplImage image) {
    if (dt.box == null) {
      return true;
    }
    Rect br = dt.box.boundingRect();
    return br.width() >= image.width() - 2 && br.height() >= image.height() - 2;
  }

  private TesseractOcr getTesseract() {
    if (tesseract != null) {
      return tesseract;
    }
    String name = ocrService == null || ocrService.trim().isEmpty() ? "tesseract" : ocrService.trim();
    tesseract = (TesseractOcr) Runtime.getService(name);
    if (tesseract == null && autoStartOcr) {
      tesseract = (TesseractOcr) Runtime.start(name, "TesseractOcr");
    }
    return tesseract;
  }

  private ArrayList<DetectedText> detectEast(IplImage image, OcrDetectionModels.Spec spec) {
    String model = resolveModel(spec, eastModelFile);
    if (model == null) {
      return null;
    }
    if (eastDetector == null) {
      eastDetector = readNet(model);
      loadedModelPath = model;
      modelStatus = "EAST loaded";
    }
    CloseableFrameConverter converter2 = new CloseableFrameConverter();
    try {
      Point2f ratio = new Point2f((float) image.width() / newWidth, (float) image.height() / newHeight);
      IplImage ret = AbstractIplImage.create(newWidth, newHeight, image.depth(), image.nChannels());
      cvResize(image, ret, Imgproc.INTER_AREA);
      Mat frame = converter2.toMat(ret);
      Mat blob = blobFromImage(frame, 1.0, new Size(newWidth, newHeight), new Scalar(123.68, 116.78, 103.94, 0.0), true, false, CV_32F);
      eastDetector.setInput(blob);
      StringVector outNames = new StringVector("feature_fusion/Conv_7/Sigmoid", "feature_fusion/concat_3");
      MatVector outs = new MatVector();
      eastDetector.forward(outs, outNames);
      ArrayList<DetectedText> results = decodeBoundingBoxes(frame, outs.get(0), outs.get(1), confThreshold);
      // scale boxes back to the original image for overlay / crop
      for (DetectedText dt : results) {
        if (dt.box == null) {
          continue;
        }
        Point2f c = dt.box.center();
        Size2f s = dt.box.size();
        dt.box = new RotatedRect(new Point2f(c.x() * ratio.x(), c.y() * ratio.y()), new Size2f(s.width() * ratio.x(), s.height() * ratio.y()), dt.box.angle());
      }
      return results;
    } catch (Exception e) {
      log.warn("EAST detection failed", e);
      return null;
    } finally {
      converter2.close();
    }
  }

  private ArrayList<DetectedText> detectDb(IplImage image, OcrDetectionModels.Spec spec) {
    String model = resolveModel(spec, dbModelFile);
    if (model == null) {
      return null;
    }
    CloseableFrameConverter converter = new CloseableFrameConverter();
    try {
      if (dbDetector == null) {
        int width = spec.inputWidth > 0 ? spec.inputWidth : 736;
        int height = spec.inputHeight > 0 ? spec.inputHeight : 736;
        dbDetector = new TextDetectionModel_DB(model);
        dbDetector.setBinaryThreshold(0.3f);
        dbDetector.setPolygonThreshold(0.5f);
        dbDetector.setUnclipRatio(2.0);
        dbDetector.setMaxCandidates(200);
        dbDetector.setInputParams(1.0 / 255.0, new Size(width, height), new Scalar(122.67891434, 116.66876762, 104.00698793, 0.0), true, false);
        loadedModelPath = model;
        modelStatus = "DBNet loaded " + spec.id + " (" + width + "x" + height + ")";
      }
      Mat frame = converter.toMat(image);
      PointVectorVector detections = new PointVectorVector();
      dbDetector.detect(frame, detections);
      ArrayList<DetectedText> results = new ArrayList<>();
      for (long i = 0; i < detections.size(); i++) {
        PointVector poly = detections.get(i);
        if (poly == null || poly.size() == 0) {
          continue;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (long j = 0; j < poly.size(); j++) {
          Point p = poly.get(j);
          minX = Math.min(minX, p.x());
          minY = Math.min(minY, p.y());
          maxX = Math.max(maxX, p.x());
          maxY = Math.max(maxY, p.y());
        }
        Point2f center = new Point2f((minX + maxX) / 2.0f, (minY + maxY) / 2.0f);
        Size2f size = new Size2f(Math.max(1, maxX - minX), Math.max(1, maxY - minY));
        results.add(new DetectedText(new RotatedRect(center, size, 0), 1.0f, null));
      }
      return results;
    } catch (Exception e) {
      log.warn("DBNet detection failed", e);
      dbDetector = null;
      return null;
    } finally {
      converter.close();
    }
  }

  private ArrayList<DetectedText> decodeBoundingBoxes(Mat frame, Mat scores, Mat geometry, float threshold) {
    int height = scores.size(2);
    int width = scores.size(3);
    FloatIndexer scoresIndexer = scores.createIndexer();
    FloatIndexer geometryIndexer = geometry.createIndexer();
    ArrayList<RotatedRect> boxes = new ArrayList<>();
    ArrayList<Float> confidences = new ArrayList<>();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        float score = scoresIndexer.get(0, 0, y, x);
        if (score < threshold) {
          continue;
        }
        float x0Data = geometryIndexer.get(0, 0, y, x);
        float x1Data = geometryIndexer.get(0, 1, y, x);
        float x2Data = geometryIndexer.get(0, 2, y, x);
        float x3Data = geometryIndexer.get(0, 3, y, x);
        float angle = geometryIndexer.get(0, 4, y, x);
        double offsetX = x * 4.0;
        double offsetY = y * 4.0;
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        double h = x0Data + x2Data;
        double w = x1Data + x3Data;
        double offset0 = offsetX + cosA * x1Data + sinA * x2Data;
        double offset1 = offsetY - sinA * x1Data + cosA * x2Data;
        double p1_0 = -sinA * h + offset0;
        double p1_1 = -cosA * h + offset1;
        double p3_0 = -cosA * w + offset0;
        double p3_1 = sinA * w + offset1;
        Point2f center = new Point2f((float) (0.5 * (p1_0 + p3_0)), (float) (0.5 * (p1_1 + p3_1)));
        Size2f size = new Size2f((float) w, (float) h);
        boxes.add(new RotatedRect(center, size, (float) (-1 * angle * 180.0 / Math.PI)));
        confidences.add(score);
      }
    }
    ArrayList<DetectedText> maxRects = Util.applyNMSBoxes(threshold, boxes, confidences, nmsThreshold);
    maxRects.sort(leftToRightTopToBottom(frame.cols()));
    return maxRects;
  }

  private static Comparator<DetectedText> leftToRightTopToBottom(int width) {
    return (rect1, rect2) -> {
      int index1 = rect1.box.boundingRect().x() + (rect1.box.boundingRect().y() * width / 100);
      int index2 = rect2.box.boundingRect().x() + (rect2.box.boundingRect().y() * width / 100);
      return Integer.compare(index1, index2);
    };
  }

  private static Mat applyAdaptiveThreshold(Mat cropped) {
    Mat dest = new Mat();
    cvtColor(cropped, dest, COLOR_RGB2GRAY);
    adaptiveThreshold(dest, dest, 255.0, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 5, 2.0);
    return dest;
  }

  /**
   * Download / locate the catalog model selected in the WebGui. Safe to call
   * when capture is not running.
   *
   * @return absolute path, or null for full-frame mode
   */
  public synchronized String installSelectedModel() {
    OcrDetectionModels.Spec spec = selectedSpec();
    detectionModel = spec.id;
    detector = spec.detector;
    downloadFailed = false;
    resetDetectors();
    if (OcrDetectionModels.NONE.equals(spec.detector)) {
      loadedModelPath = "";
      modelStatus = "full-frame (no detector)";
      refreshInstalledModels();
      return null;
    }
    String override = "db".equals(spec.detector) ? dbModelFile : eastModelFile;
    try {
      if (opencv != null) {
        opencv.info("installing OCR detection model %s", spec.id);
      }
      modelStatus = "downloading " + spec.id;
      File file = OcrDetectionModels.resolve(spec, override, true);
      if (file == null) {
        modelStatus = "not found: " + spec.id;
        return null;
      }
      loadedModelPath = file.getAbsolutePath();
      modelStatus = "ready: " + spec.id;
      refreshInstalledModels();
      return loadedModelPath;
    } catch (Exception e) {
      downloadFailed = true;
      modelStatus = "download failed: " + e.getMessage();
      log.warn("OCR model install failed for {}", spec.id, e);
      if (opencv != null) {
        opencv.error("OCR model download failed: %s", e.getMessage());
      }
      return null;
    }
  }

  private synchronized String resolveModel(OcrDetectionModels.Spec spec, String overridePath) {
    if (spec == null) {
      return null;
    }
    if (loadedModelId != null && !spec.id.equals(loadedModelId)) {
      resetDetectors();
    }
    loadedModelId = spec.id;
    if (downloadFailed) {
      return null;
    }
    try {
      boolean download = autoDownloadModel;
      File local = OcrDetectionModels.findLocal(spec, overridePath);
      if (local == null && download) {
        modelStatus = "downloading " + spec.id;
        if (opencv != null) {
          opencv.info("downloading OCR detection model %s", spec.id);
        }
      }
      File file = OcrDetectionModels.resolve(spec, overridePath, download);
      if (file == null) {
        modelStatus = "missing " + spec.id + (download ? "" : " (auto-download off)");
        return null;
      }
      loadedModelPath = file.getAbsolutePath();
      return loadedModelPath;
    } catch (Exception e) {
      downloadFailed = true;
      modelStatus = "download failed: " + e.getMessage();
      log.warn("could not resolve OCR model {}", spec.id, e);
      return null;
    }
  }

  private OcrDetectionModels.Spec selectedSpec() {
    return OcrDetectionModels.get(OcrDetectionModels.normalize(detectionModel, detector));
  }

  private void resetDetectors() {
    eastDetector = null;
    dbDetector = null;
    loadedModelId = null;
    loadedModelPath = "";
    downloadFailed = false;
  }

  private void refreshInstalledModels() {
    installedDetectionModels = OcrDetectionModels.installedIds();
  }

  @Override
  public void release() {
    resetDetectors();
    tesseract = null;
    super.release();
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    StringBuilder fullText = new StringBuilder();
    Font previousFont = graphics.getFont();
    graphics.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
    if (classifications != null) {
      for (DetectedText rr : classifications) {
        if (rr.box == null) {
          continue;
        }
        Rect bR = rr.box.boundingRect();
        graphics.setColor(Color.GREEN);
        graphics.drawRect(bR.x(), bR.y(), bR.width(), bR.height());
        graphics.setColor(Color.RED);
        String label = rr.text == null ? "" : rr.text;
        int yText = bR.y() + bR.height() / 2 + fontSize / 2;
        graphics.drawString(label, bR.x(), yText);
        if (label.length() > 0) {
          fullText.append(label).append(" ");
        }
      }
    }
    graphics.setColor(Color.YELLOW);
    String overlay = lastText != null && lastText.length() > 0 ? lastText : fullText.toString().trim();
    graphics.drawString(overlay, 20, 60);
    graphics.setFont(previousFont);
    return image;
  }

}
