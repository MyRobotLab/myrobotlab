package org.myrobotlab.lang.py;

import org.myrobotlab.framework.interfaces.ServiceInterface;

public interface PythonGenerator {
  
  public String toPython(ServiceInterface si);
  
}
