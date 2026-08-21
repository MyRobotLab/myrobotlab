package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_objdetect.DICT_4X4_50;
import static org.bytedeco.opencv.global.opencv_objdetect.DICT_4X4_100;
import static org.bytedeco.opencv.global.opencv_objdetect.DICT_5X5_50;
import static org.bytedeco.opencv.global.opencv_objdetect.DICT_6X6_250;
import static org.bytedeco.opencv.global.opencv_objdetect.DICT_7X7_250;
import static org.bytedeco.opencv.global.opencv_objdetect.DICT_ARUCO_ORIGINAL;
import static org.bytedeco.opencv.global.opencv_objdetect.getPredefinedDictionary;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_objdetect.ArucoDetector;
import org.bytedeco.opencv.opencv_objdetect.DetectorParameters;
import org.bytedeco.opencv.opencv_objdetect.Dictionary;
import org.bytedeco.opencv.opencv_objdetect.QRCodeDetectorAruco;
import org.bytedeco.opencv.opencv_objdetect.RefineParameters;
import org.myrobotlab.document.Classification;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

/**
 * QR codes via OpenCV's ArUco-based {@link QRCodeDetectorAruco}, plus optional
 * ArUco fiducial markers ({@link ArucoDetector}).
 * <p>
 * {@link #mode}: {@code qr} (default), {@code aruco}, or {@code both}.
 */
