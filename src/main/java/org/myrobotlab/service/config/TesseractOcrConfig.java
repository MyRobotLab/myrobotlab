package org.myrobotlab.service.config;

/**
 * Configuration for {@link org.myrobotlab.service.TesseractOcr}.
 *
 * Page segmentation modes (psm) match Tesseract 5:
 * 3 auto, 6 single block, 7 single line, 8 single word, 11 sparse, 13 raw line.
 */
public class TesseractOcrConfig extends ServiceConfig {

  /**
   * ISO 639-3 (Tesseract) language code, e.g. {@code eng}, {@code fra},
   * {@code chi_sim}.
   */
  public String lang = "eng";

  /**
   * Parent of a {@code tessdata} folder, or a folder that already contains
   * {@code *.traineddata}. Null means search the usual resource / data dirs.
   */
  public String tessDataPath = null;

  /**
   * Tesseract page segmentation mode. Default 3 (fully automatic). Camera /
   * sign reading often wants 7 (line) or 11 (sparse).
   */
  public int psm = 3;

  /**
   * Tesseract OCR engine mode. Default 3 (OEM_DEFAULT). 1 is LSTM only.
   */
  public int oem = 3;

  /**
   * Source resolution in DPI passed to Tesseract. 0 leaves Tesseract's default.
   */
  public int dpi = 0;

  /**
   * When set, restricts recognition to these characters
   * ({@code tessedit_char_whitelist}).
   */
  public String whitelist = null;

  /**
   * When set, these characters are ignored ({@code tessedit_char_blacklist}).
   */
  public String blacklist = null;

  /**
   * Download missing {@code {lang}.traineddata} from {@link #tessdataBaseUrl}
   * into the service data dir.
   */
  public boolean autoDownloadLang = true;

  /**
   * Base URL for tessdata_fast traineddata files (no trailing slash).
   */
  public String tessdataBaseUrl = "https://github.com/tesseract-ocr/tessdata_fast/raw/main";

  /**
   * When true, successful OCR also fires {@code publishText} so speech / chat
   * services can subscribe.
   */
  public boolean publishText = true;

}
