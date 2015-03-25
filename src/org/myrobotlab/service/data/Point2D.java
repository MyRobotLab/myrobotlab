package org.myrobotlab.service.data;

import java.io.Serializable;

public class Point2D implements Serializable {

	private static final long serialVersionUID = 1L;

	public long timestamp;

	public int x;
	public int y;
	public float value;

	public Point2D() {
	}

	public Point2D(int x, int y) {
		timestamp = System.currentTimeMillis();
		this.x = x;
		this.y = y;
	}

	public Point2D(int x, int y, float value) {
		timestamp = System.currentTimeMillis();
		this.x = x;
		this.y = y;
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("(%d,%ds)", x, y);
	}

}
