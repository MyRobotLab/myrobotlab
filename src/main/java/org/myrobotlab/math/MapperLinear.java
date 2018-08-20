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

  // clipping
  Double minOutput;
  Double maxOutput;

  Boolean inverted = false;
  Double maxInput;
  Double minInput;

  /**
   * non-parameterized contructor for "un-set" mapper use the merge function to
   * set "default" values from the motorcontroller
   */
  public MapperLinear() {
  }
  
  public MapperLinear(Double minX, Double maxX, Double minY, Double maxY) {
    map(minX, maxX, minY, maxY);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#calcOutput(double)
   */
  @Override
  final public double calcOutput(Double in) {

    double c = 0.0;
    
    if (in == null) {
      log.warn("calcOutput(null)");
      return c;
    }

    boolean willCalculate = (minX != null && maxX != null && minY != null && maxY != null);

    if (minInput != null && in < minInput) {
      log.warn("clipping input {} to {}", in, minInput);
      in = minInput;
    }
    if (maxInput != null && in > maxInput) {
      log.warn("clipping input {} to {}", in, maxInput);
      in = maxInput;
    }

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

    if (minOutput != null && c < minOutput) {
      log.warn("clipping output {} to {}", c, minOutput);
      return minOutput;
    }

    if (maxOutput != null && c > maxOutput) {
      log.warn("clipping output {} to {}", c, maxOutput);
      return maxOutput;
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

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#getMinOutput()
   */
  @Override
  public Double getMinOutput() {
    return minOutput;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#getMaxOutput()
   */
  @Override
  public Double getMaxOutput() {
    return maxOutput;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#setMinMaxOutput(double, double)
   */
  @Override
  public void setMinMaxOutput(Double min, Double max) {
    if (min != null && max != null && max < min) {
      double t = min;
      min = max;
      max = t;
    }
    minOutput = min;
    maxOutput = max;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#getMinInput()
   */
  @Override
  public Double getMinInput() {
    return minInput;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#getMaxInput()
   */
  @Override
  public Double getMaxInput() {
    return maxInput;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.math.MapperInterface#setMinMaxInput(double, double)
   */
  @Override
  public void setMinMaxInput(Double min, Double max) {
    if (min != null && max != null && max < min) {
      double t = min;
      min = max;
      max = t;
    }
    this.minInput = min;
    this.maxInput = max;
  }

  public String toString() {
    return String.format(" %.2f < i < %.2f => map(%.2f,%.2f,%.2f,%.2f) => %.2f < o < %.2f ", minInput, maxInput, minX, maxX, minY, maxY, minOutput, maxOutput);
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
      if (minInput == null)
        minInput = other.minInput;
      if (maxInput == null)
        maxInput = other.maxInput;
      if (minOutput == null)
        minOutput = other.minOutput;
      if (maxOutput == null)
        maxOutput = other.maxOutput;
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

    setMinMaxInput(minX, maxX);
    setMinMaxOutput(minY, maxY);
  }

  @Override
  public void setMaxInput(Double max) {
    if (max != null && minInput != null && max < minInput) {
      maxInput = minInput;
      minInput = max;
      return;
    }
    maxInput = max;
  }

  @Override
  public void setMaxOutput(Double max) {
    if (max != null && minOutput != null && max < minOutput) {
      maxOutput = minOutput;
      minOutput = max;
      return;
    }
    maxOutput = max;
  }

  @Override
  public void setMinInput(Double min) {
    if (min != null && maxInput != null && min > maxInput) {
      minInput = maxInput;
      maxInput = min;
      return;
    }
    minInput = min;
  }

  @Override
  public void setMinOutput(Double min) {
    if (min != null && maxOutput != null && min > maxOutput) {
      minOutput = maxOutput;
      maxOutput = min;
      return;
    }
    minOutput = min;
  }

  /**
   * everything set back to null
   */
  public void reset() {
    minX = null;
    maxX = null;
    minY = null;
    maxY = null;

    minInput = null;
    maxInput = null;
    minOutput = null;
    maxOutput = null;

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
