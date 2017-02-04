package org.myrobotlab.service.data;

public class Rectangle {
  public float x;
  public float y;
  public float width;
  public float height;

  public Rectangle() {
  }

  public Rectangle(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public String toString(){
    return String.format("x %.2f y %.2f w %.2f h %.2f", x, y, width, height);
  }
  
}
