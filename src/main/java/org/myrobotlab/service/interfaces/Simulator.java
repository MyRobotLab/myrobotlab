package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.Attachable;

/**
 * The purpose of this is to provide simulation or 3d graphic systems a common
 * interface This interface is a form of ObjectFactory pattern used to create
 * org.myrobotlab.virtual classes, whos specific implementation are managed by
 * these Simulator services Jme3 is an example of a simulator service
 * 
 * @author GroG
 *
 */

// FIXME !!! refactor this stuff out - create an AbstractSimulator ... 
public interface Simulator extends Attachable {
  // ServoController getServoController();
}
