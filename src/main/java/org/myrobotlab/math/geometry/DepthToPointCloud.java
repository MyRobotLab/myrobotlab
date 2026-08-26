package org.myrobotlab.math.geometry;

import org.myrobotlab.service.data.DepthFrame;

/**
 * Unproject a {@link DepthFrame} to a camera-frame {@link PointCloud}.
 * <p>
 * Camera: X right, Y down, Z forward, meters. See {@link org.myrobotlab.service.data.DepthFrame}.
 */
public final class DepthToPointCloud {

  private DepthToPointCloud() {
  }

  public static PointCloud convert(DepthFrame frame) {
    if (frame == null || frame.depthMm == null) {
      return new PointCloud(new Point3df[0]);
    }
    int gw = frame.gridWidth();
    int gh = frame.gridHeight();
    if (gw <= 0 || gh <= 0 || frame.fx == 0f || frame.fy == 0f) {
      return new PointCloud(new Point3df[0]);
    }

    Point3df[] points = new Point3df[gw * gh];
    float[] colors = new float[gw * gh * 4];
    int count = 0;
    for (int row = 0; row < gh; row++) {
      for (int col = 0; col < gw; col++) {
        int mm = frame.depthMm[frame.index(col, row)];
        if (mm < frame.minDepthMm || mm > frame.maxDepthMm) {
          continue;
        }
        float z = mm / 1000f;
        float u = col * frame.stride;
        float v = row * frame.stride;
        float x = (u - frame.cx) * z / frame.fx;
        float y = (v - frame.cy) * z / frame.fy;
        points[count] = new Point3df(x, y, z);
        DepthColorMap.rgba(mm, frame.minDepthMm, frame.maxDepthMm, colors, count * 4);
        count++;
      }
    }
    Point3df[] packed = new Point3df[count];
    System.arraycopy(points, 0, packed, 0, count);
    float[] packedColors = new float[count * 4];
    System.arraycopy(colors, 0, packedColors, 0, count * 4);
    PointCloud pc = new PointCloud(packed);
    pc.setColors(packedColors);
    pc.setWidth(gw);
    pc.setHeight(gh);
    return pc;
  }
}
