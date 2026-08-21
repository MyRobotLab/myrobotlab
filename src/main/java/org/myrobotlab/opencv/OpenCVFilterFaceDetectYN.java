package org.myrobotlab.opencv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;
import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

/**
 * YuNet face detector ({@code cv::FaceDetectorYN}) from OpenCV Zoo.
 * <p>
 * Replaces Haar ({@link OpenCVFilterFaceDetect}) and the 2016 Caffe SSD
 * ({@link OpenCVFilterFaceDetectDNN}) as the OpenCV 4.x face detector. The
 * ONNX weights (~233 KB) are downloaded on demand into
 * {@code data/OpenCV/zoo_models/}.
 */
public class OpenCVFilterFaceDetectYN extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceDetectYN.class);

  private static final String FACE_LABEL = "face";

  static {
    Loader.load(opencv_objdetect.class);
  }

  public String modelId = OpenCvZooModels.YUNET;

  public String modelFile = "";

  public boolean autoDownloadModel = true;

  public float scoreThreshold = 0.7f;

  public float nmsThreshold = 0.3f;

  public int topK = 5000;

  public String modelStatus = "";

  public String loadedModelPath = "";

  final List<Rectangle> bb = new ArrayList<>();

  final Map<String, List<Classification>> classifications = new TreeMap<>();

  transient final List<float[]> landmarks = new ArrayList<>();

  transient private FaceDetectorYN detector;

  transient private String loadedPath;

  transient private int loadedWidth;

  transient private int loadedHeight;

  transient private boolean loadFailed;

  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterFaceDetectYN() {
    super();
  }

  public OpenCVFilterFaceDetectYN(String name) {
    super(name);
  }

  public OpenCVFilterFaceDetectYN(String filterName, String sourceKey) {
    super(filterName, sourceKey);
  }

  public String installSelectedModel() {
    try {
      File file = OpenCvZooModels.resolve(modelId, emptyToNull(modelFile), true);
      if (file == null) {
        modelStatus = "YuNet model missing";
        return null;
      }
      loadedModelPath = file.getAbsolutePath();
      modelStatus = "cached " + file.getName();
      detector = null;
      loadFailed = false;
      broadcastFilterState();
      return loadedModelPath;
    } catch (Exception e) {
      modelStatus = "download failed: " + e.getMessage();
      log.warn("YuNet install failed", e);
      return null;
    }
  }

  @Override
  public void imageChanged(IplImage image) {
    if (detector != null && image != null) {
      detector.setInputSize(new Size(image.width(), image.height()));
      loadedWidth = image.width();
      loadedHeight = image.height();
    }
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    bb.clear();
    classifications.clear();
    landmarks.clear();
    if (!ensureDetector(image)) {
      return image;
    }
    CloseableFrameConverter conv = converter;
    Mat src = conv.toMat(image);
    if (src == null || src.empty()) {
      return image;
    }
    if (src.cols() != loadedWidth || src.rows() != loadedHeight) {
      detector.setInputSize(new Size(src.cols(), src.rows()));
      loadedWidth = src.cols();
      loadedHeight = src.rows();
    }
    Mat faces = new Mat();
    try {
      detector.detect(src, faces);
      parseFaces(faces, src.cols(), src.rows());
    } catch (Exception e) {
      log.warn("YuNet detect failed", e);
      modelStatus = "detect failed: " + e.getMessage();
    } finally {
      faces.close();
    }
    if (!classifications.isEmpty()) {
      publishClassification(classifications);
    }
    return image;
  }

  private boolean ensureDetector(IplImage image) {
    if (detector != null) {
      return true;
    }
    if (loadFailed) {
      return false;
    }
    try {
      File file = OpenCvZooModels.resolve(modelId, emptyToNull(modelFile), autoDownloadModel);
      if (file == null) {
        modelStatus = autoDownloadModel ? "YuNet model missing" : "YuNet not cached (enable auto-download)";
        loadFailed = true;
        return false;
      }
      loadedModelPath = file.getAbsolutePath();
      detector = FaceDetectorYN.create(loadedModelPath, "", new Size(image.width(), image.height()), scoreThreshold,
          nmsThreshold, topK, 0, 0);
      loadedPath = loadedModelPath;
      loadedWidth = image.width();
      loadedHeight = image.height();
      modelStatus = "YuNet loaded";
      log.info("YuNet loaded from {}", loadedPath);
      return detector != null;
    } catch (Exception e) {
      loadFailed = true;
      modelStatus = "load failed: " + e.getMessage();
      log.warn("could not create FaceDetectorYN", e);
      return false;
    }
  }

  private void parseFaces(Mat faces, int w, int h) {
    if (faces == null || faces.empty() || faces.rows() < 1) {
      data.putBoundingBoxArray(bb);
      return;
    }
    FloatIndexer idx = faces.createIndexer();
    try {
      for (int i = 0; i < faces.rows(); i++) {
        float x = idx.get(i, 0);
        float y = idx.get(i, 1);
        float bw = idx.get(i, 2);
        float bh = idx.get(i, 3);
        float score = idx.get(i, 14);
        Rectangle rect = new Rectangle(x, y, bw, bh);
        bb.add(rect);
        double centerX = ((rect.x + rect.width / 2) - w / 2.0) / w;
        double centerY = -1 * ((rect.y + rect.height / 2) - h / 2.0) / h;
        Classification c = new Classification(FACE_LABEL, score, rect, centerX, centerY);
        if (getOpenCV() != null) {
          c.setTs(getOpenCV().getFrameStartTs());
        }
        classifications.computeIfAbsent(FACE_LABEL, k -> new ArrayList<>()).add(c);
        float[] lm = new float[10];
        for (int p = 0; p < 5; p++) {
          lm[p * 2] = idx.get(i, 4 + p * 2);
          lm[p * 2 + 1] = idx.get(i, 5 + p * 2);
        }
        landmarks.add(lm);
      }
    } finally {
      idx.release();
    }
    data.putBoundingBoxArray(bb);
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    graphics.setColor(Color.GREEN);
    int i = 0;
    List<Classification> faces = classifications.get(FACE_LABEL);
    if (faces != null) {
      for (Classification c : faces) {
        Rectangle rect = c.getBoundingBox();
        int centerX = (int) (rect.x + rect.width / 2);
        int centerY = (int) (rect.y + rect.height / 2);
        graphics.drawRect((int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height);
        graphics.drawString(String.format("face %.2f", c.getConfidence()), (int) rect.x, Math.max(12, (int) rect.y - 4));
        graphics.drawLine(centerX - 2, centerY, centerX + 2, centerY);
        graphics.drawLine(centerX, centerY - 2, centerX, centerY + 2);
        if (i < landmarks.size()) {
          float[] lm = landmarks.get(i);
          graphics.setColor(Color.YELLOW);
          for (int p = 0; p < 5; p++) {
            int lx = Math.round(lm[p * 2]);
            int ly = Math.round(lm[p * 2 + 1]);
            graphics.fillOval(lx - 2, ly - 2, 4, 4);
          }
          graphics.setColor(Color.GREEN);
        }
        i++;
      }
    }
    List<Point2df> centers = new ArrayList<>();
    for (Rectangle r : bb) {
      centers.add(new Point2df(r.x + r.width / 2f, r.y + r.height / 2f, 1f));
    }
    if (!centers.isEmpty()) {
      data.put("points", centers);
    }
    return image;
  }

  @Override
  public void release() {
    super.release();
    if (detector != null) {
      detector.close();
      detector = null;
    }
    converter.close();
  }

  private static String emptyToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

}
