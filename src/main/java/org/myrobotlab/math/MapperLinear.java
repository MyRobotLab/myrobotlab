package org.myrobotlab.math;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.slf4j.Logger;

public final class MapperLinear implements Serializable, Mapper {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(MapperLinear.class);

  // mapping range related
  Double minX;
  Double maxX;
  Double minY;
  Double maxY;

  // limits
  Double min;
  Double max;

  Boolean inverted = false;

  /**
   * non-parameterized contructor for "un-set" mapper use the merge function to
   * set "default" values from the motorcontroller
   */
  public MapperLinear() {
  }

  public MapperLinear(Double minX, Double maxX, Double minY, Double maxY) {
    map(minX, maxX, minY, maxY);
  }

  @Override
  final public Double calcOutput(Double in) {

    double c = 0.0;

    if (in == null) {
      log.warn("calcOutput(null)");
      return c;
    }

    boolean willCalculate = (minX != null && maxX != null && minY != null && maxY != null);

    if (willCalculate) {
      if (maxX - minX == 0) {
        log.error("maxX - minX divide by zero error in calcOutput");
        return 0.0;
      }

      if (!inverted) {
        c = minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
      } else {
        c = minY + ((in - maxX) * (maxY - minY)) / (minX - maxX);
      }
    } else {
      log.error("mapping values are not set - will not calculate");
    }

    if (this.min != null && c < this.min) {
      log.warn("clipping output {} to {}", c, this.min);
      return this.min;
    }

    if (this.max != null && c > this.max) {
      log.warn("clipping output {} to {}", c, this.max);
      return this.max;
    }

    return c;
  }

  public double getMaxX() {
    return maxX;
  }

  public double getMaxY() {
    return maxY;
  }

  public double getMinX() {
    return minX;
  }

  public double getMinY() {
    return minY;
  }

  public boolean isInverted() {
    return inverted;
  }

  public void setInverted(boolean invert) {
    inverted = invert;
  }

  @Override
  public Double getMin() {
    return this.min;
  }

  @Override
  public Double getMax() {
    return this.max;
  }

  @Override
  public void setLimits(Double min, Double max) {
    if (min != null && max != null && max < min) {
      double t = min;
      min = max;
      max = t;
    }
    this.min = min;
    this.max = max;
  }

  public String toString() {
    return String.format(" map(%.2f,%.2f,%.2f,%.2f) => %.2f < o < %.2f %b", minX, maxX, minY, maxY, min, max, inverted);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.myrobotlab.math.MapperInterface#merge(org.myrobotlab.math.MapperLinear)
   */
  @Override
  public void merge(Mapper in) {
    if (MapperLinear.class.isAssignableFrom(in.getClass())) {
      MapperLinear other = (MapperLinear) in;
      if (minX == null)
        minX = other.minX;
      if (maxX == null)
        maxX = other.maxX;
      if (minY == null)
        minY = other.minY;
      if (maxY == null)
        maxY = other.maxY;
      if (this.min == null)
        this.min = other.min;
      if (this.max == null)
        this.max = other.max;
    } else {
      log.error("don't know how to merge %s into a MapperLinear mapper", in.getClass().getCanonicalName());
    }
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("INFO");

      Mapper mapper = new MapperLinear();
      mapper.map(-1.0, 1.0, -1.0, 1.0);
      log.info("mapper {}", mapper);
      double result = mapper.calcOutput(0.5);
      log.info("{}", result);

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  /**
   * sets mapping and both input & output limits
   */
  @Override
  public void map(Double minX, Double maxX, Double minY, Double maxY) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;

    setLimits(minY, maxY);
  }

  /**
   * everything set back to null
   */
  public void reset() {
    minX = null;
    maxX = null;
    minY = null;
    maxY = null;

    min = null;
    max = null;

    inverted = false;
  }

  @Override
  public void setMap(Double minX, Double maxX, Double minY, Double maxY) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
  }

}
