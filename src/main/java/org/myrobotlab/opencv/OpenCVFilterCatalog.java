package org.myrobotlab.opencv;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.myrobotlab.service.OpenCV;

/**
 * Aggregates {@link OpenCVFilter#catalogInfo()} from each filter class listed
 * in {@link OpenCV#POSSIBLE_FILTERS}. Used by WebGui and
 * {@code OpenCV.getPossibleFilterInfo()}.
 * <p>
 * Types with no {@code OpenCVFilter*} class (legacy names) have fallback copy
 * here so the UI can still explain why addFilter will fail.
 */
public class OpenCVFilterCatalog {

  private static final String FILTER_PACKAGE = "org.myrobotlab.opencv.OpenCVFilter";

  private static final Map<String, OpenCVFilterInfo> MISSING_TYPES = new LinkedHashMap<>();

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
    for (String type : OpenCV.POSSIBLE_FILTERS) {
      catalog.put(type, get(type));
    }
    return Collections.unmodifiableMap(catalog);
  }

  public static OpenCVFilterInfo get(String type) {
    if (type == null) {
      return null;
    }
    try {
      Class<?> clazz = Class.forName(FILTER_PACKAGE + type);
      if (!OpenCVFilter.class.isAssignableFrom(clazz)) {
        return MISSING_TYPES.get(type);
      }
      @SuppressWarnings("unchecked")
      Class<? extends OpenCVFilter> filterClass = (Class<? extends OpenCVFilter>) clazz;
      return OpenCVFilter.lookupCatalogInfo(filterClass);
    } catch (ClassNotFoundException e) {
      return MISSING_TYPES.get(type);
    }
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
      java.nio.file.Files.write(java.nio.file.Paths.get(args[0]), json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } else {
      System.out.print(json);
    }
  }

}
