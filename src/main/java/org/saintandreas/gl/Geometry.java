package org.saintandreas.gl;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.util.LinkedHashMap;

import org.saintandreas.gl.buffers.VertexArray;
import org.saintandreas.gl.buffers.VertexBuffer;
import org.saintandreas.gl.shaders.Attribute;

public class Geometry {

  public static class Builder {
    protected final VertexBuffer vbo;
    protected int drawType = GL_TRIANGLES;
    protected final int elements;
    protected final LinkedHashMap<Integer, Integer> attributes = new LinkedHashMap<Integer, Integer>();

    public Builder(VertexBuffer vbo, int elements) {
      this.vbo = vbo;
      this.elements = elements;
    }

    public Builder withDrawType(int drawType) {
      this.drawType = drawType;
      return this;
    }

    public Builder withAttribute(Attribute attribute, int size) {
      attributes.put(attribute.location, size);
      return this;
    }

    public Builder withAttribute(Attribute attribute) {
      withAttribute(attribute, 4 * 4);
      return this;
    }

    public Builder withAttribute(int attribute, int size) {
      attributes.put(attribute, size);
      return this;
    }

    public Builder withAttribute(int attribute) {
      withAttribute(attribute, 4 * 4);
      return this;
    }

    protected VertexArray buildVertexArray() {
      VertexArray vao = new VertexArray();
      vao.bind();
      vbo.bind();
      int offset = 0;
      int stride = 0;
      for (Integer size : attributes.values()) {
        stride += size;
      }
      for (Integer location : attributes.keySet()) {
        int size = attributes.get(location);
        glEnableVertexAttribArray(location);
        glVertexAttribPointer(location, size >> 2, GL_FLOAT, false, stride, offset);
        offset += size;
      }
      VertexArray.unbind();
      VertexBuffer.unbind();
      return vao;
    }

    public Geometry build() {
      return new Geometry(vbo, buildVertexArray(), drawType, elements);
    }
  }

  public final VertexBuffer vbo;
  public final VertexArray vao;
  protected final int drawType;
  protected final int elements;

  public Geometry(VertexBuffer vbo, VertexArray vao, int drawType, int elements) {
    this.vbo = vbo;
    this.vao = vao;
    this.drawType = drawType;
    this.elements = elements;
  }

  public void bindVertexArray() {
    vao.bind();
  }

  public void draw() {
    glDrawArrays(drawType, 0, elements);
  }

  public VertexBuffer getVertxBuffer() {
    return vbo;
  }

  public void destroy() {
    vao.destroy();
    vbo.destroy();
  }
}
