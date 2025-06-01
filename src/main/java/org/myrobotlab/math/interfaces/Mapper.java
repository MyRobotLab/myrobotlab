package org.myrobotlab.math.interfaces;

public interface Mapper {

  /**
   * inverse fn of calcOutput
   * 
   * @param out
   *          the output of the mapper
   * @return the reverse calculated input
   * 
   */
  double calcInput(double out);

  /**
   * main method of the Mapper - ratio of ranges are applied in addition to any
   * limits which exist
   * 
   * @param in
   *          the input value to the mapper
   * @return the calculated output
   * 
   */
  double calcOutput(double in);

  /**
   * @return This is the input minimum value for the mapper.
   */
  public double getMinX();

  /**
   * @return This is the maximum input value for the mapper.
   */
  public double getMaxX();

  /**
   * @return This is the minimum output value that the mapper will return
   *         assuming the input falls between minX and maxX.
   * 
   * 
   */
  public double getMinY();

  /**
   * @return This is the maximum output value that the mapper will return
   *         assuming the input falls between minX and maxX
   * 
   */

  public double getMaxY();

  /**
   * @return Returns true if the minY is greater than the maxY
   */

  boolean isInverted();

  /**
   * Default behavior of this function is to map one range on another range of
   * numbers. e.g. map(-1.0, 1.0, -1.0, 1.0) maps one to one the input to the
   * output when calcOutput is called output = mapper.calcOutput(0.7) output
   * would be 0.7 function which "only" sets the range mapping value without
   * setting input or output limits
   * 
   * @param minX
   *          min input
   * @param maxX
   *          max input
   * @param minY
   *          min output
   * @param maxY
   *          max output
   * 
   */
  void map(double minX, double maxX, double minY, double maxY);

  /**
   * Integer form of map
   * 
   * @param minX
   *          min input
   * @param maxX
   *          max input
   * @param minY
   *          min output
   * @param maxY
   *          max output
   */
  void map(int minX, int maxX, int minY, int maxY);

  /**
   * Inverts the calculation multiplies the output by -1
   * 
   * @param invert
   *          - true is inverted false is not
   */
  void setInverted(boolean invert);

  /**
   * set limits on output of the mapper Deprecated, this method is ambiguous.
   * explicitly call map(minX,maxX,minY,maxY) instead.
   * 
   * @param minXY
   *          (both input and output)
   * @param maxXY
   *          (both input and output)
   */
  @Deprecated
  void setMinMax(double minXY, double maxXY);

  /**
   * Integer form of setting limits Deprecated, this method is ambiguous.
   * explicitly call map(minX,maxX,minY,maxY) instead.
   * 
   * @param min
   *          (input and output max)
   * @param max
   *          (input and output max)
   */
  @Deprecated
  void setMinMax(int min, int max);

  /**
   * If true this will make sure that input values are clipped to the range
   * specified as minX and maxX. The resulting computed values will also be
   * constrained to minY and maxY If false, inputs and outputs will not be
   * clipped, but rather a normal linear mapping will apply.
   * 
   * @param clip
   *          true to clip the values
   */
  void setClip(boolean clip);

  /**
   * @return true if the mapper is set to clip the input and output values.
   *         otherwise false.
   */
  boolean isClip();

}