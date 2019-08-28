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
public class IMBuild extends Thread {
	
	transient private IntegratedMovement service = null;
	transient Node<IMArm> arms = new Node<IMArm>("root", new IMArm("root"));
	transient protected Queue<IMMsg> msgQueue = new ConcurrentLinkedQueue<IMMsg>();
	transient private IMArm reversedArm = null;
	transient private HashMap<String, IMControl> controls = new HashMap<String, IMControl>();
	private double maxDistance = 0.005;
	transient private Matrix currentOrigin = new Matrix(4,4).loadIdentity();
	private long startUpdateTs;
	private int maxTryCount = 500;
	private long loopTime = 25;
	transient private GravityCenter gravityCenter = new GravityCenter();
	
	private enum MoveType {
		NO_MOVE, POSITION, COG;
	}
	  
	public IMBuild(String name, IntegratedMovement service, Matrix origin){
		super(name);
		this.service = service;
		arms.getData().setInputMatrix(origin);
	}

	public void addArm(IMArm arm){
		addArm(arm, null, ArmConfig.DEFAULT);
	}
	
	public void addArm(IMArm arm, ArmConfig armConfig){
		addArm(arm, null, armConfig);
	}
	
	public void addArm(IMArm arm, IMArm parent) {
		addArm(arm, parent, ArmConfig.DEFAULT);
	}
	
	public void addArm(IMArm arm, IMArm parent, ArmConfig armConfig){
		Node<IMArm> parentNode = arms.find(parent);
		Node<IMArm> newArm = new Node<IMArm>(arm.getName(), arm);
		if (parentNode == null){
			parentNode = arms;
			arm.setInputMatrix(parentNode.getData().getInputMatrix());
		}
		else {
			if (parentNode.getData().getArmConfig() == ArmConfig.REVERSE){
				arm.setInputMatrix(parentNode.getData().getInputMatrix());
			}
			else{
				arm.setInputMatrix(parentNode.getData().getLastPart().getEnd());
			}
		}
		parentNode.addchild(newArm);
		arm.setArmConfig(armConfig);
		if (armConfig == ArmConfig.REVERSE){
			reversedArm = arm;
			Iterator<IMPart> it = arm.getParts().iterator();
			while (it.hasNext()){
				(it.next()).setCurrentArmConfig(armConfig);
			}
		}
		gravityCenter.updateTotalMass(newArm);
		updatePartsPosition(newArm);
		
	}

