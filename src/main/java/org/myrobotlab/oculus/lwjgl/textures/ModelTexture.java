package org.myrobotlab.oculus.lwjgl.textures;

/**
 * helper class to represent a texture that's been loaded into opengl
 * 
 * @author kwatters
 *
 */
public class ModelTexture {

  private int textureID;

  public ModelTexture(int id) {
    this.textureID = id;
  }

  public int getID() {
    return this.textureID;
  }
}
