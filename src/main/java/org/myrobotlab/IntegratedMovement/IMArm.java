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
	private String linkTo;
	
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
		Matrix transformMatrix = inputMatrix;
		Iterator<IMPart> it = parts.iterator();
		while (it.hasNext()){
			transformMatrix = transformMatrix.multiply((it.next()).transform(name));
		}
		return transformMatrix;
	}
	
	public Matrix getInputMatrix(){
		return inputMatrix;
	}
	
	public Matrix updatePosition(HashMap<String, IMControl> controls){
		Matrix m = inputMatrix;
		Iterator<IMPart> it = parts.iterator();
		while (it.hasNext()){
			IMPart part = it.next();
			part.setOrigin(m);
			DHLink link = part.getDHLink(name);
			if (controls.containsKey(part.getControl(name))){
				link.addPositionValue(controls.get(part.getControl(name)).getPos());
			}
			Matrix s = link.resolveMatrix();
			part.setTheta(link.getTheta());
			part.setInitialTheta(link.getInitialTheta());
			part.setR(link.getA());
			m = m.multiply(s);
			part.setEnd(m);
			
		}
		return m;
	}

	public void setInputMatrix(Matrix m) {
		inputMatrix = m;
	}
	
	public Point currentPosition(){
		return parts.getLast().getEndPoint();
	}

}
