/**
 * 
 */
package org.myrobotlab.genetic;

import java.util.ArrayList;


/**
 * @author Christian
 *
 */
public interface Genetic {
  void calcFitness(ArrayList<Chromosome> chromosomes);

  void decode(ArrayList<Chromosome> chromosomes);

}
