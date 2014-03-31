package org.myrobotlab.tracking;

public class ObjectTracker extends Thread {

	private boolean isRunning = false;

	public void run() {
		isRunning = true;
		while (isRunning) {

		}
	}

	public void release() {
		isRunning = false;
	}

}
