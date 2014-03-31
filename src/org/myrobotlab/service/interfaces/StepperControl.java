package org.myrobotlab.service.interfaces;


public interface StepperControl {
	/*
	public void attach(StepperController arduino, Integer pin1, Integer pin2, Integer pin3, Integer pin4);
	*/

	/**
	 * Attach a stepper controller to the stepper. The stepper and stepper controller
	 * "should be in the same instance of MRL and this reference to another
	 * service should be ok.
	 * 
	 * The stepper controller uses this method to pass a reference of itself to
	 * the stepper, to be used directly
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


}
