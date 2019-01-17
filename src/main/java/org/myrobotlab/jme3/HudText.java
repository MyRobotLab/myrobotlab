package org.myrobotlab.jme3;

import org.myrobotlab.service.JMonkeyEngine;

import com.jme3.font.BitmapText;
import com.jme3.scene.Node;

public class HudText {
  JMonkeyEngine jme;
  String color;
  String currentText;
  BitmapText node;
  private int size;
  String updateText;
  int x;

  int y;

  public HudText(JMonkeyEngine jme, String text, int x, int y) {
    this.jme = jme;
    this.x = x;
    this.y = y;
    this.updateText = text;
    if (text == null) {
      text = "";
    }
    BitmapText txt = new BitmapText(jme.getApp().loadGuiFont(), false);
    // txt.setColor(new ColorRGBA(1f, 0.1f, 0.1f, 1f));

    txt.setText(text);
    txt.setLocalTranslation(x, jme.getSettings().getHeight() - y, 0);
    node = txt;
  }

  public Node getNode() {
    return node;
  }

  public void setColor(String hexString) {
    this.color = hexString;
  }

  public void setText(String text, String color, int size) {
    this.color = color;
    this.size = size;

    if (text == null) {
      text = "";
    }
    this.updateText = text;
  }

  public void update() {
    if (!updateText.equals(currentText)) {
      node.setText(updateText);
      currentText = updateText;
      if (color != null) {
        node.setColor(Jme3Util.toColor(color));
        node.setSize(size);
      }
    }
  }
}
