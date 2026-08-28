package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.IPL_DEPTH_8U;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.indexer.UShortRawIndexer;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.geometry.DepthColorMap;
import org.myrobotlab.math.geometry.DepthToPointCloud;
import org.myrobotlab.math.geometry.PointCloud;
import org.myrobotlab.service.data.DepthFrame;
import org.slf4j.Logger;

/**
 * Convert a 16-bit 1-channel depth image (millimeters) to a camera-frame
 * {@link PointCloud} and a colorized display image.
 * <p>
 * Also registered as filter type {@code KinectPointCloud} via
 * {@link OpenCVFilterKinectPointCloud}.
 */
public class OpenCVFilterDepthToPointCloud extends OpenCVFilter {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(OpenCVFilterDepthToPointCloud.class);

  public int stride = 8;
  public float fx = 594.21f;
  public float fy = 591.04f;
  public float cx = 339.31f;
  public float cy = 242.74f;
  public int minDepthMm = 400;
  public int maxDepthMm = 4000;

  transient IplImage color;

  public OpenCVFilterDepthToPointCloud() {
    super();
  }

  public OpenCVFilterDepthToPointCloud(String name) {
    super(name);
  }

  @Override
  public void imageChanged(IplImage image) {
    color = null;
  }

  @Override
  public IplImage process(IplImage depth) throws InterruptedException {
    if (depth == null) {
      return depth;
    }
    if (depth.nChannels() != 1 || depth.depth() < 16) {
      return depth;
    }

    DepthFrame frame = fromIplImage(depth);
    PointCloud pc = DepthToPointCloud.convert(frame);
    if (data != null) {
      data.put(pc);
    }
    if (opencv != null) {
      opencv.invoke("publishPointCloud", pc);
    }

    if (color == null || color.width() != depth.width() || color.height() != depth.height()) {
      color = AbstractIplImage.create(depth.width(), depth.height(), IPL_DEPTH_8U, 3);
    }
    paintColorMap(depth, color, frame.minDepthMm, frame.maxDepthMm);
    return color;
  }

  DepthFrame fromIplImage(IplImage depth) {
    DepthFrame frame = new DepthFrame();
    frame.width = depth.width();
    frame.height = depth.height();
    frame.stride = Math.max(1, stride);
    frame.fx = fx;
    frame.fy = fy;
    frame.cx = cx;
    frame.cy = cy;
    frame.minDepthMm = minDepthMm;
    frame.maxDepthMm = maxDepthMm;
    frame.src = name;
    int gw = frame.gridWidth();
    int gh = frame.gridHeight();
    frame.depthMm = new int[gw * gh];
    UShortRawIndexer idx = depth.createIndexer();
    try {
      for (int row = 0; row < gh; row++) {
        int y = Math.min(row * frame.stride, depth.height() - 1);
        for (int col = 0; col < gw; col++) {
          int x = Math.min(col * frame.stride, depth.width() - 1);
          frame.depthMm[frame.index(col, row)] = idx.get(y, x) & 0xFFFF;
        }
      }
    } finally {
      idx.release();
    }
    return frame;
  }

  private static void paintColorMap(IplImage depth, IplImage color, int minMm, int maxMm) {
    UShortRawIndexer depthIdx = depth.createIndexer();
    org.bytedeco.javacpp.indexer.UByteIndexer colorIdx = color.createIndexer();
    try {
      for (int y = 0; y < depth.height(); y++) {
        for (int x = 0; x < depth.width(); x++) {
          int mm = depthIdx.get(y, x) & 0xFFFF;
          java.awt.Color c = DepthColorMap.colorForMm(mm, minMm, maxMm);
          colorIdx.put(y, x, 0, c.getBlue());
          colorIdx.put(y, x, 1, c.getGreen());
          colorIdx.put(y, x, 2, c.getRed());
        }
      }
    } finally {
      depthIdx.release();
      colorIdx.release();
    }
  }

  @Override
  public BufferedImage processDisplay(Graphics2D graphics, BufferedImage image) {
    return image;
  }
}
