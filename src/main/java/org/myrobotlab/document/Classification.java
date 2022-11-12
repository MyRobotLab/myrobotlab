package org.myrobotlab.document;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.Rectangle;
import org.slf4j.Logger;

public class Classification extends Document {

  public final static Logger log = LoggerFactory.getLogger(Classification.class);

  public Classification(String id) {
    super(id);
    setTs(System.currentTimeMillis());
  }

  public Classification(String id, float confidence, Rectangle rect) {
    super(id);
    setLabel(id);
    setTs(System.currentTimeMillis());
    setConfidence(confidence);
    setBoundingBox(rect);
  }

  public Classification(String FACE_LABEL, float confidence, Rectangle rect, double centerX, double centerY) {
    this(FACE_LABEL, confidence, rect);
    setCenterX(centerX);
    setCenterY(centerY);
  }

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

  public void setCenterX(double confidence) {
    setField("centerX", confidence);
  }

  public Double getCenterX() {
    return (Double) getValue("centerX");
  }

  public void setCenterY(double confidence) {
    setField("centerY", confidence);
  }

  public Double getCenterY() {
    return (Double) getValue("centerY");
  }

  public void setBoundingBox(int x, int y, int width, int height) {
    setField("bounding_box", new Rectangle(x, y, width, height));
  }

  public Rectangle getBoundingBox() {
    return (Rectangle) getValue("bounding_box");
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

  public void setBoundingBox(Rectangle rect) {
    setField("bounding_box", rect);
  }

}
