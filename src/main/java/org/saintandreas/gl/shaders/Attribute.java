package org.saintandreas.gl.shaders;

public enum Attribute {
  POSITION(0), TEX(1), NORMAL(2), COLOR(3), EXTRA_ATTR(4);

  public final int location;

  Attribute(int location) {
    this.location = location;
  }
}
