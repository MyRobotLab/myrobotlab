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
   * This is the input minimum value for the mapper.
   * 
   * @return
   */
  public Double getMinX();
  
  /**
   * This is the maximum input value for the mapper.
   * 
   * @return
   */
  public Double getMaxX();

  /**
   * This is the minimum output value that the mapper will return
   * assuming the input falls between minX and maxX.
   * 
   * @return
   */
  public Double getMinY();

  /**
   * This is the maximum output value that the mapper will return
   * assuming the input falls between minX and maxX
   * 
   * @return
   */
  
  public Double getMaxY();

  /**
   * Returns true if the minY is greater than the maxY
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

}