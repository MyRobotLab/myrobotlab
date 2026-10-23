package org.myrobotlab.service.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.Test;

public class OakDPreviewTest {

  @Test
  public void depthAndRgbSitAtTheSameSize() throws Exception {
    DepthFrame frame = new DepthFrame();
    frame.width = 16;
    frame.height = 8;
    frame.stride = 8;
    frame.minDepthMm = 100;
    frame.maxDepthMm = 1100;
    frame.depthMm = new int[] { 100, 1100 };
    frame.rgbWidth = 16;
    frame.rgbHeight = 8;
    frame.rgb = new byte[16 * 8 * 3];
    for (int i = 0; i < frame.rgb.length; i += 3) {
      frame.rgb[i] = (byte) 220;
      frame.rgb[i + 1] = 10;
      frame.rgb[i + 2] = 10;
    }

    OakDPreview preview = OakDPreview.from(frame);
    assertNotNull(preview);
    BufferedImage depth = decode(preview.depth);
    BufferedImage rgb = decode(preview.rgb);
    assertEquals(16, depth.getWidth());
    assertEquals(8, depth.getHeight());
    assertEquals(16, rgb.getWidth());
    assertEquals(8, rgb.getHeight());

    int near = depth.getRGB(2, 2);
    int far = depth.getRGB(12, 2);
    assertTrue("near depth should be redder than blue", red(near) > blue(near));
    assertTrue("far depth should be bluer than red", blue(far) > red(far));
    assertTrue(red(rgb.getRGB(4, 4)) > 150);
  }

  @Test
  public void colorJpegIsShownNextToDepth() throws Exception {
    DepthFrame frame = new DepthFrame();
    frame.width = 8;
    frame.height = 4;
    frame.stride = 1;
    frame.minDepthMm = 100;
    frame.maxDepthMm = 1000;
    frame.depthMm = new int[32];
    BufferedImage src = new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < 8; x++) {
      for (int y = 0; y < 4; y++) {
        src.setRGB(x, y, 0x0020A020);
      }
    }
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(src, "jpg", os));
    frame.rgbJpeg = os.toByteArray();

    OakDPreview preview = OakDPreview.from(frame);
    assertNotNull(preview);
    BufferedImage rgb = decode(preview.rgb);
    assertEquals(8, rgb.getWidth());
    assertEquals(4, rgb.getHeight());
    assertTrue(green(rgb.getRGB(2, 2)) > red(rgb.getRGB(2, 2)));
  }

  @Test
  public void largeFramesAreShrunkForTheSocket() {
    DepthFrame frame = new DepthFrame();
    frame.width = 640;
    frame.height = 400;
    frame.stride = 8;
    frame.minDepthMm = 100;
    frame.maxDepthMm = 4000;
    frame.depthMm = new int[frame.gridWidth() * frame.gridHeight()];
    for (int i = 0; i < frame.depthMm.length; i++) {
      frame.depthMm[i] = 1000;
    }

    OakDPreview preview = OakDPreview.from(frame);
    assertNotNull(preview);
    assertTrue(preview.width <= OakDPreview.MAX_EDGE);
    assertTrue(preview.height <= OakDPreview.MAX_EDGE);
    assertTrue(preview.width > preview.height);
  }

  @Test
  public void emptyFrameIsSkipped() {
    assertNull(OakDPreview.from(new DepthFrame()));
    assertNull(OakDPreview.from(null));
  }

  private static BufferedImage decode(String dataUrl) throws Exception {
    assertNotNull(dataUrl);
    String prefix = "data:image/jpeg;base64,";
    assertTrue(dataUrl.startsWith(prefix));
    byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(prefix.length()));
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
    assertNotNull(img);
    return img;
  }

  private static int red(int rgb) {
    return (rgb >> 16) & 0xFF;
  }

  private static int blue(int rgb) {
    return rgb & 0xFF;
  }

  private static int green(int rgb) {
    return (rgb >> 8) & 0xFF;
  }
}
