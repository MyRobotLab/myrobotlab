package org.myrobotlab.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.myrobotlab.service.config.TesseractOcrConfig;
import org.myrobotlab.service.data.OcrResult;
import org.myrobotlab.test.AbstractTest;

public class TesseractOcrTest extends AbstractTest {

  private static final String HUMANS = "src/test/resources/OpenCV/hiring_humans.jpg";

  @After
  public void tearDown() {
    Runtime.release("tesseract-ocr-test");
  }

  @Test
  public void testRecognizePrintedHello() throws Exception {
    TesseractOcr ocr = startOcr(7);
    Assume.assumeTrue("tesseract tessdata not installed", ocr.isReady());

    BufferedImage img = printedWord("HELLO");
    OcrResult result = ocr.recognize(img);
    assertNotNull(result);
    assertNotNull(result.text);
    String text = result.text.toUpperCase().replaceAll("[^A-Z]", "");
    assertTrue("expected HELLO in: " + result.text, text.contains("HELLO") || text.contains("HELL"));
    assertNotNull(result.words);
  }

  @Test
  public void testRecognizeFileDoesNotThrow() throws Exception {
    Assume.assumeTrue("sample image missing " + HUMANS, new File(HUMANS).isFile());
    TesseractOcr ocr = startOcr(11);
    Assume.assumeTrue("tesseract tessdata not installed", ocr.isReady());
    OcrResult humans = ocr.recognize(HUMANS);
    assertNotNull(humans);
    assertNotNull(humans.text);
    String viaOcr = ocr.ocr(HUMANS);
    assertNotNull(viaOcr);
    assertFalse(ocr.getInstalledLanguages().isEmpty());
  }

  @Test
  public void testPsmPresetAndWhitelistRoundTrip() {
    TesseractOcr ocr = (TesseractOcr) Runtime.create("tesseract-ocr-test", "TesseractOcr");
    ocr.getConfig().autoDownloadLang = false;
    ocr.setPsmPreset("line");
    assertTrue(ocr.getConfig().psm == 7);
    ocr.setWhitelist("ABC123");
    assertTrue("ABC123".equals(ocr.getConfig().whitelist));
    Runtime.release("tesseract-ocr-test");
  }

  private TesseractOcr startOcr(int psm) {
    TesseractOcr ocr = (TesseractOcr) Runtime.create("tesseract-ocr-test", "TesseractOcr");
    TesseractOcrConfig cfg = ocr.getConfig();
    cfg.autoDownloadLang = false;
    cfg.psm = psm;
    ocr.apply(cfg);
    ocr.startService();
    return ocr;
  }

  static BufferedImage printedWord(String word) {
    BufferedImage img = new BufferedImage(480, 96, BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = img.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, img.getWidth(), img.getHeight());
    g.setColor(Color.BLACK);
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
    g.drawString(word, 24, 72);
    g.dispose();
    return img;
  }
}
