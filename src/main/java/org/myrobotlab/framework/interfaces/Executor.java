package org.myrobotlab.framework.interfaces;

public interface Executor extends Invoker {

  public Object exec(String code);
  
  public String setName(String name);

}
