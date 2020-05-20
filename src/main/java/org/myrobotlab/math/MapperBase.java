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
   * 4 values which are related to the ratio of mapping range x over range y.
   * They are Double values because there is the possibility of them not being
   * set. This allows values of sub-types services to be merged in. For example
   * an abstract service might have x range set but not y. map(-1.0, 1.0, null,
   * null) and the sub-type concrete service can have actual values. Sabertooth
   * would mapper.merge(null, null, 128, -128) as an example
   */
  Double maxIn;
  Double maxX;
  Double maxY;
  Double minIn;

  /**
   * These are min and max input values. Values outside of this range will be
   * "clipped"
   */
  Double minX;
  Double minY;

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
  public void setMinMax(Double min, Double max) {
    this.minIn = min;
    this.maxIn = max;
  }

  @Override
  public void setMinMax(Integer min, Integer max) {
    setMinMax((min == null) ? null : min.doubleValue(), (max == null) ? null : max.doubleValue());
  }

  @Override
  public void resetLimits() {
    minIn = null;
    maxIn = null;
  }

  public String toString() {
    return String.format(" map(%.2f,%.2f,%.2f,%.2f) => %.2f < o < %.2f inverted %b", minX, maxX, minY, maxY, minIn, maxIn, inverted);
  }

}
