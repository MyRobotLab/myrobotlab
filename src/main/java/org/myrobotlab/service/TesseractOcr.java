package org.myrobotlab.service;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixRead;
import static org.bytedeco.tesseract.global.tesseract.RIL_WORD;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.ResultIterator;
import org.bytedeco.tesseract.TessBaseAPI;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.config.TesseractOcrConfig;
import org.myrobotlab.service.data.ImageData;
import org.myrobotlab.service.data.OcrResult;
import org.myrobotlab.service.data.OcrResult.OcrWord;
import org.myrobotlab.service.interfaces.ImageListener;
import org.myrobotlab.service.interfaces.ImagePublisher;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;
import org.slf4j.Logger;

/**
 * Optical character recognition using Tesseract 5 via JavaCV.
 * <p>
 * Reads printed text from files, {@link BufferedImage}s, or frames published by
 * an {@link ImagePublisher} (OpenCV). Results are published as
 * {@link OcrResult} (boxes + confidence) and optionally as
 * {@link #publishText(String)} for chat / speech.
 * <p>
 * Language models are Tesseract {@code tessdata} files. The Ivy
 * {@code tesseract:tessdata} zip plus {@code resource/TesseractOcr} are checked
 * first; missing languages can be downloaded from tessdata_fast.
 */
