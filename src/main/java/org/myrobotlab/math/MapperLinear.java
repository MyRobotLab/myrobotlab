package org.myrobotlab.math;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public final class MapperLinear extends MapperBase {

  public final static Logger log = LoggerFactory.getLogger(MapperLinear.class);
  private static final long serialVersionUID = 1L;
  
  public MapperLinear() {
    super();
  }

  public MapperLinear(Integer minX, Integer maxX, Integer minY, Integer maxY) {
    super(minX, maxX, minY, maxY);
  }
  
  public MapperLinear(Double minX, Double maxX, Double minY, Double maxY) {
    super(minX, maxX, minY, maxY);
  }

  @Override
  public Double calcOutput(Double in) {
    if (in == null) {
      log.warn("calcOutput(null)");
      return in;
    }
    in = clipValue(in, minX, maxX);
    if (!inverted) {
      return minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
    } else {
      return minY + ((in - maxX) * (maxY - minY)) / (minX - maxX);
    }
  }

  // make sure value lies between the min/max value
  private static Double clipValue(Double value, Double min, Double max) {
    // clip the input
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    } else {
      return value;
    }
  }
  
  @Override
  final public Double calcInput(Double out) { 
    // the output value needs to be between minY and maxY
    out = clipValue(out, minY, maxY);
    Double in = null;
    if (!inverted) {
      in = minX + ((out - minY) * (maxX - minX)) / (maxY - minY);
    } else {
      in = minX + ((out + minY) * (maxX - minX)) / (minY - maxY); 
    }
    return in;
  }

}
