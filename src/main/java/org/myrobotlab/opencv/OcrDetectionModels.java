package org.myrobotlab.opencv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

/**
 * Catalog, cache, and download of scene-text detection models used by
 * {@link OpenCVFilterOcr}.
 * <p>
 * EAST is shipped with the OpenCV Ivy zip when that dependency is installed.
 * DBNet ONNX files are not; they are downloaded on demand into
 * {@code data/OpenCV/ocr_models/}.
 */
public class OcrDetectionModels {

  public final static Logger log = LoggerFactory.getLogger(OcrDetectionModels.class);

  public static final String NONE = "none";
  public static final String EAST = "east";
  public static final String DB_IC15_R18 = "db_ic15_r18";
  public static final String DB_IC15_R50 = "db_ic15_r50";
  public static final String DB_TD500_R18 = "db_td500_r18";
  public static final String DB_TD500_R50 = "db_td500_r50";

  private static final String CACHE_SUBDIR = "ocr_models";
  private static final String USER_AGENT = "MyRobotLab-Ocr/1.0";

  /**
   * One detector variant. {@link #detector} is {@code east}, {@code db}, or
   * {@code none}.
   */
  public static class Spec {
    public final String id;
    public final String detector;
    public final String filename;
    public final String label;
    public final int inputWidth;
    public final int inputHeight;
    public final long minBytes;
    public final String[] urls;

    Spec(String id, String detector, String filename, String label, int inputWidth, int inputHeight, long minBytes,
        String... urls) {
      this.id = id;
      this.detector = detector;
      this.filename = filename;
      this.label = label;
      this.inputWidth = inputWidth;
      this.inputHeight = inputHeight;
      this.minBytes = minBytes;
      this.urls = urls;
    }
  }

  private static final Map<String, Spec> CATALOG = new LinkedHashMap<>();
  private static final Map<String, String> ALIASES = new LinkedHashMap<>();

  static {
    add(new Spec(NONE, NONE, "", "Full frame (Tesseract only)", 0, 0, 0));
    add(new Spec(EAST, EAST, "frozen_east_text_detection.pb", "EAST scene-text detector", 320, 320, 20_000_000L,
        "https://github.com/oyyd/frozen_east_text_detection.pb/raw/master/frozen_east_text_detection.pb"));
    // OpenCV zoo commit that still hosts the ResNet-18 ONNX files (~48 MB)
    add(new Spec(DB_IC15_R18, "db", "text_detection_DB_IC15_resnet18_2021sep.onnx",
        "DBNet IC15 ResNet-18 (English, fast)", 1280, 736, 5_000_000L,
        "https://github.com/opencv/opencv_zoo/raw/b32d27c8f138ffd625efc52e2d82f8b7d54dabc7/models/text_detection_db/text_detection_DB_IC15_resnet18_2021sep.onnx"));
    add(new Spec(DB_IC15_R50, "db", "text_detection_DB_IC15_resnet50.onnx", "DBNet IC15 ResNet-50 (English)", 1280, 736,
        20_000_000L, "https://github.com/cocoa-xu/evision-misc/releases/download/v20240216/text_detection_DB_IC15_resnet50.onnx"));
    add(new Spec(DB_TD500_R18, "db", "text_detection_DB_TD500_resnet18_2021sep.onnx",
        "DBNet TD500 ResNet-18 (English + Chinese)", 736, 736, 5_000_000L,
        "https://github.com/opencv/opencv_zoo/raw/b32d27c8f138ffd625efc52e2d82f8b7d54dabc7/models/text_detection_db/text_detection_DB_TD500_resnet18_2021sep.onnx"));
    add(new Spec(DB_TD500_R50, "db", "text_detection_DB_TD500_resnet50.onnx", "DBNet TD500 ResNet-50 (English + Chinese)",
        736, 736, 20_000_000L,
        "https://github.com/cocoa-xu/evision-misc/releases/download/v20240216/text_detection_DB_TD500_resnet50.onnx"));

    alias("full", NONE);
    alias("none", NONE);
    alias("east", EAST);
    alias("db", DB_IC15_R18);
    alias("dbnet", DB_IC15_R18);
    alias("db18", DB_IC15_R18);
    alias("db_ic15_r18", DB_IC15_R18);
    alias("db50", DB_IC15_R50);
    alias("db_ic15_r50", DB_IC15_R50);
    alias("db_td500_r18", DB_TD500_R18);
    alias("db_td500_r50", DB_TD500_R50);
  }

  private static void add(Spec spec) {
    CATALOG.put(spec.id, spec);
  }

  private static void alias(String from, String to) {
    ALIASES.put(from, to);
  }

  public static List<Spec> all() {
    return Collections.unmodifiableList(new ArrayList<>(CATALOG.values()));
  }

  public static Spec get(String id) {
    String resolved = aliasOf(id);
    Spec spec = CATALOG.get(resolved);
    return spec != null ? spec : CATALOG.get(EAST);
  }

  public static boolean isCatalogId(String id) {
    return id != null && CATALOG.containsKey(aliasOf(id));
  }

  /**
   * Prefer an explicit catalog id. If the id is still the default {@code east}
   * but {@code detector} is a legacy {@code db}/{@code none} value, honor
   * detector so older scripts keep working.
   */
  public static String normalize(String detectionModel, String detector) {
    String model = aliasOf(detectionModel);
    String det = aliasOf(detector);
    if (model != null && CATALOG.containsKey(model)) {
      if (EAST.equals(model) && det != null && !EAST.equals(det) && CATALOG.containsKey(det)) {
        return det;
      }
      return model;
    }
    if (det != null && CATALOG.containsKey(det)) {
      return det;
    }
    return EAST;
  }

