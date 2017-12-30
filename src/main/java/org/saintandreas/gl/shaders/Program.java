package org.saintandreas.gl.shaders;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_ACTIVE_ATTRIBUTES;
import static org.lwjgl.opengl.GL20.GL_ACTIVE_UNIFORMS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetActiveAttrib;
import static org.lwjgl.opengl.GL20.glGetActiveUniform;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.saintandreas.gl.BufferUtils;
import org.saintandreas.gl.OpenGL;
import org.saintandreas.math.Matrix4f;
import org.saintandreas.math.Vector3f;
import org.saintandreas.math.Vector4f;
import org.saintandreas.resources.Resource;
import org.saintandreas.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Program {

  private static final Logger LOG = LoggerFactory.getLogger(Program.class);

  enum ShaderType {
    VERTEX, GEOMETRY, FRAGMENT
  };

  private final int VERTEX_SHADER = 0;
  private final int GEOMETRY_SHADER = 1;
  private final int FRAGMENT_SHADER = 2;

  public final Map<String, Integer> uniforms = new HashMap<String, Integer>();
  public final Map<String, Integer> attributes = new HashMap<String, Integer>();
  private static Program CURRENT_PROGRAM = null;

  private Shader[] shaders = new Shader[3];
  public int program = -1;

  public Program(Resource vs, Resource fs) {
    this(ResourceManager.getProvider().getAsString(vs), ResourceManager.getProvider().getAsString(fs));
  }

  public Program(String vssf, String fssf) {
    this(new Shader(GL_VERTEX_SHADER, vssf), null, new Shader(GL_FRAGMENT_SHADER, fssf));
  }

  public Program(Shader vs, Shader fs) {
    this(vs, null, fs);
  }

  public Program(Shader vs, Shader gs, Shader fs) {
    shaders[VERTEX_SHADER] = vs;
    shaders[GEOMETRY_SHADER] = gs;
    shaders[FRAGMENT_SHADER] = fs;
  }

  public void destroy() {
    if (program != -1) {
      glDeleteProgram(program);
      program = -1;
    }
  }

  public void link() {
    for (Shader s : shaders) {
      if (null != s && s.shader == -1) {
        s.compile();
      }
    }
    int newProgram = linkProgram(shaders);
    if (program != -1) {
      glDeleteProgram(program);
      program = -1;
    }
    this.uniforms.clear();
    this.attributes.clear();
    program = newProgram;
    if (program == -1) {
      throw new IllegalStateException("Link failure");
    }
    int count = glGetProgrami(program, GL_ACTIVE_UNIFORMS);
    for (int i = 0; i < count; ++i) {
      String name = glGetActiveUniform(program, i, 256);
      int location = glGetUniformLocation(program, name);
      this.uniforms.put(name, location);
    }

    count = glGetProgrami(program, GL_ACTIVE_ATTRIBUTES);
    for (int i = 0; i < count; ++i) {
      String name = glGetActiveAttrib(program, i, 256);
      int location = glGetAttribLocation(program, name);
      this.attributes.put(name, location);
    }
  }

  public String getLog() {
    return getLog(program);
  }

  public void use() {
    if (program == -1) {
      throw new IllegalStateException("Program is not linked");
    }
    CURRENT_PROGRAM = this;
    glUseProgram(program);
    OpenGL.checkError();
  }

  public static void clear() {
    glUseProgram(0);
    CURRENT_PROGRAM = null;
  }

  protected int getUniformLocation(String string) {
    if (!uniforms.containsKey(string)) {
      return -1;
    }
    return this.uniforms.get(string);
  }

  private void checkCurrent() {
    if (this != CURRENT_PROGRAM) {
      throw new IllegalStateException("Attempting to set uniform on unbound program");
    }
  }

  public void setUniform(final String string, int value) {
    checkCurrent();
    int location = getUniformLocation(string);
    glUniform1i(location, value);
  }

  public void setUniform(final String string, float value) {
    checkCurrent();
    int location = getUniformLocation(string);
    glUniform1f(location, value);
  }

  public void setUniform(final String string, Vector4f value) {
    checkCurrent();
    int location = getUniformLocation(string);
    glUniform4f(location, value.x, value.y, value.z, value.w);
  }

  public void setUniform(final String string, Vector3f value) {
    checkCurrent();
    int location = getUniformLocation(string);
    glUniform3f(location, value.x, value.y, value.z);
  }

  public void setUniformMatrix4(final String name, FloatBuffer fb) {
    checkCurrent();
    int location = getUniformLocation(name);
    glUniformMatrix4(location, false, fb);
  }

  public void setUniformMatrix4(final String name, float[] v) {
    FloatBuffer fb = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    fb.put(v);
    fb.position(0);
    setUniformMatrix4(name, fb);
  }

  public void setUniform(final String name, Matrix4f m) {
    FloatBuffer fb = BufferUtils.getFloatBuffer(16);
    m.fillFloatBuffer(fb, true);
    fb.position(0);
    setUniformMatrix4(name, fb);
  }

  public static String getLog(int program) {
    return glGetProgramInfoLog(program, 8192);
  }

  public static int linkProgram(Shader... shaders) {
    int newProgram = glCreateProgram();
    for (Shader s : shaders) {
      if (null != s) {
        s.attach(newProgram);
      }
    }
    glLinkProgram(newProgram);
    int linkResult = glGetProgrami(newProgram, GL_LINK_STATUS);
    if (GL_TRUE != linkResult) {
      String log = getLog(newProgram);
      LOG.warn("Link failed: " + log);
      glDeleteProgram(newProgram);
      throw new RuntimeException(log);
    }
    return newProgram;
  }
}
