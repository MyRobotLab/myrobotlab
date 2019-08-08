/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.IntegratedMovement;

/**
 * @author Calamity
 *
 */
public class IMBuild extends Thread  {
	
	transient private IntegratedMovement service;
	transient Node<IMArm> arms = new Node<IMArm>(new IMArm("root"));

	public IMBuild(String name, IntegratedMovement service, Matrix origin){
		super(name);
		this.service = service;
		arms.getData().setInputMatrix(origin);
	}

	public void addArm(IMArm arm) {
		addArm(arm, null);
	}
	
	public void addArm(IMArm arm, IMArm parent){
		Node<IMArm> parentNode = arms.find(parent);
		if (parentNode == null) parentNode = arms;
		parentNode.addchild(new Node<IMArm>(arm));
	}

	public void run(){
		//service.error("test");
		while(true){
			long startUpdateTs = System.currentTimeMillis();

			updatePartsPosition();
			
			long deltaMs = System.currentTimeMillis() - startUpdateTs;
			long sleepMs = 33 - deltaMs;

			if (sleepMs < 0) {
				sleepMs = 0;
			}
			try {
				sleep(sleepMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
		}
	}

	private void updatePartsPosition() {
		HashMap<String, IMControl> controls = service.getData().getControls();
		Matrix m = arms.getData().getInputMatrix();
		updatePartsPosition(arms);
	}
	
	private void updatePartsPosition(Node<IMArm> armNode){
		Matrix m = armNode.getData().updatePosition(service.getData().getControls());
		for (Node<IMArm> arm : armNode.getChildren()){
			arm.getData().setInputMatrix(m);
			updatePartsPosition(arm);
		}
	}
	
	public Point currentPosition(String arm){
		Node<IMArm> node = arms.find(service.getData().getArm(arm));
		if (node == null){
			service.error("Couldn't find arm {}", arm);
			return new Point(0,0,0,0,0,0);
		}
		Matrix m = currentPosition(node);
		return IMUtil.matrixToPoint(m);
	}
	
	private Matrix currentPosition(Node<IMArm> arm){
		Node<IMArm> parent = arm.getParent();
		Matrix retVal = new Matrix(4,4).loadIdentity();
		if (parent != null) {
			retVal = currentPosition(parent);
		}
		retVal = retVal.multiply(arm.getData().getTransformMatrix());
		return retVal;
	}
}
