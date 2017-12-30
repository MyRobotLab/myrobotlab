package org.myrobotlab.oculus.lwjgl.shaders;

import java.io.IOException;

import org.lwjgl.util.vector.Matrix4f;
import org.myrobotlab.oculus.lwjgl.Maths;
import org.myrobotlab.oculus.lwjgl.entities.Camera;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * A shader to be used to render objects, this shader uses the hard coded vertex
 * and fragment shader code in resources/oculus ...
 * 
 * @author kwatters
 *
 */
public class StaticShader extends ShaderProgram {

  private static final String VERTEX_SHADER;
  private static final String FRAGMENT_SHADER;

  static {
    try {
      VERTEX_SHADER = Resources.toString(Resources.getResource("resource/oculus/vertexShader.txt"), Charsets.UTF_8);
      FRAGMENT_SHADER = Resources.toString(Resources.getResource("resource/oculus/fragmentShader.txt"), Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private int location_transformationMatrix;
  private int location_projectionMatrix;
  private int location_viewMatrix;

  public StaticShader() {
    // TODO: figure out how to load these from the jar or something...
    super(VERTEX_SHADER, FRAGMENT_SHADER);
  }

  @Override
  protected void bindAttributes() {
    // we currently only care about positions and texture coordinates
    // in the future we might care about other things
    super.bindAttribute(0, "position");
    super.bindAttribute(1, "textureCoords");
  }

  public void loadTransformationMatrix(Matrix4f matrix) {
    super.loadMatrix(location_transformationMatrix, matrix);
  }

  public void loadProjectionMatrix(Matrix4f matrix) {
    super.loadMatrix(location_projectionMatrix, matrix);
  }

  public void loadViewMatrix(Camera camera) {
    Matrix4f viewMatrix = Maths.createViewMatrix(camera);
    super.loadMatrix(location_viewMatrix, viewMatrix);
  }

  @Override
  protected void getAllUniformLocations() {
    // TODO Auto-generated method stub
    location_transformationMatrix = super.getUniformLocation("transformationMatrix");
    location_projectionMatrix = super.getUniformLocation("projectionMatrix");
    location_viewMatrix = super.getUniformLocation("viewMatrix");
  }
}
