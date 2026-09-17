package org.myrobotlab.opencv;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.codec.CodecUtils;

/**
 * Aggregates {@link OpenCVFilter#catalogInfo()} from each filter class listed
 * in {@link #POSSIBLE_FILTERS}. Used by WebGui and
 * {@code OpenCV.getPossibleFilterInfo()}.
 * <p>
 * Types with no {@code OpenCVFilter*} class (legacy names) have fallback copy
 * here so the UI can still explain why addFilter will fail.
 * <p>
 * Class initialization of some filters loads JavaCV natives. This aggregator
 * does not initialize those classes when it can avoid it, and falls back to
 * {@code resource/OpenCV/filter-catalog.json} if native load fails (e.g. CI).
 */
public class OpenCVFilterCatalog {

  /**
   * Canonical filter type names shown in the WebGui. Keep in sync with
   * {@code OpenCV.POSSIBLE_FILTERS} (that field aliases this array).
   */
  public static final String[] POSSIBLE_FILTERS = { "AdaptiveThreshold", "AddMask", "Affine", "And", "BlurDetector",
      "BoundingBoxToFile", "Canny", "ColorTrack", "Copy", "CreateHistogram", "Detector", "Dilate", "DL4J", "DL4JTransfer",
      "Erode", "FaceDetect", "FaceDetectDNN", "FaceDetectYN", "FaceRecognizer", "FaceTraining", "Fauvist", "FindContours",
      "Flip", "FloodFill", "FloorFinder", "FloorFinder2", "GoodFeaturesToTrack", "Gray", "HoughLines2", "Hsv",
      "ImageSegmenter", "Input", "InRange", "Invert", "KinectDepth", "KinectDepthMask", "KinectNavigate",
      "KinectPointCloud", "LKOpticalTrack", "Lloyd", "Mask", "MatchTemplate", "MiniXception", "MotionDetect", "Mouse",
      "Ocr", "Output", "Overlay", "PyramidDown", "PyramidUp", "QrCode", "ResetImageRoi", "Resize", "SampleArray",
      "SampleImage", "SetImageROI", "SimpleBlobDetector", "Smooth", "Solr", "Split", "SURF", "Tesseract", "TextDetector",
      "Threshold", "Tracker", "Transpose", "Undistort", "Yolo", "YoloOnnx", "DepthToPointCloud" };

  private static final String FILTER_PACKAGE = "org.myrobotlab.opencv.OpenCVFilter";

  private static final String JSON_RESOURCE = "/resource/OpenCV/filter-catalog.json";

  private static final Map<String, OpenCVFilterInfo> MISSING_TYPES = new LinkedHashMap<>();

  private static volatile Map<String, OpenCVFilterInfo> jsonCatalog;

  static {
    MISSING_TYPES.put("DL4J", OpenCVFilterInfo.of("DL4J",
        "Legacy Deeplearning4j image-classification filter. The OpenCVFilterDL4J class is not currently in this tree, so adding it will fail until the filter is restored.",
        "Prefer YoloOnnx or FaceDetectYN for current DNN vision. If you have an old DL4J pipeline, keep the matching Deeplearning4j service and model files.",
        "Not implemented in this build. Historically required the Deeplearning4j service and a trained model."));
    MISSING_TYPES.put("DL4JTransfer", OpenCVFilterInfo.of("DL4JTransfer",
        "Legacy Deeplearning4j transfer-learning filter. The implementation is not currently in this tree.",
        "Use FaceRecognizer / FaceTraining or an external trainer instead. Adding this type will fail until the class is restored.",
        OpenCVFilterInfo.DEP_NOT_IMPLEMENTED));
    MISSING_TYPES.put("Lloyd", OpenCVFilterInfo.of("Lloyd",
        "Placeholder name from an older Lloyd robot vision pipeline. There is no OpenCVFilterLloyd class in this build.",
        "Do not add this type — addFilter will fail. Use YoloOnnx, FaceDetectYN, Tracker, or a custom pipeline instead.",
        OpenCVFilterInfo.DEP_NOT_IMPLEMENTED));
    MISSING_TYPES.put("MiniXception", OpenCVFilterInfo.of("MiniXception",
        "Legacy Mini-Xception facial-emotion network. The OpenCVFilterMiniXception class is not in this tree (the old main() still mentions it).",
        "Adding this type will fail until the filter is restored. Use FaceDetectYN plus an external classifier if you need emotion labels.",
        "Not implemented in this build. Historically needed Mini-Xception weights."));
  }

  public static Map<String, OpenCVFilterInfo> getAll() {
    LinkedHashMap<String, OpenCVFilterInfo> catalog = new LinkedHashMap<>();
    for (String type : POSSIBLE_FILTERS) {
      catalog.put(type, get(type));
    }
    return Collections.unmodifiableMap(catalog);
  }

  public static OpenCVFilterInfo get(String type) {
    if (type == null) {
      return null;
    }
    OpenCVFilterInfo live = lookupLive(type);
    if (isComplete(live)) {
      return live;
    }
    OpenCVFilterInfo fromJson = jsonEntry(type);
    if (isComplete(fromJson)) {
      return fromJson;
    }
    if (live != null) {
      return live;
    }
    return MISSING_TYPES.get(type);
  }

