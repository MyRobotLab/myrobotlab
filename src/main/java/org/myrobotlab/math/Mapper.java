package org.myrobotlab.math;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.InMoovArm;
import org.myrobotlab.service.InMoovHead;
import org.myrobotlab.service.JMonkeyEngine;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

import com.jme3.scene.Spatial;

public final class Mapper implements Serializable {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(Mapper.class);

  // input range
  double minX;
  double maxX;

  // output range
  double minY;
  double maxY;

  // clipping
  double minOutput;
  double maxOutput;

  boolean inverted = false;

  public Mapper(double minX, double maxX, double minY, double maxY) {
    map(minX, maxX, minY, maxY);
  }

  final public double calcOutput(double in) {
    double c = minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
    if (c < minOutput) {
      log.warn("clipping {} to {}", c, minOutput);
      return minOutput;
    }
    if (c > maxOutput) {
      log.warn("clipping {} to {}", c, maxOutput);
      return maxOutput;
    }
    return c;
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

  public void setMax(double max) {
    maxOutput = max;
    // maxX = max;
  }

  public void setMin(double min) {
    minOutput = min;
  }

  public double getMinOutput() {
    return minOutput;
  }

  public double getMaxOutput() {
    return maxOutput;
  }

  public String toString() {
    return String.format("map(%.2f,%.2f,%.2f,%.2f)", minX, maxX, minY, maxY);
  }

  public void setMinMaxInput(double min, double max) {
    minX = min;
    maxX = max;
  }

  public void setMinMaxOutput(double min, double max) {
    minOutput = min;
    maxOutput = max;
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
