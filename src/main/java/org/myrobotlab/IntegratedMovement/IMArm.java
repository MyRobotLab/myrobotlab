/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;

/**
 * @author calamity
 *
 */
public class IMArm {
	
	String name;
	public transient LinkedList<IMPart> parts = new LinkedList<IMPart>();
	transient private Matrix inputMatrix = new Matrix(4,4).loadIdentity();
	private ArmConfig armConfig = ArmConfig.DEFAULT;
	private Point target = null;
	private String lastPartToUse;
	private Point previousTarget;
	private int tryCount;
	
	public IMArm(String name){
		this.name = name;
	}

	public void add(IMPart part) {
		parts.add(part);
	}
	
	public String getName() {
		return name;
	}
	
	public Matrix getTransformMatrix() {
		return getTransformMatrix(ArmConfig.DEFAULT, null);
	}
	
	public Matrix getTransformMatrix(ArmConfig armConfig, Matrix m) {
		return getTransformMatrix(armConfig, m, null);
	}

	public Matrix getTransformMatrix(ArmConfig armConfig, Matrix m, String lastPart) {
		Matrix transformMatrix = inputMatrix;
		if (m!=null) transformMatrix = m;
		Iterator<IMPart> it = parts.iterator();
		if (armConfig == ArmConfig.REVERSE) it = parts.descendingIterator();
		while (it.hasNext()){
			IMPart part = it.next();
			transformMatrix = transformMatrix.multiply(part.transform(armConfig));
			if (part.getName() == lastPart) break;
		}
		return transformMatrix;
	}
	
	public Matrix getInputMatrix(){
		return inputMatrix;
	}
	
	public Matrix updatePosition(HashMap<String, IMControl> controls){
		Matrix m = inputMatrix;
		Iterator<IMPart> it = parts.iterator();
		//if (armConfig == ArmConfig.REVERSE) it = parts.descendingIterator();
		while (it.hasNext()){
			IMPart part = it.next();
			part.setOrigin(m);
			DHLink link = part.getDHLink(ArmConfig.DEFAULT);
			if (controls.containsKey(part.getControl(ArmConfig.DEFAULT))){
				link.addPositionValue(controls.get(part.getControl(ArmConfig.DEFAULT)).getPos());
			}
			Matrix s = link.resolveMatrix();
			part.setTheta(link.getTheta());
			part.setInitialTheta(link.getInitialTheta());
			part.setR(link.getA());
			m = m.multiply(s);
			part.setEnd(m);
			
		}
		it = parts.descendingIterator();
		while (it.hasNext()){
			IMPart part = it.next();
			DHLink link = part.getDHLink(ArmConfig.REVERSE);
			if (link == null) continue;
			if (controls.containsKey(part.getControl(ArmConfig.REVERSE))){
				part.addPositionToLink(ArmConfig.REVERSE, -controls.get(part.getControl(ArmConfig.REVERSE)).getPos());
				//link.addPositionValue(-controls.get(part.getControl(ArmConfig.REVERSE)).getPos());
			}
		}
		return m;
	}

	public void setInputMatrix(Matrix m) {
		inputMatrix = m;
	}
	
	public Point currentPosition(){
		return parts.getLast().getEndPoint();
	}

	public ArmConfig getArmConfig() {
		return armConfig;
	}

	public void setArmConfig(ArmConfig armConfig) {
		this.armConfig = armConfig;
	}
	
	public IMPart getLastPart(){
		return parts.getLast();
	}

	public void setTarget(Point point) {
		target  = point;
	}

	public void setLastPartToUse(String partName) {
		lastPartToUse = partName;
	}

	/**
	 * @return the target
	 */
	public Point getTarget() {
		return target;
	}

	/**
	 * @return the lastPartToUse
	 */
	public String getLastPartToUse() {
		return lastPartToUse;
	}

	public void setPreviousTarget(String partName) {
		Iterator<IMPart> it = parts.iterator();
		if (armConfig == ArmConfig.REVERSE) it = parts.descendingIterator();
		while (it.hasNext()){
			IMPart part = it.next();
			previousTarget = part.getEndPoint();
			if (part.getName() == partName) return;
		}
	}

	public void setTryCount(int i) {
		tryCount = i;
	}

	/**
	 * @return the previousTarget
	 */
	public Point getPreviousTarget() {
		return previousTarget;
	}

	/**
	 * @param previousTarget the previousTarget to set
	 */
	public void setPreviousTarget(Point previousTarget) {
		this.previousTarget = previousTarget;
	}

	/**
	 * @return the tryCount
	 */
	public int getTryCount() {
		return tryCount;
	}

	public Point getPosition(String lastPart) {
		return IMUtil.matrixToPoint(getTransformMatrix(armConfig, null, lastPart));
	}

	public void increaseTryCount() {
		tryCount++;
	}

	public LinkedList<IMPart> getParts() {
		return parts;
	}

}
