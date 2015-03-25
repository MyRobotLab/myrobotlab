package org.myrobotlab.tracking;

public class ObjectTracker extends Thread {

	private boolean isRunning = false;

	public void release() {
		isRunning = false;
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {

		}
	}

}