	public void run(){
		while(true){
			update();
			copyControl();
			checkMsg();
			checkMove(arms);
			while (moveToGoal(arms)){
				if (breakLoopTime()) break;
			}
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
	
	
	private boolean breakLoopTime() {
		long deltaMs = System.currentTimeMillis() - startUpdateTs;
		if (deltaMs > loopTime ) return true;
		return false;
	}

	private void checkMove(Node<IMArm> arm) {
		IMArm a = arm.getData();
		if (a.getTarget() != null && a.getTarget().distanceTo(a.getPosition(a.getLastPartToUse())) > maxDistance ){
			//if (a.armReady()){
		        IntegratedMovement.log.info("distance to target {}", a.getPosition(a.getLastPartToUse()).distanceTo(a.getTarget()));
		        IntegratedMovement.log.info(a.getPosition(a.getLastPartToUse()).toString());
		        move(arm);
	        //cogRetry = 0;
			//}
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
		if (a.getTryCount() > maxTryCount  && a.getPreviousTarget() != null){
			a.setTarget(a.getPreviousTarget());
			if (a.getPreviousTarget() == null) return;
		}
		else if (a.getTryCount() > maxTryCount){
			a.setTarget(null);
			a.resetWaitTime();
			return;
		}
	}

	private void publishAngles(Node<IMArm> arm) {
		LinkedList<IMPart> link1 = arm.getData().getParts();
		if (link1 != null){
			Iterator<IMPart> it = link1.iterator();//arm.getData().getParts().iterator();
			while(it.hasNext()){
//		    	long deltaMs = System.currentTimeMillis() - startUpdateTs;
//		    	if (deltaMs > 30){
//		    		//update();
//		    	}
				IMPart part = it.next();
				if (part.getState() != ServoStatus.SERVO_POSITION_UPDATE) continue;
				String control = part.getControl();
				DHLink link = part.getDHLink();
				if (control == null || link == null) continue;
				service.sendAngles(control, link.getPositionValueDeg());
				part.setState(ServoStatus.SERVO_STOPPED);
				IMControl contrl = controls.get(control);
				long delta = (long)Math.abs(contrl.getPos() - link.getPositionValueDeg());
				arm.getData().waitTime((delta/contrl.getSpeed().longValue())+System.currentTimeMillis());
			}
		}
		for (Node<IMArm> node : arm.getChildren()){
			publishAngles(node);
		}
	}
	
	private boolean moveToGoal(Node<IMArm> arm){
        if (breakLoopTime()) return false;
		double iterStep = 0.01;
	    boolean retval = false;
	    MoveType moveType = MoveType.POSITION;
	    currentOrigin = arms.getData().getInputMatrix();
		Point target = arm.getData().getTarget();
		if (target == null){
			if (arm.getChildren().size() > 0) moveType = MoveType.NO_MOVE; //nothing to do
			else {
				moveType = MoveType.COG;
				target = gravityCenter.getCoGTarget();
			}
		}
	    if (moveType != MoveType.NO_MOVE){
    		LinkedList<IMPart> l = new LinkedList<IMPart>();
    		Node<IMArm> parent = arm;
    		while (parent.getData().getName() != "root"){
    			LinkedList<IMPart> lt = new LinkedList<IMPart>();
    			if (parent.getData().getArmConfig() == ArmConfig.REVERSE){
    				lt = parent.getData().getParts();
    				currentOrigin = lt.getLast().getEnd();
    			}
    			else{
    				Iterator<IMPart> it = parent.getData().getParts().iterator();
    				while (it.hasNext()){
    					lt.addFirst(it.next());
    				}
    				//currentOrigin = lt.getLast().getOrigin();
    			}
    			Iterator<IMPart> it = lt.iterator();
    			while (it.hasNext()){
    				l.addFirst(it.next());
    			}
    			parent = parent.getParent();
    		}
	        // vector to destination
    		Point deltaPoint = getDeltaPoint(target, l, currentOrigin, moveType);
	        Matrix dP = new Matrix(3, 1);
	        dP.elements[0][0] = deltaPoint.getX();
	        dP.elements[1][0] = deltaPoint.getY();
	        dP.elements[2][0] = deltaPoint.getZ();
	        // scale a vector towards the goal by the increment step.
	        dP = dP.multiply(iterStep);
	        Matrix dTheta = null;
	        Matrix jInverse = getJInverse(l, moveType);
	        dTheta = jInverse.multiply(dP);
	        if (dTheta == null) {
	            dTheta = new Matrix(l.size(), 1);
	            for (int i = 0; i < l.size(); i++) {
	            	dTheta.elements[i][0] = 0.000001;
	            }
	        }
	        for (int i = 0; i < dTheta.getNumRows(); i++) {
	            DHLink link = l.get(i).getDHLink();
	            if (l.get(i).getControl() != null) {
	            	if (controls.get(l.get(i).getControl()).getState() == ServoStatus.SERVO_STOPPED){
	            		// update joint positions! move towards the goal!
	            		double d = dTheta.elements[i][0];
	            		l.get(i).incrRotate(d);
	            		if (d != 0.0) l.get(i).setState(ServoStatus.SERVO_POSITION_UPDATE);
	            	}
	            	else {
	            		// servo already moving to a position, let it go to it's target
	            		link.addPositionValue(controls.get(l.get(i).getControl()).getTargetPos());
	            		l.get(i).setState(ServoStatus.SERVO_POSITION_UPDATE);
	            	}
	            }
	            if (l.get(i).getName().equals(arm.getData().getLastPartToUse())) {
	            	break;
	            }
	        }
	        retval = true;
	    }
	    for (Node<IMArm> child : arm.getChildren()){
	    	retval |= moveToGoal(child);
	    }
		return retval;
	}

	private Point getDeltaPoint(Point target, LinkedList<IMPart> l, Matrix inputMatrix, MoveType moveType) {
		if (moveType == MoveType.POSITION) return target.subtract(resolveMatrix(l, inputMatrix));
		else if (moveType == MoveType.COG) return target.subtract(gravityCenter.computeCoG(arms));
		return null;
	}

	private Point resolveMatrix(LinkedList<IMPart> l, Matrix inputMatrix) {
		Matrix m = inputMatrix;
		Iterator<IMPart> it = l.iterator();
		while (it.hasNext()){
			IMPart part = it.next();
			m = m.multiply(part.transform(part.getCurrentArmConfig()));
		}
		return IMUtil.matrixToPoint(m);
	}

	private void update() {
		startUpdateTs = System.currentTimeMillis();

		updatePartsPosition();
	}

	private Matrix getJInverse(LinkedList<IMPart> parts, MoveType moveType){
		double delta = 0.1;
	    // we need a jacobian matrix that is 6 x numLinks
	    // for now we'll only deal with x,y,z we can add rotation later. so only 3
	    // We can add rotation information into slots 4,5,6 when we add it to the
	    // algorithm.
	    Matrix jacobian = new Matrix(3, parts.size());
	    // compute the gradient of x,y,z based on the joint movement.
	    Point basePosition;
	    if (moveType == MoveType.POSITION){
	    	basePosition = resolveMatrix(parts, currentOrigin);
	    }
	    else if (moveType == MoveType.COG){
	    	basePosition = gravityCenter.computeCoG(arms);
	    	basePosition.setZ(0.0);
	    }
	    else basePosition = new Point (0,0,0,0,0,0);
	    // for each servo, we'll rotate it forward by delta (and back), and get
	    // the new positions
	    Iterator<IMPart> it = parts.iterator();
	    int j = 0;
	    while (it.hasNext()){
	    	IMPart part = it.next();
	    	if (part.getControl() == null){
	    		//that link is not moving
	    		j++;
	    		continue;
	    	}
	    	part.incrRotate(delta);
	    	Point curPos;
		    if (moveType == MoveType.POSITION){
		    	curPos = resolveMatrix(parts, currentOrigin);
		    }
		    else if (moveType == MoveType.COG){
		    	curPos = gravityCenter.computeCoG(arms);
		    	curPos.setZ(0.0);
		    }
		    else curPos = new Point (0,0,0,0,0,0);
	    	Point deltaPoint = curPos.subtract(basePosition);
	    	part.incrRotate(-delta);
	        // delta position / base position gives us the slope / rate of
	        // change
	        // this is an approx of the gradient of P
	        double dXdj = deltaPoint.getX() / delta;
	        double dYdj = deltaPoint.getY() / delta;
	        double dZdj = deltaPoint.getZ() / delta;
	        jacobian.elements[0][j] = dXdj;
	        jacobian.elements[1][j] = dYdj;
	        jacobian.elements[2][j] = dZdj;
	        // TODO: get orientation roll/pitch/yaw
	        j++;
	    }
	    // This is the MAGIC! the pseudo inverse should map
	    // deltaTheta[i] to delta[x,y,z]
	    Matrix jInverse = jacobian.pseudoInverse();
	    if (jInverse == null) {
	        jInverse = new Matrix(3, parts.size());
	      }
	      return jInverse;
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
				Matrix im = new Matrix(arm.getData().getLastPart().getEnd());
 				arm.getData().updatePosition(service.getData().getControls());
 				Matrix inputMatrix = arm.getData().getTransformMatrix(ArmConfig.REVERSE, im);
				((IMArm)(arm.getParent().getData())).setInputMatrix(inputMatrix);
				gravityCenter.setCoGTarget(IMUtil.matrixToPoint(im));
				arm = arm.getParent();
			}
		}
		else gravityCenter.setCoGTarget(IMUtil.matrixToPoint(arms.getData().getInputMatrix()));
	}

	private void updatePartsPosition(Node<IMArm> armNode){
		Matrix m = armNode.getData().updatePosition(service.getData().getControls());
		for (Node<IMArm> arm : armNode.getChildren()){
			if (armNode.getData().getArmConfig() == ArmConfig.REVERSE){
				arm.getData().setInputMatrix(armNode.getData().getInputMatrix());
			}
			else{
				arm.getData().setInputMatrix(m);
			}
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
		//arm.setPreviousTarget(partName);
		arm.setTryCount(0);
	}


	public IMArm getRoot() {
		return arms.getData();
	}

	public void setInputMatrix(Matrix inputMatrix) {
		arms.getData().setInputMatrix(inputMatrix);
		gravityCenter.setCoGTarget(inputMatrix.elements[0][3], inputMatrix.elements[1][3], inputMatrix.elements[2][3]);
	}
}
