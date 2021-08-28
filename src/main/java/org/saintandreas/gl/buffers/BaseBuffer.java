package org.saintandreas.gl.buffers;

import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BaseBuffer {

  int buffer = -1;
  final int target;

  public BaseBuffer(int buffer, int target) {
    this.target = target;
    this.buffer = buffer;
  }

  public BaseBuffer(int target) {
    this.target = target;
    buffer = glGenBuffers();
  }

  public void bind() {
    glBindBuffer(target, buffer);
  }

  public static void unbind(int target) {
    glBindBuffer(target, 0);
  }

  public void setData(ByteBuffer data) {
    setData(data, GL_STATIC_DRAW);
  }

  public void setData(FloatBuffer data) {
    setData(data, GL_STATIC_DRAW);
  }

  public void setData(IntBuffer data) {
    setData(data, GL_STATIC_DRAW);
  }

  public void setData(ShortBuffer data) {
    setData(data, GL_STATIC_DRAW);
  }

  public void setData(ByteBuffer data, int usage) {
    glBufferData(target, data, usage);
  }

  public void setData(FloatBuffer data, int usage) {
    glBufferData(target, data, usage);
  }

  public void setData(IntBuffer data, int usage) {
    glBufferData(target, data, usage);
  }

  public void setData(ShortBuffer data, int usage) {
    glBufferData(target, data, usage);
  }

  public void destroy() {
    glDeleteBuffers(buffer);
    buffer = -1;
  }
}
