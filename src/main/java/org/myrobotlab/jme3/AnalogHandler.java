package org.myrobotlab.jme3;

import org.myrobotlab.service.JMonkeyEngine;
import com.jme3.input.controls.AnalogListener;

/**
 * AnalogHandler is a shim class - JME requires a different class vs the
 * ActionListener for JMonkeyEngine to handle AnalogListener correctly because
 * of logic checking instanceof and confusion between the two interfaces ...
 */
public class AnalogHandler implements AnalogListener {
  private JMonkeyEngine jme;

  public AnalogHandler(JMonkeyEngine jme) {
    this.jme = jme;
  }

  @Override
  public void onAnalog(String name, float keyPressed, float tpf) {
    // a simple callback
    jme.onAnalog(name, keyPressed, tpf);
  }
}