public class TesseractOcr extends Service<TesseractOcrConfig> implements TextPublisher, ImageListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TesseractOcr.class);

  transient private TessBaseAPI api = null;

  /**
   * True after {@link TessBaseAPI#Init} succeeds for the current language.
   */
  protected boolean ready = false;

  /**
   * Datapath last passed to Tesseract Init (parent of tessdata).
   */
  protected String loadedTessDataPath = null;

  /**
   * Most recent OCR result, shown in WebGui.
   */
  protected OcrResult lastResult = null;

  /**
   * Language models currently on disk (tessdata_fast / Ivy zip).
   */
  protected List<String> installedLanguages = new ArrayList<>();

  public TesseractOcr(String n, String id) {
    super(n, id);
  }

  @Override
  synchronized public void startService() {
    super.startService();
    initEngine();
  }

  @Override
  synchronized public void stopService() {
    endEngine();
    super.stopService();
  }

  @Override
  public TesseractOcrConfig apply(TesseractOcrConfig c) {
    super.apply(c);
    if (isRunning()) {
      initEngine();
    }
    return c;
  }

  /**
   * Load (or reload) the Tesseract engine from {@link TesseractOcrConfig}.
   */
  public synchronized void initEngine() {
    String lang = normalizeLang(config.lang);
    config.lang = lang;
    try {
      File tessDataDir = resolveTessDataDir(lang);
      endEngine();
      api = new TessBaseAPI();
      loadedTessDataPath = tessDataDir.getAbsolutePath();
      int rc;
      if (config.oem == 3) {
        rc = api.Init(loadedTessDataPath, lang);
      } else {
        rc = api.Init(loadedTessDataPath, lang, config.oem);
      }
      if (rc != 0) {
        ready = false;
        warn("Unable to load tesseract model in %s with language %s (oem %s)", loadedTessDataPath, lang, config.oem);
        endEngine();
        return;
      }
      applyEngineVariables();
      ready = true;
      info("Tesseract ready lang=%s psm=%s datapath=%s", lang, config.psm, loadedTessDataPath);
      installedLanguages = getInstalledLanguages();
      broadcastState();
    } catch (Exception e) {
      ready = false;
      error("Tesseract init failed: %s", e.getMessage());
      log.error("Tesseract init failed", e);
      endEngine();
    }
  }

  /**
   * Load a language model, updating config and re-initing the engine.
   *
   * @param lang
   *          three-letter (or chi_sim style) Tesseract language code
   */
  public synchronized void initModel(String lang) {
    config.lang = lang;
    initEngine();
  }

  public boolean isReady() {
    return ready && api != null;
  }

  public synchronized void setLang(String lang) {
    config.lang = lang;
    if (isRunning()) {
      initEngine();
    }
    broadcastState();
  }

  public synchronized void setPsm(int psm) {
    config.psm = psm;
    if (api != null) {
      api.SetPageSegMode(psm);
    }
    broadcastState();
  }

  /**
   * Named page-seg preset: auto, block, line, word, sparse, raw.
   */
  public synchronized void setPsmPreset(String preset) {
    setPsm(psmFromName(preset));
  }

  public synchronized void setOem(int oem) {
    config.oem = oem;
    if (isRunning()) {
      initEngine();
    }
    broadcastState();
  }

  public synchronized void setWhitelist(String whitelist) {
    config.whitelist = whitelist;
    if (api != null) {
      applyEngineVariables();
    }
    broadcastState();
  }

  public synchronized void setBlacklist(String blacklist) {
    config.blacklist = blacklist;
    if (api != null) {
      applyEngineVariables();
    }
    broadcastState();
  }

  public Map<String, Integer> getPsmPresets() {
    Map<String, Integer> presets = new LinkedHashMap<>();
    presets.put("auto", 3);
    presets.put("block", 6);
    presets.put("line", 7);
    presets.put("word", 8);
    presets.put("char", 10);
    presets.put("sparse", 11);
    presets.put("raw", 13);
    return presets;
  }

  public List<String> getInstalledLanguages() {
    List<String> langs = new ArrayList<>();
    for (File dir : tessDataSearchDirs()) {
      File tessdata = tessdataFolder(dir);
      if (tessdata == null || !tessdata.isDirectory()) {
        continue;
      }
      File[] files = tessdata.listFiles((d, name) -> name.endsWith(".traineddata"));
      if (files == null) {
        continue;
      }
      for (File f : files) {
        String code = f.getName().substring(0, f.getName().length() - ".traineddata".length());
        if (!"osd".equals(code) && !langs.contains(code)) {
          langs.add(code);
        }
      }
    }
    return langs;
  }

  /**
   * Download a tessdata_fast model into the service data dir and reload if it
   * matches the configured language.
   */
  public synchronized String installLang(String lang) throws IOException {
    lang = normalizeLang(lang);
    File trained = downloadLang(lang);
    installedLanguages = getInstalledLanguages();
    if (lang.equals(config.lang) && isRunning()) {
      initEngine();
    }
    broadcastState();
    return trained.getAbsolutePath();
  }

  public String ocr(String filename) throws IOException {
    OcrResult result = recognize(filename);
    return result.text;
  }

  public String ocr(BufferedImage image) throws IOException {
    OcrResult result = recognize(image);
    return result.text;
  }

  public String ocr(SerializableImage image) throws IOException {
    return ocr(image.getImage());
  }

  public OcrResult recognize(String filename) throws IOException {
    return recognizeFile(filename, true);
  }

  public OcrResult recognize(BufferedImage image) throws IOException {
    return recognize(image, true);
  }

  public OcrResult recognize(SerializableImage image) throws IOException {
    return recognize(image.getImage(), true);
  }

  /**
   * OCR a raster without publishing (used by the OpenCV crop loop).
   */
  public synchronized OcrResult recognize(BufferedImage image, boolean publishEvents) throws IOException {
    ensureReady();
    setImage(image);
    OcrResult result = readCurrentImage(config.lang, "image");
    if (publishEvents) {
      publishResult(result);
    }
    return result;
  }

  public synchronized OcrResult recognizeFile(String filename, boolean publishEvents) throws IOException {
    ensureReady();
    String path = toLocalPath(filename);
    File file = new File(path);
    if (!file.isFile()) {
      throw new IOException("image not found: " + path);
    }
    PIX image = pixRead(file.getAbsolutePath());
    if (image == null || image.isNull()) {
      throw new IOException("leptonica could not read " + file.getAbsolutePath());
    }
    try {
      api.SetImage(image);
      OcrResult result = readCurrentImage(config.lang, file.getAbsolutePath());
      if (publishEvents) {
        publishResult(result);
      }
      return result;
    } finally {
      pixDestroy(image);
    }
  }

  /**
   * Publish an already-built result (OpenCV filter stitches crops, then calls
   * this once).
   */
  public OcrResult publishResult(OcrResult result) {
    lastResult = result;
    if (result != null && result.text != null && config.publishText) {
      invoke("publishText", result.text);
    }
    invoke("publishOcr", result);
    return result;
  }

  public OcrResult publishOcr(OcrResult result) {
    return result;
  }

  @Override
  public String publishText(String text) {
    return text;
  }

  @Override
  public void onImage(ImageData img) {
    if (img == null) {
      return;
    }
    String src = img.src != null ? img.src : img.name;
    if (src == null) {
      error("received image with no src");
      return;
    }
    try {
      recognizeFile(src, true);
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void attach(Attachable attachable) {
    if (attachable instanceof ImagePublisher) {
      attachImagePublisher((ImagePublisher) attachable);
    } else if (attachable instanceof TextListener) {
      attachTextListener((TextListener) attachable);
    } else {
      warn("don't know how to attach %s", attachable.getName());
    }
  }

  static int psmFromName(String preset) {
    if (preset == null || preset.trim().isEmpty()) {
      return 3;
    }
    String p = preset.trim().toLowerCase();
    switch (p) {
      case "osd":
        return 0;
      case "auto":
        return 3;
      case "column":
        return 4;
      case "block":
        return 6;
      case "line":
        return 7;
      case "word":
        return 8;
      case "circle":
        return 9;
      case "char":
        return 10;
      case "sparse":
        return 11;
      case "raw":
        return 13;
      default:
        try {
          return Integer.parseInt(p);
        } catch (NumberFormatException e) {
          return 3;
        }
    }
  }

  private void applyEngineVariables() {
    api.SetPageSegMode(config.psm);
    if (config.dpi > 0) {
      api.SetSourceResolution(config.dpi);
    }
    if (config.whitelist != null && !config.whitelist.isEmpty()) {
      api.SetVariable("tessedit_char_whitelist", config.whitelist);
    }
    if (config.blacklist != null && !config.blacklist.isEmpty()) {
      api.SetVariable("tessedit_char_blacklist", config.blacklist);
    }
  }

  private void ensureReady() throws IOException {
    if (!isReady()) {
      initEngine();
    }
    if (!isReady()) {
      throw new IOException("Tesseract is not initialized (lang=" + config.lang + " datapath=" + loadedTessDataPath + ")");
    }
  }

  private void endEngine() {
    ready = false;
    if (api != null) {
      try {
        api.End();
      } catch (Exception e) {
        log.warn("TessBaseAPI.End threw", e);
      }
      try {
        api.deallocate();
      } catch (Exception e) {
        log.debug("TessBaseAPI.deallocate threw", e);
      }
      api = null;
    }
  }

  private OcrResult readCurrentImage(String lang, String source) {
    OcrResult result = new OcrResult();
    result.lang = lang;
    result.source = source;
    BytePointer outText = api.GetUTF8Text();
    try {
      if (outText != null && !outText.isNull()) {
        String text = outText.getString();
        result.text = text == null ? "" : text.trim();
      }
    } finally {
      if (outText != null && !outText.isNull()) {
        outText.deallocate();
      }
    }
    result.meanConfidence = api.MeanTextConf();
    collectWords(result);
    return result;
  }

  private void collectWords(OcrResult result) {
    ResultIterator ri = null;
    IntPointer left = new IntPointer(1);
    IntPointer top = new IntPointer(1);
    IntPointer right = new IntPointer(1);
    IntPointer bottom = new IntPointer(1);
    try {
      ri = api.GetIterator();
      if (ri == null || ri.isNull()) {
        return;
      }
      do {
        BytePointer wordPtr = ri.GetUTF8Text(RIL_WORD);
        if (wordPtr == null || wordPtr.isNull()) {
          continue;
        }
        String word = wordPtr.getString();
        wordPtr.deallocate();
        if (word == null || word.trim().isEmpty()) {
          continue;
        }
        float conf = ri.Confidence(RIL_WORD);
        if (!ri.BoundingBox(RIL_WORD, left, top, right, bottom)) {
          continue;
        }
        int x = left.get();
        int y = top.get();
        result.words.add(new OcrWord(word.trim(), conf, x, y, right.get() - x, bottom.get() - y));
      } while (ri.Next(RIL_WORD));
    } catch (Exception e) {
      log.debug("word iterator failed", e);
    } finally {
      left.deallocate();
      top.deallocate();
      right.deallocate();
      bottom.deallocate();
      if (ri != null && !ri.isNull()) {
        ri.deallocate();
      }
    }
  }

  /**
   * In-memory {@link TessBaseAPI#SetImage} from a Java raster (no temp file).
   */
  private void setImage(BufferedImage image) throws IOException {
    if (image == null) {
      throw new IOException("image is null");
    }
    BufferedImage converted = toTesseractRaster(image);
    byte[] pixels = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
    int bpp = converted.getType() == BufferedImage.TYPE_BYTE_GRAY ? 1 : 3;
    int bpl = bpp * converted.getWidth();
    api.SetImage(pixels, converted.getWidth(), converted.getHeight(), bpp, bpl);
  }

  static BufferedImage toTesseractRaster(BufferedImage image) {
    int type = image.getType();
    if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_3BYTE_BGR) {
      if (image.getRaster().getDataBuffer() instanceof DataBufferByte) {
        return image;
      }
    }
    int target = type == BufferedImage.TYPE_BYTE_GRAY ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
    BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), target);
    Graphics2D g = converted.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return converted;
  }

  private File resolveTessDataDir(String lang) throws IOException {
    File trained = findTrainedData(lang);
    if (trained == null && config.autoDownloadLang) {
      trained = downloadLang(lang);
    }
    if (trained == null || !trained.isFile()) {
      throw new IOException("traineddata not found for lang " + lang);
    }
    // JavaCV Tesseract Init(datapath, lang) loads {datapath}/{lang}.traineddata
    return trained.getParentFile();
  }

  private File findTrainedData(String lang) {
    String fileName = lang + ".traineddata";
    if (config.tessDataPath != null && !config.tessDataPath.trim().isEmpty()) {
      File configured = new File(config.tessDataPath);
      File direct = new File(configured, fileName);
      if (direct.isFile()) {
        return direct;
      }
      File nested = new File(new File(configured, "tessdata"), fileName);
      if (nested.isFile()) {
        return nested;
      }
      File asFile = configured.isFile() ? configured : new File(configured, "tessdata" + File.separator + fileName);
      if (asFile.isFile()) {
        return asFile;
      }
    }
    for (File dir : tessDataSearchDirs()) {
      File nested = new File(tessdataFolder(dir), fileName);
      if (nested.isFile()) {
        return nested;
      }
      File flat = new File(dir, fileName);
      if (flat.isFile()) {
        return flat;
      }
    }
    return null;
  }

  private List<File> tessDataSearchDirs() {
    List<File> dirs = new ArrayList<>();
    dirs.add(new File(getResourceDir()));
    dirs.add(new File("resource" + File.separator + "TesseractOcr"));
    dirs.add(new File(getDataDir()));
    dirs.add(new File(getDataDir(), "tessdata"));
    return dirs;
  }

  private static File tessdataFolder(File dir) {
    if (dir == null) {
      return null;
    }
    if ("tessdata".equalsIgnoreCase(dir.getName())) {
      return dir;
    }
    File nested = new File(dir, "tessdata");
    if (nested.isDirectory()) {
      return nested;
    }
    return dir;
  }

  private File downloadLang(String lang) throws IOException {
    if (lang.contains("..") || lang.contains("/") || lang.contains("\\")) {
      throw new IllegalArgumentException("invalid language code: " + lang);
    }
    String baseUrl = config.tessdataBaseUrl == null ? "https://github.com/tesseract-ocr/tessdata_fast/raw/main" : config.tessdataBaseUrl.replaceAll("/$", "");
    String url = baseUrl + "/" + lang + ".traineddata";
    File tessdataDir = new File(getDataDir(), "tessdata");
    if (!tessdataDir.exists() && !tessdataDir.mkdirs()) {
      throw new IOException("cannot create " + tessdataDir);
    }
    File dest = new File(tessdataDir, lang + ".traineddata");
    if (dest.isFile() && dest.length() > 0) {
      return dest;
    }
    info("downloading tesseract lang %s from %s", lang, url);
    Http.getFile(url, dest.getAbsolutePath());
    if (!dest.isFile() || dest.length() == 0) {
      throw new IOException("downloaded traineddata missing or empty: " + dest);
    }
    return dest;
  }

  static String normalizeLang(String lang) {
    if (lang == null || lang.trim().isEmpty()) {
      return "eng";
    }
    return lang.trim();
  }

  static String toLocalPath(String filename) {
    if (filename == null) {
      return null;
    }
    if (filename.startsWith("file:")) {
      try {
        return new File(URI.create(filename)).getAbsolutePath();
      } catch (Exception e) {
        return filename.substring("file:".length());
      }
    }
    Path path = Paths.get(filename);
    return path.toAbsolutePath().toString();
  }

  /**
   * Convenience for tests / scripts that already have pixels.
   */
  public OcrResult recognizeBytes(byte[] imageBytes, String format) throws IOException {
    BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
    if (image == null) {
      throw new IOException("could not decode " + format + " image bytes");
    }
    return recognize(image, true);
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      TesseractOcr tesseract = (TesseractOcr) Runtime.start("tesseract", "TesseractOcr");
      String found = tesseract.ocr("src/test/resources/OpenCV/i_am_a_droid.jpg");
      log.info("found {}", found);
      Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
