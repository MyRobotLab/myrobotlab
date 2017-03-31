/**
 * 
 */
package org.myrobotlab.genetic;

/**
 * @author Christian
 *
 */
public class GeneticParameters {
  private int geneticPoolSize = 200;
  private double geneticMutationRate = 0.01;
  private double geneticRecombinationRate = 0.7;
  private int geneticGeneration = 300;
  private boolean geneticComputeSimulation = false;
	/**
	 * @return the geneticComputeSimulation
	 */
	public boolean getGeneticComputeSimulation() {
		return geneticComputeSimulation;
	}
	/**
	 * @param geneticComputeSimulation the geneticComputeSimulation to set
	 */
	public void setGeneticComputeSimulation(boolean geneticComputeSimulation) {
		this.geneticComputeSimulation = geneticComputeSimulation;
	}
	/**
	 * @return the geneticPoolSize
	 */
	public int getGeneticPoolSize() {
		return geneticPoolSize;
	}
	/**
	 * @param geneticPoolSize the geneticPoolSize to set
	 */
	public void setGeneticPoolSize(int geneticPoolSize) {
		this.geneticPoolSize = geneticPoolSize;
	}
	/**
	 * @return the geneticMutationRate
	 */
	public double getGeneticMutationRate() {
		return geneticMutationRate;
	}
	/**
	 * @param geneticMutationRate the geneticMutationRate to set
	 */
	public void setGeneticMutationRate(double geneticMutationRate) {
		this.geneticMutationRate = geneticMutationRate;
	}
	/**
	 * @return the geneticRecombinationRate
	 */
	public double getGeneticRecombinationRate() {
		return geneticRecombinationRate;
	}
	/**
	 * @param geneticRecombinationRate the geneticRecombinationRate to set
	 */
	public void setGeneticRecombinationRate(double geneticRecombinationRate) {
		this.geneticRecombinationRate = geneticRecombinationRate;
	}
	/**
	 * @return the geneticGeneration
	 */
	public int getGeneticGeneration() {
		return geneticGeneration;
	}
	/**
	 * @param geneticGeneration the geneticGeneration to set
	 */
	public void setGeneticGeneration(int geneticGeneration) {
		this.geneticGeneration = geneticGeneration;
	}
}
