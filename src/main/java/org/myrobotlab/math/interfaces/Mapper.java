package org.myrobotlab.math.interfaces;

public interface Mapper {

  /**
   * inverse fn of calcOutput
   * 
   * @param out
   * @return
   */
  Double calcInput(Double out);

  /**
   * main method of the Mapper - ratio of ranges are applied in addition to any
   * limits which exist
   * 
   * @param in
   * @return
   */
  Double calcOutput(Double in);

  /**
   * the the max input limit
   * 
   * @return
   */
  Double getMax();

  /**
   * get part of the ratio range If you want limits use getMin and getMax
   * 
   * @return
   */
  public Double getMaxX();

  /**
   * get part of the ratio range If you want limits use getMin and getMax
   * 
   * @return
   */
  public Double getMaxY();

  /**
   * get the min input limit
   * 
   * @return
   */
  Double getMin();

  /**
   * get part of the ratio range If you want limits use getMin and getMax
   * 
   * @return
   */
  public Double getMinX();

  /**
   * get part of the ratio range If you want limits use getMin and getMax
   * 
   * @return
   */
  public Double getMinY();

  /**
   * returns if the mapper is currently inverted
   * 
   * @return
   */
  boolean isInverted();

  /**
   * Default behavior of this function is to map one range on another range of
   * numbers. e.g. map(-1.0, 1.0, -1.0, 1.0) maps one to one the input to the
   * output when calcOutput is called output = mapper.calcOutput(0.7) output
   * would be 0.7 function which "only" sets the range mapping value without
   * setting input or output limits
   * 
   */
  void map(Double minX, Double maxX, Double minY, Double maxY);

  /**
   * Integer form of map
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  void map(Integer minX, Integer maxX, Integer minY, Integer maxY);

  /**
   * Merges non null values of "other" mapper with this mapper's null fields.
   * useful for setting values in MotorControl from "default" values in
   * MotorControllers. Since the MotorControl doesn't know what would be an
   * appropriate mapping value e.g -1,1 =&lt; ?,? the minY and maxY are left
   * null until a "merge" is done in the attach of the AbstractMotorController.
   * 
   * When a Sabertooth motor controller is "attached" to a MotorControl the
   * merge produces -1,1 =&lt; -127, 127 which is appropriate.
   * 
   * @param other
   *          - other mapper
   */
  void merge(Mapper other);

  /**
   * nullifying limits
   */
  void resetLimits();

  /**
   * Inverts the calculation multiplies the output by -1
   * 
   * @param invert
   */
  void setInverted(boolean invert);

  /**
   * set limits on input
   * 
   * @param minIn
   * @param maxIn
   */
  void setMinMax(Double minIn, Double maxIn);

  /**
   * Integer form of setting limits
   * 
   * @param min
   * @param max
   */
  void setMinMax(Integer min, Integer max);

  /**
   * merges any non null values to values passed in
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  void merge(Double minX, Double maxX, Double minY, Double maxY);

  /**
   * merges any non null values to values passed in
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  void merge(Integer minX, Integer maxX, Integer minY, Integer maxY);

}