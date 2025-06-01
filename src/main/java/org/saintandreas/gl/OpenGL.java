package org.saintandreas.gl;

import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glLoadMatrixf;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
import static org.lwjgl.opengl.GL31.GL_PRIMITIVE_RESTART;
import static org.lwjgl.opengl.GL31.glPrimitiveRestartIndex;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.saintandreas.gl.buffers.IndexBuffer;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.buffers.VertexBuffer;
import org.saintandreas.gl.shaders.Attribute;
import org.saintandreas.gl.shaders.Program;
import org.saintandreas.gl.textures.Texture;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Vector2f;
import org.saintandreas.math.Vector3f;
import org.saintandreas.math.Vector4f;
import org.saintandreas.resources.Images;
import org.saintandreas.resources.Resource;

import com.google.common.collect.Lists;

public final class OpenGL {

  private OpenGL() {
  }

  public static void checkError() {
    int error = glGetError();
    if (error != 0) {
      // throw new IllegalStateException("GL error " + error);
    }
  }

  public static FloatBuffer toFloatBuffer(Matrix4f matrix) {
    FloatBuffer buffer = BufferUtils.getFloatBuffer(16);
    matrix.fillFloatBuffer(buffer);
    buffer.position(0);
    return buffer;
  }

  public static List<Vector4f> makeQuad(float size) {
    return makeQuad(new Vector2f(size, size));
  }

  public static List<Vector4f> makeQuad(Vector2f size) {
    Vector2f max = size.mult(0.5f);
    Vector2f min = max.mult(-1f);
    return makeQuad(min, max);
  }

  public static List<Vector4f> makeQuad(Vector2f min, Vector2f max) {
    List<Vector4f> result = new ArrayList<>(4);
    result.add(new Vector4f(min.x, min.y, 0, 1));
    result.add(new Vector4f(max.x, min.y, 0, 1));
    result.add(new Vector4f(min.x, max.y, 0, 1));
    result.add(new Vector4f(max.x, max.y, 0, 1));
    return result;
  }

  public static List<Vector4f> transformed(Collection<Vector4f> vs, Matrix4f m) {
    List<Vector4f> result = new ArrayList<>(vs.size());
    for (Vector4f v : vs) {
      result.add(m.mult(v));
    }
    return result;
  }

  public static List<Vector4f> interleaveConstants(Collection<? extends Vector4f> vs, Vector4f... attributes) {
    List<Vector4f> result = new ArrayList<>(vs.size() * (attributes.length + 1));
    for (Vector4f v : vs) {
      result.add(v);
      for (Vector4f a : attributes) {
        result.add(a);
      }
    }
    return result;
  }

  public static IndexedGeometry COLOR_CUBE = null;
  private static final float TAU = (float) Math.PI * 2.0f;

  public static IndexedGeometry makeColorCube() {
    if (null == COLOR_CUBE) {
      List<Vector4f> vertices = makeColorCubeVertices();
      List<Short> indices = makeColorCubeIndices();
      IndexedGeometry.Builder builder = new IndexedGeometry.Builder(indices, vertices);
      builder.withDrawType(GL_TRIANGLE_STRIP).withAttribute(Attribute.POSITION).withAttribute(Attribute.COLOR);
      COLOR_CUBE = builder.build();
    }
    return COLOR_CUBE;
  }

  protected static List<Short> makeColorCubeIndices() {
    List<Short> result = new ArrayList<>();
    short offset = 0;
    for (int i = 0; i < 6; ++i) {
      if (!result.isEmpty()) {
        result.add(Short.MAX_VALUE);
      }
      result.addAll(
          Lists.newArrayList(Short.valueOf((short) (offset + 0)), Short.valueOf((short) (offset + 1)), Short.valueOf((short) (offset + 2)), Short.valueOf((short) (offset + 3))));
      offset += 4;
    }
    return result;
  }

