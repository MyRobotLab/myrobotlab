package org.myrobotlab.service.interfaces;


public interface StepperControl {
	/*
	public void attach(StepperController arduino, Integer pin1, Integer pin2, Integer pin3, Integer pin4);
	*/

	/**
	 * setting controller is needed in the "attach" process
	 */
	public boolean setController(StepperController controller);

	/**
	 * reports if a stepper is attached to a stepper controller
	 */
	public boolean isAttached();
	
	public void setSpeed(Integer rpm);
	
	public void step(Integer steps);
	
	public void step(Integer steps, Integer style);
	
	public boolean detach();
	
	/**
	 * a safety mechanism - stop and lock will stop and lock the stepper no other
	 * commands will affect the stepper until it is "unlocked"
	 */
	public void stopAndLock();

	public void stop();

	/**
	 * locks the stepper so no other commands will affect it until it becomes
	 * unlocked
	 */
	public void lock();

	/**
	 * unlocks the stepper, so other commands can affect it
	 */
	public void unlock();

	public String getName();

	public String getStepperType();

	public Integer[] getPins();

	public void setIndex(Integer index);

	public Integer getIndex();

	public int getSteps();


}
