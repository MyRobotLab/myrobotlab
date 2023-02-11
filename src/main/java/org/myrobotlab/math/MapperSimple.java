package org.myrobotlab.math;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.interfaces.Mapper;
import org.slf4j.Logger;

public class MapperSimple implements Mapper {

  public final static Logger log = LoggerFactory.getLogger(MapperSimple.class);

  public double minIn;
  public double maxIn;
  public double minOut;
  public double maxOut;
  
  public MapperSimple(){}

  public MapperSimple(double minIn, double maxIn, double minOut, double maxOut) {
    map(minIn, maxIn, minOut, maxOut);
  }

  public MapperSimple(int minIn, int maxIn, int minOut, int maxOut) {
    map((double) minIn, (double) maxIn, (double) minOut, (double) maxOut);
  }

  public void map(double minIn, double maxIn, double minOut, double maxOut) {
    this.minIn = minIn;
    this.maxIn = maxIn;
    this.minOut = minOut;
    this.maxOut = maxOut;
  }

  public double calcOutput(double input) {
    input = clipValue(input, minIn, maxIn);
    return minOut + ((input - minIn) * (maxOut - minOut)) / (maxIn - minIn);
  }

  private static double clipValue(double value, double min, double max) {
    return Math.min(Math.max(value, Math.min(min, max)), Math.max(min, max));
  }

  @Override
  public double calcInput(double out) {
    out = clipValue(out, minOut, maxOut);
    return minIn + ((out - minOut) * (maxIn - minIn)) / (maxOut - minOut);
  }

  @Override
  public double getMinX() {
    return minIn;
  }

  @Override
  public double getMaxX() {
    return maxIn;
  }

  @Override
  public double getMinY() {
    return minOut;
  }

  @Override
  public double getMaxY() {
    return maxOut;
  }

  @Override
  public boolean isInverted() {
    return maxIn < minIn;
  }

  @Override
  public void map(int minIn, int maxIn, int minOut, int maxOut) {
    this.minIn = minIn;
    this.maxIn = maxIn;
    this.minOut = minOut;
    this.maxOut = maxOut;
  }

  @Override
  public void setInverted(boolean invert) {
    double tmp = minIn;
    minIn = maxIn;
    maxIn = tmp;
  }

  @Override
  public void setMinMax(double min, double max) {
    minIn = min;
    maxIn = max;
    minOut = min;
    maxOut = max;
  }

  @Override
  public void setMinMax(int min, int max) {
    minIn = min;
    maxIn = max;
    minOut = min;
    maxOut = max;
  }

  @Override
  public void setClip(boolean clip) {
    if (!clip) {
      log.error("!clip not supported");
    }
  }

  @Override
  public boolean isClip() {
    return true;
  }

}
