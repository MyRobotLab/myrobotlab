package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.virtual.VirtualMotor;
import org.myrobotlab.virtual.VirtualServo;

/**
 * The purpose of this is to provide simulation or 3d graphic systems a common interface
 * This interface is a form of ObjectFactory pattern used to create org.myrobotlab.virtual classes, whos
 * specific implementation are managed by these Simulator services  Jme3 is an example of a simulator service
 * @author GroG
 *
 */

public interface Simulator extends NameProvider {
  
  // Generalized create ??? a good thing - router for ObjectFactory??
  //public Object create(ServiceInterface service);

  public VirtualServo createVirtualServo(String name);

  public VirtualMotor createVirtualMotor(String name);
  

}
