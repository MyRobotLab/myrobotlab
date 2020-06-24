package org.myrobotlab.service;

import static org.bytedeco.leptonica.global.lept.pixDestroy;
import static org.bytedeco.leptonica.global.lept.pixRead;
import static org.bytedeco.leptonica.global.lept.pixReadMem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.myrobotlab.framework.Service;
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
  transient private TessBaseAPI api = null;

  // define which language model to load
  String lang = "eng";
  public final static Logger log = LoggerFactory.getLogger(TesseractOcr.class);
  private static final double INCH_2_CM = 2.54;

  public TesseractOcr(String n, String id) {
    super(n, id);
  }

  @Override
  synchronized public void startService() {
    super.startService();
    System.err.println("Start Service called? ");
    initModel(lang);
  }

  @Override
  synchronized public void stopService() {
    super.stopService();
    // stop the tesseract api.
    // TODO: additional memory/object cleanup?
    api.End();
  }

  private void initModel(String lang) {
    api = new TessBaseAPI();
    String tessData = "resource/TesseractOcr";
    // load the models
    if (api.Init(tessData, lang) != 0) {
      log.warn("Unable to load tesseract model in {} with language {}", tessData, lang);
      // TODO: what's the lifecycle if a service fails to start?  maybe we should throw?
    }
  }

  public String ocr(BufferedImage image) throws IOException {
    // The tesseract api has some issues reading the image directly as a byte array.
    // so for now... until that changes, we'll write a temp file to be ocr'd and 
    // TODO: where is this.. and i think we need to randomize the name  of it
    // or synchronize this method.
    File temp = File.createTempFile("tesseract", ".png");
    // This method. in theory sets the DPI on the saved file to avoid tesseract warnings.
    // saveGridImage(image, temp);
    FileOutputStream fos = new FileOutputStream(temp);
    ImageIO.write(image, "png", fos);
    temp.deleteOnExit();
    return ocr(temp.getAbsolutePath());
    // A class called PIXConversions will convert directly
    // but I'm afraid to import the library in that it might clash with the
    // version javacv is expecting.. so we are going to do it the "file" way :P
    // TODO: is there a better way to convert the buffered image to a byte array?
    //    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //    // TODO: what format makes sense? perhaps something lossless?
    //    // TODO: try png , tif, bmp.. etc.
    //    ImageIO.write( image, "jpg", baos );
    //    baos.flush();
    //    byte[] bytes = baos.toByteArray();
    //    baos.close();
    //    PIX pixImage = pixReadMem(bytes, bytes.length);
    //    api.SetImage(pixImage);
    //    // Get OCR result
    //    BytePointer outText = api.GetUTF8Text();
    //    String ret = outText.getString();
    //    // Destroy used object and release memory
    //    outText.deallocate();
    //    pixDestroy(pixImage);
    //    return ret;
  }

  public String ocr(String filename) throws IOException {
    BytePointer outText;
    // TODO: i don't think we want ot call this each time.
    // initModel("eng");
    // Open input image with leptonica library
    // PIX image = pixRead(filename);
    // byte[] bytes = FileUtil.read(filename);
    PIX image = pixRead(filename);
    api.SetImage(image);
    // Get OCR result
    outText = api.GetUTF8Text();
    String ret = outText.getString();
    //log.info("OCR output:\n" + ret);
    // Destroy used object and release memory
    // TODO: move this to the stop service lifecycle.
    // api.End();
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
      // String found = tesseract.ocr("src/test/resources/OpenCV/hiring_humans.jpg");
      log.info("found {}", found);
      Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  // taken from https://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image
  private void saveGridImage(BufferedImage gridImage, File output) throws IOException {
    output.delete();
    final String formatName = "png";
    for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
      ImageWriter writer = iw.next();
      ImageWriteParam writeParam = writer.getDefaultWriteParam();
      ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
      IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
      if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
        continue;
      }
      setDPI(metadata);
      final ImageOutputStream stream = ImageIO.createImageOutputStream(output);
      try {
        writer.setOutput(stream);
        writer.write(metadata, new IIOImage(gridImage, null, metadata), writeParam);
      } finally {
        stream.close();
      }
      break;
    }
  }

  private void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {
    // for PMG, it's dots per millimeter
    double DPI = 70;
    double dotsPerMilli = 1.0 * DPI / 10 / INCH_2_CM;
    IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
    horiz.setAttribute("value", Double.toString(dotsPerMilli));
    IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
    vert.setAttribute("value", Double.toString(dotsPerMilli));
    IIOMetadataNode dim = new IIOMetadataNode("Dimension");
    dim.appendChild(horiz);
    dim.appendChild(vert);
    IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
    root.appendChild(dim);
    metadata.mergeTree("javax_imageio_1.0", root);
  }

}
