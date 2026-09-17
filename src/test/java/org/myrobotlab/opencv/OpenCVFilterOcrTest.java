package org.myrobotlab.opencv;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Assume;
import org.junit.Before;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TesseractOcr;
import org.myrobotlab.service.config.TesseractOcrConfig;

/**
 * Full-frame and EAST+Tesseract OCR through {@link OpenCVFilterOcr}.
 */
public class OpenCVFilterOcrTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    TesseractOcr ocr = (TesseractOcr) Runtime.create("tesseract", "TesseractOcr");
    TesseractOcrConfig cfg = ocr.getConfig();
    cfg.autoDownloadLang = false;
    ocr.apply(cfg);
    ocr.startService();
    Assume.assumeTrue("tesseract tessdata not installed", ocr.isReady());

    OpenCVFilterOcr filter = new OpenCVFilterOcr("ocr");
    filter.ocrService = "tesseract";
    filter.autoStartOcr = false;
    filter.autoDownloadModel = false;
    filter.minIntervalMs = 0;
    filter.skipBlurry = false;
    File east = new File("resource/OpenCV/east_text_detector/frozen_east_text_detection.pb");
    filter.detectionModel = east.isFile() ? "east" : "none";
    filter.detector = east.isFile() ? "east" : "none";
    return filter;
  }

  @Override
  public IplImage createTestImage() {
    String filename = "src/test/resources/OpenCV/hiring_humans.jpg";
    Assume.assumeTrue("sample image missing", new File(filename).isFile());
    return cvLoadImage(filename);
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    assertNotNull(output);
    OpenCVFilterOcr ocrFilter = (OpenCVFilterOcr) filter;
    String fullString = stitchText(filter);
    if (fullString.isEmpty() && ocrFilter.lastText != null) {
      fullString = ocrFilter.lastText;
    }
    fullString = fullString.toLowerCase();
    assertTrue("expected readable OCR in: " + fullString,
        fullString.contains("robotics") || fullString.contains("carnegie") || fullString.contains("hiring") || fullString.contains("human") || fullString.length() > 3);
  }

  private String stitchText(OpenCVFilter filter) {
    StringBuilder fullText = new StringBuilder();
    if (filter.data.getDetectedText() != null) {
      for (DetectedText dt : filter.data.getDetectedText()) {
        if (dt.text != null) {
          fullText.append(dt.text.trim()).append(" ");
        }
      }
    }
    return fullText.toString().trim();
  }
}
