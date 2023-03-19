package org.myrobotlab.service.data;

public class Position {
  
  public Double x;
  public Double y;
  public Double z;
  public String src;
  
  public Position(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  public Position(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public Position(int x, int y, int z) {
    this.x = (double) x;
    this.y = (double) y;
    this.z = (double) z;
  }
  
  public Position(int x, int y) {
    this.x = (double) x;
    this.y = (double) y;
  }

  public Position(float x, float y, float z) {
    this.x = (double) x;
    this.y = (double) y;
    this.z = (double) z;
  }
  
  public Position(float x, float y) {
    this.x = (double) x;
    this.y = (double) y;
  }
  
}
