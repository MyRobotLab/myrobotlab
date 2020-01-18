package org.myrobotlab.math;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.interfaces.Mapper;
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

    boolean willCalculate = (minX != null && maxX != null && minY != null && maxY != null);

    if (willCalculate) {

      if (minIn != null && in < minIn) {
        in = minIn;
      }

      if (maxIn != null && in > maxIn) {
        in = maxIn;
      }

      if (!inverted) {
        return minY + ((in - minX) * (maxY - minY)) / (maxX - minX);
      } else {
        return minY + ((in - maxX) * (maxY - minY)) / (minX - maxX);
      }
    }
    return in;
  }
  
  @Override
  final public Double calcInput(Double out) { 
    
    boolean willCalculate = (minX != null && maxX != null && minY != null && maxY != null);

    if (willCalculate) {

      Double in = null;
      
      if (!inverted) {
        in = minX + ((out - minY) * (maxX - minX)) / (maxY - minY);
      } else {
        in = minX + ((out + minY) * (maxX - minX)) / (minY - maxY); 
      }
     
      if (minIn != null && in < minIn) {
        in = minIn;
      }

      if (maxIn != null && in > maxIn) {
        in = maxIn;
      }

      return in;
    }
    return out;
  }

}
