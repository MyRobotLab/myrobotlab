package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;

import org.myrobotlab.kinematics.DHLink;
import org.myrobotlab.kinematics.Matrix;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

import com.jme3.scene.Spatial.CullHint;


public class IMData {

	private transient HashMap<String, IMEngine> engines = new HashMap<String, IMEngine>();
	private HashMap<String, IMPart> parts = new HashMap<String, IMPart>();
	private HashMap<String, String> firstParts = new HashMap<String, String>();
	private HashMap<String, IMControl> controls = new HashMap<String, IMControl>();
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
		for (IMEngine engine : engines.values()){
			Matrix armMatrix = engine.getInputMatrix();
			IMPart part = getPart(firstParts.get(engine.getName()));
			while (part != null){
				part.setOrigin(armMatrix);
				DHLink link = part.getDHLink(engine.getName());
				Matrix s = link.resolveMatrix();
				armMatrix = armMatrix.multiply(s);
				part.setEnd(armMatrix);
				String nextPartName = part.getNextLink(engine.getName());
				part = getPart(nextPartName);
			}
		}
	}

	public String getFirstPart(String armName) {
		return firstParts.get(armName);
	}

	public IMControl getControl(String controlName) {
		return controls.get(controlName);
	}

	public HashMap<String,IMPart> getParts() {
		return parts;
	}

	
	
}