  public static String aliasOf(String raw) {
    if (raw == null) {
      return null;
    }
    String key = raw.trim().toLowerCase();
    if (key.isEmpty()) {
      return null;
    }
    String mapped = ALIASES.get(key);
    return mapped != null ? mapped : key;
  }

  public static File cacheDir() {
    return new File(Service.getDataDir("OpenCV"), CACHE_SUBDIR);
  }

  public static File cacheFile(Spec spec) {
    if (spec == null || spec.filename == null || spec.filename.isEmpty()) {
      return null;
    }
    return new File(cacheDir(), spec.filename);
  }

  public static boolean isInstalled(String id) {
    Spec spec = CATALOG.get(aliasOf(id));
    if (spec == null || NONE.equals(spec.detector)) {
      return true;
    }
    File local = findLocal(spec, null);
    return local != null && local.isFile() && local.length() >= spec.minBytes;
  }

  public static List<String> installedIds() {
    List<String> ids = new ArrayList<>();
    for (Spec spec : CATALOG.values()) {
      if (isInstalled(spec.id)) {
        ids.add(spec.id);
      }
    }
    return ids;
  }

  /**
   * Locate a model on disk, optionally downloading it into the OpenCV data
   * cache.
   *
   * @return absolute file, or null when missing and download is disabled / failed
   */
  public static File resolve(Spec spec, String overridePath, boolean autoDownload) throws IOException {
    if (spec == null || NONE.equals(spec.detector)) {
      return null;
    }
    File local = findLocal(spec, overridePath);
    if (local != null) {
      return local;
    }
    if (!autoDownload) {
      return null;
    }
    return download(spec);
  }

  /**
   * Search cache, Ivy resource zip, and an optional user path. Does not
   * download.
   */
  public static File findLocal(Spec spec, String overridePath) {
    if (spec == null || NONE.equals(spec.detector)) {
      return null;
    }
    List<File> candidates = new ArrayList<>();
    if (overridePath != null && !overridePath.trim().isEmpty()) {
      candidates.add(new File(overridePath.trim()));
    }
    File cached = cacheFile(spec);
    if (cached != null) {
      candidates.add(cached);
    }
    String resourceRoot = Service.getResourceDir(OpenCV.class);
    if (EAST.equals(spec.detector)) {
      candidates.add(new File("resource/OpenCV/east_text_detector/frozen_east_text_detection.pb"));
      candidates.add(new File(resourceRoot, "east_text_detector/frozen_east_text_detection.pb"));
      candidates.add(new File(resourceRoot, spec.filename));
    } else {
      candidates.add(new File("resource/OpenCV/db_text_detector", spec.filename));
      candidates.add(new File(resourceRoot, "db_text_detector/" + spec.filename));
      candidates.add(new File(resourceRoot, spec.filename));
    }
    String cachedUrl = OpenCVFilter.getCacheFile(spec.filename);
    if (cachedUrl != null) {
      candidates.add(new File(cachedUrl));
    }
    for (File f : candidates) {
      if (f != null && f.isFile() && f.length() >= spec.minBytes) {
        return f.getAbsoluteFile();
      }
    }
    return null;
  }

  public static File download(Spec spec) throws IOException {
    if (spec == null || spec.urls == null || spec.urls.length == 0) {
      throw new IOException("no download URL for model " + (spec == null ? "null" : spec.id));
    }
    File dest = cacheFile(spec);
    if (dest == null) {
      throw new IOException("no cache path for " + spec.id);
    }
    File dir = dest.getParentFile();
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("cannot create " + dir);
    }
    if (dest.isFile() && dest.length() >= spec.minBytes) {
      return dest;
    }
    IOException last = null;
    for (String url : spec.urls) {
      File part = new File(dest.getAbsolutePath() + ".part");
      try {
        log.info("downloading OCR model {} from {}", spec.id, url);
        downloadUrl(url, part);
        if (!part.isFile() || part.length() < spec.minBytes) {
          throw new IOException("downloaded file too small (" + part.length() + " bytes) from " + url);
        }
        if (dest.exists() && !dest.delete()) {
          throw new IOException("cannot replace " + dest);
        }
        if (!part.renameTo(dest)) {
          throw new IOException("cannot rename " + part + " to " + dest);
        }
        log.info("cached OCR model {} at {} ({} bytes)", spec.id, dest.getAbsolutePath(), dest.length());
        return dest;
      } catch (Exception e) {
        last = e instanceof IOException ? (IOException) e : new IOException(e);
        log.warn("download failed for {}: {}", url, e.getMessage());
        if (part.exists() && !part.delete()) {
          log.debug("could not delete {}", part);
        }
      }
    }
    throw last != null ? last : new IOException("download failed for " + spec.id);
  }

  static void downloadUrl(String url, File dest) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    FileOutputStream out = null;
    try {
      conn = open(url);
      int code = conn.getResponseCode();
      if (code >= 400) {
        throw new IOException("HTTP " + code + " for " + url);
      }
      in = conn.getInputStream();
      out = new FileOutputStream(dest);
      byte[] buf = new byte[65536];
      int n;
      long total = 0;
      while ((n = in.read(buf)) >= 0) {
        out.write(buf, 0, n);
        total += n;
      }
      out.flush();
      log.info("wrote {} bytes to {}", total, dest.getAbsolutePath());
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (Exception e) {
          // ignore
        }
      }
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
          // ignore
        }
      }
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private static HttpURLConnection open(String url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setInstanceFollowRedirects(true);
    conn.setConnectTimeout(30_000);
    conn.setReadTimeout(300_000);
    conn.setRequestProperty("User-Agent", USER_AGENT);
    conn.setRequestProperty("Accept", "*/*");
    conn.setRequestMethod("GET");
    return conn;
  }

}
