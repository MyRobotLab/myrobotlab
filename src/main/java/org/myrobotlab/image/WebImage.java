package org.myrobotlab.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Pojo transportable web image
 * 
 * @author GroG
 *
 */
public class WebImage {

  static final Logger log = LoggerFactory.getLogger(WebImage.class);

  /**
   * image data of the form
   */
  public String data;

  public String source;

  public long ts;

  public Integer frameIndex;

  public WebImage(final BufferedImage img, final String source, int frameIndex) {
    this(img, source, frameIndex, null, null);
  }

  public WebImage(final BufferedImage img, final String source, Integer frameIndex, final String type, final Double quality) {
    try {
      ts = System.currentTimeMillis();
      this.frameIndex = frameIndex;

      String imgType;

      if (type == null) {
        imgType = "jpg";
      } else {
        imgType = type;
      }

      this.source = source;

      final ByteArrayOutputStream os = new ByteArrayOutputStream();

      if (quality == null) {
        ImageIO.write(img, imgType, os);
        os.close();
        data = String.format("data:image/%s;base64,%s", type, Base64.getEncoder().encodeToString(os.toByteArray()));
      } else {

        // save jpeg image with specific quality. "1f" corresponds to 100% ,
        // default is "0.7f" corresponds to 70% without quality adjustment

        ImageWriter writer = ImageIO.getImageWritersByFormatName(imgType).next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        if (quality != null) {
          writeParam.setCompressionQuality(quality.floatValue());
        }

        writer.setOutput(ImageIO.createImageOutputStream(os));
        IIOImage outputImage = new IIOImage(img, null, null);
        writer.write(null, outputImage, writeParam);
        writer.dispose();
        os.close();

        data = String.format("data:image/jpeg;base64,%s", Base64.getEncoder().encodeToString(os.toByteArray()));
      }
    } catch (Exception e) {
      log.error("could not create WebImage", e);
    }
  }

}