public class OpenCVFilterQrCode extends OpenCVFilter {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterQrCode.class);

  public static final String MODE_QR = "qr";
  public static final String MODE_ARUCO = "aruco";
  public static final String MODE_BOTH = "both";

  static {
    Loader.load(opencv_objdetect.class);
  }

  /**
   * {@code qr}, {@code aruco}, or {@code both}.
   */
  public String mode = MODE_QR;

  /**
   * Predefined ArUco dictionary name, e.g. {@code DICT_4X4_50}.
   */
  public String dictionary = "DICT_4X4_50";

  public String lastText = "";

  public ArrayList<Classification> lastResult = new ArrayList<>();

  transient private QRCodeDetectorAruco qrDetector;

  transient private ArucoDetector arucoDetector;

  transient private String loadedDictionary;

  transient private CloseableFrameConverter converter = new CloseableFrameConverter();

  public OpenCVFilterQrCode() {
    super();
  }

  public OpenCVFilterQrCode(String name) {
    super(name);
  }

  public OpenCVFilterQrCode(String filterName, String sourceKey) {
    super(filterName, sourceKey);
  }

  @Override
  public void imageChanged(IplImage image) {
  }

  @Override
  public IplImage process(IplImage image) throws InterruptedException {
    lastResult = new ArrayList<>();
    StringBuilder texts = new StringBuilder();
    Mat frame = converter.toMat(image);
    if (frame == null || frame.isNull() || frame.empty()) {
      return image;
    }
    String m = mode == null ? MODE_QR : mode.toLowerCase();
    if (MODE_QR.equals(m) || MODE_BOTH.equals(m)) {
      detectQr(frame, texts);
    }
    if (MODE_ARUCO.equals(m) || MODE_BOTH.equals(m)) {
      detectAruco(frame, texts);
    }
    lastText = texts.toString().trim();
    if (!lastResult.isEmpty()) {
      Map<String, List<Classification>> ret = new TreeMap<>();
      for (Classification c : lastResult) {
        ret.computeIfAbsent(c.getLabel(), k -> new ArrayList<>()).add(c);
      }
      invoke("publishClassification", ret);
      List<Rectangle> boxes = new ArrayList<>();
      for (Classification c : lastResult) {
        if (c.getBoundingBox() != null) {
          boxes.add(c.getBoundingBox());
        }
      }
      data.putBoundingBoxArray(boxes);
    }
    return image;
  }

  private void detectQr(Mat frame, StringBuilder texts) {
    if (qrDetector == null) {
      qrDetector = new QRCodeDetectorAruco();
    }
    StringVector decoded = new StringVector();
    Mat points = new Mat();
    MatVector straight = new MatVector();
    try {
      boolean ok = qrDetector.detectAndDecodeMulti(frame, decoded, points, straight);
      if (!ok || decoded.size() == 0) {
        return;
      }
      int n = (int) decoded.size();
      for (int i = 0; i < n; i++) {
        String text = decoded.get(i) != null ? decoded.get(i).getString() : "";
        if (text == null) {
          text = "";
        }
        Rectangle box = quadBounds(points, i);
        String label = text.isEmpty() ? "qr" : text;
        Classification c = new Classification(label, 1.0f, box);
        lastResult.add(c);
        if (!text.isEmpty()) {
          if (texts.length() > 0) {
            texts.append(" | ");
          }
          texts.append(text);
        }
      }
    } catch (Exception e) {
      log.warn("QRCodeDetectorAruco failed", e);
    } finally {
      decoded.close();
      points.close();
      straight.close();
    }
  }

  private void detectAruco(Mat frame, StringBuilder texts) {
    ensureAruco();
    MatVector corners = new MatVector();
    Mat ids = new Mat();
    try {
      arucoDetector.detectMarkers(frame, corners, ids);
      if (ids == null || ids.empty() || corners.size() == 0) {
        return;
      }
      IntIndexer idIdx = ids.createIndexer();
      try {
        int n = (int) Math.min(corners.size(), ids.rows() > 0 ? ids.rows() : ids.total());
        for (int i = 0; i < n; i++) {
          int id = idIdx.get(i);
          Rectangle box = markerBounds(corners.get(i));
          String label = "aruco-" + id;
          lastResult.add(new Classification(label, 1.0f, box));
          if (texts.length() > 0) {
            texts.append(" | ");
          }
          texts.append(label);
        }
      } finally {
        idIdx.release();
      }
    } catch (Exception e) {
      log.warn("ArucoDetector failed", e);
    } finally {
      corners.close();
      ids.close();
    }
  }

  private void ensureAruco() {
    String want = dictionary == null ? "DICT_4X4_50" : dictionary;
    if (arucoDetector != null && want.equals(loadedDictionary)) {
      return;
    }
    if (arucoDetector != null) {
      arucoDetector.close();
    }
    Dictionary dict = getPredefinedDictionary(dictionaryId(want));
    arucoDetector = new ArucoDetector(dict, new DetectorParameters(), new RefineParameters());
    loadedDictionary = want;
  }

  private static int dictionaryId(String name) {
    if (name == null) {
      return DICT_4X4_50;
    }
    switch (name.trim().toUpperCase()) {
      case "DICT_4X4_100":
        return DICT_4X4_100;
      case "DICT_5X5_50":
        return DICT_5X5_50;
      case "DICT_6X6_250":
        return DICT_6X6_250;
      case "DICT_7X7_250":
        return DICT_7X7_250;
      case "DICT_ARUCO_ORIGINAL":
        return DICT_ARUCO_ORIGINAL;
      case "DICT_4X4_50":
      default:
        return DICT_4X4_50;
    }
  }

  private Rectangle quadBounds(Mat points, int index) {
    if (points == null || points.empty()) {
      return new Rectangle(0, 0, 1, 1);
    }
    FloatIndexer idx = points.createIndexer();
    try {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      for (int c = 0; c < 4; c++) {
        float x;
        float y;
        try {
          x = idx.get(index, c, 0);
          y = idx.get(index, c, 1);
        } catch (Exception e) {
          x = idx.get(index, c * 2);
          y = idx.get(index, c * 2 + 1);
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
      return new Rectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    } finally {
      idx.release();
    }
  }

  private Rectangle markerBounds(Mat corner) {
    if (corner == null || corner.empty()) {
      return new Rectangle(0, 0, 1, 1);
    }
    FloatIndexer idx = corner.createIndexer();
    try {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      long n = Math.max(4, corner.rows() * corner.cols());
      for (int i = 0; i < 4 && i < n; i++) {
        float x;
        float y;
        try {
          x = idx.get(i, 0);
          y = idx.get(i, 1);
        } catch (Exception e) {
          x = idx.get(0, i, 0);
          y = idx.get(0, i, 1);
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
      return new Rectangle(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    } finally {
      idx.release();
    }
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
      boolean aruco = obj.getLabel() != null && obj.getLabel().startsWith("aruco-");
      graphics.setColor(aruco ? Color.CYAN : Color.GREEN);
      graphics.drawRect((int) bb.x, (int) bb.y, (int) bb.width, (int) bb.height);
      graphics.setColor(Color.WHITE);
      graphics.drawString(obj.getLabel(), (int) bb.x, Math.max(12, (int) bb.y - 4));
    }
    return image;
  }

  @Override
  public void release() {
    super.release();
    if (qrDetector != null) {
      qrDetector.close();
      qrDetector = null;
    }
    if (arucoDetector != null) {
      arucoDetector.close();
      arucoDetector = null;
    }
    converter.close();
  }

}
