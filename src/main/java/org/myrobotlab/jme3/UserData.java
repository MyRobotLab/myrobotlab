package org.myrobotlab.jme3;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;

public class UserData implements Savable {
  public final static Logger log = LoggerFactory.getLogger(UserData.class);

  // public String name;

  public String parentName;

  public transient JMonkeyEngine jme;

  public transient ServiceInterface service;

  public transient Spatial spatial;

  /**
   * bounding box
   */
  public transient Geometry bb;

  public Mapper mapper;

  public Vector3f rotationMask;
  public Vector3f localTranslation; // transitory ? init only ? INIT !!!
                                    // probably - which means its local first
                                    // loaded

  public Vector3f initialRotation;

  public Double currentAngle;

  public String assetPath;

  String bbColor;

  /**
   * bucket to hold the unit axis
   */
  public Node axis;

  public UserData(JMonkeyEngine jme, String name) {
    this.jme = jme;
    this.spatial = new Node(name);
    spatial.setUserData("data", this);
  }

  public UserData(JMonkeyEngine jme, Spatial spatial) {
    this.jme = jme;
    this.spatial = spatial;
    spatial.setUserData("data", this);
  }

  public void enableBoundingBox(boolean b) {
    enableBoundingBox(b, null);
  }

  public String getName() {
    return spatial.getName();
  }

  public Node getNode() {
    return (Node) spatial;
  }

  public Spatial getSpatial() {
    return spatial;
  }

  public Mapper getMapper() {
    return mapper;
  }

  public ServiceInterface getService() {
    return service;
  }

  /**
   * rotate object relative to its local coordinates in degrees
   * 
   * @param localAngle
   */
  public void rotateDegrees(float localAngle) {
    rotateDegrees((double) localAngle);
  }

  /**
   * rotate object relative to its local coordinates in degrees
   * 
   * @param localAngle
   */
  public void rotateDegrees(double localAngle) {
    double deltaAngle = (currentAngle - localAngle) * 0.0174533; // Math.PI /
                                                                 // 180;
    Vector3f newAngle = rotationMask.mult((float) deltaAngle);
    spatial.rotate(newAngle.x, newAngle.y, newAngle.z);
    currentAngle = localAngle;
    log.info("currentAngle {} newAngle {} deltaAngle {}", currentAngle, localAngle, deltaAngle);
  }

  public void setService(Service service) {
    this.service = service;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void read(JmeImporter im) throws IOException {
    // TODO Auto-generated method stub

  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(spatial.getName());
    return sb.toString();
  }

  // scales a node and all its children
  public void scale(float scale) {
    // node.getChildren();
    spatial.scale(scale);
    spatial.updateGeometricState();
  }

  public void enableCoordinateAxes(boolean b) {
    if (spatial instanceof Geometry) {
      UserData data = jme.getUserData(spatial.getParent());
      data.enableCoordinateAxes(b);
      return;
    }
    if (axis == null) {
      axis = jme.createUnitAxis();
      axis.setLocalTranslation(spatial.getWorldTranslation());
      axis.setLocalRotation(spatial.getWorldRotation());
      ((Node) spatial).attachChild(axis);
    }
    if (b) {
      axis.setCullHint(CullHint.Never);
    } else {
      axis.setCullHint(CullHint.Always);
    }
  }

  public void enableBoundingBox(boolean b, String color) {

    if (spatial instanceof Geometry) {
      UserData data = jme.getUserData(spatial.getParent());
      data.enableBoundingBox(b, color);
      return;
    }

    if (color == null) {
      color = Jme3Util.defaultColor;
    }

    if (bb == null) {
      bb = jme.createBoundingBox(spatial);
      if (bb != null) {
        ((Node) spatial).attachChild(bb);
      } else {
        return;
      }
    }

    if (!color.equals(bbColor)) {
      Material mat = new Material(jme.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setColor("Color", Jme3Util.toColor(color));
      mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
      bb.setMaterial(mat);
      bbColor = color;
    }

    if (b && bb != null) {
      bb.setCullHint(CullHint.Never);
    } else if (!b && bb != null) {
      bb.setCullHint(CullHint.Always);
    }

  }

}
