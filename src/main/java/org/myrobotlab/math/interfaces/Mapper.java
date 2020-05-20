package org.myrobotlab.math.interfaces;

public interface Mapper {

  /**
   * inverse fn of calcOutput
   * 
   * @param out
   * @return
   */
  double calcInput(double out);

  /**
   * main method of the Mapper - ratio of ranges are applied in addition to any
   * limits which exist
   * 
   * @param in
   * @return
   */
  double calcOutput(double in);

  /**
   * This is the input minimum value for the mapper.
   * 
   * @return
   */
  public double getMinX();
  
  /**
   * This is the maximum input value for the mapper.
   * 
   * @return
   */
  public double getMaxX();

  /**
   * This is the minimum output value that the mapper will return
   * assuming the input falls between minX and maxX.
   * 
   * @return
   */
  public double getMinY();

  /**
   * This is the maximum output value that the mapper will return
   * assuming the input falls between minX and maxX
   * 
   * @return
   */
  
  public double getMaxY();

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
  void map(double minX, double maxX, double minY, double maxY);

  /**
   * Integer form of map
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  void map(int minX, int maxX, int minY, int maxY);

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
  void setMinMax(double minIn, double maxIn);

  /**
   * Integer form of setting limits
   * 
   * @param min
   * @param max
   */
  void setMinMax(int min, int max);
  
  /**
   * If true this will make sure that input values are clipped
   * otherwise, the input values are not clipped which means output values are unbounded. 
   */
  void setClipInput(boolean clipInput);

  /**
   * Return true if the mapper is set to clip the input values.  otherwise false.
   */
  boolean isClipInput();

  
}