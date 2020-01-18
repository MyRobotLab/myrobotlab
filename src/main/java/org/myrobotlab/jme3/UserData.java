package org.myrobotlab.jme3;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class UserData implements Savable {
  public final static Logger log = LoggerFactory.getLogger(UserData.class);

  public String parentName;

  public transient JMonkeyEngine jme;

  @Deprecated
  public transient ServiceInterface service;

  public transient Spatial spatial;

  /**
   * bounding box
   */
  public transient Geometry bb;

  public Mapper mapper;

  transient public Vector3f rotationMask;
  
  transient public Vector3f localTranslation; // transitory ? init only ? INIT !!!
                                    // probably - which means its local first
                                    // loaded

  transient public Vector3f initialRotation;

  transient Node meta;
  
  public Double currentAngle;

  public String assetPath;

  String bbColor;

  /**
   * bucket to hold the unit axis
   */
  transient public Node axis;

  public UserData(JMonkeyEngine jme, Spatial spatial) {
    this.jme = jme;
    this.spatial = spatial;
    this.meta = new Node("_meta");
    spatial.setUserData("data", this);
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
    sb.append(spatial);
    //sb.append(" ") TODO - other parts
    return sb.toString();
  }
/*
  // scales a node and all its children
  public void scale(float scale) {
    spatial.scale(scale);
    spatial.updateGeometricState();
    spatial.updateModelBound();
  }
  */

}
