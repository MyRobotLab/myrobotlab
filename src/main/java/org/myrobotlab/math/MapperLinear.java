package org.myrobotlab.math;

import java.io.Serializable;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.WorkETest;
import org.slf4j.Logger;

public final class MapperLinear implements Serializable, MapperInterface {

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
   * non-parameterized contructor for "un-set" mapper
   * use the merge function to set "default" values from the motorcontroller
   */
  public MapperLinear() {
  }
  
  public MapperLinear(double minX, double maxX, double minY, double maxY) {
    map(minX, maxX, minY, maxY); 
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#calcOutput(double)
   */
  @Override
  final public double calcOutput(double in) {
    if (minInput != null && in < minInput) {
      log.warn("clipping input {} to {}", in, minInput);
      in = minInput;
    }
    if (maxInput != null && in > maxInput) {
      log.warn("clipping input {} to {}", in, maxInput);
      in = maxInput;
    }
    
    double c = minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
    
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

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#calcOutputInt(int)
   */
  @Override
  final public int calcOutputInt(int in) {
    return (int) calcOutput(in);
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#calcOutputInt(double)
   */
  @Override
  final public int calcOutputInt(double in) {
    return (int) calcOutput(in);
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
    // change state ONLY if needed
    // we need only 1 logic : inverted=output inverted / output
    // inverted=inverted

    this.minOutput = minY;
    this.maxOutput = maxY;
    double t = minY;
    if (invert) {
      if (minY < maxY) {
        minY = maxY;
        maxY = t;
      }
      this.minOutput = maxY;
      this.maxOutput = minY;
    } else {
      if (minY > maxY) {
        minY = maxY;
        maxY = t;
      }
      this.minOutput = minY;
      this.maxOutput = maxY;
    }

    inverted = invert;
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#getMinOutput()
   */
  @Override
  public double getMinOutput() {
    return minOutput;
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#getMaxOutput()
   */
  @Override
  public double getMaxOutput() {
    return maxOutput;
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#setMinMaxOutput(double, double)
   */
  @Override
  public void setMinMaxOutput(double minOutput, double maxOutput) {
    this.minOutput = minOutput;
    this.maxOutput = maxOutput;
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#getMinInput()
   */
  @Override
  public double getMinInput() {
    return minInput;
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#getMaxInput()
   */
  @Override
  public double getMaxInput() {
    return maxInput;
  }

  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#setMinMaxInput(double, double)
   */
  @Override
  public void setMinMaxInput(double minInput, double maxInput) {
    this.minInput = minInput;
    this.maxInput = maxInput;
  }

  public String toString() {
    return String.format(" < i < %.2f => map(%.2f,%.2f,%.2f,%.2f) => %.2f < o < %.2f ", minInput, maxInput, minX, maxX, minY, maxY, minOutput, maxOutput);
  }

  public void map(double minX, double maxX, double minY, double maxY) {
    if (maxY - minY == 0) {
      log.error("maxY - minY == 0 ..  DIVIDE BY ZERO ERROR COMING !");
    }

    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;

    if (minY < maxY) {
      setInverted(false);
    } else {
      setInverted(true);
    }
  }
  
  /* (non-Javadoc)
   * @see org.myrobotlab.math.MapperInterface#merge(org.myrobotlab.math.MapperLinear)
   */
  @Override
  public void merge(MapperInterface in) {
    if (MapperLinear.class.isAssignableFrom(in.getClass())) {
      MapperLinear other = (MapperLinear)in; 
    if (minX == null) minX = other.minX;
    if (maxX == null) maxX = other.maxX;
    if (minY == null) minY = other.minY;
    if (maxY == null) maxY = other.maxY;
    if (minInput == null) minInput = other.minInput;
    if (maxInput == null) maxInput = other.maxInput;
    if (minOutput == null) minOutput = other.minOutput;
    if (maxOutput == null) maxOutput = other.maxOutput;
    } else {
      log.error("don't know how to merge %s into a MapperLinear mapper", in.getClass().getCanonicalName());
    }
  }

  public static void main(String[] args) {
    try {
      LoggingFactory.init("INFO");
    
      MapperInterface mapper = new MapperLinear(-1, 1, -1, 1);
      log.info("mapper {}", mapper);
      double result = mapper.calcOutput(0.5);
      log.info("{}", result);
      
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

}
