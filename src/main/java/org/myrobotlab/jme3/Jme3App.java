package org.myrobotlab.jme3;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;

/**
 * This is a base abstract class to implement common data structures and methods
 * across "all" Jme3Apps
 * 
 * @author GroG
 *
 */
public class Jme3App extends SimpleApplication {

  public final static Logger log = LoggerFactory.getLogger(Jme3App.class);
  protected transient JMonkeyEngine jme = null;
  transient Nifty nifty;
  transient NiftyJmeDisplay niftyDisplay;

  public Jme3App(JMonkeyEngine jme) {
    this.jme = jme;
  }

  // FIXME NECESSARY ?!!?
  public void attach(Attachable service) {

  }

  public BitmapFont loadGuiFont() {
    return super.loadGuiFont();
  }

  public Node getGuiNode() {
    return guiNode;
  }

  @Override
  public void simpleInitApp() {
    // callbacks to Service - since it cannot "extend" from SimpleApplication
    // jme service provides the "default app" - if you really need something
    // different
    // you should derive from this class and write your own init...

    niftyDisplay = NiftyJmeDisplay.newNiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
    jme.simpleInitApp();
  }

  public void simpleUpdate(float tpf) {
    // callbacks to Service - since it cannot "extend" from SimpleApplication
    // jme service provides the "default app" - if you really need something
    // different
    // you should derive from this class and write your own simpleUpdate...
    jme.simpleUpdate(tpf);
  }
  
  public NiftyJmeDisplay getNiftyDisplay() {
    return niftyDisplay;
  }

}
