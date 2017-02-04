package org.saintandreas.gl.shaders;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glShaderSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shader {
  private static final Logger LOG = LoggerFactory.getLogger(Shader.class);
  private final String source;
  private final int type;
  int shader = -1;

  public Shader(int type, String source) {
    this.source = source;
    this.type = type;
  }

  public void attach(int program) {
    glAttachShader(program, shader);
  }

  public void compile() {
    try {
      int newShader = compile(source, type);
      if (-1 != shader) {
        glDeleteShader(shader);
      }
      shader = newShader;
    } catch (Exception e) {
      if (shader != -1) {
        glDeleteShader(shader);
        shader = -1;
      }
      throw e;
    }
  }

  public String getLog() {
    return getLog(shader);
  }

  // printShaderInfoLog
  // From OpenGL Shading Language 3rd Edition, p215-216
  // Display (hopefully) useful error messages if shader fails to compile
  public static String getLog(int shader) {
    return glGetShaderInfoLog(shader, 8192);
  }

  public static int compile(String source, int type) {
    int newShader = glCreateShader(type);
    glShaderSource(newShader, source);
    glCompileShader(newShader);
    int compileResult = glGetShaderi(newShader, GL_COMPILE_STATUS);
    if (GL_TRUE != compileResult) {
      String log = getLog(newShader);
      LOG.warn("shader compile failed :" + log);
      glDeleteShader(newShader);
      throw new IllegalStateException("Shader compile error" + log);
    }
    return newShader;
  }

}
