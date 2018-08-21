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
   *      
   * In addition it sets min and max inputs with the minX & maxX values
   * and the min max of output with minY maxY respectively
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   * </pre>
   */
  void map(Double minX, Double maxX, Double minY, Double maxY);
  
  /**
   * function which "only" sets the range mapping value without setting input or output limits
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  void setMap(Double minX, Double maxX, Double minY, Double maxY);

  void merge(Mapper mapperInterface);
  
  void setLimits(Double minOutput, Double maxOutput);

}