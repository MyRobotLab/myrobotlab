package org.myrobotlab.service;

import static org.bytedeco.leptonica.global.lept.pixDestroy;
import static org.bytedeco.leptonica.global.lept.pixRead;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.TesseractOcrConfig;
import org.slf4j.Logger;

/**
 * FIXME - consider -
 * https://stackoverflow.com/questions/1813881/java-ocr-implementation If this
 * thing is no worky ...
 * 
 * TesseractOCR - This service will use the open source project tesseract.
 * Tesseract will take an Image and extract any recognizable text from that
 * image as a string.
 * 
 * ara - arabic chi_sim - simplified chinese deu - german eng - english fra -
 * french grc - greek heb - hebrew hin - hindi isl - icelandic ita - italian jpn
 * - japanese kor - korean por - portugues rus - russian spa - spanish tha -
 * thai vie - vietnamese
 * 
 */
public class TesseractOcr extends Service<TesseractOcrConfig> {

  private static final long serialVersionUID = 1L;
  transient private TessBaseAPI api = null;
  // define which language model to load
  String lang = "eng";
  public final static Logger log = LoggerFactory.getLogger(TesseractOcr.class);

  public TesseractOcr(String n, String id) {
    super(n, id);
  }

  @Override
  synchronized public void startService() {
    super.startService();
    initModel(lang);
  }

  @Override
  synchronized public void stopService() {
    super.stopService();
    // stop the tesseract api.
    // TODO: additional memory/object cleanup?
    api.End();
  }

  /**
   * Load the language specific model as the current language model.
   * 
   * @param lang
   *          - the 3 leter code
   */
  public void initModel(String lang) {
    api = new TessBaseAPI();
    String tessData = "resource/TesseractOcr";
    // load the models
    if (api.Init(tessData, lang) != 0) {
      warn("Unable to load tesseract model in %s with language %s", tessData, lang);
    }
  }

  public String ocr(BufferedImage image) throws IOException {
    // The tesseract api has some issues reading the image directly as a byte
    // array.
    // so for now... until that changes, we'll write a temp file to be ocr'd and
    String tempFilename = "tesseract." + UUID.randomUUID().toString() + ".png";
    File tempFile = new File(getDataDir(), tempFilename);
    FileOutputStream fos = new FileOutputStream(tempFile);
    ImageIO.write(image, "png", fos);
    String result = ocr(tempFile.getAbsolutePath());
    tempFile.delete();
    return result;
  }

  public String ocr(String filename) throws IOException {
    BytePointer outText;
    PIX image = pixRead(filename);
    api.SetImage(image);
    // Get OCR result
    outText = api.GetUTF8Text();
    String ret = outText.getString();
    // log.info("OCR output:\n" + ret);
    // Destroy used object and release memory
    outText.deallocate();
    pixDestroy(image);
    return ret;
  }

  public String ocr(SerializableImage image) throws IOException {
    return ocr(image.getImage());
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    try {
      TesseractOcr tesseract = (TesseractOcr) Runtime.start("tesseract", "TesseractOcr");
      String found = tesseract.ocr("src/test/resources/OpenCV/i_am_a_droid.jpg");
      // String found =
      // tesseract.ocr("src/test/resources/OpenCV/hiring_humans.jpg");
      log.info("found {}", found);
      Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
