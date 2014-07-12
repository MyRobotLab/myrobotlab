package org.myrobotlab.inmoov;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;

public class Arm {
	private String side;
	// public Hand hand;
	public Servo bicep;
	public Servo rotate;
	public Servo shoulder;
	public Servo omoplate;
	// ------------- added  pins and defaults
	public int bicepPin=8;
	public int rotatePin=9;
	public int shoulderPin=10;
	public int omoplatePin=11;

	public Arm() {
	}
	
	
	public void rest() {
		
		setSpeed(1.0f,1.0f,1.0f,1.0f);

		// initial position
		bicep.moveTo(0);
		rotate.moveTo(90);
		shoulder.moveTo(30);
		omoplate.moveTo(10);
	}
	// ------------- added set pins
	public void setpins(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		 bicepPin=bicep;
		 rotatePin=rotate;
		 shoulderPin=shoulder;
		 omoplatePin=omoplate;
		
	}
	public void attach(Arduino arduino, String key) {
		// name = String.format("%sArm", key);
		side = key;
		bicep = (Servo) Runtime.createAndStart(String.format("bicep%s", key), "Servo");
		rotate = (Servo) Runtime.createAndStart(String.format("rotate%s", key), "Servo");
		shoulder = (Servo) Runtime.createAndStart(String.format("shoulder%s", key), "Servo");
		omoplate = (Servo) Runtime.createAndStart(String.format("omoplate%s", key), "Servo");

		// attach to controller
		// ------------- changed to use set pins
		arduino.servoAttach(bicep.getName(), bicepPin);
		arduino.servoAttach(rotate.getName(), rotatePin);
		arduino.servoAttach(shoulder.getName(), shoulderPin);
		arduino.servoAttach(omoplate.getName(), omoplatePin);

		// servo limits
		bicep.setMinMax(0,90);
		omoplate.setMinMax(10,80);
		rotate.setMinMax(40,180);

		rest();

		broadcastState();

	}

	public void broadcastState() {
		// notify the gui
		bicep.broadcastState();
		rotate.broadcastState();
		shoulder.broadcastState();
		omoplate.broadcastState();
	}

	public void detach() {
		if (bicep != null) {
			bicep.detach();
		}
		if (rotate != null) {
			rotate.detach();
		}
		if (shoulder != null) {
			shoulder.detach();
		}
		if (omoplate != null) {
			omoplate.detach();
		}
	}

	public void release() {
		detach();
		if (bicep != null) {
			bicep.releaseService();
			bicep = null;
		}
		if (rotate != null) {
			rotate.releaseService();
			rotate = null;
		}
		if (shoulder != null) {
			shoulder.releaseService();
			shoulder = null;
		}
		if (omoplate != null) {
			omoplate.releaseService();
			omoplate = null;
		}

	}

	public void setSpeed(Float bicep, Float rotate, Float shoulder, Float omoplate) {
		this.bicep.setSpeed(bicep);
		this.rotate.setSpeed(rotate);
		this.shoulder.setSpeed(shoulder);
		this.omoplate.setSpeed(omoplate);
	}

	public String getScript(String inMoovServiceName) {
		return String
				.format("%s.moveArm(\"%s\",%d,%d,%d,%d)\n", inMoovServiceName, side, bicep.getPosFloat(), rotate.getPosFloat(), shoulder.getPosFloat(), omoplate.getPosFloat());
	}

	public void moveTo(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		this.bicep.moveTo(bicep);
		this.rotate.moveTo(rotate);
		this.shoulder.moveTo(shoulder);
		this.omoplate.moveTo(omoplate);

	}
	
	public boolean isValid()
	{
		bicep.moveTo(2);
		rotate.moveTo(92);
		shoulder.moveTo(32);
		omoplate.moveTo(12);	
		return true;
	}
}
