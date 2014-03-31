package org.myrobotlab.service;

import java.awt.Color;

import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.mapper.gui.Simbad;
import org.myrobotlab.mapper.sim.Agent;
import org.myrobotlab.mapper.sim.Arch;
import org.myrobotlab.mapper.sim.Box;
import org.myrobotlab.mapper.sim.EnvironmentDescription;
import org.myrobotlab.mapper.sim.Wall;

/**
 * @author GroG
 * 
 * 
 * 
 *         Dependencies : Java3D simbad-1.4.jar
 * 
 *         Reference : http://simbad.sourceforge.net/guide.php#robotapi
 *         http://www.ibm.com/developerworks/java/library/j-robots/ - simbad &
 *         subsumption JMonkey
 */
public class SLAMBad extends Service {

	private static final long serialVersionUID = 1L;
	// Simbad simbad = new Simbad(new MyEnv() ,false);
	Simbad simbad;

	public final static Logger log = LoggerFactory.getLogger(SLAMBad.class.getCanonicalName());

	public static class MyEnv extends EnvironmentDescription {
		public MyEnv() {
			add(new Arch(new Vector3d(3, 0, -3), this));
			/*
			 * for (int i = 0; i < 20; ++i) { double x = (Math.random() * 20) -
			 * 10; double y = (Math.random() * 20) - 10; float xdim =
			 * (float)(Math.random() * 4) - 2; float ydim =
			 * (float)(Math.random() * 4) - 2; float zdim =
			 * (float)(Math.random() * 10); //add(new Box(new Vector3d(x, 0, y),
			 * new Vector3f(xdim, 1, ydim),this)); Wall wall; if
			 * (Math.random()*100 > 50) { wall= new Wall(new Vector3d(x, 0, y),
			 * zdim, 0.1f, 0.5f, this); } else { wall= new Wall(new Vector3d(x,
			 * 0, y), 0.f, zdim, 0.5f, this); } wall.setColor(new Color3f(new
			 * Color(Color.HSBtoRGB((float)Math.random(), 0.9f, 0.7f))));
			 * add(wall);
			 * 
			 * }
			 */
			add(new MyRobot(new Vector3d(0, 0, 0), "my robot"));
		}

		public void addWall() {

			/*
			 * for (int i = 0; i < 20; ++i) { double x = (Math.random() * 20) -
			 * 10; double y = (Math.random() * 20) - 10; float xdim =
			 * (float)(Math.random() * 4) - 2; float ydim =
			 * (float)(Math.random() * 4) - 2; float zdim =
			 * (float)(Math.random() * 10); //add(new Box(new Vector3d(x, 0, y),
			 * new Vector3f(xdim, 1, ydim),this)); Wall wall; if
			 * (Math.random()*100 > 50) { wall= new Wall(new Vector3d(x, 0, y),
			 * zdim, 0.1f, 0.5f, this); } else { wall= new Wall(new Vector3d(x,
			 * 0, y), 0.f, zdim, 0.5f, this); } wall.setColor(new Color3f(new
			 * Color(Color.HSBtoRGB((float)Math.random(), 0.9f, 0.7f))));
			 * add(wall);
			 * 
			 * }
			 */
		}
	}

	public static class MyRobot extends Agent {
		public MyRobot(Vector3d position, String name) {
			super(position, name);
		}

		public void initBehavior() {
		}

		public void performBehavior() {
			if (collisionDetected()) {
				// stop the robot
				setTranslationalVelocity(0.0);
				setRotationalVelocity(0);
			} else {
				// progress at 0.5 m/s
				setTranslationalVelocity(0.5);
				// frequently change orientation
				if ((getCounter() % 100) == 0)
					setRotationalVelocity(Math.PI / 2 * (0.5 - Math.random()));
			}
		}
	}

	public SLAMBad(String n) {
		super(n);
	}

	public void startService() {
		super.startService();
		if (simbad == null) {
			startSimulator();
		}
	}

	MyEnv env;

	public void addWall(Double x, Double y, Double z, Float x1, Float y1, Float z1) {
		Wall wall = new Wall(new Vector3d(x, y, z), x1, y1, z1, env);
		wall.setColor(new Color3f(new Color(0, 0, 0, 0)));
		simbad.attach(wall);
	}

	public void addRandomWall() {
		double x = (Math.random() * 20) - 10;
		double y = (Math.random() * 20) - 10;

		float xdim = (float) (Math.random() * 4);
		float ydim = (float) (Math.random() * 4);
		float zdim = (float) (Math.random() * 2);

		Wall wall = new Wall(new Vector3d(x, 0, y), xdim, zdim, ydim, env);
		wall.setColor(new Color3f(new Color(Color.HSBtoRGB((float) Math.random(), 0.9f, 0.7f))));
		simbad.attach(wall);
	}

	public void startSimulator() {

		env = new MyEnv();
		simbad = new Simbad(env, false);
		env.add(new Box(new Vector3d(3, 0, 0), new Vector3f(1, 1, 1), env));
		simbad.setVisible(true);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		MyEnv env = new MyEnv();

		Simbad simbad = new Simbad(env, false);

		env.add(new Box(new Vector3d(3, 0, 0), new Vector3f(1, 1, 1), env));
		// simbad.
		/*
		 * Simbad template = new Simbad("simulator"); template.startService();
		 */

		GUIService gui = new GUIService("gui");
		gui.startService();
		
	}

}