  /**
   * Reads {@code catalogInfo()} from the filter class. Native {@code Loader.load}
   * in a static initializer runs when the method is invoked; failures return
   * null so {@link #get(String)} can use the JSON resource instead of the
   * generic {@link OpenCVFilter#lookupCatalogInfo} fallback.
   */
  private static OpenCVFilterInfo lookupLive(String type) {
    try {
      Class<?> clazz = Class.forName(FILTER_PACKAGE + type, false, OpenCVFilterCatalog.class.getClassLoader());
      java.lang.reflect.Method m = clazz.getDeclaredMethod("catalogInfo");
      if (!java.lang.reflect.Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 0) {
        return null;
      }
      m.setAccessible(true);
      return (OpenCVFilterInfo) m.invoke(null);
    } catch (ClassNotFoundException e) {
      return MISSING_TYPES.get(type);
    } catch (NoSuchMethodException e) {
      return MISSING_TYPES.get(type);
    } catch (Throwable t) {
      return null;
    }
  }

  private static boolean isComplete(OpenCVFilterInfo info) {
    return info != null && !isBlank(info.type) && !isBlank(info.description) && !isBlank(info.usage)
        && !isBlank(info.dependencies);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  @SuppressWarnings("unchecked")
  private static OpenCVFilterInfo jsonEntry(String type) {
    Map<String, OpenCVFilterInfo> catalog = loadJsonCatalog();
    if (catalog == null) {
      return null;
    }
    return catalog.get(type);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, OpenCVFilterInfo> loadJsonCatalog() {
    Map<String, OpenCVFilterInfo> cached = jsonCatalog;
    if (cached != null) {
      return cached;
    }
    synchronized (OpenCVFilterCatalog.class) {
      if (jsonCatalog != null) {
        return jsonCatalog;
      }
      try {
        String json = readCatalogJson();
        if (json == null || json.isBlank()) {
          jsonCatalog = Collections.emptyMap();
          return jsonCatalog;
        }
        Map<String, Object> raw = CodecUtils.fromJson(json, Map.class);
        LinkedHashMap<String, OpenCVFilterInfo> parsed = new LinkedHashMap<>();
        if (raw != null) {
          for (Map.Entry<String, Object> entry : raw.entrySet()) {
            parsed.put(entry.getKey(), toInfo(entry.getKey(), entry.getValue()));
          }
        }
        jsonCatalog = Collections.unmodifiableMap(parsed);
      } catch (Exception e) {
        jsonCatalog = Collections.emptyMap();
      }
      return jsonCatalog;
    }
  }

  @SuppressWarnings("unchecked")
  private static OpenCVFilterInfo toInfo(String key, Object value) {
    if (value instanceof OpenCVFilterInfo) {
      return (OpenCVFilterInfo) value;
    }
    if (!(value instanceof Map)) {
      return null;
    }
    Map<String, Object> map = (Map<String, Object>) value;
    OpenCVFilterInfo info = new OpenCVFilterInfo();
    info.type = stringVal(map.get("type"), key);
    info.description = stringVal(map.get("description"), null);
    info.usage = stringVal(map.get("usage"), null);
    info.dependencies = stringVal(map.get("dependencies"), null);
    return info;
  }

  private static String stringVal(Object value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String text = String.valueOf(value);
    return text.isEmpty() ? fallback : text;
  }

  private static String readCatalogJson() throws Exception {
    InputStream in = OpenCVFilterCatalog.class.getResourceAsStream(JSON_RESOURCE);
    if (in == null) {
      in = OpenCVFilterCatalog.class.getClassLoader().getResourceAsStream("resource/OpenCV/filter-catalog.json");
    }
    if (in != null) {
      try (InputStream stream = in) {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
    Path[] files = new Path[] { Path.of("src/main/resources/resource/OpenCV/filter-catalog.json"),
        Path.of("resource/OpenCV/filter-catalog.json") };
    for (Path file : files) {
      if (Files.isRegularFile(file)) {
        return Files.readString(file, StandardCharsets.UTF_8);
      }
    }
    return null;
  }

  private static String jsonEscape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
  }

  /**
   * Pretty JSON for the WebGui static resource
   * {@code resource/OpenCV/filter-catalog.json}.
   */
  public static String toJson() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    boolean first = true;
    for (Map.Entry<String, OpenCVFilterInfo> entry : getAll().entrySet()) {
      if (!first) {
        sb.append(",\n");
      }
      first = false;
      OpenCVFilterInfo info = entry.getValue();
      sb.append("  \"").append(jsonEscape(entry.getKey())).append("\": {\n");
      sb.append("    \"type\": \"").append(jsonEscape(info.type)).append("\",\n");
      sb.append("    \"description\": \"").append(jsonEscape(info.description)).append("\",\n");
      sb.append("    \"usage\": \"").append(jsonEscape(info.usage)).append("\",\n");
      sb.append("    \"dependencies\": \"").append(jsonEscape(info.dependencies)).append("\"\n");
      sb.append("  }");
    }
    sb.append("\n}\n");
    return sb.toString();
  }

  public static void main(String[] args) throws Exception {
    String json = toJson();
    if (args != null && args.length > 0) {
      Files.write(Path.of(args[0]), json.getBytes(StandardCharsets.UTF_8));
    } else {
      System.out.print(json);
    }
  }

}
