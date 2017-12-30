package org.saintandreas.gl.buffers;

import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class VertexArray {
  int vao = -1;

  public VertexArray() {
    vao = glGenVertexArrays();
  }

  public void bind() {
    glBindVertexArray(vao);
  }

  public static void unbind() {
    glBindVertexArray(0);
  }

  public void destroy() {
    glDeleteVertexArrays(vao);
    vao = -1;
  }
}
