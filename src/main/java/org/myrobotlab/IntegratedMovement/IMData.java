package org.myrobotlab.IntegratedMovement;

import java.util.HashMap;

import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoData;
import org.myrobotlab.service.interfaces.ServoData.ServoStatus;


public class IMData {

	private transient HashMap<String, IMEngine> engines = new HashMap<String, IMEngine>();
	private HashMap<String, IMPart> parts = new HashMap<String, IMPart>();
	private HashMap<String, String> firstParts = new HashMap<String, String>();
	private HashMap<String, IMControl> controls = new HashMap<String, IMControl>();
	
	
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
		// TODO Auto-generated method stub
		return null;
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
		control.setState(ServoStatus.SERVO_START);
		control.setSpeed(data.getSpeed());
		control.setTargetPos(data.getTargetPos());
	}

	public void onServoData(ServoData data) {
		IMControl control = controls.get(data.name);
		control.setState(data.state);
		control.setPos(data.pos);
	}

	
	
}
