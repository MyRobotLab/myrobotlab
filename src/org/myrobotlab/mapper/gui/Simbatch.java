/* author: Louis Hugues - created on 12. 2005  */
package org.myrobotlab.mapper.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;

import javax.media.j3d.Canvas3D;

import org.myrobotlab.mapper.sim.EnvironmentDescription;
import org.myrobotlab.mapper.sim.Simulator;
import org.myrobotlab.mapper.sim.World;

/**
 * Runs simbad simulator in batch mode with no user interface (only small 3d
 * window). Using the folling scenario: construct-&gt;reset-&gt;step, step ,..., step
 * -&gt; dispose-&gt; System.exit
 */
public class Simbatch {
  int counter;
  Frame frame;
  World world;
  Simulator simulator;
  Canvas3D canvas3d;
  Panel panel;

  /* Construct a batch version of Simbad simulator */
  public Simbatch(EnvironmentDescription ed, boolean do3DRendering) {
    counter = 0;
    world = new World(ed);
    // !!!!
    // We need absolutly to show the 3d world in a window
    // otherwise it reveal a memory bug in java3d
    // see Bug ID: 4727054
    // !!!!!
    canvas3d = world.getCanvas3D();
    frame = new Frame();
    panel = new Panel();
    panel.setLayout(new BorderLayout());
    panel.add(canvas3d);

    frame.add(panel);
    frame.pack();
    frame.setSize(100, 100);
    frame.setVisible(true);
    if (!do3DRendering) {
      frame.hide();
      // Do not render
      canvas3d.stopRenderer();
    }
    simulator = new Simulator(null, world, ed);
  }

  /** Dispose resource at end. **/
  public void dispose() {

    simulator.dispose();
    world.dispose();
    simulator = null;
    world = null;
    canvas3d = null;

    frame.dispose();
    System.runFinalization();
    System.gc();
  }

  /** Restart the simulation */
  public void reset() {
    simulator.resetSimulation();
    simulator.initBehaviors();
  }

  /** perform one step - call it in your main loop */
  public void step() {
    simulator.simulateOneStep();
  }

}
