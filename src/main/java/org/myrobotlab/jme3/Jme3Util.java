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

  public void moveTo(String name, float x, float y, float z) {
    log.info(String.format("moveTo %s, %.2f,%.2f,%.2f", name, x, y, z));
    UserData o = jme.getUserData(name);
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

  // TODO - generalized rotate("-x", 39.3f) which uses default rotation mask
  public void rotateOnAxis(String name, String axis, float degrees) {
    log.info(String.format("rotateOnAxis %s, %s %.2f", name, axis, degrees));
    UserData o = jme.getUserData(name);
    float angle = degrees * FastMath.PI / 180;
    Vector3f uv = getUnitVector(axis);
    Vector3f rot = uv.mult(angle);
    o.getNode().rotate(rot.x, rot.y, rot.z);
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
  public void rotateTo(String name, Float degrees) {
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

    log.info("rotateTo {}, degrees {} around axis {}", name, degrees, rotMask);    
    // int angleIndex = getIndexFromUnitVector(rotMask);
    if (o.mapper != null) {
      degrees = (float) o.mapper.calcOutput(degrees);
      log.info(String.format("rotateTo map %s, degrees %.2f", name, degrees));
    }

    // get current local rotations
    Node n = o.getNode();
    Quaternion q = n.getLocalRotation();
    float[] angles = new float[3];
    q.toAngles(angles);
    // log.info(String.format("rotate - before %s, %.2f", name,
    // angles[angleIndex] * 180 / FastMath.PI));

    q.fromAngleAxis(((degrees) * FastMath.PI / 180), rotMask);// FIXME optimize
                                                              // final Y_AXIS =
                                                              // new
                                                              // Vector3f(0,1,0)

    // apply map if it exists (shifted)
    n.setLocalRotation(q);
    q.toAngles(angles);
    // log.info(String.format("rotate - after %s, %.2f", name,
    // angles[angleIndex] * 180 / FastMath.PI));
  }

  public void bind(String child, String parent) {
    log.info("binding {} to {}", child, parent);
    UserData childNode = jme.getUserData(child);
    if (childNode == null) {
      log.error("bind child {} not found", child);
      return;
    }
    UserData parentNode = jme.getUserData(parent);
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

  public void scale(String name, float scale) {
    Spatial s = jme.get(name);
    log.info("rescaling {} to {}", name, scale);
    s.scale(scale);
  }

}
