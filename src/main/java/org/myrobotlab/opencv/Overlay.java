package org.myrobotlab.opencv;

import java.awt.Color;

public class Overlay {

  public String text;
  public int x;
  public int y;
  public Color color;

  public Overlay(int x, int y, String text) {
    this.x = x;
    this.y = y;
    this.text = text;
  }

  public Overlay(int x, int y, String text, String color) {
    this.x = x;
    this.y = y;
    this.text = text;

    if (color != null) {
      // "#FFCCEE"
      this.color = Color.decode(color);
    }

  }

}
