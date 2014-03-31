package org.myrobotlab.gp;

/**
 * A GP object sends this message to its observers whenever the genetic
 * algorithm has found a new best individual.
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public class GPMessageBestFound extends GPMessage {
	public int generation;
	public String program;
	public RealPoint[] data;
	public double fitness;
	public double standardFitness;

	GPMessageBestFound(int generation, String program, RealPoint[] data, double fitness, double standardFitness) {
		this.generation = generation;
		this.program = program;
		this.data = data;
		this.fitness = fitness;
		this.standardFitness = standardFitness;
	}
}
