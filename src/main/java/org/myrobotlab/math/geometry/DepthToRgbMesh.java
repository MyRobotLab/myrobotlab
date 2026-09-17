package org.myrobotlab.math.geometry;

import org.myrobotlab.service.data.DepthFrame;

/**
 * Organized depth grid → triangle mesh in camera meters, with UVs into an RGB
 * (or colormap) texture. Invalid depth and large Z jumps are not triangulated
 * so object edges do not sprout rubber sheets.
 */
public final class DepthToRgbMesh {

  public static final float DEFAULT_MAX_EDGE_M = 0.12f;

  private DepthToRgbMesh() {
  }

  public static final class Result {
    /** Camera-frame meters, length {@code vertexCount * 3} (X right, Y down, Z forward). */
    public float[] positions;
    /** Texture coordinates, length {@code vertexCount * 2}. */
    public float[] uvs;
    /** Triangle indices, length {@code indexCount} (multiples of 3). */
    public int[] indices;
    public int vertexCount;
    public int indexCount;
    public int gridWidth;
    public int gridHeight;
  }

  public static Result convert(DepthFrame frame) {
    return convert(frame, DEFAULT_MAX_EDGE_M);
  }

  public static Result convert(DepthFrame frame, float maxEdgeJumpM) {
    Result r = new Result();
    if (frame == null || frame.depthMm == null) {
      r.positions = new float[0];
      r.uvs = new float[0];
      r.indices = new int[0];
      return r;
    }
    int gw = frame.gridWidth();
    int gh = frame.gridHeight();
    r.gridWidth = gw;
    r.gridHeight = gh;
    if (gw <= 0 || gh <= 0 || frame.fx == 0f || frame.fy == 0f) {
      r.positions = new float[0];
      r.uvs = new float[0];
      r.indices = new int[0];
      return r;
    }

    int vertexCount = gw * gh;
    float[] positions = new float[vertexCount * 3];
    float[] uvs = new float[vertexCount * 2];
    boolean[] valid = new boolean[vertexCount];
    float[] zM = new float[vertexCount];
    float jump = maxEdgeJumpM > 0f ? maxEdgeJumpM : DEFAULT_MAX_EDGE_M;

    boolean rgbTex = frame.rgbWidth > 0 && frame.rgbHeight > 0;
    int texW = rgbTex ? frame.rgbWidth : Math.max(1, gw);
    int texH = rgbTex ? frame.rgbHeight : Math.max(1, gh);

    for (int row = 0; row < gh; row++) {
      for (int col = 0; col < gw; col++) {
        int i = row * gw + col;
        int mm = frame.depthMm[frame.index(col, row)];
        float uPix = col * frame.stride;
        float vPix = row * frame.stride;
        if (rgbTex) {
          uvs[i * 2] = (uPix + 0.5f) / texW;
          // OpenGL v=0 is bottom; camera row 0 is top of the image.
          uvs[i * 2 + 1] = 1f - (vPix + 0.5f) / texH;
        } else {
          uvs[i * 2] = (col + 0.5f) / texW;
          uvs[i * 2 + 1] = 1f - (row + 0.5f) / texH;
        }
        if (mm < frame.minDepthMm || mm > frame.maxDepthMm) {
          continue;
        }
        float z = mm / 1000f;
        positions[i * 3] = (uPix - frame.cx) * z / frame.fx;
        positions[i * 3 + 1] = (vPix - frame.cy) * z / frame.fy;
        positions[i * 3 + 2] = z;
        zM[i] = z;
        valid[i] = true;
      }
    }

    int[] idx = new int[Math.max(0, (gw - 1) * (gh - 1) * 6)];
    int nidx = 0;
    for (int row = 0; row < gh - 1; row++) {
      for (int col = 0; col < gw - 1; col++) {
        int i00 = row * gw + col;
        int i10 = i00 + 1;
        int i01 = i00 + gw;
        int i11 = i01 + 1;
        if (!valid[i00] || !valid[i10] || !valid[i01] || !valid[i11]) {
          continue;
        }
        if (Math.abs(zM[i00] - zM[i10]) > jump || Math.abs(zM[i00] - zM[i01]) > jump
            || Math.abs(zM[i10] - zM[i11]) > jump || Math.abs(zM[i01] - zM[i11]) > jump) {
          continue;
        }
        idx[nidx++] = i00;
        idx[nidx++] = i10;
        idx[nidx++] = i01;
        idx[nidx++] = i10;
        idx[nidx++] = i11;
        idx[nidx++] = i01;
      }
    }
    int[] packed = new int[nidx];
    System.arraycopy(idx, 0, packed, 0, nidx);
    r.positions = positions;
    r.uvs = uvs;
    r.indices = packed;
    r.vertexCount = vertexCount;
    r.indexCount = nidx;
    return r;
  }
}
