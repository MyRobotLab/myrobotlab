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
  public boolean inverted = false;
  public boolean clip = true;
  /**
   * a mapper takes an a range of inputs minX to maxX applies some math to it
   * and calculates an output Y that falls in the range of minY to maxY.
   * 
   * Note:This is really specific only to the linear mapper. and will/should
   * move to the MapperLinear class to make this interface generic.
   * 
   */
  public double minX;
  public double maxX;
  public double minY;
  public double maxY;

  /**
   * non-parameterized constructor for "un-set" mapper use the merge function to
   * set "default" values from the MotorController
   */
  public MapperBase() {
  }

  public MapperBase(double minX, double maxX, double minY, double maxY) {
    map(minX, maxX, minY, maxY);
  }

  public MapperBase(double minX, double maxX, double minY, double maxY, boolean clip, boolean inverted) {
    setClip(clip);
    setInverted(inverted);
    map(minX, maxX, minY, maxY);
  }

  public MapperBase(int minX, int maxX, int minY, int maxY) {
    map((double) minX, (double) maxX, (double) minY, (double) maxY);
  }

  @Override
  abstract public double calcInput(double out);

  @Override
  abstract public double calcOutput(double in);

  @Override
  public double getMaxX() {
    return maxX;
  }

  @Override
  public double getMaxY() {
    return maxY;
  }

  @Override
  public double getMinX() {
    return minX;
  }

  @Override
  public double getMinY() {
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
  public void map(double minX, double maxX, double minY, double maxY) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
  }

  @Override
  public void map(int minX, int maxX, int minY, int maxY) {
    map((double) minX, (double) maxX, (double) minY, (double) maxY);
  }

  /**
   * invert the Y range
   */
  @Override
  public void setInverted(boolean invert) {
    inverted = invert;
  }

  /**
   * set limits for both the input and output of the mapper. Calling this will
   * set the minimum input and the minimum output to minXY The maximum input and
   * maximum output values will be set to maxXY Inputs and outputs will be
   * constrained by the same minXY and maxXY value.
   */
  @Override
  public void setMinMax(double minXY, double maxXY) {
    this.minX = minXY;
    this.maxX = maxXY;
    this.minY = minXY;
    this.maxY = maxXY;
  }

  @Override
  public void setMinMax(int minXY, int maxXY) {
    setMinMax((double) minXY, (double) maxXY);
  }

  @Override
  public boolean isClip() {
    return clip;
  }

  @Override
  public void setClip(boolean clip) {
    this.clip = clip;
  }

  @Override
  public String toString() {
    return String.format(" map(%.2f,%.2f,%.2f,%.2f) inverted %b", minX, maxX, minY, maxY, inverted);
  }

}
