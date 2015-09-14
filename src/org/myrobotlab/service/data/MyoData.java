package org.myrobotlab.service.data;

import java.io.Serializable;

import com.thalmic.myo.enums.PoseType;

/**
 * @author GroG
 * 
 *         
 * 
 */
public class MyoData implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public long timestamp;

	public double roll = 0;
	public double pitch = 0;
	public double yaw = 0;
	public PoseType currentPose;

	// default constructor (values will be null until set)
	public MyoData() {
	}
	
	// constructor with initial values for roll/pitch/yaw
	public MyoData(long timestamp, double roll, double pitch, double yaw, PoseType currentPose) {
		this.timestamp = timestamp;
		this.roll = roll;
		this.pitch = pitch;
		this.yaw = yaw;
		this.currentPose = currentPose;
	}
	

	
	public double getRoll() {
		return roll;
	}

	public double getPitch() {
		return pitch;
	}
	
	public double getYaw() {
		return yaw;
	}
	
	public PoseType getPose() {
		return currentPose;
	}


	public void setRoll(double roll) {
		this.roll = roll;
	}

	public void setPitch(double pitch) {
		this.pitch = pitch;
	}

	public void setYaw(double yaw) {
		this.yaw = yaw;
	}

	@Override
	public String toString() {
		return "MyoData [roll=" + roll + ", pitch=" + pitch + ", yaw=" + yaw +", pose=" + currentPose
				+ "]";
	}

}
