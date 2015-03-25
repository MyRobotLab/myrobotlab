package org.myrobotlab.tracking;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

/**
 * ControlSystem is responsible for the underlying control of the tracking
 * system. Motors, platforms, or servos might need control from the control
 * system.
 * 
 * Direct control or messaging?
 * 
 * Decided to go with direct control to avoid the possibility of stacked message
 * buffer. This requires local actuator services.
 * 
 */
public class ControlSystem {

	public final static Logger log = LoggerFactory.getLogger(ControlSystem.class.getCanonicalName());

	// private boolean isRunning = false;
	private Servo x;
	private Servo y;

	public int getMaxX() {
		return x.getMax();
	}

	public int getMaxY() {
		return y.getMax();
	}

	public int getMinX() {
		return x.getMin();
	}

	public int getMinY() {
		return y.getMin();
	}

	// externally defined servos
	public void init(Servo x, Servo y) {
		this.x = x;
		this.y = y;
	}

	public boolean isReady() {
		if (x != null && y != null && x.isAttached() && y.isAttached()) {
			return true;
		}

		return false;
	}

	public void moveXTo(double pos) {
		x.moveTo((int) Math.round(pos));
	}

	public void moveXTo(int pos) {
		x.moveTo(pos);
	}

	public void moveYTo(double pos) {
		y.moveTo((int) Math.round(pos));
	}

	public void moveYTo(int pos) {
		y.moveTo(pos);
	}
}
