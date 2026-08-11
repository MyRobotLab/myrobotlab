package org.myrobotlab.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.Http;
import org.myrobotlab.service.abstracts.AbstractSpeechRecognizer;
import org.myrobotlab.service.config.VoskSpeechRecognitionConfig;
import org.myrobotlab.service.data.Locale;
import org.slf4j.Logger;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

/**
 * Offline speech recognition using
 * <a href="https://alphacephei.com/vosk/">Vosk</a>. Language models are
 * downloaded on demand into {@code data/VoskSpeechRecognition/models/}.
 */
public class VoskSpeechRecognition extends AbstractSpeechRecognizer<VoskSpeechRecognitionConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VoskSpeechRecognition.class);

  /** Base path under the service data dir for extracted models. */
  public static final String MODELS_DIR = "models";

  /** Staging directory for downloaded zip archives. */
  public static final String DOWNLOADS_DIR = "downloads";

  /**
   * Catalog of recommended small (desktop / Pi) models keyed by locale tag.
   * Values are Vosk model directory names (without .zip).
   */
  private static final Map<String, String> DEFAULT_MODELS_BY_LOCALE;

  /**
   * Full installable model list from
   * <a href="https://alphacephei.com/vosk/models">alphacephei.com/vosk/models</a>
   * (Model list section — ASR models only; punctuation models omitted).
   */
  private static final List<ModelInfo> MODEL_CATALOG;

  static {
    Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put("en-US", "vosk-model-small-en-us-0.15");
    defaults.put("en-IN", "vosk-model-small-en-in-0.4");
    defaults.put("de-DE", "vosk-model-small-de-0.15");
    defaults.put("es-ES", "vosk-model-small-es-0.42");
    defaults.put("fr-FR", "vosk-model-small-fr-0.22");
    defaults.put("it-IT", "vosk-model-small-it-0.22");
    defaults.put("pt-BR", "vosk-model-small-pt-0.3");
    defaults.put("ru-RU", "vosk-model-small-ru-0.22");
    defaults.put("zh-CN", "vosk-model-small-cn-0.22");
    defaults.put("ja-JP", "vosk-model-small-ja-0.22");
    defaults.put("ko-KR", "vosk-model-small-ko-0.22");
    defaults.put("hi-IN", "vosk-model-small-hi-0.22");
    defaults.put("nl-NL", "vosk-model-small-nl-0.22");
    defaults.put("pl-PL", "vosk-model-small-pl-0.22");
    defaults.put("tr-TR", "vosk-model-small-tr-0.3");
    defaults.put("uk-UA", "vosk-model-small-uk-v3-small");
    defaults.put("ca-ES", "vosk-model-small-ca-0.4");
    defaults.put("cs-CZ", "vosk-model-small-cs-0.4-rhasspy");
    defaults.put("sv-SE", "vosk-model-small-sv-rhasspy-0.15");
    defaults.put("el-GR", "vosk-model-el-gr-0.7");
    defaults.put("ar", "vosk-model-ar-mgb2-0.4");
    defaults.put("ar-TN", "vosk-model-small-ar-tn-0.1-linto");
    defaults.put("fa", "vosk-model-small-fa-0.42");
    defaults.put("vi-VN", "vosk-model-small-vn-0.4");
    defaults.put("tl-PH", "vosk-model-tl-ph-generic-0.6");
    defaults.put("uz", "vosk-model-small-uz-0.22");
    defaults.put("kk", "vosk-model-small-kz-0.42");
    defaults.put("eo", "vosk-model-small-eo-0.42");
    defaults.put("br", "vosk-model-br-0.8");
    defaults.put("gu-IN", "vosk-model-small-gu-0.42");
    defaults.put("tg", "vosk-model-small-tg-0.22");
    defaults.put("te-IN", "vosk-model-small-te-0.42");
    defaults.put("ky", "vosk-model-small-ky-0.42");
    defaults.put("ka", "vosk-model-small-ka-0.42");
    DEFAULT_MODELS_BY_LOCALE = Collections.unmodifiableMap(defaults);

    List<ModelInfo> catalog = new ArrayList<>();
    // English
    catalog.add(model("vosk-model-small-en-us-0.15", "en-US", "English (US)", "40M", "Lightweight wideband model for Android and RPi"));
    catalog.add(model("vosk-model-en-us-0.22", "en-US", "English (US)", "1.8G", "Accurate generic US English model"));
    catalog.add(model("vosk-model-en-us-0.22-lgraph", "en-US", "English (US)", "128M", "Big US English model with dynamic graph"));
    catalog.add(model("vosk-model-en-us-0.42-gigaspeech", "en-US", "English (US)", "2.3G", "Accurate Gigaspeech model — podcasts, not telephony"));
    // English Other (older)
    catalog.add(model("vosk-model-en-us-daanzu-20200905", "en-US", "English (US)", "1.0G", "Older — Kaldi-active-grammar dictation (AGPL)"));
    catalog.add(model("vosk-model-en-us-daanzu-20200905-lgraph", "en-US", "English (US)", "129M", "Older — Kaldi-active-grammar with configurable graph (AGPL)"));
    catalog.add(model("vosk-model-en-us-librispeech-0.2", "en-US", "English (US)", "845M", "Older — repackaged Librispeech, not very accurate"));
    catalog.add(model("vosk-model-small-en-us-zamia-0.5", "en-US", "English (US)", "49M", "Older — Zamia f_250, mainly for research (LGPL-3.0)"));
    catalog.add(model("vosk-model-en-us-aspire-0.2", "en-US", "English (US)", "1.4G", "Older — Kaldi ASPIRE, not very accurate"));
    catalog.add(model("vosk-model-en-us-0.21", "en-US", "English (US)", "1.6G", "Older — previous-generation wideband model"));
    // Indian English
    catalog.add(model("vosk-model-en-in-0.5", "en-IN", "English (India)", "1G", "Generic Indian English for telecom and broadcast"));
    catalog.add(model("vosk-model-small-en-in-0.4", "en-IN", "English (India)", "36M", "Lightweight Indian English for mobile"));
    // Chinese
    catalog.add(model("vosk-model-small-cn-0.22", "zh-CN", "Chinese", "42M", "Lightweight model for Android and RPi"));
    catalog.add(model("vosk-model-cn-0.22", "zh-CN", "Chinese", "1.3G", "Big generic Chinese model for servers"));
    catalog.add(model("vosk-model-cn-kaldi-multicn-0.15", "zh-CN", "Chinese", "1.5G", "Older — Kaldi multi-cn with Vosk LM"));
    // Russian
    catalog.add(model("vosk-model-ru-0.42", "ru-RU", "Russian", "1.8G", "Big mixed-band model for servers"));
    catalog.add(model("vosk-model-small-ru-0.22", "ru-RU", "Russian", "45M", "Lightweight wideband for Android/iOS and RPi"));
    catalog.add(model("vosk-model-ru-0.22", "ru-RU", "Russian", "1.5G", "Older — big mixed-band server model"));
    catalog.add(model("vosk-model-ru-0.10", "ru-RU", "Russian", "2.5G", "Older — big narrowband server model"));
    // French
    catalog.add(model("vosk-model-small-fr-0.22", "fr-FR", "French", "41M", "Lightweight wideband for Android/iOS and RPi"));
    catalog.add(model("vosk-model-fr-0.22", "fr-FR", "French", "1.4G", "Big accurate model for servers"));
    catalog.add(model("vosk-model-small-fr-pguyot-0.3", "fr-FR", "French", "39M", "Older — Paul Guyot / Zamia small (CC-BY-NC-SA 4.0)"));
    catalog.add(model("vosk-model-fr-0.6-linto-2.2.0", "fr-FR", "French", "1.5G", "Older — LINTO project model (AGPL)"));
    // German
    catalog.add(model("vosk-model-de-0.21", "de-DE", "German", "1.9G", "Big German model for telephony and server"));
    catalog.add(model("vosk-model-de-tuda-0.6-900k", "de-DE", "German", "4.4G", "Latest big wideband from Tuda-DE"));
    catalog.add(model("vosk-model-small-de-zamia-0.3", "de-DE", "German", "49M", "Zamia f_250 small — not recommended (LGPL-3.0)"));
    catalog.add(model("vosk-model-small-de-0.15", "de-DE", "German", "45M", "Lightweight wideband for Android and RPi"));
    // Spanish
    catalog.add(model("vosk-model-small-es-0.42", "es-ES", "Spanish", "39M", "Lightweight wideband for Android and RPi"));
    catalog.add(model("vosk-model-es-0.42", "es-ES", "Spanish", "1.4G", "Big model for Spanish"));
    // Portuguese
    catalog.add(model("vosk-model-small-pt-0.3", "pt-BR", "Portuguese", "31M", "Lightweight wideband for Android and RPi"));
    catalog.add(model("vosk-model-pt-fb-v0.1.1-20220516_2113", "pt-BR", "Portuguese", "1.6G", "Big model from FalaBrazil (GPLv3)"));
    // Greek
    catalog.add(model("vosk-model-el-gr-0.7", "el-GR", "Greek", "1.1G", "Big narrowband Greek model for servers"));
    // Turkish
    catalog.add(model("vosk-model-small-tr-0.3", "tr-TR", "Turkish", "35M", "Lightweight wideband for Android and RPi"));
    // Vietnamese
    catalog.add(model("vosk-model-small-vn-0.4", "vi-VN", "Vietnamese", "32M", "Lightweight Vietnamese model"));
    catalog.add(model("vosk-model-vn-0.4", "vi-VN", "Vietnamese", "78M", "Bigger Vietnamese model for server"));
    // Italian
    catalog.add(model("vosk-model-small-it-0.22", "it-IT", "Italian", "48M", "Lightweight model for Android and RPi"));
    catalog.add(model("vosk-model-it-0.22", "it-IT", "Italian", "1.2G", "Big generic Italian model for servers"));
    // Dutch
    catalog.add(model("vosk-model-small-nl-0.22", "nl-NL", "Dutch", "39M", "Lightweight model for Dutch"));
    catalog.add(model("vosk-model-nl-spraakherkenning-0.6", "nl-NL", "Dutch", "860M", "Medium Dutch from Kaldi_NL (CC-BY-NC-SA)"));
    catalog.add(model("vosk-model-nl-spraakherkenning-0.6-lgraph", "nl-NL", "Dutch", "100M", "Smaller Dutch with dynamic graph (CC-BY-NC-SA)"));
    // Catalan
    catalog.add(model("vosk-model-small-ca-0.4", "ca-ES", "Catalan", "42M", "Lightweight wideband for Android and RPi"));
    // Arabic
    catalog.add(model("vosk-model-ar-mgb2-0.4", "ar", "Arabic", "318M", "Repackaged MGB2 Arabic model from Kaldi"));
    catalog.add(model("vosk-model-ar-0.22-linto-1.1.0", "ar", "Arabic", "1.3G", "Big LINTO project model (AGPL)"));
    // Arabic Tunisian
    catalog.add(model("vosk-model-small-ar-tn-0.1-linto", "ar-TN", "Arabic (Tunisian)", "158M", "Small Tunisian Arabic from Linagora"));
    catalog.add(model("vosk-model-ar-tn-0.1-linto", "ar-TN", "Arabic (Tunisian)", "517M", "Tunisian Arabic from Linagora"));
    // Farsi
    catalog.add(model("vosk-model-fa-0.42", "fa", "Persian (Farsi)", "1.6G", "Large-vocabulary Persian model"));
    catalog.add(model("vosk-model-small-fa-0.42", "fa", "Persian (Farsi)", "53M", "Small model for desktop and mobile"));
    catalog.add(model("vosk-model-fa-0.5", "fa", "Persian (Farsi)", "1G", "Older — large-vocabulary Persian"));
    catalog.add(model("vosk-model-small-fa-0.5", "fa", "Persian (Farsi)", "60M", "Older — small Persian for desktop"));
    // Filipino
    catalog.add(model("vosk-model-tl-ph-generic-0.6", "tl-PH", "Filipino (Tagalog)", "320M", "Medium wideband Tagalog by feddybear (CC-BY-NC-SA 4.0)"));
    // Ukrainian
    catalog.add(model("vosk-model-small-uk-v3-nano", "uk-UA", "Ukrainian", "73M", "Nano model from Speech Recognition for Ukrainian"));
    catalog.add(model("vosk-model-small-uk-v3-small", "uk-UA", "Ukrainian", "133M", "Small model from Speech Recognition for Ukrainian"));
    catalog.add(model("vosk-model-uk-v3", "uk-UA", "Ukrainian", "343M", "Bigger Ukrainian model"));
    catalog.add(model("vosk-model-uk-v3-lgraph", "uk-UA", "Ukrainian", "325M", "Big dynamic Ukrainian model"));
    // Kazakh
    catalog.add(model("vosk-model-small-kz-0.42", "kk", "Kazakh", "58M", "Small mobile model for Kazakh"));
    catalog.add(model("vosk-model-kz-0.42", "kk", "Kazakh", "1.3G", "Bigger model for Kazakh"));
    // Swedish
    catalog.add(model("vosk-model-small-sv-rhasspy-0.15", "sv-SE", "Swedish", "289M", "Repackaged Rhasspy Swedish model (MIT)"));
    // Japanese
    catalog.add(model("vosk-model-small-ja-0.22", "ja-JP", "Japanese", "48M", "Lightweight wideband for Japanese"));
    catalog.add(model("vosk-model-ja-0.22", "ja-JP", "Japanese", "1G", "Big model for Japanese"));
    // Esperanto
    catalog.add(model("vosk-model-small-eo-0.42", "eo", "Esperanto", "42M", "Lightweight model for Esperanto"));
    // Hindi
    catalog.add(model("vosk-model-small-hi-0.22", "hi-IN", "Hindi", "42M", "Lightweight model for Hindi"));
    catalog.add(model("vosk-model-hi-0.22", "hi-IN", "Hindi", "1.5G", "Big accurate model for servers"));
    // Czech
    catalog.add(model("vosk-model-small-cs-0.4-rhasspy", "cs-CZ", "Czech", "44M", "Lightweight Czech from Rhasspy (MIT)"));
    // Polish
    catalog.add(model("vosk-model-small-pl-0.22", "pl-PL", "Polish", "50M", "Lightweight model for Polish"));
    // Uzbek
    catalog.add(model("vosk-model-small-uz-0.22", "uz", "Uzbek", "49M", "Lightweight model for Uzbek"));
    // Korean
    catalog.add(model("vosk-model-small-ko-0.22", "ko-KR", "Korean", "82M", "Lightweight model for Korean"));
    // Breton
    catalog.add(model("vosk-model-br-0.8", "br", "Breton", "70M", "Breton model from vosk-br (MIT)"));
    // Gujarati
    catalog.add(model("vosk-model-gu-0.42", "gu-IN", "Gujarati", "700M", "Big Gujarati model"));
    catalog.add(model("vosk-model-small-gu-0.42", "gu-IN", "Gujarati", "100M", "Lightweight model for Gujarati"));
    // Tajik
    catalog.add(model("vosk-model-tg-0.22", "tg", "Tajik", "327M", "Big Tajik model"));
    catalog.add(model("vosk-model-small-tg-0.22", "tg", "Tajik", "50M", "Lightweight model for Tajik"));
    // Telugu
    catalog.add(model("vosk-model-small-te-0.42", "te-IN", "Telugu", "58M", "Lightweight model for Telugu"));
    // Kyrgyz
    catalog.add(model("vosk-model-small-ky-0.42", "ky", "Kyrgyz", "49M", "Small mobile model for Kyrgyz"));
    catalog.add(model("vosk-model-ky-0.42", "ky", "Kyrgyz", "1.1G", "Bigger model for Kyrgyz"));
    // Georgian
    catalog.add(model("vosk-model-small-ka-0.42", "ka", "Georgian", "45M", "Small mobile model for Georgian"));
    catalog.add(model("vosk-model-ka-0.42", "ka", "Georgian", "700M", "Bigger model for Georgian"));
    // Speaker identification (listed on models page; not ASR)
    catalog.add(model("vosk-model-spk-0.4", "und", "Speaker ID", "13M", "Speaker identification — all languages (not ASR)"));

    MODEL_CATALOG = Collections.unmodifiableList(catalog);
  }

  private static ModelInfo model(String name, String locale, String language, String size, String description) {
    ModelInfo info = new ModelInfo();
    info.name = name;
    info.locale = locale;
    info.language = language;
    info.size = size;
    info.description = description;
    info.label = language + " — " + description + " (" + size + ") [" + name + "]";
    return info;
  }

  /**
   * Installable Vosk model metadata for WebGui / scripts.
   */
  public static class ModelInfo {
    public String name;
    public String locale;
    public String language;
    public String size;
    public String description;
    /** Preformatted dropdown label: language — description (size) [name] */
    public String label;
  }

  /**
   * Status text for WebGui / diagnostics.
   */
  protected String status = "idle";

  /**
   * Absolute path of the currently loaded model directory (or null).
   */
  protected String loadedModelPath = null;

  /**
   * Available downloadable models (language + description). Included in
   * broadcastState for the WebGui dropdown.
   */
  protected List<ModelInfo> availableModels = MODEL_CATALOG;

  transient private Model voskModel;
  transient private Recognizer voskRecognizer;
  transient private TargetDataLine microphone;
  transient private RecognitionThread recognitionThread;
  transient private volatile boolean micRunning = false;

  public VoskSpeechRecognition(String n, String id) {
    super(n, id);
  }

  @Override
  public Map<String, Locale> getLocales() {
    return Locale.getLocaleMap(DEFAULT_MODELS_BY_LOCALE.keySet().toArray(new String[0]));
  }

  /**
   * @return ordered list of known models with language and description
   */
  public List<ModelInfo> getAvailableModels() {
    return availableModels;
  }

  /**
   * @return map of model name → description (legacy / script convenience)
   */
  public Map<String, String> getModelCatalog() {
    Map<String, String> map = new LinkedHashMap<>();
    for (ModelInfo info : MODEL_CATALOG) {
      map.put(info.name, info.language + " — " + info.description + " (" + info.size + ")");
    }
    return map;
  }

  /**
   * @return locale tag → default small model name
   */
  public Map<String, String> getDefaultModelsByLocale() {
    return DEFAULT_MODELS_BY_LOCALE;
  }

  /**
   * Root directory where models are stored:
   * {@code data/VoskSpeechRecognition/models}.
   */
  public String getModelsRoot() {
    return FileIO.gluePaths(getDataDir(), MODELS_DIR);
  }

  /**
   * Resolve the filesystem path for a named model under the models root.
   *
   * @param modelName
   *          Vosk model directory name
   * @return absolute path string
   */
  public String getModelPath(String modelName) {
    if (modelName == null || modelName.trim().isEmpty()) {
      return null;
    }
    return new File(getModelsRoot(), modelName.trim()).getAbsolutePath();
  }

  /**
   * @return list of installed model directory names under the models root
   */
  public List<String> getInstalledModels() {
    List<String> installed = new ArrayList<>();
    File root = new File(getModelsRoot());
    if (!root.isDirectory()) {
      return installed;
    }
    File[] kids = root.listFiles();
    if (kids == null) {
      return installed;
    }
    for (File kid : kids) {
      if (kid.isDirectory() && isValidModelDir(kid)) {
        installed.add(kid.getName());
      }
    }
    Collections.sort(installed);
    return installed;
  }

  /**
   * @param modelName
   *          model directory name
   * @return true if a usable model directory exists
   */
  public boolean isModelInstalled(String modelName) {
    if (modelName == null) {
      return false;
    }
    return isValidModelDir(new File(getModelPath(modelName)));
  }

  /**
   * Basic sanity check for a Vosk model directory (has {@code am} or
   * {@code conf}).
   */
  public static boolean isValidModelDir(File dir) {
    if (dir == null || !dir.isDirectory()) {
      return false;
    }
    return new File(dir, "am").isDirectory() || new File(dir, "conf").isDirectory() || new File(dir, "ivector").isDirectory();
  }

  /**
   * Pick the recommended small model for a locale tag (falls back to en-US).
   *
   * @param localeTag
   *          e.g. {@code en-US}, {@code de}, {@code fr-FR}
   * @return model directory name
   */
  public String getDefaultModelForLocale(String localeTag) {
    if (localeTag == null || localeTag.trim().isEmpty()) {
      return DEFAULT_MODELS_BY_LOCALE.get("en-US");
    }
    String tag = localeTag.trim().replace('_', '-');
    if (DEFAULT_MODELS_BY_LOCALE.containsKey(tag)) {
      return DEFAULT_MODELS_BY_LOCALE.get(tag);
    }
    // try language-only (e.g. "de" from "de-AT")
    String lang = tag.contains("-") ? tag.substring(0, tag.indexOf('-')) : tag;
    for (Map.Entry<String, String> e : DEFAULT_MODELS_BY_LOCALE.entrySet()) {
      if (e.getKey().equalsIgnoreCase(lang) || e.getKey().toLowerCase().startsWith(lang.toLowerCase() + "-")) {
        return e.getValue();
      }
    }
    return DEFAULT_MODELS_BY_LOCALE.get("en-US");
  }

  /**
   * Download and extract a Vosk model if it is not already installed.
   *
   * @param modelName
   *          directory name, e.g. {@code vosk-model-small-en-us-0.15}
   * @return absolute path to the installed model directory
   * @throws IOException
   *           on download / extract failure
   */
  public synchronized String installModel(String modelName) throws IOException {
    if (modelName == null || modelName.trim().isEmpty()) {
      throw new IllegalArgumentException("modelName is required");
    }
    modelName = modelName.trim();
    if (modelName.contains("..") || modelName.contains("/") || modelName.contains("\\")) {
      throw new IllegalArgumentException("invalid model name: " + modelName);
    }

    File modelDir = new File(getModelPath(modelName));
    if (isValidModelDir(modelDir)) {
      info("model already installed: %s", modelDir.getAbsolutePath());
      status = "model ready: " + modelName;
      broadcastState();
      return modelDir.getAbsolutePath();
    }

    String baseUrl = (config.modelBaseUrl != null) ? config.modelBaseUrl.replaceAll("/$", "") : "https://alphacephei.com/vosk/models";
    String url = baseUrl + "/" + modelName + ".zip";

    File downloadDir = new File(FileIO.gluePaths(getDataDir(), DOWNLOADS_DIR));
    if (!downloadDir.exists() && !downloadDir.mkdirs()) {
      throw new IOException("cannot create download dir " + downloadDir);
    }
    File zipFile = new File(downloadDir, modelName + ".zip");

    status = "downloading " + modelName;
    broadcastState();
    info("downloading Vosk model %s from %s", modelName, url);
    try {
      Http.getFile(url, zipFile.getAbsolutePath());
    } catch (Exception e) {
      status = "download failed: " + modelName;
      broadcastState();
      throw new IOException("failed to download " + url, e);
    }
    if (!zipFile.isFile() || zipFile.length() == 0) {
      status = "download failed: empty file";
      broadcastState();
      throw new IOException("downloaded file missing or empty: " + zipFile);
    }

    File modelsRoot = new File(getModelsRoot());
    if (!modelsRoot.exists() && !modelsRoot.mkdirs()) {
      throw new IOException("cannot create models dir " + modelsRoot);
    }

    status = "extracting " + modelName;
    broadcastState();
    info("extracting %s to %s", zipFile.getName(), modelsRoot.getAbsolutePath());
    Zip.unzip(zipFile.getAbsolutePath(), modelsRoot.getAbsolutePath());

    // zip usually contains a top-level folder named modelName
    if (!isValidModelDir(modelDir)) {
      // sometimes content extracts without nesting — try locate
      File found = findModelDir(modelsRoot, modelName);
      if (found != null && !found.equals(modelDir)) {
        log.info("model extracted to {}, expected {}", found, modelDir);
        modelDir = found;
      }
    }

    if (!isValidModelDir(modelDir)) {
      status = "install failed: invalid model layout";
      broadcastState();
      throw new IOException("model directory invalid after extract: " + modelDir);
    }

    status = "model installed: " + modelName;
    broadcastState();
    info("installed Vosk model at %s", modelDir.getAbsolutePath());
    return modelDir.getAbsolutePath();
  }

  private File findModelDir(File root, String modelName) {
    File direct = new File(root, modelName);
    if (isValidModelDir(direct)) {
      return direct;
    }
    File[] kids = root.listFiles();
    if (kids == null) {
      return null;
    }
    for (File kid : kids) {
      if (kid.isDirectory() && kid.getName().startsWith(modelName) && isValidModelDir(kid)) {
        return kid;
      }
    }
    return null;
  }

  /**
   * Ensure the configured model is on disk (download if needed) and load it
   * into memory.
   *
   * @return absolute path of the loaded model
   * @throws IOException
   *           if the model cannot be obtained or loaded
   */
  public synchronized String loadModel() throws IOException {
    String path = resolveModelPath(true);
    return loadModelFromPath(path);
  }

  /**
   * Load a specific catalog model (installs first when
   * {@link VoskSpeechRecognitionConfig#autoDownloadModel} is true).
   *
   * @param modelName
   *          model directory name
   * @return absolute path loaded
   */
  public synchronized String setModel(String modelName) throws IOException {
    config.model = modelName;
    config.modelPath = null;
    String path;
    if (config.autoDownloadModel || !isModelInstalled(modelName)) {
      if (!config.autoDownloadModel && !isModelInstalled(modelName)) {
        throw new IOException("model not installed and autoDownloadModel is false: " + modelName);
      }
      path = installModel(modelName);
    } else {
      path = getModelPath(modelName);
    }
    return loadModelFromPath(path);
  }

  /**
   * Select the default small model for a locale, install if needed, and load
   * it.
   *
   * @param localeTag
   *          e.g. {@code en-US}
   */
  public synchronized String setLanguage(String localeTag) throws IOException {
    setLocale(localeTag);
    String modelName = getDefaultModelForLocale(localeTag);
    return setModel(modelName);
  }

  private String resolveModelPath(boolean allowDownload) throws IOException {
    if (config.modelPath != null && !config.modelPath.trim().isEmpty()) {
      File override = new File(config.modelPath.trim());
      if (!isValidModelDir(override)) {
        throw new IOException("modelPath is not a valid Vosk model directory: " + override);
      }
      return override.getAbsolutePath();
    }

    String modelName = config.model;
    if (modelName == null || modelName.trim().isEmpty()) {
      modelName = getDefaultModelForLocale(locale != null ? locale.getTag() : "en-US");
      config.model = modelName;
    }

    if (isModelInstalled(modelName)) {
      return getModelPath(modelName);
    }
    if (allowDownload && config.autoDownloadModel) {
      return installModel(modelName);
    }
    throw new IOException("Vosk model not installed: " + modelName + " (set autoDownloadModel=true or call installModel)");
  }

  private synchronized String loadModelFromPath(String path) throws IOException {
    if (path == null) {
      throw new IOException("model path is null");
    }
    File dir = new File(path);
    if (!isValidModelDir(dir)) {
      throw new IOException("invalid Vosk model directory: " + path);
    }

    boolean wasRecording = micRunning;
    if (wasRecording) {
      stopMicrophoneCapture();
    }

    closeVosk();

    status = "loading model " + dir.getName();
    broadcastState();
    try {
      LibVosk.setLogLevel(LogLevel.WARNINGS);
      voskModel = new Model(dir.getAbsolutePath());
      voskRecognizer = new Recognizer(voskModel, config.sampleRate);
      loadedModelPath = dir.getAbsolutePath();
      status = "model loaded: " + dir.getName();
      info("loaded Vosk model from %s", loadedModelPath);
      broadcastState();
    } catch (Exception e) {
      status = "load failed";
      loadedModelPath = null;
      broadcastState();
      throw new IOException("failed to load Vosk model from " + path, e);
    }

    if (wasRecording || config.recording) {
      startMicrophoneCapture();
    }
    return loadedModelPath;
  }

  private void closeVosk() {
    if (voskRecognizer != null) {
      try {
        voskRecognizer.close();
      } catch (Exception e) {
        log.warn("error closing recognizer", e);
      }
      voskRecognizer = null;
    }
    if (voskModel != null) {
      try {
        voskModel.close();
      } catch (Exception e) {
        log.warn("error closing model", e);
      }
      voskModel = null;
    }
    loadedModelPath = null;
  }

  @Override
  public void startListening() {
    super.startListening();
    try {
      ensureReadyAndCapture();
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void startRecording() {
    super.startRecording();
    try {
      ensureReadyAndCapture();
    } catch (Exception e) {
      error(e);
    }
  }

  @Override
  public void stopRecording() {
    stopMicrophoneCapture();
    super.stopRecording();
    status = "stopped";
    broadcastState();
  }

  @Override
  public void stopService() {
    stopMicrophoneCapture();
    closeVosk();
    super.stopService();
  }

  @Override
  public VoskSpeechRecognitionConfig apply(VoskSpeechRecognitionConfig c) {
    super.apply(c);
    // parent startListening/startRecording may have run; ensure capture if needed
    if (c.recording || c.listening) {
      try {
        ensureReadyAndCapture();
      } catch (Exception e) {
        error(e);
      }
    }
    return c;
  }

  private void ensureReadyAndCapture() throws IOException {
    if (voskModel == null || voskRecognizer == null) {
      loadModel();
    }
    if (!micRunning) {
      startMicrophoneCapture();
    }
  }

  private synchronized void startMicrophoneCapture() throws IOException {
    if (micRunning) {
      return;
    }
    if (voskRecognizer == null) {
      throw new IOException("Vosk recognizer not loaded");
    }

    AudioFormat format = new AudioFormat(config.sampleRate, 16, 1, true, false);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    if (!AudioSystem.isLineSupported(info)) {
      throw new IOException("microphone line not supported for format " + format);
    }

    try {
      microphone = (TargetDataLine) AudioSystem.getLine(info);
      microphone.open(format);
      microphone.start();
    } catch (Exception e) {
      throw new IOException("cannot open microphone", e);
    }

    micRunning = true;
    recognitionThread = new RecognitionThread();
    recognitionThread.start();
    status = "listening";
    config.recording = true;
    broadcastState();
    invoke("publishListening", true);
  }

  private synchronized void stopMicrophoneCapture() {
    micRunning = false;
    if (recognitionThread != null) {
      try {
        recognitionThread.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      recognitionThread = null;
    }
    if (microphone != null) {
      try {
        microphone.stop();
        microphone.close();
      } catch (Exception e) {
        log.warn("error closing microphone", e);
      }
      microphone = null;
    }
  }

  /**
   * Capture thread: PCM 16-bit mono → Vosk → {@link #processResults}.
   */
  class RecognitionThread extends Thread {
    RecognitionThread() {
      super(VoskSpeechRecognition.this.getName() + "-vosk");
      setDaemon(true);
    }

    @Override
    public void run() {
      byte[] buffer = new byte[4096];
      info("Vosk recognition thread started");
      try {
        while (micRunning && microphone != null) {
          int nbytes = microphone.read(buffer, 0, buffer.length);
          if (nbytes <= 0) {
            continue;
          }
          Recognizer recognizer = voskRecognizer;
          if (recognizer == null) {
            continue;
          }
          if (recognizer.acceptWaveForm(buffer, nbytes)) {
            handleResultJson(recognizer.getResult(), true);
          } else if (config.publishPartial) {
            handleResultJson(recognizer.getPartialResult(), false);
          }
        }
        Recognizer recognizer = voskRecognizer;
        if (recognizer != null) {
          handleResultJson(recognizer.getFinalResult(), true);
        }
      } catch (Exception e) {
        error(e);
        status = "recognition error";
        broadcastState();
      } finally {
        info("Vosk recognition thread stopped");
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void handleResultJson(String json, boolean isFinal) {
    if (json == null || json.trim().isEmpty()) {
      return;
    }
    try {
      Map<String, Object> map = CodecUtils.fromJson(json, Map.class);
      if (map == null) {
        return;
      }
      String text = null;
      if (isFinal) {
        Object t = map.get("text");
        if (t != null) {
          text = t.toString();
        }
      } else {
        Object p = map.get("partial");
        if (p != null) {
          text = p.toString();
        }
      }
      if (text == null || text.trim().isEmpty()) {
        return;
      }

      if (!config.listening) {
        return;
      }

      ListeningEvent event = new ListeningEvent();
      event.text = text.trim();
      event.isFinal = isFinal;
      event.isListening = config.listening;
      event.isRecording = config.recording;
      event.isSpeaking = isSpeaking;
      event.isAwake = isAwake;

      // finals go through processResults (wake word / publish gating)
      if (isFinal) {
        processResults(new ListeningEvent[] { event });
      } else {
        invoke("publishListeningEvent", event);
      }
    } catch (Exception e) {
      log.warn("failed to parse Vosk result: {}", json, e);
    }
  }

  public String getStatus() {
    return status;
  }

  public String getLoadedModelPath() {
    return loadedModelPath;
  }

  /**
   * Diagnostic snapshot for WebGui.
   */
  public Map<String, Object> getModelStatus() {
    Map<String, Object> m = new TreeMap<>();
    m.put("status", status);
    m.put("model", config.model);
    m.put("modelPath", config.modelPath);
    m.put("loadedModelPath", loadedModelPath);
    m.put("installed", getInstalledModels());
    m.put("autoDownloadModel", config.autoDownloadModel);
    m.put("recording", config.recording);
    m.put("listening", config.listening);
    return m;
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);
      VoskSpeechRecognition ear = (VoskSpeechRecognition) Runtime.start("ear", "VoskSpeechRecognition");
      Runtime.start("webgui", "WebGui");
      // ear.installModel("vosk-model-small-en-us-0.15");
      // ear.startListening();
      log.info("installed models: {}", ear.getInstalledModels());
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
