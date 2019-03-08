package org.myrobotlab.math.interfaces;

public interface Mapper {

  Double calcOutput(Double in);

  Double getMin();

  Double getMax();

  /**
   * <pre>
   * Default behavior of this function is to map one range on another range of numbers.
   * e.g. map(-1.0, 1.0, -1.0, 1.0) maps one to one the input to the output when calcOutput is called
   *      output = mapper.calcOutput(0.7) output would be 0.7
   *      
   * limits are also set from minY, maxY if there is an inverted mapping, the limits are still appropriately set.
   * 
   * &#64;param minX
   * &#64;param maxX
   * &#64;param minY
   * &#64;param maxY
   * </pre>
   */
  void map(Double minX, Double maxX, Double minY, Double maxY);

  /**
   * function which "only" sets the range mapping value without setting input or
   * output limits
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  void setMap(Double minX, Double maxX, Double minY, Double maxY);

  /**
   * Merges non null values of "other" mapper with this mapper's null fields.
   * useful for setting values in MotorControl from "default" values in
   * MotorControllers. Since the MotorControl doesn't know what would be an
   * appropriate mapping value e.g -1,1 =&lt; ?,? the minY and maxY are left null
   * until a "merge" is done in the attach of the AbstractMotorController.
   * 
   * When a Sabertooth motor controller is "attached" to a MotorControl the
   * merge produces -1,1 =&lt; -127, 127 which is appropriate.
   * 
   * @param mapperInterface
   */
  void merge(Mapper other);

  /**
   * <pre>
   * setLimits sets the output limits of the Mapper.  If an input is calculated
   * outside the limits it will be "clipped" to the max or min output depending on the input.
   * 
   *   mapper.map(minX, maxX, minY, maxY) will automatically set the limits to minY, maxY
   *   
   *   you can later adjust the map set limits by calling this method explicitly
   *   mapper.setLimits(-3, 3)
   *   
   *   or remove the limits
   *   mapper.setLimits(null, null)
   * 
   * 
   * </pre>
   * 
   * @param minOutput
   * @param maxOutput
   */
  void setLimits(Double minOutput, Double maxOutput);

  /**
   * returns if the mapper is currently inverted
   * 
   * @return
   */
  boolean isInverted();

  /**
   * Inverts the calculation multiplies the output by -1
   * 
   * @param invert
   */
  void setInverted(boolean invert);

}