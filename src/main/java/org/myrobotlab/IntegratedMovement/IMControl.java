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
	private Double speed = 0.1;
	private Double targetPos = 0.0;
	private Double pos = 0.0;
	private ServoStatus state;

	IMControl(String name){
		this.name = name;
	}

	public void setState(ServoStatus state) {
		this.state = state;
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
	public Double getPos(){
		return pos;
	}
}
