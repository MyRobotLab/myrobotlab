package org.myrobotlab.service.interfaces;

public interface Platform {

	public void move(float newPowerLevel);

	public void turn(float degrees);

	public void stop();

	public void lock();

	public void stopAndLock();

	public void setMaxPower(float max);

}
