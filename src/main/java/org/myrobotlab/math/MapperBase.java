package org.myrobotlab.math;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.slf4j.Logger;

public abstract class MapperBase implements Serializable, Mapper {

  public final static Logger log = LoggerFactory.getLogger(MapperBase.class);
  private static final long serialVersionUID = 1L;

  /**
   * a convienence method to invert Y output
   */
  boolean inverted = false;

/**
 * a mapper takes an a range of inputs minX to maxX
 *  applies some math to it and calculates an output Y that falls in the range 
 *  of minY to maxY.
 *  
 *  Note:This is really specific only to the linear mapper.  and will/should
 *  move to the MapperLinear class to make this interface generic.
 * 
 */
  Double minX;
  Double maxX;
  Double minY;
  Double maxY;

  /**
   * non-parameterized constructor for "un-set" mapper use the merge function to
   * set "default" values from the MotorController
   */
  public MapperBase() {
  }

  public MapperBase(Double minX, Double maxX, Double minY, Double maxY) {
    map(minX, maxX, minY, maxY);
  }

  public MapperBase(Integer minX, Integer maxX, Integer minY, Integer maxY) {
    map((minX == null) ? null : minX.doubleValue(), (maxX == null) ? null : maxX.doubleValue(), (minY == null) ? null : minY.doubleValue(),
        (maxY == null) ? null : maxY.doubleValue());
  }

  @Override
  abstract public Double calcInput(Double out);

  @Override
  abstract public Double calcOutput(Double in);

  @Override
  public Double getMaxX() {
    return maxX;
  }

  @Override
  public Double getMaxY() {
    return maxY;
  }

  @Override
  public Double getMinX() {
    return minX;
  }

  @Override
  public Double getMinY() {
    return minY;
  }

  @Override
  public boolean isInverted() {
    return inverted;
  }

  /**
   * sets mapping and both input and output limits
   */
  @Override
  public void map(Double minX, Double maxX, Double minY, Double maxY) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
  }

  @Override
  public void map(Integer minX, Integer maxX, Integer minY, Integer maxY) {
    this.minX = (minX == null) ? null : minX.doubleValue();
    this.minX = (maxX == null) ? null : maxX.doubleValue();
    this.minY = (minY == null) ? null : minY.doubleValue();
    this.maxY = (maxY == null) ? null : maxY.doubleValue();
  }

  /**
   * invert the Y range
   */
  public void setInverted(boolean invert) {
    inverted = invert;
  }

  /**
   * set limits which will clip input to these values if setMinMax (-1.0, 1.0)
   * values sent through the calcOutput(value) if &gt; max or &lt; min will be set to
   * max and min respectively
   */
  @Override
  public void setMinMax(Double minInput, Double maxInput) {
    this.minX = minInput;
    this.maxX = maxInput;
  }

  @Override
  public void setMinMax(Integer min, Integer max) {
    setMinMax((min == null) ? null : min.doubleValue(), (max == null) ? null : max.doubleValue());
  }

  public String toString() {
    return String.format(" map(%.2f,%.2f,%.2f,%.2f) inverted %b", minX, maxX, minY, maxY, inverted);
  }

}
