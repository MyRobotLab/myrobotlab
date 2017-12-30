/* author: Louis Hugues - created on 12. 2005  */
package org.myrobotlab.mapper.gui;

import javax.vecmath.Vector3d;

import org.myrobotlab.mapper.sim.Agent;
import org.myrobotlab.mapper.sim.EnvironmentDescription;
import org.myrobotlab.mapper.sim.Wall;

/**
 * Test of the batch mode - test Simbatch class.
 */
public class TestSimbatch extends EnvironmentDescription {

  public class MyRobot extends Agent {

    public MyRobot(Vector3d position, String name) {
      super(position, name);
    }

    /** Initialize Agent's Behavior */
    @Override
    public void initBehavior() {
      // nothing particular in this case
    }

    /** Perform one step of Agent's Behavior */
    @Override
    public void performBehavior() {

      if (collisionDetected()) {
        moveToStartPosition();
      } else {
        // progress at 0.5 m/s
        setTranslationalVelocity(0.1);
        setRotationalVelocity(0);
      }
      if (getCounter() % 100000 == 0)
        System.out.println("Counter " + getCounter());
    }
  }

  public static void main(String[] args) {
    Simbatch sim = new Simbatch(new TestSimbatch(), true);
    sim.reset();
    for (int i = 0; i < 10000000; i++) {
      sim.step();
    }
    System.out.println("Done...");
    sim.dispose();
    System.exit(0);
  }

  public TestSimbatch() {
    Wall w1 = new Wall(new Vector3d(9, 0, 0), 19, 1, this);
    w1.rotate90(1);
    add(w1);
    Wall w2 = new Wall(new Vector3d(-9, 0, 0), 19, 2, this);
    w2.rotate90(1);
    add(w2);
    Wall w3 = new Wall(new Vector3d(0, 0, 9), 19, 1, this);
    add(w3);
    Wall w4 = new Wall(new Vector3d(0, 0, -9), 19, 2, this);
    add(w4);

    add(new MyRobot(new Vector3d(-4, 0, -3.5f), "robot"));
  }
}