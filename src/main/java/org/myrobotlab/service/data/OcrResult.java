package org.myrobotlab.service.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured OCR output published by {@code TesseractOcr} (and consumed by the
 * OpenCV OCR filter overlay).
 */
public class OcrResult {

  public String text = "";
  public int meanConfidence;
  public String lang;
  public String source;
  public long timestamp = System.currentTimeMillis();
  public List<OcrWord> words = new ArrayList<>();

  public static class OcrWord {
    public String text;
    public float confidence;
    public int x;
    public int y;
    public int width;
    public int height;

    public OcrWord() {
    }

    public OcrWord(String text, float confidence, int x, int y, int width, int height) {
      this.text = text;
      this.confidence = confidence;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }
  }

  @Override
  public String toString() {
    return text == null ? "" : text;
  }
}
