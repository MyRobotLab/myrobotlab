package org.myrobotlab.IntegratedMovement;

import java.util.Iterator;

import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;

/**
 * This class will compute the center of gravity of the links composing a robot
 * 
 * @author calamity
 *
 */
public class GravityCenter {

	private Point cogTarget = new Point(0, 0, 0, 0, 0, 0);
	private double totalMass = 0.0;

	public GravityCenter() {
	}

	/**
	 * Set the mass and center of mass of a link
	 * 
	 * @param name
	 *          name
	 * @param mass
	 *          mass
	 * @param centerOfMass
	 *          (0.0 - 1.0) representing where the center of mass is located, from
	 *          the origin point. If you don't know, it's safe to put 0.5
	 */

	private Point computeCoG(Node<IMArm> arm, Point cog, Matrix m) {
		Iterator<IMPart> it = arm.getData().getParts().iterator();
		if (arm.getData().getArmConfig() == ArmConfig.REVERSE){
			it = arm.getData().getParts().descendingIterator();
			m = arm.getData().getLastPart().getEnd();
		}
		while (it.hasNext()){
			IMPart part = it.next();
			Point o = IMUtil.matrixToPoint(m);
			m = m.multiply(part.transform(part.getCurrentArmConfig()));
			Point e = IMUtil.matrixToPoint(m);
			Point icog = e.subtract(o).unitVector(1).multiplyXYZ(part.getCenterOfMass()).multiplyXYZ(part.getLength()).add(o);
			double mass = part.getMass() / totalMass;
			Point ic = icog.multiplyXYZ(mass);
			cog = cog.add(ic);
		}
		Matrix endPoint = m;
		for (Node<IMArm> child : arm.getChildren()){
			cog = computeCoG(child, cog, m);
			m = endPoint;
		}
		return cog;
	}

	public Point getCoG() {
		return null;
	}

	public Point getCoGTarget() {
		return cogTarget;
	}

	public void setCoGTarget(double x, double y, double z) {
		cogTarget = new Point(x, y, z, 0, 0, 0);
	}

	public void setCoGTarget(Point target) {
		cogTarget = target;
	}

	public Point computeCoG(Node<IMArm> arm) {
		Point cog = IMUtil.matrixToPoint(arm.getData().getInputMatrix());
		Matrix m = arm.getData().getInputMatrix();
		cog = computeCoG(arm, cog, m);
		return cog;
	}

	
	public void updateTotalMass(Node<IMArm> arm){
		if (arm.getParent() == null) totalMass = 0.0;
		for (IMPart part : arm.getData().getParts()){
			totalMass += part.getMass();
		}
		for (Node<IMArm> child : arm.getChildren()){
			updateTotalMass(child);
		}
	}


}
