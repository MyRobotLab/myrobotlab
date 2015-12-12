package org.myrobotlab.oculus.lwjgl.models;

public class RawModel {

	private int vaoID;
	
	private int vertexCount;
	
	public RawModel(int vaoID, int vertexCount) {
		super();
		this.vaoID = vaoID;
		this.vertexCount = vertexCount;
	}
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
