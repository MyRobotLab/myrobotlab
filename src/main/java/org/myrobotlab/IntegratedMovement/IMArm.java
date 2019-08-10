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
		Matrix transformMatrix = inputMatrix;
		if (m!=null) transformMatrix = m;
		Iterator<IMPart> it = parts.iterator();
		if (armConfig == ArmConfig.REVERSE) it = parts.descendingIterator();
		while (it.hasNext()){
			transformMatrix = transformMatrix.multiply((it.next()).transform(armConfig));
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
				link.addPositionValue(-controls.get(part.getControl(ArmConfig.REVERSE)).getPos());
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

}
