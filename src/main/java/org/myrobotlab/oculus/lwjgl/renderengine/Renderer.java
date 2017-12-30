package org.myrobotlab.oculus.lwjgl.renderengine;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.util.vector.Matrix4f;
import org.myrobotlab.oculus.lwjgl.Maths;
import org.myrobotlab.oculus.lwjgl.entities.Entity;
import org.myrobotlab.oculus.lwjgl.models.RawModel;
import org.myrobotlab.oculus.lwjgl.models.TexturedModel;
import org.myrobotlab.oculus.lwjgl.shaders.StaticShader;

/**
 * An OpenGL renderer class to render textured models in the viewport.
 * 
 * @author kwatters
 *
 */
public class Renderer {

  private static final float FOV = 70;
  private static final float NEAR_PLANE = 0.1f;
  private static final float FAR_PLANE = 1000;

  private Matrix4f projectionMatrix;

  public Renderer(StaticShader shader) {
    createProjectionMatrix();
    shader.start();
    shader.loadProjectionMatrix(projectionMatrix);
    shader.stop();
  }

  // prepare the frame for rendering.
  public void prepare() {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    GL11.glClearColor(1, 0, 0, 1);
  };

  // render the model
  public void render(Entity entity, StaticShader shader) {
    TexturedModel model = entity.getModel();
    RawModel rawModel = model.getRawModel();
    try {
      GL30.glBindVertexArray(rawModel.getVaoID());
      GL20.glEnableVertexAttribArray(0);
      GL20.glEnableVertexAttribArray(1);

      Matrix4f transformationMatrix = Maths.createTransformationMatrix(entity.getPosition(), entity.getRotX(), entity.getRotY(), entity.getRotZ(), entity.getScale());

      shader.loadTransformationMatrix(transformationMatrix);

      GL13.glActiveTexture(GL13.GL_TEXTURE0);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getID());
      GL11.glDrawElements(GL11.GL_TRIANGLES, rawModel.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
      GL20.glDisableVertexAttribArray(0);
      GL20.glDisableVertexAttribArray(1);
      GL30.glBindVertexArray(0);
    } catch (OpenGLException e) {
      // TODO: figure out what the heck is going on sometimes.. we miss the
      // texture.
      // it seems to become null or isn't bound?!
      // log.error("OPEN GL EXCEPTION");
      e.printStackTrace();
    }
  }

  private void createProjectionMatrix() {
    float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
    float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))) * aspectRatio);
    float x_scale = y_scale / aspectRatio;
    float frustum_length = FAR_PLANE - NEAR_PLANE;
    projectionMatrix = new Matrix4f();
    projectionMatrix.m00 = x_scale;
    projectionMatrix.m11 = y_scale;
    projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
    projectionMatrix.m23 = -1;
    projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
    projectionMatrix.m33 = 0;
  }
}
