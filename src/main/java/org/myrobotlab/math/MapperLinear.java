package org.myrobotlab.math;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * This class provides a basic linear mapping between an input and an output.
 * The input is a range from minX to maxX. The output is a range from minY to
 * maxY. The inputs will be scaled from the input range to the output range.
 * Inputs that fall outside of the input range will be clipped to be minX/maxX
 * values.
 */
public final class MapperLinear extends MapperBase {

  public final static Logger log = LoggerFactory.getLogger(MapperLinear.class);
  private static final long serialVersionUID = 1L;

  public MapperLinear() {
    super();
  }

  public MapperLinear(int minX, int maxX, int minY, int maxY) {
    super(minX, maxX, minY, maxY);
  }

  public MapperLinear(double minX, double maxX, double minY, double maxY) {
    super(minX, maxX, minY, maxY);
  }

  public MapperLinear(double minX, double maxX, double minY, double maxY, boolean clip, boolean inverted) {
    super(minX, maxX, minY, maxY, clip, inverted);
  }

  @Override
  public double calcOutput(double input) {
    if (isClip()) {
      input = clipValue(input, minX, maxX);
    }
    if (!inverted) {
      return minY + ((input - minX) * (maxY - minY)) / (maxX - minX);
    } else {
      return minY + ((input - maxX) * (maxY - minY)) / (minX - maxX);
    }
  }

  // make sure value lies between the min/max value
  private static double clipValue(double value, double min, double max) {
    // the input value must be between min and max.
    if (value < Math.min(min, max)) {
      // if it's lower than the lowest, return the lowest
      return Math.min(min, max);
    } else if (value > Math.max(min, max)) {
      // if it's higher than the highest, return the highest.
      return Math.max(min, max);
    } else {
      // the original value is between min/max, return it without change.
      return value;
    }
  }

  @Override
  final public double calcInput(double out) {
    // the output value needs to be between minY and maxY
    if (isClip()) {
      out = clipValue(out, minY, maxY);
    }
    if (!inverted) {
      return minX + ((out - minY) * (maxX - minX)) / (maxY - minY);
    } else {
      return maxX + ((out - minY) * (maxX - minX)) / (minY - maxY);
    }
  }

}
