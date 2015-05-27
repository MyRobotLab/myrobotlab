package org.myrobotlab.kinematics;

import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.slf4j.Logger;

public class DHRobotArm {
	
	transient public final static Logger log = LoggerFactory.getLogger(DHRobotArm.class);


	private ArrayList<DHLink> links;

	public DHRobotArm() {
		super();
		links = new ArrayList<DHLink>();
	}

	public ArrayList<DHLink> addLink(DHLink link) {
		links.add(link);
		return links;
	}

	public Matrix getJInverse() {
		// TODO Auto-generated method stub
		// something small.
		double delta = 0.000001;

		int numLinks = this.getNumLinks();

		// we need a jacobian matrix that is 6 x numLinks
		// for now we'll only deal with x,y,z we can add rotation later.
		// so only 3
		Matrix jacobian = new Matrix(3, numLinks);

		// this will be used to compute the gradient of x,y,z based on the joint
		// movement.
		Point basePosition = this.getPalmPosition();
		// log.debug("Base Position : " + basePosition);
		// for each servo, we'll rotate it forward by delta (and back), and get
		// the new positions
		for (int j = 0; j < numLinks; j++) {
			this.getLink(j).incrRotate(delta);
			Point palmPoint = this.getPalmPosition();
			Point deltaPoint = palmPoint.subtract(basePosition);
			this.getLink(j).incrRotate(-delta);
			// delta position / base position gives us the slope / rate of
			// change
			// this is an approx of the gradient of P
			// UHoh,, what about divide by zero?!
			// log.debug("Delta Point" + deltaPoint);
			double dXdj = deltaPoint.getX() / delta;
			double dYdj = deltaPoint.getY() / delta;
			double dZdj = deltaPoint.getZ() / delta;
			jacobian.elements[0][j] = dXdj;
			jacobian.elements[1][j] = dYdj;
			jacobian.elements[2][j] = dZdj;
			// TODO: get orientation roll/pitch/yaw
		}

		// log.debug("Jacobian(p)approx");
		// log.debug(jacobian);

		// This is the MAGIC! the pseudo inverse should map
		// deltaTheta[i] to delta[x,y,z]
		Matrix jInverse = jacobian.pseudoInverse();
		// log.debug("Pseudo inverse Jacobian(p)approx\n" + jInverse);
		return jInverse;
	}

	public DHLink getLink(int i) {
		if (links.size() >= i) {
			return links.get(i);
		} else {
			// TODO log a warning or something?
			return null;
		}
	}

	public ArrayList<DHLink> getLinks() {
		return links;
	}

	public int getNumLinks() {
		return links.size();
	}

	public Point getPalmPosition() {
		// TODO Auto-generated method stub
		// return the position of the end effector wrt the base frame
		Matrix m = new Matrix(4, 4);
		// TODO: init to the ident?

		// initial frame orientated around x
		m.elements[0][0] = 1;
		m.elements[1][1] = 1;
		m.elements[2][2] = 1;
		m.elements[3][3] = 1;

		// initial frame orientated around z
		// m.elements[0][2] = 1;
		// m.elements[1][1] = 1;
		// m.elements[2][0] = 1;
		// m.elements[3][3] = 1;

		// log.debug("-------------------------");
		// log.debug(m);
		// TODO: validate this approach..
		for (DHLink link : links) {
			Matrix s = link.resolveMatrix();
			// log.debug(s);
			m = m.multiply(s);
			// log.debug("-------------------------");
			// log.debug(m);
		}
		// now m should be the total translation for the arm
		// given the arms current position
		double x = m.elements[0][3];
		double y = m.elements[1][3];
		double z = m.elements[2][3];
		// double ws = m.elements[3][3];
		// log.debug("World Scale : " + ws);
		Point palm = new Point(x, y, z);
		return palm;
	}

	void moveToGoal(Point goal) {
		// we know where we are.. we know where we want to go.
		int numSteps = 0;
		double iterStep = 0.01;
		double errorThreshold = 0.01;
		// what's the current point
		while (true) {
			numSteps++;
			// TODO: what if its unreachable!
			Point currentPos = this.getPalmPosition();
			log.info("Current Position " + currentPos);

			System.out.println("Current Position " + currentPos);
			// vector to destination
			Point deltaPoint = goal.subtract(currentPos);
			Matrix dP = new Matrix(3, 1);
			dP.elements[0][0] = deltaPoint.getX();
			dP.elements[1][0] = deltaPoint.getY();
			dP.elements[2][0] = deltaPoint.getZ();
			// scale a vector towards the goal by the increment step.
			dP = dP.multiply(iterStep);

			Matrix jInverse = this.getJInverse();
			// why is this zero?
			Matrix dTheta = jInverse.multiply(dP);
			System.out.println("delta Theta + " + dTheta);
			for (int i = 0; i < dTheta.getNumRows(); i++) {
				// update joint positions! move towards the goal!
				double d = dTheta.elements[i][0];
				this.getLink(i).incrRotate(d);
			}
			// delta point represents the direction we need to move in order to
			// get there.
			// we should figure out how to scale the steps.
			if (deltaPoint.magnitude() < errorThreshold) {
				log.debug("We made it!  It took " + numSteps + " iterations to get there.");
				break;
			}
		}

	}

	public void setLinks(ArrayList<DHLink> links) {
		this.links = links;
	}

}
