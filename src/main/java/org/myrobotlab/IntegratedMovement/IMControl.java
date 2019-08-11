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
	private Double min;
	private Double max;

	IMControl(String name){
		this.setName(name);
	}
	
	IMControl(IMControl copy){
		name = copy.name;
		speed = new Double(copy.speed);
		targetPos = new Double(copy.targetPos);
		pos = new Double(copy.pos);
		state = copy.state;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the speed
	 */
	public Double getSpeed() {
		return speed;
	}

	/**
	 * @return the targetPos
	 */
	public Double getTargetPos() {
		return targetPos;
	}

	/**
	 * @return the state
	 */
	public ServoStatus getState() {
		return state;
	}

	public void setMinMax(Double min, Double max) {
		this.setMin(min);
		this.setMax(max);
	}

	/**
	 * @return the min
	 */
	public Double getMin() {
		return min;
	}

	/**
	 * @param min the min to set
	 */
	public void setMin(Double min) {
		this.min = min;
	}

	/**
	 * @return the max
	 */
	public Double getMax() {
		return max;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(Double max) {
		this.max = max;
	}
}
