/**
 * 
 */
package org.myrobotlab.genetic;

import java.util.ArrayList;


/**
 * @author Christian/Calamity
 *
 */
public interface Genetic {
  void calcFitness(ArrayList<Chromosome> chromosomes);

  void decode(ArrayList<Chromosome> chromosomes);

}
