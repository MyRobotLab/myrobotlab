package org.myrobotlab.jme3;

import org.myrobotlab.math.MapperLinear;
/**
 * POJO for moving config from/to JMonkeyEngine simulator
 * 
 * @author GroG
 *
 */
public class UserDataConfig {

  /**
   * this could be just a Mapper interface, however, it cuts down on the saved
   * yml if its a concrete class
   */
  public MapperLinear mapper;

  /**
   * Rotation axis mask to be applied to a node Can be x, y, z -
   */
  public String rotationMask;
  
  public UserDataConfig() {    
  }
  
  public UserDataConfig(MapperLinear mapper, String rotationMask) {
    this.mapper = mapper;
    this.rotationMask = rotationMask;
  }

}
