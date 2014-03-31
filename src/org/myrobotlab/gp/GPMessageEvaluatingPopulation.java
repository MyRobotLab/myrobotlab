package org.myrobotlab.gp;

/**
 * A GP object sends this message to its observers whenever the genetic
 * algorithm has evaluated the fitnesses of all individuals in the population.
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public class GPMessageEvaluatingPopulation extends GPMessage {
	public int generation;
	public double[] fitness;

	GPMessageEvaluatingPopulation(int generation, double[] fitness) {
		this.generation = generation;
		this.fitness = fitness;
	}
}
