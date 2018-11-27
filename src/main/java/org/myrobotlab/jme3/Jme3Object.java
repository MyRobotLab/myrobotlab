package org.myrobotlab.jme3;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Jme3Object {
  public final static Logger log = LoggerFactory.getLogger(Jme3Object.class);

  String name;

  String parentName;

  transient JMonkeyEngine jme;

  transient ServiceInterface service;

  transient Node node;

  transient Spatial spatial;

  Mapper mapper;

  Vector3f rotationMask;
  Vector3f localTranslation;

  /**
   * current angle of default rotation in degrees - 3? X,Y,Z ???
   */
  Double currentAngle;

  String assetPath;

  public Jme3Object(String name) {
    this.name = name;
  }

  public Jme3Object(Node node) {
    this.node = node;
  }

  public Jme3Object(Node node, Mapper mapper) {
    this.node = node;
    this.mapper = mapper;
  }

  // FIXME - this is to be the m
  public Jme3Object(Jme3App jme, String name, String parentName, Service service, Node node, Mapper mapper, Vector3f rotationMask, Float currentAngle, Spatial spatial) {
    this.node = node;
    this.mapper = mapper;
    this.rotationMask = rotationMask;
    this.currentAngle = (double) currentAngle;
  }

  public Jme3Object(Node node2, Mapper mapper2, Vector3f defaultRotationAxis) {
    // TODO Auto-generated constructor stub
  }

  public Jme3Object(JMonkeyEngine jme, String name, String parentName, String assetPath, Mapper mapper, Vector3f rotationMask, Vector3f localTranslation, double currentAngle) {
    this.jme = jme;
    this.name = name;
    this.rotationMask = rotationMask;
    this.localTranslation = localTranslation;
    this.currentAngle = currentAngle;
    this.mapper = mapper;
    this.assetPath = assetPath;

    node = new Node(name);
    this.parentName = parentName;
    Node parentNode = jme.getNode(parentName);
    if (parentNode != null) {
      parentNode.attachChild(node);
    }
    if (assetPath != null) {
      try {
        spatial = jme.loadModel(assetPath);
      } catch (Exception e) {
        log.error("could not load model {}", assetPath);
      }
      spatial.setName(name);
      node.attachChild(spatial);
    }
    node.setLocalTranslation(localTranslation);
  }

  public String getName() {
    return service.getName();
  }

  public Node getNode() {
    return node;
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
    node.rotate(newAngle.x, newAngle.y, newAngle.z);
    currentAngle = localAngle;
    log.info("currentAngle {} newAngle {} deltaAngle {}", currentAngle, localAngle, deltaAngle);
  }

  public void setService(Service service) {
    this.service = service;
  }
}
