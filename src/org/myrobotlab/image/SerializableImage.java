/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Vision;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         This class is a serializable image - depending on constuctor and
 *         source the image info can come from a variety of sources converting
 *         between return types "should" cache the resultant type
 * 
 */
public class SerializableImage implements Serializable {

  public final static Logger log = LoggerFactory.getLogger(Vision.class.getCanonicalName());

  private static final long serialVersionUID = 1L;

  /**
   * internal buffered image
   */
  transient private BufferedImage image;

  /**
   * jpg encoded byte buffer - TODO offer type png tff etc? TODO - consider
   * hashmap cache similar to the OpenCVData ???
   */
  private ByteBuffer buffer;

  private byte[] bytes;

  private String source;
  private long timestamp;
  public int frameIndex;

  public static void main(String[] args) throws Exception {
    try {
      LoggingFactory.getInstance().configure();
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("object.data"));

      ImageIO.write(null, "jpg", new MemoryCacheImageOutputStream(out));
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public static void writeToFile(BufferedImage img, String filename) {
    try {
      FileOutputStream out = new FileOutputStream(new File(filename));
      String extension = null;
      int i = filename.lastIndexOf('.');
      if (i > 0) {
        extension = filename.substring(i + 1);
      }

      if (extension != null) {
        ImageIO.write(img, extension, new MemoryCacheImageOutputStream(out));
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  public SerializableImage(BufferedImage image, String source) {
    this.source = source;
    this.image = image;
    this.timestamp = System.currentTimeMillis();
  }

  public SerializableImage(BufferedImage image, String source, int frameIndex) {
    this.source = source;
    this.image = image;
    this.frameIndex = frameIndex;
    this.timestamp = System.currentTimeMillis();
  }

  public SerializableImage(byte[] buffer, String source, int frameIndex) {
    this.source = source;
    this.bytes = buffer;
    this.frameIndex = frameIndex;
    this.timestamp = System.currentTimeMillis();
  }

  public SerializableImage(ByteBuffer buffer, String source, int frameIndex) {
    this.source = source;
    this.buffer = buffer;
    this.frameIndex = frameIndex;
    this.timestamp = System.currentTimeMillis();
  }

  public ByteBuffer getByteBuffer() {
    return buffer;
  }

  public byte[] getBytes() {
    if (bytes != null) {
      return bytes;
    }

    if (buffer != null) {
      bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      return bytes;
    }

    if (image != null) {
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", new MemoryCacheImageOutputStream(bos));
        bytes = bos.toByteArray();
        return bytes;
      } catch (Exception e) {
        Logging.logError(e);
      }
    }
    // TODO image --to--> bytes
    return null;
  }

  public int getHeight() {
    return image.getHeight();
  }

  public BufferedImage getImage() {
    if (image != null)
      return image;

    try {
      if (bytes != null) {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        image = ImageIO.read(inputStream);
        return image;
      }

      if (buffer != null) {
        // FIXME - this does not work (always) :(
        // not thread safe
        bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        InputStream inputStream = new ByteArrayInputStream(bytes);
        image = ImageIO.read(inputStream);
      }
    } catch (Exception e) {
      Logging.logError(e);
    }

    return null;
  }

  public String getSource() {
    return source;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getWidth() {
    return image.getWidth();
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    image = (ImageIO.read(new MemoryCacheImageInputStream(in)));
    Logging.logTime("readObject");
  }

  public void setImage(BufferedImage image) {
    this.image = image;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  // FIXME ??? use OpenCV cvEncode ???
  // FIXME !! PNG default ???
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    ImageIO.write(image, "jpg", new MemoryCacheImageOutputStream(out));
    Logging.logTime("writeObject");
  }

  public void writeToFile(String filename) {
    writeToFile(image, filename);
  }

}