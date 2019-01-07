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

  // public transient Spatial spatial;

  /**
   * bounding box
   */
  public transient Geometry bb;

  public Mapper mapper;

  public Vector3f rotationMask;
  public Vector3f localTranslation; // transitory ? init only ?

  public Double currentAngle;

  public String assetPath;
  

  String bbColor;

  
  // FIXME !!! MAKE POJO !!
  // FIXME !!! NO CREATION OF NODES IN THIS CLASS !!! ONLY IN JME OR UTIL
  public Jme3Object(JMonkeyEngine jme, String name) {
    this.jme = jme;
    this.name = name;
    this.node = new Node(name);
    node.setUserData("data", this);
  }

  public Jme3Object(JMonkeyEngine jme, Node node) {
    this.jme = jme;
    name = node.getName();
    this.node = node;
    node.setUserData("data", this);
  }

  public void enableBoundingBox(boolean b) {
    enableBoundingBox(b, null);
  }

  public void enableBoundingBox(boolean b, String color) {
    /*boolean test = true;
    if (test) {
      return;
    }
    */
    
    if (color == null) {
      color = Jme3Util.defaultColor;
    }
    
    ColorRGBA c = Jme3Util.toColor(color);

    if (b && bb == null) {
      // Geometry newBb = WireBox.makeGeometry((BoundingBox)
      // spatial.getWorldBound());
      Geometry newBb = WireBox.makeGeometry((BoundingBox) node.getWorldBound());
      // Material mat = new Material(jme.getAssetManager(),
      // "Common/MatDefs/Light/PBRLighting.j3md");

      Material mat = new Material(jme.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setColor("Color", c);
      mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

      // mat1.setMode(Mesh.Mode.Lines);
      // newBb.setLineWidth(2.0f);
      newBb.setMaterial(mat);
      
      bb = newBb;
      bbColor = color;
      if (node != null) {
        node.attachChild(bb);
        // jme.getRootNode().attachChild(bb);// <- ??? should it be root ???
      }
    } else if (b && bb != null) {
      bb.setCullHint(CullHint.Never);
    } else if (!b && bb != null) {
      bb.setCullHint(CullHint.Always);
    }
    
    if (bb != null && !color.equals(bbColor)) {
      Material mat = new Material(jme.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setColor("Color", c);
      mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
      bb.setMaterial(mat);
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
