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
