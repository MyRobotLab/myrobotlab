package org.myrobotlab.jme3;

import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.font.BitmapFont;

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

  public Jme3App(JMonkeyEngine jme) {
    super(new StatsAppState(), new DebugKeysAppState(), new BasicProfilerState(false), new ScreenshotAppState("", System.currentTimeMillis()));
    this.jme = jme;
    // setShowSettings(true);
  }

  // FIXME NECESSARY ?!!?
  public void attach(Attachable service) {

  }

  @Override
  public BitmapFont loadGuiFont() {
    return super.loadGuiFont();
  }

  @Override
  public void simpleInitApp() {

    // callbacks to Service - since it cannot "extend" from SimpleApplication
    // jme service provides the "default app" - if you really need something
    // different
    // you should derive from this class and write your own init...
    jme.simpleInitApp();

  }

  @Override
  public void simpleUpdate(float tpf) {
    // callbacks to Service - since it cannot "extend" from SimpleApplication
    // jme service provides the "default app" - if you really need something
    // different
    // you should derive from this class and write your own simpleUpdate...
    jme.simpleUpdate(tpf);
  }

}
