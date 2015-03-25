package org.myrobotlab.service.interfaces;

public interface StepperControl {
	public boolean detach();

	public Integer getIndex();

	public String getName();

	public Integer[] getPins();

	public String getStepperType();

	public int getSteps();

	/**
	 * reports if a stepper is attached to a stepper controller
	 */
	public boolean isAttached();

	/**
	 * locks the stepper so no other commands will affect it until it becomes
	 * unlocked
	 */
	public void lock();

	/**
	 * setting controller is needed in the "attach" process
	 */
	public boolean setController(StepperController controller);

	public void setIndex(Integer index);

	public void setSpeed(Integer rpm);

	public void step(Integer steps);

	public void step(Integer steps, Integer style);

	public void stop();

	/**
	 * a safety mechanism - stop and lock will stop and lock the stepper no
	 * other commands will affect the stepper until it is "unlocked"
	 */
	public void stopAndLock();

	/**
	 * unlocks the stepper, so other commands can affect it
	 */
	public void unlock();

}
