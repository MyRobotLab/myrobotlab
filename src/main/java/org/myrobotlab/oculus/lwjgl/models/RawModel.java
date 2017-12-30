package org.myrobotlab.oculus.lwjgl.models;

/**
 * Object representing an untextured model.
 * 
 * @author kwatters
 *
 */
public class RawModel {
  // the vertex array object id
  private int vaoID;
  // count of vertices
  private int vertexCount;

  // constructor
  public RawModel(int vaoID, int vertexCount) {
    super();
    this.vaoID = vaoID;
    this.vertexCount = vertexCount;
  }

  // getters and setters
  public int getVaoID() {
    return vaoID;
  }

  public void setVaoID(int vaoID) {
    this.vaoID = vaoID;
  }

  public int getVertexCount() {
    return vertexCount;
  }

  public void setVertexCount(int vertexCount) {
    this.vertexCount = vertexCount;
  }

}
