package org.myrobotlab.math;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public final class Mapper implements Serializable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Mapper.class);

  /**
   * input range
   */
  double minX;
  double maxX;

  /**
   * output range
   */
  double minY;
  double maxY;

  boolean inverted = false;

  public Mapper(double minX, double maxX, double minY, double maxY) {
    map(minX, maxX, minY, maxY);
  }

  final public double calcOutput(double in) {
    return  minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
  }

  final public int calcOutputInt(int in) {
    return (int) calcOutput(in);
  }

  final public int calcOutputInt(double in) {
    return (int) calcOutput(in);
  }

  final public double calcInput(double out) {
    double c = minX + ((out - minY) * (maxX - minX)) / (maxY - minY);
    if (c < minX) {
      return minX;
    }
    if (c > maxX) {
      return maxX;
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
    // change state ONLY if needed
    // we need only 1 logic : inverted=output inverted / output
    // inverted=inverted

    double t = minY;
    if (invert) {
      if (minY < maxY) {
        minY = maxY;
        maxY = t;
      }
    } else {
      if (minY > maxY) {
        minY = maxY;
        maxY = t;
      }
    }

    inverted = invert;
  }

  public String toString() {
    return String.format("map(%.2f,%.2f,%.2f,%.2f)", minX, maxX, minY, maxY);
  }

  public void setMinMaxInput(double min, double max) {
    minX = min;
    maxX = max;
  }

  public double getMaxInput() {
    return maxX;
  }

  public double getMinInput() {
    return minX;
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
  
  public static void main(String[] args) {
    try {
      LoggingFactory.init("info");

      Mapper m = new Mapper(0, 180, 90, -90);
      log.info("in {} out {} inverse {}", 5, m.calcOutput(5), m.calcInput(m.calcOutput(5)));
      log.info("in {} out {} inverse {}", -5, m.calcOutput(-5), m.calcInput(m.calcOutput(-5)));
      log.info("in {} out {} inverse {}", 270, m.calcOutput(270), m.calcInput(m.calcOutput(270)));
      m = new Mapper(0, 360, 180, -180);
      log.info("in {} out {} inverse {}", 5, m.calcOutput(5), m.calcInput(m.calcOutput(5)));
      log.info("in {} out {} inverse {}", -5, m.calcOutput(-5), m.calcInput(m.calcOutput(-5)));
      log.info("in {} out {} inverse {}", 270, m.calcOutput(270), m.calcInput(m.calcOutput(270)));

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }


}