  protected static List<Vector4f> makeColorCubeVertices() {
    List<Vector4f> result = new ArrayList<>(6 * 4 * 2);
    Matrix4f m;
    List<Vector4f> q = makeQuad(1.0f);
    // Front
    m = new Matrix4f().translate(new Vector3f(0, 0, 0.5f));
    result.addAll(interleaveConstants(transformed(q, m), Colors.B));

    // Back
    m = new Matrix4f().rotate(TAU / 2f, Vector3f.UNIT_X).translate(new Vector3f(0, 0, 0.5f));
    result.addAll(interleaveConstants(transformed(q, m), Colors.Y));

    // Top
    m = new Matrix4f().rotate(TAU / -4f, Vector3f.UNIT_X).translate(new Vector3f(0, 0, 0.5f));
    result.addAll(interleaveConstants(transformed(q, m), Colors.G));

    // Bottom
    m = new Matrix4f().rotate(TAU / 4f, Vector3f.UNIT_X).translate(new Vector3f(0, 0, 0.5f));
    result.addAll(interleaveConstants(transformed(q, m), Colors.M));

    // Left
    m = new Matrix4f().rotate(TAU / -4f, Vector3f.UNIT_Y).translate(new Vector3f(0, 0, 0.5f));
    result.addAll(interleaveConstants(transformed(q, m), Colors.R));

    // Right
    m = new Matrix4f().rotate(TAU / 4f, Vector3f.UNIT_Y).translate(new Vector3f(0, 0, 0.5f));
    result.addAll(interleaveConstants(transformed(q, m), Colors.C));

    return result;
  }

  public static VertexBuffer toVertexBuffer(Collection<Vector4f> vertices) {
    FloatBuffer fb = BufferUtils.getFloatBuffer(vertices.size() * 4);
    for (Vector4f v : vertices) {
      v.fillBuffer(fb);
    }
    fb.position(0);
    VertexBuffer result = new VertexBuffer();
    result.bind();
    result.setData(fb);
    VertexBuffer.unbind();
    return result;
  }

  public static IndexBuffer toShortIndexBuffer(Collection<? extends Number> vertices) {
    ShortBuffer fb = BufferUtils.getShortBuffer(vertices.size());
    for (Number v : vertices) {
      fb.put(v.shortValue());
    }
    fb.position(0);
    IndexBuffer result = new IndexBuffer();
    result.bind();
    result.setData(fb);
    IndexBuffer.unbind();
    return result;
  }

  public static IndexBuffer toIntIndexBuffer(Collection<? extends Number> vertices) {
    IntBuffer fb = BufferUtils.getIntBuffer(vertices.size());
    for (Number v : vertices) {
      fb.put(v.intValue());
    }
    fb.position(0);
    IndexBuffer result = new IndexBuffer();
    result.bind();
    result.setData(fb);
    IndexBuffer.unbind();
    return result;
  }

  public static IndexedGeometry makeTexturedQuad(Vector2f min, Vector2f max) {
    return makeTexturedQuad(min, max, new Vector2f(0, 0), new Vector2f(1, 1));
  }

  public static IndexedGeometry makeTexturedQuad(Vector2f min, Vector2f max, Vector2f tmin, Vector2f tmax) {
    Vector2f texMin = tmin;
    Vector2f texMax = tmax;
    List<Vector4f> vertices = new ArrayList<>();
    vertices.add(new Vector4f(min.x, min.y, 0, 1));
    vertices.add(new Vector4f(texMin.x, texMin.y, 0, 0));
    vertices.add(new Vector4f(max.x, min.y, 0, 1));
    vertices.add(new Vector4f(texMax.x, texMin.y, 0, 0));
    vertices.add(new Vector4f(max.x, max.y, 0, 1));
    vertices.add(new Vector4f(texMax.x, texMax.y, 0, 0));
    vertices.add(new Vector4f(min.x, max.y, 0, 1));
    vertices.add(new Vector4f(texMin.x, texMax.y, 0, 0));
    List<Short> indices = new ArrayList<>();
    indices.add((short) 0); // LL
    indices.add((short) 1); // LR
    indices.add((short) 3); // UL
    indices.add((short) 2); // UR
    IndexedGeometry.Builder builder = new IndexedGeometry.Builder(indices, vertices);
    builder.withDrawType(GL_TRIANGLE_STRIP).withAttribute(Attribute.POSITION).withAttribute(Attribute.TEX);
    return builder.build();
  }

  private static final Map<Resource, Texture> CUBE_MAPS = new HashMap<>();
  private static final int RESOURCE_ORDER[] = { GL_TEXTURE_CUBE_MAP_NEGATIVE_X, GL_TEXTURE_CUBE_MAP_POSITIVE_X, GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
      GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, GL_TEXTURE_CUBE_MAP_POSITIVE_Z, };

