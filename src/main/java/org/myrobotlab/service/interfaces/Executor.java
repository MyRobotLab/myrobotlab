package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.JsonInvoker;
import org.myrobotlab.framework.interfaces.JsonSender;

/**
 * Interface to a Executor - currently only utilized by Py4j to
 * represent an interface for python from java-land, implemented
 * by MessageHandler in /resource/Py4j/Py4j.py
 * 
 * @author GroG
 *
 */
public interface Executor extends JsonInvoker, JsonSender {

  /**
   * exec in Python - executes arbitrary code
   * @param code
   * @return
   */
  public Object exec(String code);
  
  /**
   * To use the service framework appropriately, you only need
   * a reference to runtime and the service name.  This method
   * is required in Java-land to set the MessageHandler's name in
   * Python-land
   * @param name
   * @return
   */
  public String setName(String name);

}
