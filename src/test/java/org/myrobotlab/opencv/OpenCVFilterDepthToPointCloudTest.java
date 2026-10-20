package org.myrobotlab.opencv;

import static org.bytedeco.opencv.global.opencv_core.IPL_DEPTH_16U;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bytedeco.javacpp.indexer.UShortRawIndexer;
import org.bytedeco.opencv.opencv_core.AbstractIplImage;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.junit.Before;
import org.myrobotlab.math.geometry.PointCloud;

public class OpenCVFilterDepthToPointCloudTest extends AbstractOpenCVFilterTest {

  @Before
  public void setup() {
    debug = false;
  }

  @Override
  public OpenCVFilter createFilter() {
    OpenCVFilterDepthToPointCloud f = new OpenCVFilterDepthToPointCloud("filter");
    f.stride = 1;
    f.fx = 100f;
    f.fy = 100f;
    f.cx = 1f;
    f.cy = 1f;
    f.minDepthMm = 1;
    f.maxDepthMm = 10000;
    return f;
  }

  @Override
  public IplImage createTestImage() {
    IplImage depth = AbstractIplImage.create(3, 3, IPL_DEPTH_16U, 1);
    UShortRawIndexer idx = depth.createIndexer();
    try {
      for (int y = 0; y < 3; y++) {
        for (int x = 0; x < 3; x++) {
          idx.put(y, x, 0);
        }
      }
      idx.put(1, 1, 1500);
    } finally {
      idx.release();
    }
    return depth;
  }

  @Override
  public void verify(OpenCVFilter filter, IplImage input, IplImage output) {
    assertNotNull(output);
    PointCloud pc = filter.data.getPointCloud();
    assertNotNull(pc);
    assertEquals(1, pc.size());
    assertEquals(1.5f, pc.getData()[0].z, 1e-3f);
    assertTrue(Math.abs(pc.getData()[0].x) < 1e-3f);
    assertTrue(Math.abs(pc.getData()[0].y) < 1e-3f);
  }
}
