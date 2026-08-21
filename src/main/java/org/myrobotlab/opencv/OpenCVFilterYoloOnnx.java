package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.CV_32FC3;
import static org.bytedeco.opencv.global.opencv_dnn.NMSBoxes;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNet;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.DetectionModel;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.myrobotlab.document.Classification;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

/**
 * ONNX YOLO object detector. Default weights are OpenCV Zoo YOLOX-s (COCO),
 * decoded the same way as {@code opencv_zoo/models/object_detection_yolox}.
 * <p>
 * Set {@link #decoder} to {@code detection_model} to use OpenCV's
 * {@link DetectionModel} instead (SSD / Darknet-style YOLO ONNX).
 */
public class OpenCVFilterYoloOnnx extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterYoloOnnx.class);

  public static final String DECODER_YOLOX = "yolox";
  public static final String DECODER_DETECTION_MODEL = "detection_model";

  static {
    Loader.load(opencv_dnn.class);
  }

  public static final String[] COCO_NAMES = { "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
      "truck", "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
      "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie",
      "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
      "surfboard", "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
      "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant",
      "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
      "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush" };

  public String modelId = OpenCvZooModels.YOLOX;

  public String modelFile = "";

  public boolean autoDownloadModel = true;

  /**
   * {@code yolox} (OpenCV Zoo) or {@code detection_model} (generic ONNX SSD/YOLO).
   */
  public String decoder = DECODER_YOLOX;

  public float confThreshold = 0.5f;

  public float nmsThreshold = 0.5f;

  public float objThreshold = 0.5f;

  public int inputWidth = 640;

  public int inputHeight = 640;

  public int minIntervalMs = 200;

  public String modelStatus = "";

  public String loadedModelPath = "";

  public ArrayList<Classification> lastResult = new ArrayList<>();

  transient private Net net;

  transient private DetectionModel detectionModel;

  transient private String loadedPath;

  transient private boolean loadFailed;

  transient private long lastInferTs;

  transient private float[] gridsX;

  transient private float[] gridsY;

  transient private float[] strides;

  transient private final DecimalFormat df2 = new DecimalFormat("#.###");

  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterYoloOnnx() {
    super();
  }

  public OpenCVFilterYoloOnnx(String name) {
    super(name);
  }

  public OpenCVFilterYoloOnnx(String filterName, String sourceKey) {
    super(filterName, sourceKey);
  }

  public String installSelectedModel() {
    try {
      File file = OpenCvZooModels.resolve(modelId, emptyToNull(modelFile), true);
      if (file == null) {
        modelStatus = "YOLO ONNX model missing";
        return null;
      }
      loadedModelPath = file.getAbsolutePath();
      modelStatus = "cached " + file.getName();
      releaseNets();
      loadFailed = false;
      broadcastFilterState();
      return loadedModelPath;
    } catch (Exception e) {
      modelStatus = "download failed: " + e.getMessage();
      log.warn("YOLO ONNX install failed", e);
      return null;
    }
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    long now = System.currentTimeMillis();
    if (lastInferTs != 0 && now - lastInferTs < Math.max(0, minIntervalMs)) {
      return image;
    }
    if (!ensureModel()) {
      return image;
    }
    try {
      lastResult = DECODER_DETECTION_MODEL.equalsIgnoreCase(decoder) ? detectWithDetectionModel(image) : detectYolox(image);
      lastInferTs = System.currentTimeMillis();
      publishLast();
    } catch (Exception e) {
      log.warn("YOLO ONNX inference failed", e);
      modelStatus = "infer failed: " + e.getMessage();
    }
    return image;
  }

  private void publishLast() {
    if (lastResult == null || lastResult.isEmpty()) {
      return;
    }
    Map<String, List<Classification>> ret = new TreeMap<>();
    for (Classification c : lastResult) {
      ret.computeIfAbsent(c.getLabel(), k -> new ArrayList<>()).add(c);
    }
    invoke("publishClassification", ret);
  }

  private boolean ensureModel() {
    if (net != null || detectionModel != null) {
      return true;
    }
    if (loadFailed) {
      return false;
    }
    try {
      File file = OpenCvZooModels.resolve(modelId, emptyToNull(modelFile), autoDownloadModel);
      if (file == null) {
        modelStatus = autoDownloadModel ? "YOLO ONNX missing" : "YOLO ONNX not cached (enable auto-download)";
        loadFailed = true;
        return false;
      }
      loadedModelPath = file.getAbsolutePath();
      loadedPath = loadedModelPath;
      if (DECODER_DETECTION_MODEL.equalsIgnoreCase(decoder)) {
        detectionModel = new DetectionModel(loadedPath);
        detectionModel.setInputParams(1.0 / 255.0, new Size(inputWidth, inputHeight), new Scalar(0.0), true, false);
        modelStatus = "DetectionModel loaded";
      } else {
        net = readNet(loadedPath);
        buildYoloxAnchors();
        modelStatus = "YOLOX loaded";
      }
      log.info("YOLO ONNX loaded from {} decoder={}", loadedPath, decoder);
      return true;
    } catch (Exception e) {
      loadFailed = true;
      modelStatus = "load failed: " + e.getMessage();
      log.warn("could not load YOLO ONNX", e);
      return false;
    }
  }

  private ArrayList<Classification> detectYolox(IplImage image) {
    ArrayList<Classification> found = new ArrayList<>();
    Mat bgr = converter.toMat(image);
    Mat rgb = new Mat();
    cvtColor(bgr, rgb, COLOR_BGR2RGB);
    float[] ratioHolder = new float[1];
    Mat padded = letterbox(rgb, inputWidth, inputHeight, ratioHolder);
    float ratio = ratioHolder[0];
    Mat blob = blobFromImage(padded, 1.0, new Size(inputWidth, inputHeight), new Scalar(0.0), false, false, CV_32F);
    net.setInput(blob);
    Mat output = net.forward();
    decodeYolox(output, ratio, image.width(), image.height(), found);
    blob.close();
    output.close();
    padded.close();
    rgb.close();
    return found;
  }

  private ArrayList<Classification> detectWithDetectionModel(IplImage image) {
    ArrayList<Classification> found = new ArrayList<>();
    Mat frame = converter.toMat(image);
    IntPointer classIds = new IntPointer();
    FloatPointer confidences = new FloatPointer();
    RectVector boxes = new RectVector();
    detectionModel.detect(frame, classIds, confidences, boxes, confThreshold, nmsThreshold);
    long n = boxes.size();
    for (long i = 0; i < n; i++) {
      int ci = classIds.get((int) i);
      String label = className(ci);
      Rect box = boxes.get(i);
      Classification obj = new Classification(label, confidences.get((int) i),
          new Rectangle(box.x(), box.y(), box.width(), box.height()));
      found.add(obj);
    }
    return found;
  }

  private void decodeYolox(Mat output, float ratio, int imgW, int imgH, ArrayList<Classification> found) {
    if (output == null || output.empty() || gridsX == null) {
      return;
    }
    int n = gridsX.length;
    FloatIndexer idx = output.createIndexer();
    try {
      ArrayList<Rect> rawBoxes = new ArrayList<>();
      ArrayList<Float> scores = new ArrayList<>();
      ArrayList<Integer> classes = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        float obj = getOut(idx, output, i, 4);
        if (obj < objThreshold) {
          continue;
        }
        int best = 0;
        float bestScore = 0;
        int channels = (int) (output.dims() == 3 ? output.size(2) : output.cols());
        if (output.dims() == 3 && output.size(1) == 85) {
          channels = (int) output.size(1);
        }
        int numClasses = Math.min(COCO_NAMES.length, channels - 5);
        for (int c = 0; c < numClasses; c++) {
          float s = obj * getOut(idx, output, i, 5 + c);
          if (s > bestScore) {
            bestScore = s;
            best = c;
          }
        }
        if (bestScore < confThreshold) {
          continue;
        }
        float cx = (getOut(idx, output, i, 0) + gridsX[i]) * strides[i];
        float cy = (getOut(idx, output, i, 1) + gridsY[i]) * strides[i];
        float bw = (float) (Math.exp(getOut(idx, output, i, 2)) * strides[i]);
        float bh = (float) (Math.exp(getOut(idx, output, i, 3)) * strides[i]);
        float x = (cx - bw / 2f) / ratio;
        float y = (cy - bh / 2f) / ratio;
        float w = bw / ratio;
        float h = bh / ratio;
        int ix = clamp((int) x, 0, imgW - 1);
        int iy = clamp((int) y, 0, imgH - 1);
        int iw = clamp((int) w, 1, imgW - ix);
        int ih = clamp((int) h, 1, imgH - iy);
        rawBoxes.add(new Rect(ix, iy, iw, ih));
        scores.add(bestScore);
        classes.add(best);
      }
      if (rawBoxes.isEmpty()) {
        return;
      }
      RectVector boxesRV = new RectVector();
      for (Rect r : rawBoxes) {
        boxesRV.push_back(r);
      }
      FloatPointer confidencesFV = Util.arrayListToFloatPointer(scores);
      IntPointer indicesIp = new IntPointer();
      NMSBoxes(boxesRV, confidencesFV, confThreshold, nmsThreshold, indicesIp);
      for (int m = 0; m < indicesIp.limit(); m++) {
        int i = indicesIp.get(m);
        Rect box = rawBoxes.get(i);
        String label = className(classes.get(i));
        found.add(new Classification(label, scores.get(i),
            new Rectangle(box.x(), box.y(), box.width(), box.height())));
      }
    } finally {
      idx.release();
    }
  }

  /**
   * YOLOX zoo output is {@code [1, 8400, 85]} or {@code [8400, 85]}. Some
   * exports swap the last two dims.
   */
  private float getOut(FloatIndexer idx, Mat output, int row, int ch) {
    if (output.dims() == 3) {
      long d1 = output.size(1);
      long d2 = output.size(2);
      if (d1 == 85 && d2 >= row) {
        return idx.get(0, ch, row);
      }
      return idx.get(0, row, ch);
    }
    if (output.cols() == 85) {
      return idx.get(row, ch);
    }
    return idx.get(ch, row);
  }

  private void buildYoloxAnchors() {
    int[] strideVals = { 8, 16, 32 };
    ArrayList<Float> gx = new ArrayList<>();
    ArrayList<Float> gy = new ArrayList<>();
    ArrayList<Float> st = new ArrayList<>();
    for (int stride : strideVals) {
      int hsize = inputHeight / stride;
      int wsize = inputWidth / stride;
      // match numpy meshgrid(arange(hsize), arange(wsize)) used in opencv_zoo
      for (int y = 0; y < wsize; y++) {
        for (int x = 0; x < hsize; x++) {
          gx.add((float) x);
          gy.add((float) y);
          st.add((float) stride);
        }
      }
    }
    gridsX = toArray(gx);
    gridsY = toArray(gy);
    strides = toArray(st);
  }

  private Mat letterbox(Mat src, int targetW, int targetH, float[] ratioOut) {
    int h = src.rows();
    int w = src.cols();
    float ratio = Math.min(targetH / (float) h, targetW / (float) w);
    int nw = Math.max(1, Math.round(w * ratio));
    int nh = Math.max(1, Math.round(h * ratio));
    Mat resized = new Mat();
    resize(src, resized, new Size(nw, nh), 0, 0, INTER_LINEAR);
    Mat resizedF = new Mat();
    resized.convertTo(resizedF, CV_32F);
    Mat padded = new Mat(targetH, targetW, CV_32FC3, new Scalar(114.0, 114.0, 114.0, 0.0));
    Mat roi = new Mat(padded, new Rect(0, 0, nw, nh));
    resizedF.copyTo(roi);
    resized.close();
    resizedF.close();
    ratioOut[0] = ratio;
    return padded;
  }

  private String className(int id) {
    if (id >= 0 && id < COCO_NAMES.length) {
      String label = COCO_NAMES[id];
      if (opencv != null) {
        String localized = opencv.localize(label);
        if (localized != null) {
          return localized;
        }
      }
      return label;
    }
    return "class-" + id;
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    if (lastResult == null) {
      return image;
    }
    for (Classification obj : lastResult) {
      Rectangle bb = obj.getBoundingBox();
      if (bb == null) {
        continue;
      }
      String label = obj.getLabel() + " (" + df2.format(obj.getConfidence() * 100) + "%)";
      int x = (int) bb.x;
      int y = (int) bb.y;
      int width = (int) bb.width;
      int height = (int) bb.height;
      graphics.setColor(Color.BLACK);
      graphics.drawRect(x, y, width, height);
      graphics.fillRect(x, Math.max(0, y - 20), Math.max(40, 7 * label.length()), 20);
      graphics.setColor(Color.WHITE);
      graphics.drawString(label, x + 6, Math.max(12, y - 6));
    }
    return image;
  }

  @Override
  public void release() {
    super.release();
    releaseNets();
    converter.close();
  }

  private void releaseNets() {
    if (net != null) {
      net.close();
      net = null;
    }
    if (detectionModel != null) {
      detectionModel.close();
      detectionModel = null;
    }
  }

  private static float[] toArray(List<Float> list) {
    float[] a = new float[list.size()];
    for (int i = 0; i < list.size(); i++) {
      a[i] = list.get(i);
    }
    return a;
  }

  private static int clamp(int v, int min, int max) {
    return Math.max(min, Math.min(max, v));
  }

  private static String emptyToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

}
