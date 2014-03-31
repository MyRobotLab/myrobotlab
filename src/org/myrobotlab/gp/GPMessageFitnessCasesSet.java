package org.myrobotlab.gp;

/**
 * A GP object sends this message to its observers whenever the fitness cases
 * change. This happens for the first time when the GP creates some initial
 * default fitness cases. The same message is sent whenever the user edits the
 * fitness cases.
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public class GPMessageFitnessCasesSet extends GPMessage {
	public RealPoint[] data;

	GPMessageFitnessCasesSet(RealPoint[] data) {
		this.data = data;
	}
}
