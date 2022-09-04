package org.saintandreas.gl.buffers;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;

public class IndexBuffer extends BaseBuffer {
  public IndexBuffer() {
    super(GL_ELEMENT_ARRAY_BUFFER);
  }

  public static void unbind() {
    unbind(GL_ELEMENT_ARRAY_BUFFER);
  }

}
