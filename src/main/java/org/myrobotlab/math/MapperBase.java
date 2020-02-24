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
  public Double getMax() {
    return this.maxIn;
  }

  @Override
  public Double getMaxX() {
    return maxX;
  }

  @Override
  public Double getMaxY() {
    return maxY;
  }

  @Override
  public Double getMin() {
    return this.minIn;
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
   * merge sets any values which have not already been set - useful when one
   * service wishes to control the input range and a different service should
   * control the output range e.g. motorControl.map(motorController.getMap())
   * where motorControl wants -1.0, 1.0 on the input range and motorController
   * range might be -128, 128 like the SaberTooth motor controller
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  @Override
  public void merge(Double minX, Double maxX, Double minY, Double maxY) {
    if (this.minX == null) {
      this.minX = minX;
    }
    if (this.maxX == null) {
      this.maxX = maxX;
    }
    if (this.minY == null) {
      this.minY = minY;
    }
    if (this.maxY == null) {
      this.maxY = maxY;
    }
  }

  /**
   * merge sets any values which have not already been set - useful when one
   * service wishes to control the input range and a different service should
   * control the output range e.g. motorControl.map(motorController.getMap())
   * where motorControl wants -1.0, 1.0 on the input range and motorController
   * range might be -128, 128 like the SaberTooth motor controller
   * 
   * @param minX
   * @param maxX
   * @param minY
   * @param maxY
   */
  @Override
  public void merge(Integer minX, Integer maxX, Integer minY, Integer maxY) {
    if (this.minX == null) {
      this.minX = (minX == null) ? null : minX.doubleValue();
    }
    if (this.maxX == null) {
      this.maxX = (maxX == null) ? null : maxX.doubleValue();
    }
    if (this.minY == null) {
      this.minY = (minY == null) ? null : minY.doubleValue();
    }
    if (this.maxY == null) {
      this.maxY = (maxY == null) ? null : maxY.doubleValue();
    }
  }

  /**
   * Merging leaves the original if set and sets the original with the new
   * values if the original is null
   */
  @Override
  public void merge(Mapper in) {
    if (MapperBase.class.isAssignableFrom(in.getClass())) {
      MapperBase other = (MapperBase) in;
      if (minX == null) {
        minX = other.minX;
      }
      if (maxX == null) {
        maxX = other.maxX;
      }
      if (minY == null) {
        minY = other.minY;
      }
      if (maxY == null) {
        maxY = other.maxY;
      }
      if (this.minIn == null) {
        this.minIn = other.minIn;
      }
      if (this.maxIn == null) {
        this.maxIn = other.maxIn;
      }
    } else {
      log.error("don't know how to merge %s into a MapperLinear mapper", in.getClass().getCanonicalName());
    }
  }

  /**
   * everything set back to null
   */
  public void reset() {
    minX = null;
    maxX = null;
    minY = null;
    maxY = null;

    minIn = null;
    maxIn = null;

    inverted = false;
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
