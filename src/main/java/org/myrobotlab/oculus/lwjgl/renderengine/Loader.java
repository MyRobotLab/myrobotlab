package org.myrobotlab.oculus.lwjgl.renderengine;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.myrobotlab.oculus.lwjgl.models.RawModel;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.BufferedImageUtil;

import com.google.common.io.Resources;

/**
 * This is an openGL loader class. It will load stuff into opengl land.
 * 
 * @author kwatters
 *
 */
public class Loader {
  // list of all vaos that have been loaded
  private List<Integer> vaos = new ArrayList<Integer>();
  // list of all vbos that have been loaded
  private List<Integer> vbos = new ArrayList<Integer>();
  // list of all textures that have been loaded
  private List<Integer> textures = new ArrayList<Integer>();

  /*
   * create a VAO and store the values, return the raw model that represents it.
   * 
   */
  public RawModel loadToVAO(float[] positions, float[] textureCoords, int[] indicies) {
    int vaoID = createVAO();
    bindIndiciesBuffer(indicies);
    storeDataInAttributeList(0, 3, positions);
    storeDataInAttributeList(1, 2, textureCoords);
    unbindVAO();
    return new RawModel(vaoID, indicies.length);
  };

  /*
   * Load a texture from a buffered image and return the texture id
   * 
   */
  public int loadTexture(BufferedImage bi) {
    Texture texture = null;
    try {
      texture = BufferedImageUtil.getTexture("opencv", bi);
    } catch (IOException e) {
      e.printStackTrace();
    }
    int textureID = texture.getTextureID();
    textures.add(textureID);
    return textureID;
  };

  /*
   * load a texture from a filename and return the texture id
   */
  public int loadTexture(String fileName) {
    Texture texture = null;
    try {
      // FileInputStream is = new FileInputStream("src/resource/" + fileName +
      // ".png");
      // InputStream is = new
      // URL("https://upload.wikimedia.org/wikipedia/en/2/24/Lenna.png").openStream();
      InputStream is = Resources.getResource("resource/" + fileName + ".png").openStream();
      texture = TextureLoader.getTexture("PNG", is);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    int textureID = texture.getTextureID();
    textures.add(textureID);
    return textureID;
  }

  /**
   * clean up and release the loaded VAO/VBO and textures
   */
  public void cleanUp() {
    // clean up vaos
    for (int vao : vaos) {
      GL30.glDeleteVertexArrays(vao);
    }
    ;
    // clean up vbos
    for (int vbo : vbos) {
      GL15.glDeleteBuffers(vbo);
    }
    ;
    // clean up textures
    for (int texture : textures) {
      GL11.glDeleteTextures(texture);
    }
  };

  private void storeDataInAttributeList(int attributeNumber, int coordinateSize, float[] data) {
    int vboID = GL15.glGenBuffers();
    vbos.add(vboID);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
    FloatBuffer buffer = storeDataInFloatBuffer(data);
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);
    // unbind the current vbo
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
  };

  private int createVAO() {
    int vaoID = GL30.glGenVertexArrays();
    vaos.add(vaoID);
    // activate vertex array
    GL30.glBindVertexArray(vaoID);
    // track that we created this, so we can clean up.
    return vaoID;
  };

  private void unbindVAO() {
    // unbind the vertex array (0) unbinds currently bound vao.
    GL30.glBindVertexArray(0);
  };

  private void bindIndiciesBuffer(int[] indicies) {
    int vboID = GL15.glGenBuffers();
    vbos.add(vboID);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID);
    IntBuffer buffer = storeDataInIntBuffer(indicies);
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

  }

  private IntBuffer storeDataInIntBuffer(int[] data) {
    IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
    buffer.put(data);
    buffer.flip();
    return buffer;
  }

  private FloatBuffer storeDataInFloatBuffer(float[] data) {
    FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
    buffer.put(data);
    buffer.flip();
    return buffer;
  }

}
