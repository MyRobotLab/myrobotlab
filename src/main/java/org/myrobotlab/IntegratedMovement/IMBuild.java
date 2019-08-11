/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.service.IntegratedMovement;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

/**
 * @author Calamity
 *
 */
public class IMBuild extends Thread  {
	
	transient private IntegratedMovement service;
	transient Node<IMArm> arms = new Node<IMArm>(new IMArm("root"));
	protected Queue<IMMsg> msgQueue = new ConcurrentLinkedQueue<IMMsg>();
	private IMArm reversedArm = null;
	private HashMap<String, IMControl> controls;
	private double maxDistance = 0.001;
	
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

			copyControl();
			updatePartsPosition();
			
			checkMsg();
			
			checkMove(arms);
			
			publishAngles(arms);
			
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
	
	
	private void checkMove(Node<IMArm> arm) {
		IMArm a = arm.getData();
		if (a.getTarget() != null && a.getTarget().distanceTo(a.getPosition(a.getLastPartToUse())) > maxDistance){
	        service.info("distance to target {}", a.getPosition(a.getLastPartToUse()).distanceTo(a.getTarget()));
	        service.info(a.getPosition(a.getLastPartToUse()).toString());
	        move(arm);
	        //cogRetry = 0;
		}
		else if (a.getTarget() != null){
			a.setTarget(null);
			a.setLastPartToUse(null);
		}
		for (Node<IMArm> child : arm.getChildren()){
			checkMove(child);
		}
	}

	private void move(Node<IMArm> arm) {
		IMArm a = arm.getData();
		a.increaseTryCount();
		if (a.getTryCount() > 500 && a.getPreviousTarget() != null){
			a.setTarget(a.getPreviousTarget());
		}
		moveToGoal(arm);
	}

	private void publishAngles(Node<IMArm> arm) {
		IMArm a = arm.getData();
		LinkedList<IMPart> parts = a.getParts();
		Iterator<IMPart> it = parts.iterator();
		while (it.hasNext()){
			IMPart part = it.next();
			if (part.getState() != ServoStatus.SERVO_POSITION_UPDATE) continue;
			String control = part.getControl(a.getArmConfig());
			DHLink link = part.getDHLink(a.getArmConfig());
			if (control == null || link == null) continue;
			service.sendAngles(control, link.getPositionValueDeg());
			part.setState(ServoStatus.SERVO_STOPPED);
		}
		for (Node<IMArm> child : arm.getChildren()){
			publishAngles(child);
		}
		
	}

	private boolean moveToGoal(Node<IMArm> arm) {
		// TODO Auto-generated method stub
		return false;
	}

	private void copyControl() {
		controls = new HashMap<String, IMControl>();
		for (IMControl c: service.getData().getControls().values()){
			controls.put(c.getName(), new IMControl(c));
		}
	}

	private void checkMsg() {
	    while (msgQueue.size() > 0) {
	        IMMsg msg = null;
	        try {
	          msg = msgQueue.remove();
	          invoke(msg);
	        } catch (Exception e) {
	          service.error("checkMsg failed for {} - targetName", msg, e);
	        }
	      }
	}

	public Object invoke(IMMsg msg) {
		return service.invokeOn(this, msg.method, msg.data);
	}

	public void reverseArm(String armName){
		IMArm arm = service.getData().getArm(armName);
		if (arm.getArmConfig() == ArmConfig.REVERSE){
			Node<IMArm> armNode = arms.find(arm);
			while (armNode.getParent() != null){
				arm.setArmConfig(ArmConfig.DEFAULT);
				armNode = armNode.getParent();
			}
			reversedArm  = null;
		}
		else {
			Node<IMArm> armNode = arms.find(arm);
			reversedArm = arm;
			while (armNode.getParent() != null){
				arm.setArmConfig(ArmConfig.REVERSE);
				Matrix im = arm.getLastPart().getEnd();
				((IMArm)(armNode.getParent().getData())).setInputMatrix(arm.getTransformMatrix(ArmConfig.REVERSE, im));
				armNode = armNode.getParent();
			}
		}
	}

	private void updatePartsPosition() {
		updateReverseArm();
		updatePartsPosition(arms);
	}
	
	private void updateReverseArm() {
		if (reversedArm != null){
			Node<IMArm> arm = arms.find(reversedArm);
			while (arm.getParent() != null) {
				Matrix im = arm.getData().getLastPart().getEnd();
				arm.getData().updatePosition(service.getData().getControls());
				((IMArm)(arm.getParent().getData())).setInputMatrix(arm.getData().getTransformMatrix(ArmConfig.REVERSE, im));
				arm = arm.getParent();
			}
		}
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
	
	public void addMsg(String method, Object... params) {
		msgQueue.add(new IMMsg(method, params));
	}
	
	public void moveTo(String armName, String partName, Point point){
		IMArm arm = service.getArm(armName);
		arm.setTarget(point);
		arm.setLastPartToUse(partName);
		arm.setPreviousTarget(partName);
		arm.setTryCount(0);
	}
}
