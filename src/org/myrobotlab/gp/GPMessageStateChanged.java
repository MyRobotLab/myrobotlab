package org.myrobotlab.gp;

/**
 * A GP object sends this message to its observers whenever the execution state
 * of the genetic algorithm changes, such as from "Running" to "Stopped".
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public class GPMessageStateChanged extends GPMessage {
	public int newState;
	public String text;

	GPMessageStateChanged(int newState, String text) {
		this.newState = newState;
		this.text = text;
	}
}
