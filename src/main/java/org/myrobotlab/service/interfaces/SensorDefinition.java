package org.myrobotlab.service.interfaces;

import java.io.Serializable;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.math.Mapper;

public abstract class SensorDefinition implements NameProvider, Serializable {
  private static final long serialVersionUID = 1L;
  Mapper outputMapper;
  String serviceName;
  
  public SensorDefinition(String SensorDefinition){
    this.serviceName = SensorDefinition;    
  }
  
  public String getName() {
    return serviceName;
  }

}
