package org.myrobotlab.opencv;

import java.io.File;
import java.io.IOException;
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
 * Catalog and on-demand download of OpenCV Zoo (and NanoTrack) ONNX files used
 * by YuNet, YOLOX, TrackerNano, and TrackerVit.
 * <p>
 * Files are cached under {@code data/OpenCV/zoo_models/}. HuggingFace hosts the
 * zoo weights as real binaries (GitHub opencv_zoo uses Git LFS pointers).
 */
public class OpenCvZooModels {

  public final static Logger log = LoggerFactory.getLogger(OpenCvZooModels.class);

  public static final String YUNET = "yunet";
  public static final String YOLOX = "yolox";
  public static final String NANOTRACK_BACKBONE = "nanotrack_backbone";
  public static final String NANOTRACK_HEAD = "nanotrack_head";
  public static final String VITTRACK = "vittrack";

  private static final String CACHE_SUBDIR = "zoo_models";

  public static class Spec {
    public final String id;
    public final String filename;
    public final String label;
    public final long minBytes;
    public final String[] urls;

    Spec(String id, String filename, String label, long minBytes, String... urls) {
      this.id = id;
      this.filename = filename;
      this.label = label;
      this.minBytes = minBytes;
      this.urls = urls;
    }
  }

  private static final Map<String, Spec> CATALOG = new LinkedHashMap<>();

  static {
    add(new Spec(YUNET, "face_detection_yunet_2023mar.onnx", "YuNet face detector (OpenCV 4.x)", 200_000L,
        "https://huggingface.co/opencv/face_detection_yunet/resolve/main/face_detection_yunet_2023mar.onnx",
        "https://huggingface.co/opencv/face_detection_yunet/resolve/main/face_detection_yunet_2023mar.onnx?download=true"));
    add(new Spec(YOLOX, "object_detection_yolox_2022nov.onnx", "YOLOX-s COCO object detector", 10_000_000L,
        "https://huggingface.co/opencv/object_detection_yolox/resolve/main/object_detection_yolox_2022nov.onnx",
        "https://huggingface.co/opencv/object_detection_yolox/resolve/main/object_detection_yolox_2022nov.onnx?download=true"));
    add(new Spec(NANOTRACK_BACKBONE, "nanotrack_backbone_sim.onnx", "NanoTrack backbone", 500_000L,
        "https://raw.githubusercontent.com/HonglinChu/SiamTrackers/master/NanoTrack/models/nanotrackv2/nanotrack_backbone_sim.onnx"));
    add(new Spec(NANOTRACK_HEAD, "nanotrack_head_sim.onnx", "NanoTrack neck/head", 400_000L,
        "https://raw.githubusercontent.com/HonglinChu/SiamTrackers/master/NanoTrack/models/nanotrackv2/nanotrack_head_sim.onnx"));
    add(new Spec(VITTRACK, "object_tracking_vittrack_2023sep.onnx", "ViT object tracker", 400_000L,
        "https://huggingface.co/opencv/object_tracking_vittrack/resolve/main/object_tracking_vittrack_2023sep.onnx",
        "https://huggingface.co/opencv/object_tracking_vittrack/resolve/main/object_tracking_vittrack_2023sep.onnx?download=true"));
  }

  private static void add(Spec spec) {
    CATALOG.put(spec.id, spec);
  }

  public static List<Spec> all() {
    return Collections.unmodifiableList(new ArrayList<>(CATALOG.values()));
  }

  public static Spec get(String id) {
    if (id == null) {
      return null;
    }
    return CATALOG.get(id.trim().toLowerCase());
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
    Spec spec = get(id);
    if (spec == null) {
      return false;
    }
    File local = findLocal(spec, null);
    return local != null && local.isFile() && local.length() >= spec.minBytes;
  }

  /**
   * Locate a model on disk. Does not download.
   */
  public static File findLocal(Spec spec, String overridePath) {
    if (spec == null) {
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
    candidates.add(new File("resource/OpenCV/zoo_models", spec.filename));
    candidates.add(new File(resourceRoot, "zoo_models/" + spec.filename));
    candidates.add(new File(resourceRoot, spec.filename));
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

  /**
   * Locate a model, optionally downloading it into the OpenCV data cache.
   *
   * @return absolute file, or null when missing and download is disabled / failed
   */
  public static File resolve(String id, String overridePath, boolean autoDownload) throws IOException {
    return resolve(get(id), overridePath, autoDownload);
  }

  public static File resolve(Spec spec, String overridePath, boolean autoDownload) throws IOException {
    if (spec == null) {
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
        log.info("downloading OpenCV zoo model {} from {}", spec.id, url);
        OcrDetectionModels.downloadUrl(url, part);
        if (!part.isFile() || part.length() < spec.minBytes) {
          throw new IOException("downloaded file too small (" + part.length() + " bytes) from " + url);
        }
        if (dest.exists() && !dest.delete()) {
          throw new IOException("cannot replace " + dest);
        }
        if (!part.renameTo(dest)) {
          throw new IOException("cannot rename " + part + " to " + dest);
        }
        log.info("cached OpenCV zoo model {} at {} ({} bytes)", spec.id, dest.getAbsolutePath(), dest.length());
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

}
