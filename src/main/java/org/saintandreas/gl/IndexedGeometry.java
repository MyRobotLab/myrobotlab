package org.saintandreas.gl;

import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;

import java.util.List;

import org.saintandreas.gl.buffers.IndexBuffer;
import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.buffers.VertexBuffer;
import org.saintandreas.math.Vector4f;

public class IndexedGeometry extends Geometry {
  public final IndexBuffer ibo;

  public static class Builder extends Geometry.Builder {
    protected final IndexBuffer ibo;

    public Builder(List<? extends Number> indices, List<Vector4f> vertices) {
      this(OpenGL.toIntIndexBuffer(indices), OpenGL.toVertexBuffer(vertices), indices.size());
    }

    public Builder(IndexBuffer ibo, VertexBuffer vbo, int elements) {
      super(vbo, elements);
      this.ibo = ibo;
    }

    @Override
    protected VertexArray buildVertexArray() {
      VertexArray vao = super.buildVertexArray();
      return vao;
    }

    @Override
    public IndexedGeometry build() {
      return new IndexedGeometry(ibo, vbo, buildVertexArray(), drawType, elements);
    }
  }

  public IndexedGeometry(IndexBuffer ibo, VertexBuffer vbo, VertexArray vao, int drawType, int elements) {
    super(vbo, vao, drawType, elements);
    this.ibo = ibo;
  }

  @Override
  public void bindVertexArray() {
    super.bindVertexArray();
    ibo.bind();
  }

  @Override
  public void draw() {
    glDrawElements(drawType, elements, GL_UNSIGNED_INT, 0);
  }

  @Override
  public void destroy() {
    super.destroy();
    ibo.destroy();
  }

}
