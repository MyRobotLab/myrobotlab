package org.myrobotlab.jme3;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.debug.WireBox;

public class Jme3Object implements Savable {
  public final static Logger log = LoggerFactory.getLogger(Jme3Object.class);

  public String name;

  public String parentName;

  public transient JMonkeyEngine jme;

  public transient ServiceInterface service;

  public transient Node node;

  public transient Spatial spatial;

  /**
   * bounding box
   */
  public transient Geometry bb;

  public Mapper mapper;

  public Vector3f rotationMask;
  public Vector3f localTranslation; // transitory ? init only ?

  public Double currentAngle;

  public String assetPath;

  public Jme3Object(JMonkeyEngine jme, String name) {
    this.jme = jme;
    this.name = name;
    this.node = new Node(spatial.getName());
    node.setUserData("data", this);
  }

  public Jme3Object(JMonkeyEngine jme, Spatial spatial) {
    this.jme = jme;
    name = spatial.getName();
    this.spatial = spatial;
    node = new Node(spatial.getName());
    node.attachChild(spatial); // spatial.setUserData ????
    node.setUserData("data", this);    
  }

  // FIXME - defaultRotation is ok - init rotation IS NOT !!!
  // FIXME - DEPRECATE
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

    float scaleFactor = 1;

    Jme3Object po = jme.getJme3Object(parentName);
    if (po != null) {
      po.attachChild(node);
    }

    /*
     * Node parentNode = jme.getNode(parentName); if (parentNode != null) {
     * parentNode.attachChild(node); }
     */

    if (assetPath != null) {
      try {
        spatial = jme.loadModel(assetPath);
        spatial.setUserData("data", this);
        node.attachChild(spatial);
      } catch (Exception e) {
        log.error("could not load model {}", assetPath);
      }
      // spatial.setName(String.format("%s-geometry", name));
      // spatial.scale(1 / scaleFactor); // FIXME - import 1000 scale data      
    }

    if (localTranslation != null) {
      localTranslation.x = localTranslation.x / scaleFactor; // FIXME scale 1000
      localTranslation.y = localTranslation.y / scaleFactor;
      localTranslation.z = localTranslation.z / scaleFactor;
      node.setLocalTranslation(localTranslation);
    }
    node.setUserData("data", this);
    
  }

  public void attachChild(Node node2) {
    node.attachChild(node);
  }

  public void enableBoundingBox(boolean b) {

    if (b && bb == null) {
      // Geometry bb = WireBox.makeGeometry((BoundingBox)
      // spatial.getWorldBound());
      // spatial.getWorldBound();
      // Geometry newBb = WireBox.makeGeometry((BoundingBox) spatial.getWorldBound());
      Geometry newBb = WireBox.makeGeometry((BoundingBox) node.getWorldBound());
      // Material mat = new Material(jme.getAssetManager(), "Common/MatDefs/Light/PBRLighting.j3md");
      
      Material mat = new Material(jme.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setColor("Color", ColorRGBA.Green);
      mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
      
      // mat1.setMode(Mesh.Mode.Lines);
      // newBb.setLineWidth(2.0f);
      newBb.setMaterial(mat);
      bb = newBb;
      if (node != null) {
        node.attachChild(bb);
        // jme.getRootNode().attachChild(bb);
      }
    } else if (b && bb != null) {
      bb.setCullHint(CullHint.Never);
    } else if (!b && bb != null) {
      bb.setCullHint(CullHint.Always);
    }

  }

  public String getName() {
    return name;
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

  @Override
  public void write(JmeExporter ex) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void read(JmeImporter im) throws IOException {
    // TODO Auto-generated method stub

  }

  public Spatial getSpatial() {
    return spatial;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    return sb.toString();
  }

  // scales a node and all its children
  public void scale(float scale) {
    // node.getChildren();
    node.scale(scale);
    node.updateGeometricState();
  }

  public void loadModel(String assetPath) {
    // TODO Auto-generated method stub

  }
}
