package org.myrobotlab.service;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVFilterOcr;
import org.myrobotlab.service.data.OcrResult;
import org.slf4j.Logger;

/**
 * Starts WebGui, TesseractOcr, and OpenCV with the {@code Ocr} filter so both
 * extraction paths can be compared: full-frame Tesseract vs detect-then-OCR.
 *
 * <pre>
 * Run: org.myrobotlab.service.TesseractOcrDemo
 * </pre>
 *
 * Open http://localhost:8888 — TesseractOcr panel shows full-frame text;
 * OpenCV panel shows the Ocr filter boxes and lastText.
 */
public class TesseractOcrDemo {

  public final static Logger log = LoggerFactory.getLogger(TesseractOcrDemo.class);

  private static final String DROID = "src/test/resources/OpenCV/i_am_a_droid.jpg";
  private static final String HUMANS = "src/test/resources/OpenCV/hiring_humans.jpg";

  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");

      WebGui webgui = (WebGui) Runtime.create("webgui", "WebGui");
      webgui.autoStartBrowser(true);
      webgui.startService();

      TesseractOcr tesseract = (TesseractOcr) Runtime.start("tesseract", "TesseractOcr");
      tesseract.setPsmPreset("auto");

      log.info("=== 1) TesseractOcr full-frame ===");
      OcrResult droid = tesseract.recognize(DROID);
      log.info("i_am_a_droid.jpg -> {}", droid.text);
      OcrResult humans = tesseract.recognize(HUMANS);
      log.info("hiring_humans.jpg -> {}", humans.text);

      OpenCV cv = (OpenCV) Runtime.start("opencv", "OpenCV");
      cv.setNativeViewer(false);
      cv.setWebViewer(true);
   //   cv.setGrabberType("ImageFile");
   //   cv.setInputSource("imagefile");
   //   cv.setInputFileName(HUMANS);
      cv.addFilter("ocr", "Ocr");
      OpenCVFilterOcr ocr = (OpenCVFilterOcr) cv.getFilter("ocr");
      ocr.ocrService = tesseract.getName();
      ocr.detectionModel = "east";
      ocr.detector = "east";
      ocr.minIntervalMs = 0;
      ocr.skipBlurry = false;
      ocr.autoStartOcr = false;

      log.info("=== 2) OpenCV Ocr filter (EAST boxes + Tesseract) ===");
      cv.capture();
      long deadline = System.currentTimeMillis() + 15000;
      while ((ocr.lastText == null || ocr.lastText.isEmpty()) && System.currentTimeMillis() < deadline) {
        Thread.sleep(250);
      }
      log.info("OpenCV Ocr filter lastText -> {}", ocr.lastText);
      log.info("Compare the two results in the log and in WebGui (TesseractOcr vs OpenCV Ocr filter).");
    } catch (Exception e) {
      log.error("TesseractOcrDemo failed", e);
    }
  }
}
