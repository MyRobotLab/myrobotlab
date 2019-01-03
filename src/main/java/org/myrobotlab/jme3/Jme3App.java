package org.myrobotlab.jme3;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.service.JMonkeyEngine;

import com.jme3.app.SimpleApplication;

/**
 * This is a base abstract class to implement common data structures and methods
 * across "all" Jme3Apps
 * 
 * @author GroG
 *
 */
public class Jme3App extends SimpleApplication {

  protected transient JMonkeyEngine jme = null;

  public Jme3App(JMonkeyEngine jme) {
    this.jme = jme;
  }

  // FIXME NECESSARY ?!!?
  public void attach(Attachable service) {

  }

  @Override
  public void simpleInitApp() {
    // callbacks to Service - since it cannot "extend" from SimpleApplication
    // jme service provides the "default app" - if you really need something
    // different
    // you should derive from this class and write your own init...
    jme.simpleInitApp();
  }

  public void simpleUpdate(float tpf) {
    // callbacks to Service - since it cannot "extend" from SimpleApplication
    // jme service provides the "default app" - if you really need something
    // different
    // you should derive from this class and write your own simpleUpdate...
    jme.simpleUpdate(tpf);
  }
}
