package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;


public class IMData {

	private transient HashMap<String, IMEngine> engines = new HashMap<String, IMEngine>();
	private ConcurrentHashMap<String, IMPart> parts = new ConcurrentHashMap<String, IMPart>();
	private HashMap<String, String> firstParts = new HashMap<String, String>();
	private HashMap<String, IMControl> controls = new HashMap<String, IMControl>();
	private HashMap<String, Matrix> inputMatrixs = new HashMap<String, Matrix>();
	private HashMap<String, String> armLinkTos = new HashMap<String, String>();
	private long lastUpdatePositionTimeMs;
	
	
	public void addArm(IMEngine engine) {
		engines.put(engine.getName(), engine);
	}
	
	public IMEngine getArm(String name){
		return engines.get(name);
	}

	public HashMap<String, IMEngine> getArms() {
		return engines;
	}

	public boolean removeArm(String armName) {
		if (engines.containsKey(armName)){
			engines.remove(armName);
			return true;
		}
		return false;
	}

	public IMPart getPart(String partName) {
		if (partName == null) return null;
		return parts.get(partName);
	}

	public void addPart(IMPart part) {
		parts.put(part.getName(),part);
		for (String control : part.getControls().values()){
			if (!controls.containsKey(control)){
				controls.put(control, new IMControl(control));
			}
		}
	}

	public void setFirstPart(String armName, String partName) {
		firstParts.put(armName, partName);
		
	}

	public void onMoveTo(ServoControl data) {
		IMControl control = controls.get(data.getName());
		if (control == null) return;
		control.setState(ServoStatus.SERVO_START);
		control.setSpeed(data.getSpeed());
		control.setTargetPos(data.getTargetPos());
	}

	public void onServoData(ServoData data) {
		IMControl control = controls.get(data.name);
		if (control == null) return;
		control.setState(data.state);
		control.setPos(data.pos);
		updatePartPosition();
	}

	private void updatePartPosition() {
		//update no more than once/10 ms
		//if (lastUpdatePositionTimeMs - System.currentTimeMillis() < 10) return;
		Set<String> done = new HashSet<String>();
		
		for (IMEngine engine : engines.values()){
			checkPreRequireUpdatePosition(engine, done);
			if (!done.contains(engine.getName())) updateArmPosition(engine);
			done.add(engine.getName());
			String pre = armLinkTos.get(engine.getName());
			if (pre != null){
				parts.get(firstParts.get(engine.getName())).setAlpha(engines.get(pre).getNextAlpha());;
			}
		}
	}
	
	private void checkPreRequireUpdatePosition(IMEngine engine, Set<String> done){
		if (armLinkTos.containsKey(engine.getName()) && !done.contains(engine.getName())){
			String pre = armLinkTos.get(engine.getName());
			checkPreRequireUpdatePosition(getArm(armLinkTos.get(engine.getName())),done);
			inputMatrixs.put(engine.getName(),updateArmPosition(getArm(pre)));
			done.add(pre);
		}
		
	}

	private Matrix updateArmPosition(IMEngine engine) {
		Matrix armMatrix = inputMatrixs.getOrDefault(engine.getName(), new Matrix(4,4).loadIdentity());
		IMPart part = getPart(firstParts.get(engine.getName()));
		double nextAlpha =0;
		while (part != null){
			part.setOrigin(armMatrix);
			DHLink link = part.getDHLink(engine.getName());
			if (controls.containsKey(part.getName())){
				link.addPositionValue(getControl(part.getControl(engine.getName())).getPos());
			}
			Matrix s = link.resolveMatrix();
			part.setTheta(link.getTheta());
			part.setInitialTheta(link.getInitialTheta());
			part.setAlpha(nextAlpha);
			part.setR(link.getA());
			nextAlpha = link.getAlpha();
			Matrix armMatrix1 = armMatrix.multiply(s);
			part.setInternTransform(s);
			s = link.resolveMatrixZeroAlpha();
			Matrix armMatrix2 = armMatrix.multiply(s);
			part.setEnd(armMatrix1);
			armMatrix = armMatrix1;
			String nextPartName = part.getNextLink(engine.getName());
			parts.put(part.getName(), part);
			part = getPart(nextPartName);
		}
		engine.setNextAlpha(nextAlpha);
		return armMatrix;
	}

	public String getFirstPart(String armName) {
		return firstParts.get(armName);
	}

	public IMControl getControl(String controlName) {
		return controls.get(controlName);
	}

	public ConcurrentHashMap<String,IMPart> getParts() {
		return parts;
	}

	public void addInputMatrix(String armName, Matrix inputMatrix) {
		inputMatrixs.put(armName, inputMatrix);
	}

	public void linkArmTo(String armName, String linkTo) {
		armLinkTos.put(armName, linkTo);
	}

	public Matrix getInputMatrix(String arm) {
		return inputMatrixs.getOrDefault(arm, Util.getIdentityMatrix());
	}

	
	
}
