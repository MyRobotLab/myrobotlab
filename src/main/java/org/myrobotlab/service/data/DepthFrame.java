package org.myrobotlab.service.data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

/**
 * A depth image in camera coordinates.
 * <p>
 * <b>Camera frame (OpenCV / DepthAI / Kinect):</b> X right, Y down, Z forward,
 * depth in millimeters along Z. Pinhole:
 * {@code x = (u - cx) * z / fx}, {@code y = (v - cy) * z / fy}, {@code z} in
 * meters after converting mm.
 * <p>
 * <b>JMonkeyEngine:</b> X right, Y up, Z forward, meters. Convert camera
 * points with {@code (x, y, z)_jme = (x, -y, z)_cam}. The overlay node copies
 * the chest camera world pose at scale 1 so 1 m of depth is 1 m for IK.
 */
public class DepthFrame implements Serializable {

  private static final long serialVersionUID = 1L;

  public int width;
  public int height;

  /**
   * Sample every N pixels. {@link #depthMm} is the packed strided grid
   * ({@link #gridWidth()} by {@link #gridHeight()}).
   */
  public int stride = 1;

  /** Packed row-major millimeters, length {@code gridWidth() * gridHeight()}. */
  public int[] depthMm;

  /**
   * Optional JPEG of the color camera, typically aligned to {@link #width} x
   * {@link #height}. Decoded into {@link #rgb} by {@link #decodeRgb()}.
   */
  public byte[] rgbJpeg;

  /**
   * Packed RGB888, row-major, top of the image first. Size
   * {@code rgbWidth * rgbHeight * 3}. Used as the JME mesh ColorMap.
   */
  public byte[] rgb;

  public int rgbWidth;
  public int rgbHeight;

  public float fx = 250f;
  public float fy = 250f;
  public float cx = 160f;
  public float cy = 120f;

  public int minDepthMm = 200;
  public int maxDepthMm = 8000;

  public String src;
  public long ts = System.currentTimeMillis();
  public long frameId;

  public int gridWidth() {
    if (width <= 0 || stride <= 0) {
      return 0;
    }
    return (width + stride - 1) / stride;
  }

  public int gridHeight() {
    if (height <= 0 || stride <= 0) {
      return 0;
    }
    return (height + stride - 1) / stride;
  }

  public int index(int col, int row) {
    return row * gridWidth() + col;
  }

  /**
   * Scale {@link #fx}/{@link #fy}/{@link #cx}/{@link #cy} into this frame's
   * pixel size. DepthAI {@code getCameraIntrinsics(socket, w, h)} usually does
   * this; if it returns a full-sensor matrix (principal point outside the
   * image) XY unprojection is too small — objects look doll-sized at the
   * correct Z. Assumes a roughly centered principal point on the native sensor.
   *
   * @return true if the matrix was rescaled
   */
  public boolean normalizeIntrinsics() {
    if (width <= 0 || height <= 0) {
      return false;
    }
    boolean changed = false;
    if (cx > width * 1.05f && cx > 1f) {
      float s = width / (2f * cx);
      fx *= s;
      cx *= s;
      changed = true;
    }
    if (cy > height * 1.05f && cy > 1f) {
      float s = height / (2f * cy);
      fy *= s;
      cy *= s;
      changed = true;
    }
    return changed;
  }

  /**
   * Fill {@link #rgb} from {@link #rgbJpeg} when packed RGB is missing.
   */
  public void decodeRgb() {
    if (rgb != null && rgbWidth > 0 && rgbHeight > 0 && rgb.length >= rgbWidth * rgbHeight * 3) {
      return;
    }
    if (rgbJpeg == null || rgbJpeg.length == 0) {
      return;
    }
    try {
      BufferedImage bi = ImageIO.read(new ByteArrayInputStream(rgbJpeg));
      if (bi == null) {
        return;
      }
      rgbWidth = bi.getWidth();
      rgbHeight = bi.getHeight();
      rgb = new byte[rgbWidth * rgbHeight * 3];
      int i = 0;
      for (int y = 0; y < rgbHeight; y++) {
        for (int x = 0; x < rgbWidth; x++) {
          int argb = bi.getRGB(x, y);
          rgb[i++] = (byte) ((argb >> 16) & 0xFF);
          rgb[i++] = (byte) ((argb >> 8) & 0xFF);
          rgb[i++] = (byte) (argb & 0xFF);
        }
      }
    } catch (Exception ignored) {
      // leave rgb unset; mesh path falls back to the depth colormap
    }
  }
}
