package org.saintandreas.gl.buffers;

import static org.lwjgl.opengl.GL15.*;

public class VertexBuffer extends BaseBuffer {
  public VertexBuffer() {
    super(GL_ARRAY_BUFFER);
  }

  public static void unbind() {
    BaseBuffer.unbind(GL_ARRAY_BUFFER);
  }
}
