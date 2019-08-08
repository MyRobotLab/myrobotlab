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
	private HashMap<String, IMArm> arms = new HashMap<String, IMArm>();
	private HashMap<String, IMBuild> builds = new HashMap<String, IMBuild>();
	private long lastUpdatePositionTimeMs;
	
	
	public IMArm getArm(String name){
		return arms.get(name);
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
		return inputMatrixs.getOrDefault(arm, IMUtil.getIdentityMatrix());
	}

	public void addArm(IMArm arm) {
		arms.put(arm.getName(), arm);
	}

	public void addBuild(IMBuild build) {
		builds.put(build.getName(), build);
	}

	public HashMap<String, IMControl> getControls(){
		return controls;
	}

	public IMBuild getBuild(String buildName) {
		return builds.get(buildName);
	}

	public void addControl(String srvCtrlName) {
		controls.put(srvCtrlName, new IMControl(srvCtrlName));
	}
	
}
