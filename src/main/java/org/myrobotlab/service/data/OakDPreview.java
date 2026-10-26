package org.myrobotlab.service.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.myrobotlab.math.geometry.DepthColorMap;

/**
 * JPEG data-URLs of one OAK-D frame for the WebGui: color camera and a
 * colorized depth map at the same pixel size. Near depth is red, far is blue,
 * and missing returns are black.
 */
public class OakDPreview implements Serializable {

  private static final long serialVersionUID = 1L;

  /** Longest edge of the WebGui images. Keeps a frame pair under a typical socket limit. */
  static final int MAX_EDGE = 320;

  /** {@code data:image/jpeg;base64,...} or null when no color frame arrived. */
  public String rgb;

  /** {@code data:image/jpeg;base64,...} colorized depth, or null. */
  public String depth;

  public int width;
  public int height;

  public long ts;
  public long frameId;

  /**
   * Build a web preview from a depth frame. The depth map is nearest-neighbor
   * expanded from the strided grid to {@link DepthFrame#width} x
   * {@link DepthFrame#height} so it lines up with the color image.
   *
   * @return null when the frame has neither a depth image nor a color image
   */
  public static OakDPreview from(DepthFrame frame) {
    if (frame == null) {
      return null;
    }
    OakDPreview preview = new OakDPreview();
    preview.width = frame.width;
    preview.height = frame.height;
    preview.ts = frame.ts;
    preview.frameId = frame.frameId;
    BufferedImage depth = scale(depthImage(frame));
    BufferedImage rgb = scale(rgbImage(frame));
    preview.depth = jpegDataUrl(depth);
    preview.rgb = jpegDataUrl(rgb);
    if (preview.rgb == null && preview.depth == null) {
      return null;
    }
    if (depth != null) {
      preview.width = depth.getWidth();
      preview.height = depth.getHeight();
    } else if (rgb != null) {
      preview.width = rgb.getWidth();
      preview.height = rgb.getHeight();
    }
    return preview;
  }

  static BufferedImage rgbImage(DepthFrame frame) {
    if (frame.rgbJpeg != null && frame.rgbJpeg.length > 0) {
      try {
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(frame.rgbJpeg));
        if (decoded != null) {
          return decoded;
        }
      } catch (IOException ignored) {
        // fall through to packed RGB
      }
    }
    if (frame.rgb == null || frame.rgbWidth <= 0 || frame.rgbHeight <= 0) {
      return null;
    }
    int w = frame.rgbWidth;
    int h = frame.rgbHeight;
    int n = w * h;
    if (frame.rgb.length < n * 3) {
      return null;
    }
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    int[] pixels = new int[n];
    int i = 0;
    for (int p = 0; p < n; p++) {
      int r = frame.rgb[i++] & 0xFF;
      int g = frame.rgb[i++] & 0xFF;
      int b = frame.rgb[i++] & 0xFF;
      pixels[p] = (r << 16) | (g << 8) | b;
    }
    img.setRGB(0, 0, w, h, pixels, 0, w);
    return img;
  }

  /**
   * Colorized depth at the frame's full width and height. Each strided sample
   * is painted across the pixels it covers.
   */
  static BufferedImage depthImage(DepthFrame frame) {
    if (frame.depthMm == null || frame.width <= 0 || frame.height <= 0) {
      return null;
    }
    int gw = frame.gridWidth();
    int gh = frame.gridHeight();
    int cells = gw * gh;
    if (gw <= 0 || gh <= 0 || frame.depthMm.length < cells) {
      return null;
    }
    int stride = Math.max(1, frame.stride);
    int[] cell = new int[cells];
    for (int i = 0; i < cells; i++) {
      Color c = DepthColorMap.colorForMm(frame.depthMm[i], frame.minDepthMm, frame.maxDepthMm);
      cell[i] = c.getRGB();
    }
    int[] pixels = new int[frame.width * frame.height];
    for (int y = 0; y < frame.height; y++) {
      int row = Math.min(y / stride, gh - 1);
      int rowOff = row * gw;
      int dest = y * frame.width;
      for (int x = 0; x < frame.width; x++) {
        int col = Math.min(x / stride, gw - 1);
        pixels[dest + x] = cell[rowOff + col];
      }
    }
    BufferedImage img = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, frame.width, frame.height, pixels, 0, frame.width);
    return img;
  }

  static BufferedImage scale(BufferedImage img) {
    if (img == null) {
      return null;
    }
    int w = img.getWidth();
    int h = img.getHeight();
    int longest = Math.max(w, h);
    if (longest <= MAX_EDGE || longest <= 0) {
      return img;
    }
    int nw = Math.max(1, w * MAX_EDGE / longest);
    int nh = Math.max(1, h * MAX_EDGE / longest);
    BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(img, 0, 0, nw, nh, null);
    g.dispose();
    return out;
  }

  private static String jpegDataUrl(BufferedImage img) {
    if (img == null) {
      return null;
    }
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      if (!ImageIO.write(img, "jpg", os)) {
        return null;
      }
      return dataUrl(os.toByteArray());
    } catch (IOException e) {
      return null;
    }
  }

  private static String dataUrl(byte[] jpeg) {
    return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
  }
}
