package org.myrobotlab.gp;

/**
 * A GP object sends this message to its observers whenever the genetic
 * algorithm has evaluated the fitness of an individual.
 * 
 * @version 1.0
 * @author Hans U. Gerber (<a
 *         href="mailto:gerber@ifh.ee.ethz.ch">gerber@ifh.ee.ethz.ch</a>)
 */
public class GPMessageEvaluatingIndividual extends GPMessage {
	public int generationNr;
	public int individualNr;
	public double fitness;
	public double rawFitness;
	public RealPoint[] data;

	GPMessageEvaluatingIndividual(int generationNr, int individualNr, RealPoint[] data, double fitness, double rawFitness) {
		this.generationNr = generationNr;
		this.individualNr = individualNr;
		this.data = data;
		this.fitness = fitness;
		this.rawFitness = rawFitness;
	}
}
