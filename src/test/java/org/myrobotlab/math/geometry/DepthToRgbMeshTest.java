package org.myrobotlab.math.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.myrobotlab.service.data.DepthFrame;

public class DepthToRgbMeshTest {

  @Test
  public void planeIsTwoTriangles() {
    DepthFrame frame = flat(2, 2, 1000);
    DepthToRgbMesh.Result mesh = DepthToRgbMesh.convert(frame);
    assertEquals(4, mesh.vertexCount);
    assertEquals(6, mesh.indexCount);
    assertEquals(1f, mesh.positions[2], 1e-4f);
  }

  @Test
  public void depthJumpDropsTheQuad() {
    DepthFrame frame = flat(2, 2, 1000);
    frame.depthMm[3] = 5000;
    DepthToRgbMesh.Result mesh = DepthToRgbMesh.convert(frame, 0.12f);
    assertEquals(0, mesh.indexCount);
  }

  @Test
  public void rgbUvsSampleColorCameraPixels() {
    DepthFrame frame = flat(2, 2, 1000);
    frame.width = 4;
    frame.height = 4;
    frame.stride = 2;
    frame.depthMm = new int[] { 1000, 1000, 1000, 1000 };
    frame.rgbWidth = 4;
    frame.rgbHeight = 4;
    DepthToRgbMesh.Result mesh = DepthToRgbMesh.convert(frame);
    // col=1, stride=2 → uPix=2, u = 2.5/4
    assertEquals(2.5f / 4f, mesh.uvs[2], 1e-4f);
  }

  @Test
  public void syntheticHasRgbAndMesh() {
    DepthFrame frame = SyntheticDepth.planeWithBox(0);
    assertTrue(frame.rgb != null && frame.rgb.length == frame.width * frame.height * 3);
    DepthToRgbMesh.Result mesh = DepthToRgbMesh.convert(frame);
    assertTrue(mesh.indexCount > 100);
    assertEquals(frame.gridWidth() * frame.gridHeight(), mesh.vertexCount);
  }

  @Test
  public void decodeRgbFromJpeg() throws Exception {
    BufferedImage bi = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
    bi.setRGB(0, 0, 0xFF0000);
    bi.setRGB(1, 0, 0x00FF00);
    bi.setRGB(2, 0, 0x0000FF);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(bi, "jpg", out));
    DepthFrame frame = new DepthFrame();
    frame.rgbJpeg = out.toByteArray();
    frame.decodeRgb();
    assertEquals(3, frame.rgbWidth);
    assertEquals(2, frame.rgbHeight);
    assertEquals(18, frame.rgb.length);
  }

  private static DepthFrame flat(int w, int h, int mm) {
    DepthFrame frame = new DepthFrame();
    frame.width = w;
    frame.height = h;
    frame.stride = 1;
    frame.fx = 100f;
    frame.fy = 100f;
    frame.cx = (w - 1) / 2f;
    frame.cy = (h - 1) / 2f;
    frame.minDepthMm = 1;
    frame.maxDepthMm = 10000;
    frame.depthMm = new int[w * h];
    java.util.Arrays.fill(frame.depthMm, mm);
    return frame;
  }
}
