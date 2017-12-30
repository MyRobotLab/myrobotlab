package org.myrobotlab.oculus.lwjgl.models;

import org.myrobotlab.oculus.lwjgl.textures.ModelTexture;

/**
 * the base model of an object that has a texture.
 * 
 * @author kwatters
 *
 */
public class TexturedModel {
  private RawModel rawModel;
  private ModelTexture texture;

  public TexturedModel(RawModel model, ModelTexture texture) {
    this.rawModel = model;
    this.texture = texture;
  }

  public RawModel getRawModel() {
    return rawModel;
  }

  public void setRawModel(RawModel rawModel) {
    this.rawModel = rawModel;
  }

  public ModelTexture getTexture() {
    return texture;
  }

  public void setTexture(ModelTexture texture) {
    this.texture = texture;
  }

}
