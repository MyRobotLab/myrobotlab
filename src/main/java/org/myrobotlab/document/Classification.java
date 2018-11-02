package org.myrobotlab.document;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.myrobotlab.service.Elasticsearch;
import org.slf4j.Logger;

public class Classification extends Document {

  public final static Logger log = LoggerFactory.getLogger(Classification.class);

  public Rectangle boundingBox;

  public Classification(String id) {
    super(id);
    setTs(System.currentTimeMillis());
  }
/*
  public Classification(String id, Rect boundingBox, float confidence, String label, int frameIndex, BufferedImage cropped, String sublabel) {
    super(id);
    setTs(System.currentTimeMillis());
  }
*/  

  public void setTs(long ts) {
    setField("ts", ts);
  }

  public Long getTs() {
    return (Long) getValue("ts");
  }

  public void setConfidence(float confidence) {
    setField("confidence", confidence);
  }

  public Float getConfidence() {
    return (Float) getValue("confidence");
  }

  public void setBoundingBox(int x, int y, int width, int height) {
    setField("boundingBox", new Rectangle(x, y, width, height));
  }

  public Rectangle getBoundingBox() {
    return (Rectangle) getValue("boundingBox");
  }

  public void setLabel(String label) {
    setField("label", label);
  }

  public String getLabel() {
    return (String) getValue("label");
  }

  public void setImage(BufferedImage image) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      baos.flush();
      byte[] bytes = baos.toByteArray();
      baos.close();
      setField("image", bytes);
    } catch (Exception e) {
      log.error("setImage threw", e);
    }
  }

  public BufferedImage getImage() {
    try {
      byte[] bytes = (byte[]) getValue("image");
      if (bytes != null) {
        InputStream in = new ByteArrayInputStream(bytes);
        BufferedImage bi = ImageIO.read(in);
        return bi;
      }

    } catch (Exception e) {
      log.error("setImage threw", e);
    }
    return null;
  }

  public void setObject(Object frame) {
    setField("imageObject", frame);
  }

  public Object getObject() {
    return getValue("imageObject");
  }

}
