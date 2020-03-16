package org.myrobotlab.service;

import static org.bytedeco.leptonica.global.lept.pixDestroy;
import static org.bytedeco.leptonica.global.lept.pixRead;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
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
 */
public class TesseractOcr extends Service {

  private static final long serialVersionUID = 1L;
  transient private TessBaseAPI api;

  public final static Logger log = LoggerFactory.getLogger(TesseractOcr.class);

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {

      TesseractOcr tesseract = (TesseractOcr) Runtime.start("tesseract", "TesseractOcr");
      // String found = tesseract.ocr("phototest.jpg");
      // String found = tesseract.ocr("30.speed.JPG");
      String found = tesseract.ocr("traffic.sign.jpg");

      // String found = tesseract.ocr("test.jpg");
      // String found = tesseract.ocr("test.tif");
      log.info("found {}", found);
      Runtime.start("gui", "SwingGui");

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public TesseractOcr(String n, String id) {
    super(n, id);
  }

  public String ocr(BufferedImage image) throws IOException {
    // A class called PIXConversions will convert directly
    // but I'm afraid to import the library in that it might clash with the
    // version
    // javacv is expecting.. so we are going to do it the "file" way :P
    File temp = File.createTempFile("tesseract", ".jpg");
    FileOutputStream fos = new FileOutputStream(temp);
    ImageIO.write(image, "jpg", fos);
    temp.deleteOnExit();
    return ocr(temp.getAbsolutePath());
  }

  public String ocr(String filename) throws IOException {

    BytePointer outText;
    if (api == null) {
      api = new TessBaseAPI();
    }
    // Initialize tesseract-ocr with English, without specifying tessdata path
    // FIXME - maybe don't just dump in the root - perhaps subdirectory - and
    // what
    // about integrating with other /resources ?
    if (api.Init(System.getProperty("user.dir"), "eng") != 0) {
      log.error("Could not initialize tesseract.");
    }

    // Open input image with leptonica library
    PIX image = pixRead(filename);
    api.SetImage(image);
    // Get OCR result
    outText = api.GetUTF8Text();
    String ret = outText.getString();
    log.info("OCR output:\n" + ret);

    // Destroy used object and release memory
    api.End();
    outText.deallocate();
    pixDestroy(image);
    return ret;
  }

  public String ocr(SerializableImage image) throws IOException {
    return ocr(image.getImage());
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {
    ServiceType meta = new ServiceType(TesseractOcr.class);
    meta.addDescription("Optical character recognition - the ability to read");
    meta.addCategory("ai","vision");
    meta.addDependency("org.bytedeco", "tesseract", "4.1.0-1.5.2");
    meta.addDependency("org.bytedeco", "tesseract-platform", "4.1.0-1.5.2");
    return meta;
  }

}
