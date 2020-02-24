package org.myrobotlab.jme3;

import java.awt.Color;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.WireBox;

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

  public void log(UserData o) {
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

  public void setTransform(String name, double x, double y, double z, double xRot, double yRot, double zRot) {
    setTranslation(name, x, y, z);
    setRotation(name, xRot, yRot, zRot);
  }

  public void setTranslation(String name, double x, double y, double z) {
    log.info(String.format("setTranslation %s, %.2f,%.2f,%.2f", name, x, y, z));
    Spatial s = jme.get(name);
    s.setLocalTranslation((float) x, (float) y, (float) z);
    if (currentMenuView != null && s == selectedForView) {
      currentMenuView.putText(selectedForView);
    }
  }

  public void setRotation(String name, double xRot, double yRot, double zRot) {
    Spatial s = jme.get(name);
    Quaternion q = new Quaternion();
    float xRotInit = (float) xRot * FastMath.DEG_TO_RAD;
    float yRotInit = (float) yRot * FastMath.DEG_TO_RAD;
    float zRotInit = (float) zRot * FastMath.DEG_TO_RAD;
    q.fromAngles(zRotInit, xRotInit, yRotInit);
    s.setLocalRotation(q);
    if (currentMenuView != null && s == selectedForView) {
      currentMenuView.putText(selectedForView);
    }
  }

  public static Integer getIndexFromUnitVector(Vector3f vector) {
    if (vector == null) {
      // default is Y
      return 1;
    }

    if (vector.equals(Vector3f.UNIT_X)) {
      return 0;
    } else if (vector.equals(Vector3f.UNIT_Y)) {
      return 1;
    } else if (vector.equals(Vector3f.UNIT_Z)) {
      return 2;
    }

    log.error("vector %s does not equal a unit vector");
    return null;
  }

  /**
   * absolute (local) rotation ..
   * 
   * @param name
   * @param degrees
   */
  public void rotateTo(String name, String axis, double degrees) {
    UserData o = jme.getUserData(name);
    if (o == null) {
      jme.error("no user data for %s", name);
      return;
    }

    // default rotation is around Y axis unless specified
    Vector3f rotMask = Vector3f.UNIT_Y;
    if (o.rotationMask != null) {
      rotMask = o.rotationMask;
    }

    // highest priority override is if the parameter is supplied
    if (axis != null) {
      rotMask = getUnitVector(axis);
    }

    log.debug("rotateTo {}, degrees {} around axis {}", name, degrees, rotMask);
    // int angleIndex = getIndexFromUnitVector(rotMask);
    if (o.mapper != null) {
      degrees = o.mapper.calcOutput(degrees);
      log.debug(String.format("rotateTo map %s, degrees %.2f", name, degrees));
    }

    // get current local rotations
    Node n = o.getNode();

    // convert current local to euler representation
    Quaternion q = n.getLocalRotation();
    float[] euler = new float[3];
    q.toAngles(euler);

    // find the masking axis - replace that value with desired value
    int indexOfAxisRotation = getIndexFromUnitVector(rotMask);
    euler[indexOfAxisRotation] = ((float) degrees) * FastMath.PI / 180;
    q.fromAngles(euler[0], euler[1], euler[2]);
    n.setLocalRotation(q);

    if (currentMenuView != null && n == selectedForView) {
      currentMenuView.putText(selectedForView);
    }
  }

  public void bind(String child, String parent) {
    log.info("binding {} to {}", child, parent);
    Spatial childNode = jme.get(child);
    if (childNode == null) {
      log.error("bind child {} not found", child);
      return;
    }
    Spatial parentNode = jme.get(parent);
    if (parentNode == null) {
      log.error("bind parent {} not found", parent);
      return;
    }

    if (parentNode instanceof Geometry) {
      log.error("parent {} must be of type Node !!! - cannot bind Geometry");
      return;
    }

    Node p = (Node) parentNode;
    Spatial c = childNode;

    // moving one object to another object

    Vector3f childWorld1 = c.getWorldTranslation().clone();
    log.info("worldPos1 {}", childWorld1);

    Vector3f parentWorld1 = p.getWorldTranslation();
    Vector3f parentWorld = p.getWorldTranslation();

    p.attachChild(c);

    // FIXME - possibly subtract out the parents "world" transform & rotation ???

  }

  public Node createUnitAxis(String name) {

    Node n = new Node(name);
    Arrow arrow = new Arrow(Vector3f.UNIT_X);
    arrow.setLineWidth(4); // make arrow thicker
    n.attachChild(createAxis("y", arrow, ColorRGBA.Red));

    arrow = new Arrow(Vector3f.UNIT_Y);
    arrow.setLineWidth(4); // make arrow thicker
    n.attachChild(createAxis("y", arrow, ColorRGBA.Green));

    arrow = new Arrow(Vector3f.UNIT_Z);
    arrow.setLineWidth(4); // make arrow thicker
    n.attachChild(createAxis("z", arrow, ColorRGBA.Blue));
    return n;
  }

  // FIXME !!! - string of object name type ...
  public Geometry createAxis(String name, Mesh shape, ColorRGBA color) {
    Geometry g = new Geometry(name, shape);
    Material mat = new Material(jme.getApp().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    mat.getAdditionalRenderState().setWireframe(true);
    mat.setColor("Color", color);
    g.setMaterial(mat);
    return g;
  }

  public Geometry createBoundingBox(Spatial spatial, String color) {
    // Geometry newBb = WireBox.makeGeometry((BoundingBox)
    // spatial.getWorldBound());
    BoundingVolume bv = spatial.getWorldBound();
    if (bv == null) {
      log.warn("createBoundingBox({}) has no volume", spatial.getName());
      return null;
    }

    Geometry newBb = WireBox.makeGeometry((BoundingBox) spatial.getWorldBound());
    // Material mat = new Material(jme.getAssetManager(),
    // "Common/MatDefs/Light/PBRLighting.j3md");

    newBb.setName(jme.getBbName(spatial));

    Material mat = new Material(jme.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", toColor(color));
    mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

    // mat1.setMode(Mesh.Mode.Lines);
    // newBb.setLineWidth(2.0f);
    newBb.setMaterial(mat);

    return newBb;
  }

  public void lookAt(String viewer, String viewee) {
    log.info("lookAt({}, {})", viewer, viewee);
    Spatial viewerSpatial = jme.get(viewer);
    Spatial vieweeSpatial = jme.get(viewee);
    if (viewerSpatial == null) {
      log.error("could not find {}", viewer);
      return;
    }
    if (vieweeSpatial == null) {
      log.error("could not find {}", viewee);
      return;
    }
    viewerSpatial.lookAt(vieweeSpatial.getWorldTranslation(), Vector3f.UNIT_Y);
  }

  public Geometry createBoundingBox(Spatial spatial) {
    return createBoundingBox(spatial, defaultColor);
  }

  public void scale(String name, Double scale) {
    Spatial s = jme.get(name);
    if (s == null || scale == null) {
      return;
    }
    log.info("rescaling {} to {}", name, scale);
    s.scale(scale.floatValue());
  }

  public Vector3f getLocalUnitVector(Spatial spatial, String axis) {
    // FIXME !!!!
    Vector3f rootUnitVector = getUnitVector(axis);

    // find the delta between the two axis
    Quaternion wr = spatial.getWorldRotation();
    Vector3f[] axes = new Vector3f[3];
    wr.toAxes(axes);
    // rootUnitVector.subtract(vec)
    // return axes[0];
    return rootUnitVector; // FIXME - THIS IS NOT CORRECT !
  }

  public Vector3f getUnitVector(String axis) {
    if (axis == null) {
      return Vector3f.UNIT_Y; // default Y
    }
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

  Spatial selectedForView;
  MainMenuState currentMenuView;

  public void setSelectedForView(MainMenuState menu, Spatial selectedForView) {
    this.currentMenuView = menu;
    this.selectedForView = selectedForView;
  }

  public void addNode(String name) {
    Spatial s = jme.find(name);
    if (s != null) {
      log.error("addNode({}} already exists", name);
      return;
    }
    Node n = new Node(name);
    jme.getRootNode().attachChild(n);
  }

}