  public static Texture getCubemapTextures(Resource... resources) {
    assert (resources.length > 0);
    Resource firstResource = resources[0];
    assert (null != firstResource);
    if (!CUBE_MAPS.containsKey(firstResource)) {

      Texture texture = new Texture(GL_TEXTURE_CUBE_MAP);
      texture.bind();
      texture.parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      texture.parameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      texture.parameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      texture.parameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
      texture.parameter(GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
      for (int i = 0; i < 6 && i < resources.length; ++i) {
        Resource imageResource = resources[i];
        if (null == imageResource) {
          continue;
        }
        int loadTarget = RESOURCE_ORDER[i];
        texture.loadImageData(Images.load(imageResource), loadTarget);
      }
      texture.unbind();
      CUBE_MAPS.put(firstResource, texture);
    }
    return CUBE_MAPS.get(firstResource);
  }

  public static IndexedGeometry makeTexturedQuad() {
    return makeTexturedQuad(1, Measure.valueOf(1.0f, SI.METER));
  }

  public static IndexedGeometry makeTexturedQuad(float aspect) {
    return makeTexturedQuad(aspect, Measure.valueOf(1.0f, SI.METER));
  }

  public static IndexedGeometry makeTexturedQuad(float aspect, Measure<Float, Length> size) {
    float halfSize = Math.abs(size.floatValue(SI.METER)) / 2.0f;
    Vector2f min = new Vector2f(-halfSize, -halfSize / aspect);
    Vector2f max = new Vector2f(halfSize, halfSize / aspect);
    Vector2f texMin = new Vector2f(0, 0);
    Vector2f texMax = new Vector2f(1, 1);
    return makeTexturedQuad(min, max, texMin, texMax);
  }

  public static void bindProjection(Program program) {
    program.setUniform("Projection", MatrixStack.PROJECTION.top());
  }

  public static void bindModelview(Program program) {
    program.setUniform("ModelView", MatrixStack.MODELVIEW.top());
  }

  public static void bindAll(Program program) {
    bindProjection(program);
    bindModelview(program);
  }

  @Deprecated
  public static void bindAll() {
    bindProjection();
    bindModelview();
  }

  @Deprecated
  public static void bindProjection() {
    glMatrixMode(GL_PROJECTION);
    loadMatrix(MatrixStack.PROJECTION.top());
  }

  @Deprecated
  public static void bindModelview() {
    glMatrixMode(GL_MODELVIEW);
    loadMatrix(MatrixStack.MODELVIEW.top());
  }

  // WARNING: not thread safe
  private static final FloatBuffer MATRIX_FLOAT_BUFFER = BufferUtils.getFloatBuffer(16);

  @Deprecated
  public static void loadMatrix(Matrix4f m) {
    glMatrixMode(GL_PROJECTION);
    MATRIX_FLOAT_BUFFER.rewind();
    m.fillFloatBuffer(MATRIX_FLOAT_BUFFER, true);
    MATRIX_FLOAT_BUFFER.rewind();
    glLoadMatrixf(MATRIX_FLOAT_BUFFER);
  }

  private static IndexedGeometry COLOR_CUBE_GEOMETRY = null;
  private static Program COLOR_CUBE_PROGRAM = null;

  public static void drawColorCube() {
    glEnable(GL_PRIMITIVE_RESTART);
    glPrimitiveRestartIndex(Short.MAX_VALUE);
    if (null == COLOR_CUBE_GEOMETRY) {
      COLOR_CUBE_GEOMETRY = makeColorCube();
    }
    if (null == COLOR_CUBE_PROGRAM) {
      COLOR_CUBE_PROGRAM = new Program(GlamourResources.SHADERS_COLORED_VS, GlamourResources.SHADERS_COLORED_FS);
      COLOR_CUBE_PROGRAM.link();
    }
    COLOR_CUBE_PROGRAM.use();
    bindAll(COLOR_CUBE_PROGRAM);
    COLOR_CUBE_GEOMETRY.ibo.bind();
    COLOR_CUBE_GEOMETRY.bindVertexArray();
    COLOR_CUBE_GEOMETRY.draw();
    VertexArray.unbind();
    IndexBuffer.unbind();
    Program.clear();
  }
}
