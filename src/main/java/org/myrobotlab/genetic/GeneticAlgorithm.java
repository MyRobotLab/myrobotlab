package org.myrobotlab.genetic;

import java.util.ArrayList;
import java.util.Random;

/**
 * Genetic Algorithms implementation
 * 
 * This implementation of the Genetic algorithms describe on this site http://www.ai-junkie.com/ga/intro/gat1.html
 * 
 * The principle is simple, you have a bank of chromosomes that contains a serie of bits that can be decoded by your application
 * Each chromosomes are then check to see how they can solve what you try to intend, and get a score (fitness) to express how good they solves it.
 * Chromosomes are then picks at random from the pool, with chromosomes weighted by their fitness, then recombine (swap part of their bits serie)
 * and mutate (changing random bits in their serie) and put into a new pool of chromosome (generation). The algorithm run a number of generation
 * and try to improve his best fitting chromosome.
 * 
 * Usage: 
 *    implements Genetic interface in the class you want to use the algorithm for the decode (how you convert the serie of bits to your data)
 *        and the calcFitness  (give a score to your chromosome) methods
 *    Instanciate GeneticAlgorithm with it's config parameters
 *        populationSize: number of chromosome in the pool
 *        genomeSize: the number of gene in your chromosome (the number of data you want to use)
 *        geneSize: the number of byte you use for each gene (data)
 *        recombinationRate: Is the chance that two chromosomes get mixed together, 0.7 is usually a good start
 *        mutationRate: Is the chance that a giving bit get modified, 0.001 is usually a good start
 *    call doGeneration(number of generation) to get the best fitting chromosome    
 *
 * The parameters are very empiric. They will influence how quick and precise the chromosome will evolve toward your best solution. 
 * 
 * @author Christian/Calamity
 *
 */


public class GeneticAlgorithm {

  double recombinationRate = 0.7;
  double mutationRate = 0.001;
  int populationPoolSize = 100;
  ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
  int geneSize = 8;
  private Genetic geneticClass;

  public GeneticAlgorithm(Genetic geneticClass, int populationSize, int genomeSize, int geneSize, double recombinationRate, double mutationRate) {
    // TODO Auto-generated constructor stub
    populationPoolSize = populationSize;
    this.recombinationRate = recombinationRate;
    this.mutationRate = mutationRate;
    this.geneSize = geneSize;
    this.geneticClass = geneticClass;
    for (int i = 0; i < populationSize; i++) {
      Chromosome chromo = new Chromosome(genomeSize * geneSize);
      chromosomes.add(chromo);
    }
  }
  
  public GeneticAlgorithm(Genetic geneticClass, int genomeSize, GeneticParameters param) {
  	populationPoolSize = param.getGeneticPoolSize();
  	recombinationRate = param.getGeneticRecombinationRate();
  	mutationRate = param.getGeneticMutationRate();
  	geneSize = genomeSize;
  	this.geneticClass = geneticClass;
    for (int i = 0; i < populationPoolSize; i++) {
      Chromosome chromo = new Chromosome(genomeSize * geneSize);
      chromosomes.add(chromo);
    }
  }
  
  public Chromosome doGeneration(int generation) {
    //decode the genes
    //calculate the fitness of the pool
    Chromosome bestFit=chromosomes.get(0);
    for (int i = 0; i < generation; i++) {
      geneticClass.decode(chromosomes);
      geneticClass.calcFitness(chromosomes);
      Double totalFitness=0.0;
      for (Chromosome chromosome : chromosomes) {
        totalFitness += chromosome.fitness;
        if (bestFit != null && chromosome.fitness > bestFit.fitness) {
          bestFit = chromosome;
        }
      }
      if (i != generation-1){ //last iteration, no need to mutate
        ArrayList<Chromosome> newPool = new ArrayList<Chromosome>();
        newPool.add(bestFit);
        while (newPool.size() < populationPoolSize) {
          Chromosome c1 = new Chromosome(RandomWheel(chromosomes, totalFitness));
          Chromosome c2 = new Chromosome(RandomWheel(chromosomes, totalFitness));
          newPool.add(c1.recombine(c2, recombinationRate).mutate(mutationRate));
          newPool.add(c2.recombine(c1, recombinationRate).mutate(mutationRate));
        }
        chromosomes = newPool;
      }
    }
    return bestFit;
  }
  
  private Chromosome RandomWheel(ArrayList<Chromosome> chromosomes, Double totalFitness) {
    Random rand = new Random();
    double randomNumber = rand.nextDouble() * totalFitness;
    Double fitnessCount = 0.0;
    for (Chromosome chromosome : chromosomes) {
      fitnessCount += chromosome.fitness;
      if (randomNumber <= fitnessCount) {
        return chromosome;
      }
    }
    return null;
  }

  public void setGeneticClass(Genetic geneticClass) {
    this.geneticClass = geneticClass;
  }

}
