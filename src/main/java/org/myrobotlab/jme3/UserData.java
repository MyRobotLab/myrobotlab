package org.myrobotlab.jme3;

import java.io.IOException;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MapperLinear;
import org.myrobotlab.math.interfaces.Mapper;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class UserData implements Savable {

  public final static Logger log = LoggerFactory.getLogger(UserData.class);

  public transient JMonkeyEngine jme;

  public transient Spatial spatial;

  /**
   * bounding box
   */
  public transient Geometry bb;

  /**
   * this could be just a Mapper interface, however, it cuts down on the saved
   * yml if its a concrete class
   */
  public MapperLinear mapper;

  /**
   * Rotation axis mask to be applied to a node Can be x, y, z -
   */
  public String rotationMask;

  /**
   * The node's local rotation before any joint angle was applied, captured once.
   * Joint angles are then {@code bindRotation * fromAngleAxis(theta, axis)}, a
   * true rotation about the bone's own axis.
   *
   * <p>
   * The previous approach decomposed the local rotation to Euler angles, replaced
   * one component and recomposed. That is not a rotation about the bone axis for
   * any bone with a non-trivial bind rotation, and JMonkeyEngine's Z Euler term is
   * {@code asin}-limited to ±90°, so it could not even represent the omoplate's
   * configured range. The error grew with the commanded angle, which is exactly
   * what made the simulator drift away from the IK solution on large moves.
   * </p>
   */
  transient public Quaternion bindRotation;

  /**
   * Last joint angle applied by {@link Jme3Util#rotateTo}, in mesh degrees
   * relative to {@link #bindRotation}. Null until the node is first driven.
   */
  transient public Double currentAngleDeg;

  transient Node meta;

  String bbColor;

  /**
   * bucket to hold the unit axis
   */
  transient public Node axis;

  public UserData() {
  }

  public UserData(MapperLinear mapper, String rotationMask) {
    this.mapper = mapper;
    this.rotationMask = rotationMask;
  }

  public UserData(JMonkeyEngine jme, Spatial spatial) {
    this.jme = jme;
    this.spatial = spatial;
    this.meta = new Node("_meta");
    spatial.setUserData("data", this);
  }

  public UserData(UserDataConfig userDataConfig) {
    this.mapper = userDataConfig.mapper;
    this.rotationMask = userDataConfig.rotationMask;
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

  /**
   * Remember the node's authored bind rotation the first time it is needed. Safe
   * to call repeatedly; only the first call captures.
   *
   * @return the bind rotation, or null if there is no spatial yet
   */
  public Quaternion captureBindRotation() {
    if (bindRotation == null && spatial != null) {
      bindRotation = spatial.getLocalRotation().clone();
      currentAngleDeg = 0.0;
    }
    return bindRotation;
  }

  /** @return the joint angle in mesh degrees relative to the bind pose. */
  public double getCurrentAngleDeg() {
    return currentAngleDeg == null ? 0.0 : currentAngleDeg;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    // TODO Auto-generated method stub
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(spatial);
    sb.append(mapper);
    sb.append(rotationMask);
    // sb.append(" ") TODO - other parts
    return sb.toString();
  }

}
