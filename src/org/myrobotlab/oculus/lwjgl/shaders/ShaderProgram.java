package org.myrobotlab.oculus.lwjgl.shaders;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Very simple shader program
 * 
 * @author kwatters
 *
 */
public abstract class ShaderProgram {

  private int programID;
  private int vertexShaderID;
  private int fragmentShaderID;

  private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

  public ShaderProgram(String vertexFile, String fragmentFile) {
    vertexShaderID = loadShader(vertexFile, GL20.GL_VERTEX_SHADER, false);
    fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER, false);
    programID = GL20.glCreateProgram();
    GL20.glAttachShader(programID, vertexShaderID);
    GL20.glAttachShader(programID, fragmentShaderID);
    bindAttributes();
    GL20.glLinkProgram(programID);
    GL20.glValidateProgram(programID);
    getAllUniformLocations();
  }

  protected abstract void getAllUniformLocations();

  protected int getUniformLocation(String uniformName) {
    return GL20.glGetUniformLocation(programID, uniformName);
  }

  public void start() {
    GL20.glUseProgram(programID);
  }

  public void stop() {
    GL20.glUseProgram(0);
  }

  public void cleanUp() {
    stop();
    GL20.glDetachShader(programID, vertexShaderID);
    GL20.glDetachShader(programID, fragmentShaderID);
    GL20.glDeleteShader(vertexShaderID);
    GL20.glDeleteShader(fragmentShaderID);
    GL20.glDeleteProgram(programID);
  };

  protected abstract void bindAttributes();

  protected void bindAttribute(int attribute, String variableName) {
    GL20.glBindAttribLocation(programID, attribute, variableName);
  }

  protected void loadFloat(int location, float value) {
    GL20.glUniform1f(location, value);
  }

  protected void loadVector(int location, Vector3f vector) {
    GL20.glUniform3f(location, vector.x, vector.y, vector.z);
  }

  protected void loadBoolean(int location, boolean value) {
    float toLoad = 0;
    if (value) {
      toLoad = 1;
    }
    GL20.glUniform1f(location, toLoad);
  }

  protected void loadMatrix(int location, Matrix4f matrix) {
    matrix.store(matrixBuffer);
    matrixBuffer.flip();
    GL20.glUniformMatrix4(location, false, matrixBuffer);
  }

  // if isFile = true , load the file name. o/w first arg is the actual source
  // for the shader.
  private static int loadShader(String file, int type, boolean isFile) {

    // load the shader from the file
    String shaderSource = null;
    if (isFile) {
      StringBuilder shaderSourceBuilder = new StringBuilder();
      BufferedReader reader;
      try {
        reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
          shaderSourceBuilder.append(line).append("\n");
        }
        ;
        reader.close();
        shaderSource = shaderSourceBuilder.toString();
      } catch (IOException e) {
        System.err.println("Could not read file!");
        e.printStackTrace();
        System.exit(-1);
      }
    } else {
      shaderSource = file;
    }

    int shaderID = GL20.glCreateShader(type);
    GL20.glShaderSource(shaderID, shaderSource);
    // compile the shader
    GL20.glCompileShader(shaderID);
    // report an error during compilation.
    if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
      System.out.println(GL20.glGetShaderInfoLog(shaderID, 500));
      System.err.println("Could not compile Shader.");
      System.exit(-1);
    }
    return shaderID;

  }

}
