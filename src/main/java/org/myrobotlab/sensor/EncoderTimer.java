package org.myrobotlab.sensor;

import org.myrobotlab.logging.Logging;

/**
 * and encoding sensor which senses "time" and delivers the necessary pulses to
 * a future Encoder interface (TODO)
 * 
 * @author GroG
 *
 */

public class EncoderTimer extends Thread implements EncoderPublisher {
	boolean isRunning = false;
	EncoderListener listener;

	public EncoderTimer(String name, long duration) {
		super(name + "_duration");
	}

	// Thread in .wait
	@Override
	public void run() {
		try {
			isRunning = true;
			while (isRunning) {

				listener.onPulse(null);
				Thread.sleep(100);

			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	// TODO - RESET ... ABSOLUTE VS RELATIVE !!!

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public EncoderData publishEncoderData(EncoderData Data) {
		// TODO Auto-generated method stub
		return null;
	}

}
