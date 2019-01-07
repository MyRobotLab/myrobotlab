package org.myrobotlab.jme3;

import java.awt.Color;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.JMonkeyEngine.Jme3Msg;
import org.slf4j.Logger;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Jme3Util {

  public final static Logger log = LoggerFactory.getLogger(Jme3Util.class);

  JMonkeyEngine jme;
  public static String defaultColor = "00FF00"; // green

  public Jme3Util(JMonkeyEngine jme) {
    this.jme = jme;
  }

  static public ColorRGBA toColor(String userColor) {
    if (userColor == null) {
      userColor = defaultColor;
    }

    String clean = userColor.replace("0x", "").replace("#", "");

    ColorRGBA retColor = null;
    Color c = null;
    try {
      int cint = Integer.parseInt(clean, 16);
      c = new Color(cint);
      retColor = new ColorRGBA((float) c.getRed() / 255, (float) c.getGreen() / 255, (float) c.getBlue() / 255, 1);
      return retColor;
    } catch (Exception e) {
      log.error("creating color threw", e);
    }
    return ColorRGBA.Green;
  }

  public Object invoke(Jme3Msg msg) {
    return jme.invokeOn(this, msg.method, msg.data);
  }

  public void info(String format, Object... params) {
    jme.info(format, params);
  }

  public void warn(String format, Object... params) {
    jme.warn(format, params);
  }

  public void error(String format, Object... params) {
    jme.error(format, params);
  }

  public void log(Jme3Object o) {
    Node n = o.getNode();
    StringBuilder sb = new StringBuilder();
    sb.append(n.getParent());
    sb.append(" parent->");
    sb.append("(");
    sb.append(n);
    sb.append(")->");
    List<Spatial> children = n.getChildren();
    for (int i = 0; i < children.size(); ++i) {
      sb.append(n.getChild(i));
      if (i != children.size() - 1) {
        sb.append(",");
      }
    }

    log.info(sb.toString());
  }

  public void moveTo(String name, float x, float y, float z) {
    log.info(String.format("moveTo %s, %.2f,%.2f,%.2f", name, x, y, z));
    Jme3Object o = jme.get(name);
    if (o == null) {
      log.error("moveTo %s jme3object is null !!!", name);
      return;
    }
    if (o.getNode() == null) {
      log.error("moveTo %s node is null !!!", name);
      return;
    }
    o.getNode().setLocalTranslation(x, y, z);
  }

  public Vector3f getUnitVector(String axis) {
    Vector3f unitVector = null;
    axis = axis.toLowerCase().trim();

    // get axis
    String unitAxis = axis.substring(axis.length() - 1);

    axis = axis.substring(0, axis.length() - 1);
    Float multiplier = null;
    // rest should be a float unless its just "-"
    if ("-".equals(axis)) {
      multiplier = -1f;
    } else if ("".equals(axis)) {
      multiplier = 1f;
    } else {
      multiplier = Float.parseFloat(axis);
    }

    // TODO - handle multiple
    if (unitAxis.contains("x")) {
      unitVector = Vector3f.UNIT_X.mult(multiplier);
    } else if (unitAxis.contains("y")) {
      unitVector = Vector3f.UNIT_Y.mult(multiplier);
    } else if (unitAxis.contains("z")) {
      unitVector = Vector3f.UNIT_Z.mult(multiplier);
    }
    return unitVector;
  }

  // TODO - generalized rotate("-x", 39.3f) which uses default rotation mask
  public void rotateOnAxis(String name, String axis, float degrees) {
    log.info(String.format("rotateOnAxis %s, %s %.2f", name, axis, degrees));
    Jme3Object o = jme.get(name);
    float angle = degrees * FastMath.PI / 180;
    Vector3f uv = getUnitVector(axis);
    Vector3f rot = uv.mult(angle);
    o.getNode().rotate(rot.x, rot.y, rot.z);
  }

  public void rotateTo(String name, float degrees) {
    log.info(String.format("rotateTo %s, %.2f", name, degrees));
    Jme3Object o = jme.get(name);
    float angle = degrees * FastMath.PI / 180;
    Vector3f rot = o.rotationMask.mult(angle);
    o.getNode().rotate(rot.x, rot.y, rot.z);
  }

  public void bind(String child, String parent) {
    log.info("binding {} to {}", child, parent);
    Jme3Object childNode = jme.get(child);
    if (childNode == null) {
      log.error("bind child {} not found", child);
      return;
    }
    Jme3Object parentNode = jme.get(parent);
    if (parentNode == null) {
      log.error("bind parent {} not found", parent);
      return;
    }

    log(parentNode);
    log(childNode);
    // log.info("child {} {}", childNode.getNode().getChildren().size(),
    // childNode.getNode().getChild(0).getName());
    // parentNode.getNode().attachChild(childNode.getSpatial());
    // Node newNode = new Node("meta");
    // newNode.attachChild(childNode.getNode());
    // parentNode.getNode().attachChild(childNode.getNode().getChild(0));
    // parentNode.getNode().attachChild(newNode);
    parentNode.getNode().attachChild(childNode.getNode());
    // parentNode.getNode().updateModelBound();
    // parentNode.getNode().updateGeometricState();

    // childNode.getNode().updateModelBound();
    // childNode.getNode().updateGeometricState();
    // childNode.getNode().updateGeometricState();
    log(parentNode);
    log(childNode);
  }

}
