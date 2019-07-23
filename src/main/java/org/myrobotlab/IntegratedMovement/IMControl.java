/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import org.myrobotlab.service.interfaces.ServoData.ServoStatus;

/**
 * data about integrated movement control
 * 
 * @author calamity
 *
 */
public class IMControl {
	private String name;
	private Double speed;
	private Double targetPos;
	private Double pos;

	IMControl(String name){
		this.name = name;
	}

	public void setState(ServoStatus state) {
		this.setState(state);
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public void setTargetPos(Double targetPos) {
		this.targetPos = targetPos;
	}

	public void setPos(Double pos) {
		this.pos = pos; 
	}
}
